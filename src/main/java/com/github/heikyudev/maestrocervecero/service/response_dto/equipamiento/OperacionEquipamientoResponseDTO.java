package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con una operación del ciclo de vida de un equipamiento (limpieza, falla o mantenimiento) para su
 * historial.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperacionEquipamientoResponseDTO {

    /**
     * Identificador único de la operación.
     */
    private Long id;

    /**
     * Tipo de la operación.
     */
    private TipoOperacionEquipamiento tipo;

    /**
     * Estado transaccional de la operación (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora de inicio de la operación.
     */
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora de fin de la operación.
     */
    private LocalDateTime fechaFin;

    /**
     * Observaciones sobre la operación.
     */
    private String observaciones;

    /**
     * Usuario que registró la operación (auditoría), para el detalle de la operación.
     */
    private String registradoPor;

    /**
     * Fecha y hora en la que se registró la operación.
     */
    private LocalDateTime fechaRegistro;

    /**
     * Fecha y hora en la que se anuló la operación, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la operación, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Estado operativo al que quedó el equipamiento (solo en las limpiezas), o {@code null} en los demás tipos.
     */
    private EstadoOperativo estadoOperativoResultante;

    /**
     * {@code true} solo en la última operación registrada del equipamiento (la de mayor fecha de fin entre sus
     * limpiezas, fallas y mantenimientos), la única que admite anulación.
     */
    private boolean anulable;
}
