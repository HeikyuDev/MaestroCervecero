package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una limpieza registrada sobre un equipamiento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimpiezaEquipamientoResponseDTO {

    /**
     * Identificador único de la limpieza de equipamiento.
     */
    private Long id;

    /**
     * Fecha y hora en la que se realizó la limpieza.
     */
    private LocalDateTime fechaLimpieza;

    /**
     * Observaciones sobre la limpieza.
     */
    private String observaciones;

    /**
     * Estado transaccional de la limpieza (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Estado operativo al que quedó el equipamiento como resultado de esta limpieza puntual
     * (DISPONIBLE o EN_MANTENIMIENTO).
     */
    private EstadoOperativo estadoOperativoResultante;

    /**
     * Fecha y hora en la que se anuló la limpieza, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la limpieza, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Equipamiento sobre el que se registró la limpieza.
     */
    private EquipamientoResponseDTO equipamiento;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
