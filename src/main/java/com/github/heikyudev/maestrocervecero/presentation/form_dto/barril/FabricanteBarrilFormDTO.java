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
     * Nombre del fabricante de barril, que indica el modelo de fabricación del barril. Debe ser único entre los
     * fabricantes activos (sin distinguir mayúsculas). Ejemplo: "Tonelería del Sur".
     */
    private String nombre;
}
