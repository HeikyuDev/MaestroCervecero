package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una falla registrada sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallaBarrilResponseDTO {

    /**
     * Identificador único de la falla de barril.
     */
    private Long id;

    /**
     * Fecha y hora en la que ocurrió la falla.
     */
    private LocalDateTime fechaFalla;

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
     * Barril sobre el que se registró la falla.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
