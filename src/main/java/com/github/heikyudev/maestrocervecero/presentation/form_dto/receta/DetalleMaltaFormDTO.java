package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación del detalle de malta que se va a utilizar en la receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMaltaFormDTO {

    /**
     * Cantidad de malta planificada para la receta, en kilogramos.
     */
    private Double cantidad;

    /**
     * Identificador de la malta asociada a este detalle de receta.
     * <p>
     * Se espera que este identificador corresponda a una {@link MaltaEntity} existente en la base de datos.
     * </p>
     */
    private Long idMalta;
}
