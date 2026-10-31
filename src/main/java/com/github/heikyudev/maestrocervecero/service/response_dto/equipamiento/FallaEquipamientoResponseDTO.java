package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una falla registrada sobre un equipamiento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallaEquipamientoResponseDTO {

    /**
     * Identificador único de la falla de equipamiento.
     */
    private Long id;

    /**
     * Fecha y hora de inicio de la falla.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora de fin de la falla.
     */
    private LocalDateTime fechaFin;

    /**
     * Estado transaccional de la falla (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Observaciones sobre la falla.
     */
    private String observaciones;

    /**
     * Fecha y hora en la que se anuló la falla, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la falla, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Equipamiento sobre el que se registró la falla.
     */
    private EquipamientoResponseDTO equipamiento;
}
