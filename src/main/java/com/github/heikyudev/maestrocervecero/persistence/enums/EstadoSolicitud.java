package com.github.heikyudev.maestrocervecero.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una solicitud (planificación de producción u orden de compra). Cada valor trae su
 * nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoSolicitud {
    PENDIENTE("Pendiente"),
    FINALIZADA("Finalizada"),
    ANULADA("Anulada");

    private final String etiqueta;
}
