package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un despacho registrado sobre un barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DespachoBarrilResponseDTO {

    /**
     * Identificador único del despacho de barril.
     */
    private Long id;

    /**
     * Fecha y hora en la que se despachó el barril.
     */
    private LocalDateTime fechaDespacho;

    /**
     * Observaciones sobre el despacho.
     */
    private String observaciones;

    /**
     * Estado transaccional del despacho (REGISTRADO/ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Fecha y hora en la que se anuló el despacho, o {@code null} si sigue registrado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo por el cual se anuló el despacho, o {@code null} si sigue registrado.
     */
    private String motivoAnulacion;

    /**
     * Fecha estimada en la que se espera que el cliente devuelva el barril.
     */
    private LocalDate fechaDevolucionEstimada;

    /**
     * Barril sobre el que se registró el despacho.
     */
    private BarrilResponseDTO barril;

    /**
     * Cliente al que se le despachó el barril.
     */
    private ClienteResponseDTO cliente;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
