package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación del detalle de la levadura que se va a utilizar en la receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleLevaduraFormDTO {
    /**
     * Cantidad de levadura planificada para la versión de la receta, en gramos.
     */
    private Double cantidad;

    /**
     * Identificador de la levadura que se va a utilizar en la receta.
     * <p>
     * Se espera que este identificador corresponda a una {@link LevaduraEntity} existente en la base de datos.
     * </p>
     */
    private Long idLevadura;

}
