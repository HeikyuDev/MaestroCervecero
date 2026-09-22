package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un mantenimiento registrado sobre un equipamiento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoEquipamientoResponseDTO {

    /**
     * Identificador único del mantenimiento de equipamiento.
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
     * Equipamiento sobre el que se registró el mantenimiento.
     */
    private EquipamientoResponseDTO equipamiento;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
