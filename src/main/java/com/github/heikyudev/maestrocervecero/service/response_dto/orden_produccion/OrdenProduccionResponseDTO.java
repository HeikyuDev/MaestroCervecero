package com.github.heikyudev.maestrocervecero.service.response_dto.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la entidad {@link OrdenProduccionEntity}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class OrdenProduccionResponseDTO {

    /**
     * Identificador único de la orden de producción.
     */
    private Long id;

    /**
     * Fecha estimada de inicio de la orden de producción.
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización de la orden de producción.
     */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Cantidad a producir, en litros.
     */
    private Double cantidadAProducir;

    /**
     * Fecha y hora en la que se finalizó la orden de producción (finalización normal o forzada).
     * Queda en {@code null} mientras la orden está en estado {@code PENDIENTE}.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Fecha y hora en la que se anuló la orden de producción.
     * Queda en {@code null} si la orden nunca fue anulada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se finalizó de forma forzada la orden de producción.
     * Queda en {@code null} si la orden nunca fue finalizada de forma forzada.
     */
    private String motivoFinalizacion;

    /**
     * Motivo por el cual se anuló la orden de producción.
     * Queda en {@code null} si la orden nunca fue anulada.
     */
    private String motivoAnulacion;

    /**
     * Estado actual de la orden de producción (PENDIENTE, FINALIZADA, ANULADA).
     */
    private EstadoOrden estado;

    /**
     * Versión de receta utilizada en esta orden de producción.
     */
    private VersionRecetaResponseDTO versionReceta;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró la orden de producción.
     */
    private String createdBy;

    /**
     * Fecha de creación de la orden de producción.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la orden de producción por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la orden de producción.
     */
    private LocalDateTime lastModifiedDate;
}
