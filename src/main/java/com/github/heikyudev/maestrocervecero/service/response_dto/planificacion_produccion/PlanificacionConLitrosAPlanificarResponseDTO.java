package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con una planificación de producción Pendiente que todavía tiene litros sin planificar: el aviso que
 * recuerda al gerente de producción que le falta confirmar un plan de producción. Se calcula al momento y nunca se
 * persiste.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PlanificacionConLitrosAPlanificarResponseDTO {

    /** ID de la planificación de producción; con él se arma el enlace a sus planes de producción. */
    private Long id;

    /** Etiqueta de la planificación tal como se muestra en todas las pantallas (por ejemplo, "Plan #7 - Pale Ale"). */
    private String etiqueta;

    /** Fecha de inicio estimada de la planificación. */
    private LocalDate fechaInicioEstimada;

    /** Litros que le faltan planificar a la planificación, siempre mayores a 0. */
    private Double litrosAPlanificar;
}
