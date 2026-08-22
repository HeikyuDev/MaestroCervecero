package com.github.heikyudev.maestrocervecero.service.response_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un proveedor.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ProveedorResponseDTO {

    /**
     * Identificador único del proveedor.
     */
    private Long id;

    /**
     * Estado del proveedor.
     */
    private Estado estado;

    /**
     * Última versión del proveedor.
     */
    private VersionProveedorResponseDTO version;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el proveedor.
     */
    private String createdBy;

    /**
     * Fecha de creación del proveedor.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el proveedor por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del proveedor.
     */
    private LocalDateTime lastModifiedDate;
}
