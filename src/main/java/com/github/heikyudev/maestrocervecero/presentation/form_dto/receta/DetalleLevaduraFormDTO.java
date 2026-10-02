package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación del detalle de la levadura que se va a utilizar en la receta.
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
public class DetalleLevaduraFormDTO {
    /**
     * Cantidad de levadura planificada para la versión de la receta, en gramos.
     */
    @NotNull(message = "La cantidad de cada levadura es obligatoria")
    @Positive(message = "La cantidad de cada levadura debe ser mayor a 0")
    private Double cantidad;

    /**
     * Identificador de la levadura que se va a utilizar en la receta.
     * <p>
     * Se espera que este identificador corresponda a una {@link LevaduraEntity} existente en la base de datos.
     * </p>
     */
    @NotNull(message = "Cada levadura de la receta debe estar seleccionada")
    private Long idLevadura;

}
