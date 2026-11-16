package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;

import java.time.LocalDate;

/**
 * Criterios del reporte "Lotes". El período es obligatorio; la receta y el estado no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período (por fecha de inicio estimada del lote).
 * @param hasta Último día del período (por fecha de inicio estimada del lote).
 * @param idReceta ID de la receta: incluye los lotes de todas sus versiones.
 * @param textoReceta Texto con el que se muestra la receta elegida en los criterios aplicados, o {@code null}.
 * @param estado Estado del lote.
 */
public record CriteriosReporteLotes(LocalDate desde, LocalDate hasta, Long idReceta, String textoReceta, EstadoLote estado) {
}
