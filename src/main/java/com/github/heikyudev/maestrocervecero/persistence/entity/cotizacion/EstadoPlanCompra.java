package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de un plan de órdenes de compra: {@code PROPUESTO} al generarse y mientras tiene órdenes propuestas pendientes
 * de confirmar o descartar, {@code FINALIZADO} cuando ya no le quedan órdenes pendientes (todas confirmadas o
 * descartadas, o se anuló la planificación de producción con órdenes ya confirmadas) y {@code CANCELADO} si se anuló la
 * planificación de producción sin órdenes confirmadas. Cada valor trae su nombre legible ({@code etiqueta}).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoPlanCompra {
    PROPUESTO("Propuesto"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;
}
