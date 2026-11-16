package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import java.time.LocalDate;

/**
 * Criterios del reporte "Costos". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param idReceta ID de la receta: incluye los lotes de todas sus versiones.
 * @param textoReceta Texto con el que se muestra la receta elegida en los criterios aplicados, o {@code null}.
 */
public record CriteriosReporteCostos(LocalDate desde, LocalDate hasta, Long idReceta, String textoReceta) {
}
