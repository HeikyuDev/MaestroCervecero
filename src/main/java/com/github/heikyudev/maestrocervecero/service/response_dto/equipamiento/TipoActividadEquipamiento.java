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
    LOTE("Lote", null),
    LIMPIEZA("Limpieza", "Observaciones de la limpieza"),
    FALLA("Falla", "Observaciones de la falla"),
    MANTENIMIENTO("Mantenimiento", "Observaciones del mantenimiento");

    /** Nombre del tipo para mostrar en pantalla. */
    private final String etiqueta;

    /** Título del diálogo con las observaciones de una operación de este tipo, o {@code null} para un lote. */
    private final String tituloObservaciones;
}
