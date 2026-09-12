package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleParametroControlResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una medición registrada sobre una etapa de un lote.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MedicionLoteResponseDTO {

    /**
     * Identificador único de la medición.
     */
    private Long id;

    /**
     * Valor medido para el parámetro de control.
     */
    private Double valorMedido;

    /**
     * Fecha y hora en que se realizó la medición.
     */
    private LocalDateTime fechaMedicion;

    /**
     * Fecha y hora de anulación de la medición. Nula salvo que haya sido anulada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo de anulación de la medición. Nulo salvo que haya sido anulada.
     */
    private String motivoAnulacion;

    /**
     * Estado de la medición (REGISTRADO o ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Indica si el valor medido cayó fuera del rango definido por el parámetro de control.
     */
    private boolean hayAlerta;

    /**
     * Detalle del parámetro de control (con sus valores mínimo, máximo e ideal) que se midió.
     */
    private DetalleParametroControlResponseDTO detalleParametroControl;

    /**
     * Etapa del lote sobre la que se registró la medición.
     */
    private EtapaLoteResponseDTO etapaLote;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró la medición.
     */
    private String createdBy;

    /**
     * Fecha de creación de la medición.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la medición por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la medición.
     */
    private LocalDateTime lastModifiedDate;
}
