package com.github.heikyudev.maestrocervecero.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una transacción registrada (consumo, envasado, ingreso, etc.). Cada valor trae su nombre legible
 * ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoTransaccion {
    REGISTRADO("Registrado"),
    ANULADO("Anulado");

    private final String etiqueta;
}
