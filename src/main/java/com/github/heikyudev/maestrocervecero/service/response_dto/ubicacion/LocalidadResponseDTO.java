package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una localidad.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LocalidadResponseDTO {

    /**
     * Identificador único de la localidad.
     */
    private Long id;

    /**
     * Nombre de la localidad.
     */
    private String nombre;

    /**
     * Código postal de la localidad.
     */
    private String codigoPostal;

    /**
     * Provincia a la que pertenece la localidad.
     */
    private ProvinciaResponseDTO provincia;

    /**
     * Estado lógico de la localidad (activa o dada de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la localidad.
     */
    private String createdBy;

    /**
     * Fecha de creación de la localidad.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la localidad por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la localidad.
     */
    private LocalDateTime lastModifiedDate;
}
