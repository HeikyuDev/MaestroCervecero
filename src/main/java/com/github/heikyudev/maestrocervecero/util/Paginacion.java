package com.github.heikyudev.maestrocervecero.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/**
 * Normaliza el número de página recibido por parámetro en las pantallas paginadas.
 * <p>
 * El desplazamiento que se envía a la base de datos es {@code página × tamaño} y se calcula con enteros de 32 bits:
 * un número de página enorme lo desborda y rompe la consulta. Por eso la página se acota a un máximo muy superior a
 * cualquier listado real; una página fuera de rango simplemente devuelve una página vacía; las pantallas que lo requieren la reconsultan con
 * {@link #consultar} para mostrar la última página.
 * </p>
 */
public final class Paginacion {

    /** Mayor número de página (base 0) aceptado. Con el tamaño de página más grande en uso, el desplazamiento no desborda. */
    public static final int PAGINA_MAXIMA = 100_000;

    private Paginacion() {
    }

    /**
     * Acota el número de página recibido al rango {@code [0, PAGINA_MAXIMA]}.
     *
     * @param pagina El número de página pedido (base 0), posiblemente negativo o desmedido.
     * @return El número de página a consultar.
     */
    public static int numeroDePagina(int pagina) {
        return Math.min(Math.max(pagina, 0), PAGINA_MAXIMA);
    }

    /**
     * Ejecuta la consulta con el número de página acotado y, si queda más allá de la última, la repite con la última página.
     *
     * @param pagina   El número de página pedido (base 0).
     * @param tamanio  El tamaño de página.
     * @param orden    El orden de los resultados.
     * @param consulta La consulta paginada.
     * @return La página pedida o, si estaba fuera de rango, la última.
     */
    public static <T> Page<T> consultar(int pagina, int tamanio, Sort orden, Function<Pageable, Page<T>> consulta) {
        Pageable pageable = PageRequest.of(numeroDePagina(pagina), tamanio, orden);
        return ajustarAlRango(consulta.apply(pageable), pageable, consulta);
    }

    /**
     * Si la página pedida está más allá de la última (resultado vacío con páginas existentes), repite la consulta con la
     * última página para que la pantalla nunca muestre una tabla vacía ni "Página 3 de 2".
     *
     * @param resultado La página obtenida con el {@code pageable} pedido.
     * @param pageable  El paginado usado en la consulta.
     * @param consulta  La consulta a repetir con la última página.
     * @return El resultado original, o el de la última página si el pedido estaba fuera de rango.
     */
    private static <T> Page<T> ajustarAlRango(Page<T> resultado, Pageable pageable, Function<Pageable, Page<T>> consulta) {
        if (resultado.getContent().isEmpty() && resultado.getTotalPages() > 0
                && pageable.getPageNumber() >= resultado.getTotalPages()) {
            return consulta.apply(PageRequest.of(resultado.getTotalPages() - 1, pageable.getPageSize(), pageable.getSort()));
        }
        return resultado;
    }
}
