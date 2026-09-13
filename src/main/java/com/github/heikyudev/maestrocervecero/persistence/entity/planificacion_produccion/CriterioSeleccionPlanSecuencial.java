package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

/**
 * Criterio que usa la generación automática de planes de producción para elegir el fermentador
 * de un plan en modalidad secuencial (un lote a la vez, sin superponerse con otro).
 */
public enum CriterioSeleccionPlanSecuencial {
    FERMENTADOR_MAYOR_CAPACIDAD,
    FERMENTADOR_LIBERACION_MAS_TEMPRANA
}
