package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de una planificación de producción Pendiente que todavía no tiene una orden de compra
 * vigente, en el panel del gerente de compras.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionSinOrdenResponseDTO {

    /** Identificador único de la planificación; con él se arman los enlaces a su detalle y al registro de la orden. */
    private Long id;

    /** Etiqueta de la planificación tal como se muestra en todas las pantallas (por ejemplo, "Plan #7 - Pale Ale"). */
    private String etiqueta;

    /** Fecha estimada de inicio de la planificación. */
    private LocalDate fechaInicioEstimada;

    /** Prioridad de la planificación. */
    private PrioridadPlanificacion prioridad;
}
