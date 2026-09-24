package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una limpieza registrada sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimpiezaBarrilResponseDTO {

    /**
     * Identificador único de la limpieza de barril.
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
     * Estado operativo al que quedó el barril como resultado de esta limpieza puntual
     * (DISPONIBLE o EN_MANTENIMIENTO).
     */
    private EstadoOperativoBarril estadoOperativoResultante;

    /**
     * Fecha y hora en la que se anuló la limpieza, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la limpieza, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Barril sobre el que se registró la limpieza.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
