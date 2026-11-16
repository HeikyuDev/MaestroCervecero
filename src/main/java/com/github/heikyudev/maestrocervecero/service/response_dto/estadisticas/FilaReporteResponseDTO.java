package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Fila de la tabla de un reporte de estadísticas: una celda por columna, en el orden de las columnas.
 */
@Getter
@AllArgsConstructor
public final class FilaReporteResponseDTO {

    private final List<CeldaReporteResponseDTO> celdas;
}
