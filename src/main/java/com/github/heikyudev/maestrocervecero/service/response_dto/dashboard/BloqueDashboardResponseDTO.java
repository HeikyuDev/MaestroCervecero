package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
}
