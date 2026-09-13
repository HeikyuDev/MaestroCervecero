package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

/**
 * Criterio que usa la generación automática de planes de producción para elegir la combinación
 * de equipos de un plan en modalidad concurrente (varios lotes superpuestos en simultáneo).
 */
public enum CriterioSeleccionPlanConcurrente {
    MENOR_CANTIDAD_DE_LOTES,
    EQUIPOS_LIBERACION_MAS_TEMPRANA
}
