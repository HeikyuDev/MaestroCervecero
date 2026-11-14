package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de una planificación de producción Pendiente en el panel del gerente de producción.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionPendienteResponseDTO {

    /** Identificador único de la planificación; con él se arma el enlace a su detalle. */
    private Long id;

    /** Etiqueta de la planificación tal como se muestra en todas las pantallas (por ejemplo, "Plan #7 - Pale Ale"). */
    private String etiqueta;

    /** Prioridad de la planificación. */
    private PrioridadPlanificacion prioridad;

    /** Fecha estimada de inicio de la planificación. */
    private LocalDate fechaInicioEstimada;

    /** Fecha estimada de finalización de la planificación. */
    private LocalDate fechaFinalizacionEstimada;
}
