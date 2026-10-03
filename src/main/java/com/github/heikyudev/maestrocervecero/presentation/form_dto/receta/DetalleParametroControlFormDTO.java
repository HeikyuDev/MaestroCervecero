package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación del detalle de parametros de control
 * que se va a utilizar en la receta.
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
public class DetalleParametroControlFormDTO {

    /**
     * Valor mínimo planificado para el parámetro de control en la receta.
     */
    @NotNull(message = "El valor mínimo de cada parámetro de control es obligatorio")
    private Double valorMinimo;

    /**
     * Valor máximo planificado para el parámetro de control en la receta.
     */
    @NotNull(message = "El valor máximo de cada parámetro de control es obligatorio")
    private Double valorMaximo;

    /**
     * Valor ideal planificado para el parámetro de control en la receta.
     */
    @NotNull(message = "El valor ideal de cada parámetro de control es obligatorio")
    private Double valorIdeal;

    /**
     * Identificador del parámetro de control asociado a este detalle de receta.
     * <p>
     * Se espera que este identificador corresponda a un {@link ParametroControlEntity}
     * existente en la base de datos.
     * </p>
     */
    @NotNull(message = "Cada parámetro de control del plan debe estar seleccionado")
    private Long idParametroControl;
}
