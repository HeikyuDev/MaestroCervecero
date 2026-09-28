package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una solicitud de búsqueda de barril.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudBusquedaResponseDTO {

    /**
     * Identificador único de la solicitud de búsqueda.
     */
    private Long id;

    /**
     * Fecha y hora en la que el cliente propuso que se realice la búsqueda.
     */
    private LocalDateTime fechaBusqueda;

    /**
     * Observaciones del cliente sobre la búsqueda, o {@code null} si no informó ninguna.
     */
    private String observaciones;

    /**
     * Indica si ya se realizó la búsqueda del barril sobre la que se hizo esta solicitud.
     */
    private boolean buscado;

    /**
     * Despacho de barril sobre el que se solicitó la búsqueda.
     */
    private DespachoBarrilResponseDTO despachoBarril;

    // === AUDITABLE ENTITY ===
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
