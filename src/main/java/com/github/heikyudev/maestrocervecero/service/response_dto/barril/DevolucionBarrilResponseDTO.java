package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una devolución registrada sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevolucionBarrilResponseDTO {

    /**
     * Identificador único de la devolución de barril.
     */
    private Long id;

    /**
     * Fecha y hora en la que se recibió la devolución.
     */
    private LocalDateTime fechaDevolucion;

    /**
     * Observaciones sobre la devolución.
     */
    private String observaciones;

    /**
     * Estado transaccional de la devolución (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en la que se anuló la devolución, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la devolución, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Barril sobre el que se registró la devolución.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
