package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un país.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PaisResponseDTO {

    /**
     * Identificador único del país.
     */
    private Long id;

    /**
     * Nombre del país.
     */
    private String nombre;

    /**
     * Estado lógico del país (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el país.
     */
    private String createdBy;

    /**
     * Fecha de creación del país.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el país por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del país.
     */
    private LocalDateTime lastModifiedDate;
}
