package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Criterio que usa la generación automática de planes de producción para elegir el fermentador
 * de un plan en modalidad secuencial (un lote a la vez, sin superponerse con otro). Cada valor
 * trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum CriterioSeleccionPlanSecuencial {
    FERMENTADOR_MAYOR_CAPACIDAD("Fermentador de mayor capacidad"),
    FERMENTADOR_LIBERACION_MAS_TEMPRANA("Fermentador que se libera más temprano");

    private final String etiqueta;
}
