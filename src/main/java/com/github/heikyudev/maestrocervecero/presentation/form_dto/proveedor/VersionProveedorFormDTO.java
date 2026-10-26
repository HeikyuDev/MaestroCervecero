package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario para el alta y la modificación de una versión de un proveedor.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar los campos del formulario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionProveedorFormDTO {

    /**
     * Razón social del proveedor.
     */
    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    /**
     * Nombre comercial del proveedor.
     */
    @NotBlank(message = "El nombre comercial es obligatorio")
    private String nombreComercial;

    /**
     * CUIT del proveedor, con o sin guiones.
     */
    @NotBlank(message = "El CUIT es obligatorio")
    private String cuit;

    /**
     * Teléfono de contacto del proveedor. Es opcional.
     */
    private String telefono;

    /**
     * Correo electrónico de contacto del proveedor.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    private String email;

    /**
     * Dirección (calle y número) del proveedor.
     */
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    /**
     * Identificador de la localidad del proveedor. A partir de la localidad se asocia
     * transitivamente la provincia y el país.
     */
    @NotNull(message = "La localidad es obligatoria")
    private Long idLocalidad;

    /**
     * Catálogo de productos que ofrece el proveedor (insumo + presentación comercial).
     */
    @Valid
    @NotEmpty(message = "El catálogo debe tener al menos un ítem")
    @Builder.Default
    private List<CatalogoProveedorFormDTO> catalogoProveedor = new ArrayList<>();
}
