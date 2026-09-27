package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un fraccionamiento registrado sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraccionamientoBarrilResponseDTO {

    /**
     * Identificador único del fraccionamiento de barril.
     */
    private Long id;

    /**
     * Fecha y hora en la que se realizó el fraccionamiento.
     */
    private LocalDateTime fecha;

    /**
     * Observaciones sobre el fraccionamiento.
     */
    private String observaciones;

    /**
     * Estado transaccional del fraccionamiento (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en la que se anuló el fraccionamiento, o {@code null} si sigue registrado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el fraccionamiento, o {@code null} si sigue registrado.
     */
    private String motivoAnulacion;

    /**
     * Cantidad de litros extraídos del barril.
     */
    private Double cantidadExtraida;

    /**
     * Estado operativo al que quedó el barril como resultado de este fraccionamiento puntual
     * (CON_CERVEZA o EN_LIMPIEZA).
     */
    private EstadoOperativoBarril estadoOperativoResultante;

    /**
     * Barril del cual se extrajo la cerveza.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
