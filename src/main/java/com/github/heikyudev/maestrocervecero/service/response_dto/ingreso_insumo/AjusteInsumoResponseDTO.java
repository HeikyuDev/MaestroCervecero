package com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un ajuste de insumo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AjusteInsumoResponseDTO {

    /**
     * Identificador único del ajuste de insumo.
     */
    private Long id;

    /**
     * Cantidad afectada por el ajuste.
     */
    private Double cantidad;

    /**
     * Observación que describe el ajuste realizado.
     */
    private String observacion;

    /**
     * Motivo del ajuste (determina si el ajuste suma o resta cantidad al lote de insumo).
     */
    private MotivoAjusteResponseDTO motivoAjuste;

    /**
     * Lote de insumo sobre el que se aplicó el ajuste.
     */
    private LoteInsumoResponseDTO loteInsumo;

    /**
     * Estado del ajuste de insumo (REGISTRADO o ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en la que se anuló el ajuste de insumo. {@code null} si sigue REGISTRADO.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el ajuste de insumo. {@code null} si sigue REGISTRADO.
     */
    private String motivoAnulacion;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el ajuste de insumo.
     */
    private String createdBy;

    /**
     * Fecha de creación del ajuste de insumo.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el ajuste de insumo por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del ajuste de insumo.
     */
    private LocalDateTime lastModifiedDate;
}
