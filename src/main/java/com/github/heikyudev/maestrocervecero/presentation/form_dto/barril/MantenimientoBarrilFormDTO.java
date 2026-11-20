package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el registro de un mantenimiento de barril.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La fecha y hora de la operación la asigna el servicio al momento del registro.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoBarrilFormDTO {

    /**
     * Identificador del barril sobre el que se registra el mantenimiento.
     */
    @NotNull(message = "El barril es obligatorio")
    private Long idBarril;

    /**
     * Observaciones sobre el mantenimiento.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
