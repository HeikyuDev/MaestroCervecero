package com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de un parámetro de control.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos). La unidad de medida (por ejemplo, "°C", "pH", "°P" o "g/L") es obligatoria y
 * los valores mínimo y máximo se expresan en esa unidad.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroControlFormDTO {

    /**
     * Nombre del parámetro de control. Debe ser único entre los parámetros de control activos (case-insensitive).
     */
    @NotBlank(message = "El nombre del parámetro de control es obligatorio")
    private String nombre;

    /**
     * Descripción del parámetro de control. Puede ser nula o vacía.
     */
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    /**
     * Unidad de medida en la que se expresan los valores del parámetro de control (por ejemplo, "°C").
     */
    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 20, message = "La unidad de medida no puede superar los 20 caracteres")
    private String unidadDeMedida;

    /**
     * Valor mínimo aceptable para el parámetro de control (Estrictamente mayor a 0 debe ser).
     */
    @NotNull(message = "El valor mínimo es obligatorio")
    private Double valorMinimo;

    /**
     * Valor máximo aceptable para el parámetro de control.
     */
    @NotNull(message = "El valor máximo es obligatorio")
    private Double valorMaximo;
}
