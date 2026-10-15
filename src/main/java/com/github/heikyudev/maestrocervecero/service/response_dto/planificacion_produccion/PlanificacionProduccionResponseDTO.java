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
     * Litros producidos: suma de la cantidad envasada de los envasados {@code REGISTRADO} (no anulados) de
     * cualquier lote de la planificación. Lo completan {@code filtrarPlanificacionesProduccion} y
     * {@code buscarPorId}; vale {@code 0.0} si no hay envasados registrados o si el DTO proviene de otra operación.
     */
    @Builder.Default
    private Double litrosProducidos = 0.0;

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
     * Usuario que anuló la planificación de producción, según la auditoría de la última modificación.
     * Queda en {@code null} si la planificación nunca fue anulada.
     */
    private String usuarioAnulacion;

    /**
     * Estado actual de la planificación de producción (PENDIENTE, FINALIZADA, ANULADA).
     */
    private EstadoSolicitud estado;

    /**
     * Versión de receta utilizada en esta planificación de producción.
     */
    private VersionRecetaResponseDTO versionReceta;

    /**
     * The label of this plan shown on every screen ("Plan #7 - Pale Ale").
     */
    public String getEtiqueta() {
        return com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion.de(id, versionReceta != null ? versionReceta.getNombre() : null);
    }
}
