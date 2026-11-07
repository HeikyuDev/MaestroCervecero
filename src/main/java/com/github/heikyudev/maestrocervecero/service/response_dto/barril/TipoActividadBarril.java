package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de una actividad del historial de un barril: un envasado en un lote o una operación registrada de su ciclo de
 * vida.
 */
@Getter
@RequiredArgsConstructor
public enum TipoActividadBarril {

    ENVASADO("Envasado", null),
    LIMPIEZA("Limpieza", "Observaciones de la limpieza"),
    FALLA("Falla", "Observaciones de la falla"),
    FRACCIONAMIENTO("Fraccionamiento", "Observaciones del fraccionamiento"),
    MANTENIMIENTO("Mantenimiento", "Observaciones del mantenimiento"),
    DESPACHO("Despacho", "Observaciones del despacho"),
    DEVOLUCION("Devolución", "Observaciones de la devolución");

    /** Nombre del tipo para mostrar en pantalla. */
    private final String etiqueta;

    /** Título del diálogo con las observaciones de una operación de este tipo, o {@code null} para un envasado. */
    private final String tituloObservaciones;
}
