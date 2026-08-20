package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import lombok.*;

import java.util.List;

/**
 * DTO de formulario para el alta y la modificación de un proveedor.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorFormDTO {

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
     * Identificador de la localidad del proveedor. A partir de la localidad se asocia
     * transitivamente la provincia y el país.
     */
    private Long idLocalidad;

    /**
     * Catálogo de productos que ofrece el proveedor (insumo + presentación comercial).
     */
    private List<CatalogoProveedorFormDTO> catalogoProveedor;
}
