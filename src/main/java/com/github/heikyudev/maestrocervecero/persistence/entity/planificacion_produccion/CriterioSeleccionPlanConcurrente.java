package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Criterio que usa la generación automática de planes de producción para elegir la combinación
 * de equipos de un plan en modalidad concurrente (varios lotes superpuestos en simultáneo). Cada
 * valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum CriterioSeleccionPlanConcurrente {
    MENOR_CANTIDAD_DE_LOTES("Menor cantidad de lotes"),
    EQUIPOS_LIBERACION_MAS_TEMPRANA("Equipos que se liberan más temprano");

    private final String etiqueta;
}
