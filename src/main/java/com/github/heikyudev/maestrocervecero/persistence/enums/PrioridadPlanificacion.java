package com.github.heikyudev.maestrocervecero.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Prioridad de una planificación de producción. Define el lugar que ocupan sus lotes nuevos en la cola de pendientes de
 * un fermentador: un lote se ubica delante de los lotes pendientes de planificaciones de prioridad estrictamente menor.
 * Los valores se declaran de menor a mayor prioridad, por lo que el orden natural del enum es el orden de prioridad.
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum PrioridadPlanificacion {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta");

    private final String etiqueta;

    /**
     * Indica si esta prioridad es estrictamente mayor que la recibida.
     *
     * @param otra La prioridad contra la que se compara; {@code null} se trata como {@link #MEDIA}.
     * @return {@code true} si esta prioridad es mayor.
     */
    public boolean esMayorQue(PrioridadPlanificacion otra) {
        return compareTo(otra == null ? MEDIA : otra) > 0;
    }
}
