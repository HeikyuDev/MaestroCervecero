package com.github.heikyudev.maestrocervecero.service.response_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


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
}
