package com.github.heikyudev.maestrocervecero.service.response_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
     * Razón social del proveedor.
     */
    private String razonSocial;

    /**
     * Nombre comercial del proveedor.
     */
    private String nombreComercial;

    /**
     * CUIT del proveedor.
     */
    private String cuit;

    /**
     * Teléfono de contacto del proveedor.
     */
    private String telefono;

    /**
     * Correo electrónico de contacto del proveedor.
     */
    private String email;

    /**
     * Dirección (calle y número) del proveedor.
     */
    private String direccion;

    /**
     * Localidad del proveedor (de la cual se desprenden transitivamente la provincia y el país).
     */
    private LocalidadResponseDTO localidad;

    /**
     * Catálogo de productos que ofrece el proveedor (insumo + presentación comercial).
     */
    private List<CatalogoProveedorResponseDTO> catalogoProveedor;

    /**
     * Estado lógico del proveedor (activo o dado de baja).
     */
    private Estado estado;

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
