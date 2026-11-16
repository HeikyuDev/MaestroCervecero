package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Criterio de filtro de un reporte de estadísticas con el valor aplicado, tal como lo muestran los datos de emisión del
 * reporte impreso (por ejemplo, "Receta: Todas").
 */
@Getter
@AllArgsConstructor
public final class CriterioAplicadoResponseDTO {

    /** Nombre del criterio (por ejemplo, "Receta"). */
    private final String etiqueta;

    /** Valor aplicado, o "Todos" o "Todas" si el criterio no se indicó. */
    private final String valor;

    /**
     * El criterio con su valor: el indicado o, si no se indicó ninguno, el texto de "sin filtro".
     *
     * @param etiqueta Nombre del criterio.
     * @param valor Valor aplicado, o {@code null} si el criterio no se indicó.
     * @param sinFiltro Texto cuando no se indicó ("Todos" o "Todas", según el género del criterio).
     * @return El criterio con su valor.
     */
    public static CriterioAplicadoResponseDTO de(String etiqueta, String valor, String sinFiltro) {
        return new CriterioAplicadoResponseDTO(etiqueta, valor == null || valor.isBlank() ? sinFiltro : valor);
    }
}
