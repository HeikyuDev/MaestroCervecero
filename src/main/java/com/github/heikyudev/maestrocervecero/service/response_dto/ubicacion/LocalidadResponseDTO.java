package com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una localidad.
 * <p>
 * Excluye intencionalmente cualquier atributo de estado o borrado lógico
 * (soft delete): la capa de servicio solo expone localidades activas.
 * </p>
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
