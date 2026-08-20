package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación del detalle de parametros de control
 * que se va a utilizar en la receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleParametroControlFormDTO {

    /**
     * Valor mínimo planificado para el parámetro de control en la receta.
     */
    private Double valorMinimo;

    /**
     * Valor máximo planificado para el parámetro de control en la receta.
     */
    private Double valorMaximo;

    /**
     * Valor ideal planificado para el parámetro de control en la receta.
     */
    private Double valorIdeal;

    /**
     * Identificador del parámetro de control asociado a este detalle de receta.
     * <p>
     * Se espera que este identificador corresponda a un {@link ParametroControlEntity}
     * existente en la base de datos.
     * </p>
     */
    private Long idParametroControl;
}
