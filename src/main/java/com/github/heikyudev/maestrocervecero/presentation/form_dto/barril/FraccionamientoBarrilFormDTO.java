package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un fraccionamiento de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraccionamientoBarrilFormDTO {

    /**
     * Identificador del barril del cual se extrae la cerveza.
     */
    private Long idBarril;

    /**
     * Fecha y hora en la que se realizó el fraccionamiento.
     */
    private LocalDateTime fecha;

    /**
     * Cantidad de litros extraídos del barril.
     */
    private Double cantidadExtraida;

    /**
     * Observaciones sobre el fraccionamiento (por ejemplo, el motivo: "Merma", "Testeo", "Embotellado").
     */
    private String observaciones;
}
