package com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
/**
 * DTO de formulario para el alta y la modificación de un parametro de control.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroControlFormDTO {

    /**
     * Nombre del parámetro de control. Debe ser único entre los parámetros de control activos (case-insensitive).
     */
    private String nombre;

    /**
     * Descripción del parámetro de control. Puede ser nula o vacía.
     */
    private String descripcion;

    /**
     * Valor mínimo aceptable para el parámetro de control (Estrictamente mayor a 0 debe ser).
     */
    private Double valorMinimo;

    /**
     * Valor máximo aceptable para el parámetro de control.
     */
    private Double valorMaximo;
}
