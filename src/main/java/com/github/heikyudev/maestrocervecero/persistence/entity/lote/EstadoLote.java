package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de un lote. Cada valor trae su nombre legible ({@code etiqueta}), que es el que se
 * muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoLote {
    PENDIENTE("Pendiente"),
    EN_EJECUCION("En ejecución"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;
}
