package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una provincia.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ProvinciaResponseDTO {

    /**
     * Identificador único de la provincia.
     */
    private Long id;

    /**
     * Nombre de la provincia.
     */
    private String nombre;

    /**
     * País al que pertenece la provincia.
     */
    private PaisResponseDTO pais;

    /**
     * Estado lógico de la provincia (activa o dada de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la provincia.
     */
    private String createdBy;

    /**
     * Fecha de creación de la provincia.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la provincia por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la provincia.
     */
    private LocalDateTime lastModifiedDate;
}
