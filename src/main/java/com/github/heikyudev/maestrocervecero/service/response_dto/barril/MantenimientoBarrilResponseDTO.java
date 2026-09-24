package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un mantenimiento registrado sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoBarrilResponseDTO {

    /**
     * Identificador único del mantenimiento de barril.
     */
    private Long id;

    /**
     * Fecha y hora en la que se realizó el mantenimiento.
     */
    private LocalDateTime fechaMantenimiento;

    /**
     * Estado transaccional del mantenimiento (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Observaciones sobre el mantenimiento.
     */
    private String observaciones;

    /**
     * Fecha y hora en la que se anuló el mantenimiento, o {@code null} si sigue registrado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el mantenimiento, o {@code null} si sigue registrado.
     */
    private String motivoAnulacion;

    /**
     * Barril sobre el que se registró el mantenimiento.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
