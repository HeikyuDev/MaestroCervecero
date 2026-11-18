package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

/**
 * Evento que publica el registro de una planificación de producción. Lo escucha la generación automática de planes de
 * órdenes de compra, recién cuando el registro se guardó.
 *
 * @param idPlanificacion ID de la planificación de producción registrada.
 */
public record PlanificacionProduccionRegistradaEvento(Long idPlanificacion) {
}
