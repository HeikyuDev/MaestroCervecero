package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;

import java.time.LocalDate;

/**
 * Criterios del reporte "Recepción". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param idProveedor ID del proveedor: con un proveedor indicado no se incluyen los ingresos directos.
 * @param textoProveedor Texto con el que se muestra el proveedor elegido en los criterios aplicados, o {@code null}.
 * @param tipoIngreso Tipo de ingreso (por compra o directo).
 */
public record CriteriosReporteRecepcion(LocalDate desde, LocalDate hasta, Long idProveedor, String textoProveedor, TipoIngreso tipoIngreso) {
}
