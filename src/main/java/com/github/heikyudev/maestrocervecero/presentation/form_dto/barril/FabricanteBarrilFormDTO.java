package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de un fabricante de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FabricanteBarrilFormDTO {

    /**
     * Razón social del fabricante de barril.
     */
    private String razonSocial;

    /**
     * Nombre comercial del fabricante de barril.
     */
    private String nombreComercial;

    /**
     * CUIT del fabricante de barril.
     */
    private String cuit;

    /**
     * Teléfono de contacto del fabricante de barril.
     */
    private String telefono;

    /**
     * Email de contacto del fabricante de barril.
     */
    private String email;
}
