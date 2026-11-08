package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.GrupoAlta;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de el macerador.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos) ni el estado operativo (lo define el sistema).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaceradorFormDTO {

    /**
     * Capacidad total en litros. Se informa solo en el alta: en la modificación no se envía ni se aplica, porque
     * la capacidad total no se puede cambiar (la fuente de verdad es el valor guardado).
     */
    @NotNull(message = "La capacidad total es obligatoria", groups = GrupoAlta.class)
    private Double capacidadTotal;

    /**
     * Capacidad útil en litros (menor a la total).
     */
    @NotNull(message = "La capacidad útil es obligatoria")
    private Double capacidadUtil;

    /**
     * Espacio muerto del macerador en litros.
     */
    @NotNull(message = "El espacio muerto es obligatorio")
    private Double espacioMuerto;

    /**
     * Eficiencia de maceración en porcentaje (entre 40 y 100).
     */
    @NotNull(message = "La eficiencia de maceración es obligatoria")
    private Double eficienciaMaceracion;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    @NotNull(message = "Los usos antes de mantenimiento son obligatorios")
    private Integer usosMaximosAntesMantenimiento;
}
