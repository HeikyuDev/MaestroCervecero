package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación del detalle de malta que se va a utilizar en la receta.
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
public class DetalleMaltaFormDTO {

    /**
     * Cantidad de malta planificada para la receta, en kilogramos.
     */
    @NotNull(message = "La cantidad de cada malta es obligatoria")
    @Positive(message = "La cantidad de cada malta debe ser mayor a 0")
    private Double cantidad;

    /**
     * Identificador de la malta asociada a este detalle de receta.
     * <p>
     * Se espera que este identificador corresponda a una {@link MaltaEntity} existente en la base de datos.
     * </p>
     */
    @NotNull(message = "Cada malta de la receta debe estar seleccionada")
    private Long idMalta;
}
