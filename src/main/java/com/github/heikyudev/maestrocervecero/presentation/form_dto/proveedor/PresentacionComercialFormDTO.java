package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de una presentación comercial.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentacionComercialFormDTO {

    /**
     * Nombre de la presentación comercial. Debe ser único entre las presentaciones activas
     * (case-insensitive). Ejemplo: "Bolsa de 25 Kg".
     */
    private String nombre;

    /**
     * Cantidad expresada en la unidad de medida indicada. Debe ser mayor a cero.
     * Ejemplo: 25.0.
     */
    private Double cantidad;

    /**
     * Unidad de medida de la cantidad (GRAMO, KILOGRAMO, TONELADA).
     */
    private UnidadDeMedida unidadDeMedida;
}
