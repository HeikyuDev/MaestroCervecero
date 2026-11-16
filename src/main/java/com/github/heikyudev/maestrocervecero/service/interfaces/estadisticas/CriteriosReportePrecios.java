package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;

import java.time.LocalDate;

/**
 * Criterios del reporte "Precios". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param tipoInsumo Tipo de insumo.
 * @param idProveedor ID del proveedor.
 * @param textoProveedor Texto con el que se muestra el proveedor elegido en los criterios aplicados, o {@code null}.
 */
public record CriteriosReportePrecios(LocalDate desde, LocalDate hasta, TipoInsumo tipoInsumo, Long idProveedor, String textoProveedor) {
}
