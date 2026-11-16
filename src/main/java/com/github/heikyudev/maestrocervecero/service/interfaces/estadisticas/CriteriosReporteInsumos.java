package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;

import java.time.LocalDate;

/**
 * Criterios del reporte "Insumos". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param tipoInsumo Tipo de insumo.
 * @param idReceta ID de la receta del lote del consumo: incluye todas sus versiones.
 * @param textoReceta Texto con el que se muestra la receta elegida en los criterios aplicados, o {@code null}.
 */
public record CriteriosReporteInsumos(LocalDate desde, LocalDate hasta, TipoInsumo tipoInsumo, Long idReceta, String textoReceta) {
}
