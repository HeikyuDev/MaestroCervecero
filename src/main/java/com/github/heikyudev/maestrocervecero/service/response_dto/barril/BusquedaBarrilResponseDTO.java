package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una búsqueda registrada sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaBarrilResponseDTO {

    /**
     * Identificador único de la búsqueda de barril.
     */
    private Long id;

    /**
     * Estado transaccional de la búsqueda (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en la que se anuló la búsqueda, o {@code null} si sigue registrada.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló la búsqueda, o {@code null} si sigue registrada.
     */
    private String motivoAnulacion;

    /**
     * Solicitud de búsqueda sobre la que se registró esta búsqueda.
     */
    private SolicitudBusquedaResponseDTO solicitudBusqueda;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
