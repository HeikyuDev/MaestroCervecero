package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de una presentación comercial.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentacionComercialFormDTO {

    /**
     * Nombre de la presentación comercial. Debe ser único entre las presentaciones activas
     * (sin distinguir mayúsculas). Ejemplo: "Bolsa de 25 Kg".
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Cantidad expresada en la unidad de medida indicada. Debe ser mayor a cero.
     * Ejemplo: 25.0.
     */
    @NotNull(message = "La cantidad es obligatoria")
    private Double cantidad;

    /**
     * Unidad de medida de la cantidad (GRAMO, KILOGRAMO, TONELADA).
     */
    @NotNull(message = "La unidad de medida es obligatoria")
    private UnidadDeMedida unidadDeMedida;
}
