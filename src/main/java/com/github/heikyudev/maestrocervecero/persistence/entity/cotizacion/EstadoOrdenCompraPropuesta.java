package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una orden de compra propuesta por un plan: {@code PROPUESTA} mientras espera la decisión del gerente de
 * compras, {@code CONFIRMADA} cuando se registró la orden de compra definitiva a partir de ella y {@code DESCARTADA}
 * cuando el gerente la descartó o su plan dejó de estar vigente. Cada valor trae su nombre legible ({@code etiqueta}).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoOrdenCompraPropuesta {
    PROPUESTA("Propuesta"),
    CONFIRMADA("Confirmada"),
    DESCARTADA("Descartada");

    private final String etiqueta;
}
