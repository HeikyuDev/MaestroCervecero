package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de una actividad del historial de un equipamiento: el uso del equipo en una etapa de un lote o una operación
 * registrada de su ciclo de vida.
 */
@Getter
@RequiredArgsConstructor
public enum TipoActividadEquipamiento {
    LOTE("Lote"),
    LIMPIEZA("Limpieza"),
    FALLA("Falla"),
    MANTENIMIENTO("Mantenimiento");

    /** Nombre del tipo para mostrar en pantalla. */
    private final String etiqueta;
}
