package com.github.heikyudev.maestrocervecero.util;

/**
 * Normaliza el número de página recibido por parámetro en las pantallas paginadas.
 * <p>
 * El desplazamiento que se envía a la base de datos es {@code página × tamaño} y se calcula con enteros de 32 bits:
 * un número de página enorme lo desborda y rompe la consulta. Por eso la página se acota a un máximo muy superior a
 * cualquier listado real; una página fuera de rango simplemente devuelve una página vacía.
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
}
