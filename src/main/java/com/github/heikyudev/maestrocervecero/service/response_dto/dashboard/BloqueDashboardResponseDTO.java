package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * DTO de respuesta con un bloque del panel del operario: las primeras filas de una lista y el total real de
 * registros que la integran.
 *
 * @param <T> Tipo de cada fila del bloque.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueDashboardResponseDTO<T> {

    /**
     * Total real de registros del bloque, no solo los de {@link #filas}. Es el número que muestran los contadores.
     * Nunca es negativo.
     */
    private long total;

    /**
     * Filas visibles del bloque: hasta 5, en el orden propio del bloque. Nunca es nula; es una lista vacía si el
     * bloque no tiene registros.
     */
    private List<T> filas;

    /**
     * Arma un bloque a partir de una página: el total es el total real de la página y las filas son su contenido mapeado.
     *
     * @param pagina Página con las filas visibles y el total real. No puede ser nula.
     * @param mapeo  Conversión de cada elemento de la página a una fila del bloque.
     * @param <E>    Tipo de los elementos de la página.
     * @param <F>    Tipo de cada fila del bloque.
     * @return El bloque con el total real y las filas mapeadas; sus filas son una lista vacía si la página no tiene elementos.
     */
    public static <E, F> BloqueDashboardResponseDTO<F> de(Page<E> pagina, Function<E, F> mapeo) {
        return BloqueDashboardResponseDTO.<F>builder()
                .total(pagina.getTotalElements())
                .filas(pagina.getContent().stream().map(mapeo).toList())
                .build();
    }
}
