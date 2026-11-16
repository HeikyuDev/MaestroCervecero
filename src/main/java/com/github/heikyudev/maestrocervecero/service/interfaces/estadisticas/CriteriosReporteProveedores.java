package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;

import java.time.LocalDate;

/**
 * Criterios del reporte "Proveedores". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param idProveedor ID del proveedor.
 * @param textoProveedor Texto con el que se muestra el proveedor elegido en los criterios aplicados, o {@code null}.
 * @param tipoInsumo Tipo de insumo de los ítems que se consideran.
 */
public record CriteriosReporteProveedores(LocalDate desde, LocalDate hasta, Long idProveedor, String textoProveedor, TipoInsumo tipoInsumo) {
}
