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

    ENVASADO("Envasado"),
    LIMPIEZA("Limpieza"),
    FALLA("Falla"),
    FRACCIONAMIENTO("Fraccionamiento"),
    MANTENIMIENTO("Mantenimiento"),
    DESPACHO("Despacho"),
    DEVOLUCION("Devolución");

    /** Nombre del tipo para mostrar en pantalla. */
    private final String etiqueta;
}
