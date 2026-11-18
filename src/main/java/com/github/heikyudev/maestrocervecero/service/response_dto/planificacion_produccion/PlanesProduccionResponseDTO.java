package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los dos planes de producción alternativos que el sistema propone para una planificación: el
 * secuencial y el concurrente. Se calculan al momento y nunca se persisten.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PlanesProduccionResponseDTO {

    /** ID de la planificación de producción para la que se generaron los planes. */
    private Long idPlanificacion;

    /** Litros que le faltan planificar a la planificación (0 si sus lotes ya cubren la cantidad a producir). */
    private Double litrosAPlanificar;

    /** Excedente máximo de un plan de la configuración de producción, en litros. */
    private Double excedenteMaximoLitros;

    /** Plan en modalidad secuencial: un único fermentador reutilizado lote tras lote. */
    private PlanProduccionResponseDTO secuencial;

    /** Plan en modalidad concurrente: varios fermentadores en paralelo. */
    private PlanProduccionResponseDTO concurrente;
}
