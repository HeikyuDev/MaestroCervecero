package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el registro de un fraccionamiento de barril.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La fecha y hora de la operación la asigna el servicio al momento del registro.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraccionamientoBarrilFormDTO {

    /**
     * Identificador del barril del cual se extrae la cerveza.
     */
    @NotNull(message = "El barril es obligatorio")
    private Long idBarril;

    /**
     * Cantidad de litros extraídos del barril.
     */
    @NotNull(message = "La cantidad extraída es obligatoria")
    @Positive(message = "La cantidad extraída debe ser mayor a 0")
    private Double cantidadExtraida;

    /**
     * Observaciones sobre el fraccionamiento (por ejemplo, el motivo: "Merma", "Testeo", "Embotellado").
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
