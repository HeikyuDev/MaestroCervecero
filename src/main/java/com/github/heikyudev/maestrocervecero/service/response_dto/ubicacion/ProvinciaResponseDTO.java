package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una provincia.
 * <p>
 * Excluye intencionalmente cualquier atributo de estado o borrado lógico
 * (soft delete): la capa de servicio solo expone provincias activas.
 * </p>
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
