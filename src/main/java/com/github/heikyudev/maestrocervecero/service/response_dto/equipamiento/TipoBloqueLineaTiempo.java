package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

/**
 * Tipo de bloque de la línea de tiempo de un equipamiento: el uso del equipo en un lote o una de sus operaciones
 * del ciclo de vida (limpieza, falla o mantenimiento).
 */
public enum TipoBloqueLineaTiempo {
    LOTE,
    LIMPIEZA,
    FALLA,
    MANTENIMIENTO
}
