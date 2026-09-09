package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.DetalleCostoDirectoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta que representa un lote.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LoteResponseDTO {

    /**
     * Identificador único del lote.
     */
    private Long id;

    /**
     * Identificador interno autogenerado del lote (nombre de la receta + número de lote de esa receta). Ej: "PILSEN_CLASICA-3".
     */
    private String identificadorInterno;

    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;

    /**
     * Estado del lote (PENDIENTE, EN_EJECUCION, FINALIZADO, ANULADO).
     */
    private EstadoLote estado;

    /**
     * Fecha estimada de inicio del lote.
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización del lote.
     */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Fecha y hora en que efectivamente arrancó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora en que efectivamente finalizó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaFinalizacion;

    /**
     * Motivo de anulación del lote. Nulo salvo que el lote haya sido anulado.
     */
    private String motivoAnulacion;

    /**
     * Planificación de producción a la que está asociado el lote.
     */
    private PlanificacionProduccionResponseDTO planificacionProduccion;

    /**
     * Las 6 etapas por las que atraviesa el lote durante su producción.
     */
    private List<EtapaLoteResponseDTO> etapas;

    /**
     * Detalle de los costos directos adicionales aplicados al lote.
     */
    private List<DetalleCostoDirectoResponseDTO> detallesCostoDirecto;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró el lote.
     */
    private String createdBy;

    /**
     * Fecha de creación del lote.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el lote por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del lote.
     */
    private LocalDateTime lastModifiedDate;
}
