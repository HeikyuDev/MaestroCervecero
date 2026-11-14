package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Causa por la que un equipo o un barril está pendiente de mantenimiento en el panel del operario.
 */
@Getter
@RequiredArgsConstructor
public enum CausaMantenimiento {
    FALLA("Falla"),
    PREVENTIVO("Preventivo");

    /** Nombre de la causa para mostrar en pantalla. */
    private final String etiqueta;
}
