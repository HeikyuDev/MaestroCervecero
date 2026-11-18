package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una solicitud de cotización: {@code PENDIENTE_DE_CONFIRMACION} desde que se genera hasta que el gerente de
 * compras confirma el envío a los proveedores, {@code ABIERTA} mientras los proveedores pueden responder, {@code CERRADA}
 * al vencer el plazo de respuesta (todavía sin planes), {@code PLANES_GENERADOS} cuando ya tiene sus planes de órdenes
 * de compra y {@code CANCELADA} si se anuló la planificación de producción que la originó. Cada valor trae su nombre
 * legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoSolicitudCotizacion {
    PENDIENTE_DE_CONFIRMACION("Pendiente de confirmación"),
    ABIERTA("Abierta"),
    CERRADA("Cerrada"),
    PLANES_GENERADOS("Planes generados"),
    CANCELADA("Cancelada");

    private final String etiqueta;
}
