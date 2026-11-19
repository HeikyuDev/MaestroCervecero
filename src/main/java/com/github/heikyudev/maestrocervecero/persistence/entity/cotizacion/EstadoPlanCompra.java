package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de un plan de órdenes de compra: {@code PROPUESTO} al generarse, {@code ELEGIDO} cuando el gerente de compras
 * confirma la primera orden de compra del plan (desde ese momento la solicitud no se puede cancelar) y {@code CANCELADO}
 * si se canceló la solicitud de cotización o se anuló la planificación de producción. Cada valor trae su nombre legible
 * ({@code etiqueta}).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoPlanCompra {
    PROPUESTO("Propuesto"),
    ELEGIDO("Elegido"),
    CANCELADO("Cancelado");

    private final String etiqueta;
}
