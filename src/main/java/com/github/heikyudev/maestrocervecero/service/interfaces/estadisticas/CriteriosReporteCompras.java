package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;

import java.time.LocalDate;

/**
 * Criterios del reporte "Compras". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param idProveedor ID del proveedor.
 * @param textoProveedor Texto con el que se muestra el proveedor elegido en los criterios aplicados, o {@code null}.
 * @param estado Estado de la orden de compra.
 */
public record CriteriosReporteCompras(LocalDate desde, LocalDate hasta, Long idProveedor, String textoProveedor, EstadoSolicitud estado) {
}
