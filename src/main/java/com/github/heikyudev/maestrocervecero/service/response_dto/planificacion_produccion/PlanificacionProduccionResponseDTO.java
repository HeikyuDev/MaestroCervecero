package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la entidad {@link PlanificacionProduccionEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PlanificacionProduccionResponseDTO {

    /**
     * Identificador único de la planificación de producción.
     */
    private Long id;

    /**
     * Fecha estimada de inicio de la planificación de producción.
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización de la planificación de producción.
     */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Cantidad a producir, en litros.
     */
    private Double cantidadAProducir;

    /**
     * Fecha y hora en la que se finalizó la planificación de producción (finalización normal o forzada).
     * Queda en {@code null} mientras la planificación está en estado {@code PENDIENTE}.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Fecha y hora en la que se anuló la planificación de producción.
     * Queda en {@code null} si la planificación nunca fue anulada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se finalizó de forma forzada la planificación de producción.
     * Queda en {@code null} si la planificación nunca fue finalizada de forma forzada.
     */
    private String motivoFinalizacion;

    /**
     * Motivo por el cual se anuló la planificación de producción.
     * Queda en {@code null} si la planificación nunca fue anulada.
     */
    private String motivoAnulacion;

    /**
     * Estado actual de la planificación de producción (PENDIENTE, FINALIZADA, ANULADA).
     */
    private EstadoSolicitud estado;

    /**
     * Versión de receta utilizada en esta planificación de producción.
     */
    private VersionRecetaResponseDTO versionReceta;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró la planificación de producción.
     */
    private String createdBy;

    /**
     * Fecha de creación de la planificación de producción.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la planificación de producción por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la planificación de producción.
     */
    private LocalDateTime lastModifiedDate;
}
