package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de un motivo de ajuste de insumo: suma cantidad al lote (ingreso) o la resta (egreso). Cada valor trae su nombre
 * legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum TipoAjuste {
    INGRESO("Ingreso"),
    EGRESO("Egreso");

    private final String etiqueta;
}
