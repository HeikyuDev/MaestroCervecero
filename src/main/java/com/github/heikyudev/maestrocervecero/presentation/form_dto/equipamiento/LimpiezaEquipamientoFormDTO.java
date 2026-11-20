package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

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
 * DTO de formulario para el registro de una limpieza de equipamiento.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. La fecha y hora de la operación la asigna el servicio al momento del registro.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimpiezaEquipamientoFormDTO {

    /**
     * Identificador del equipamiento sobre el que se registra una limpieza.
     */
    @NotNull(message = "El equipamiento es obligatorio")
    private Long idEquipamiento;

    /**
     * Observaciones sobre la limpieza.
     */
    @NotBlank(message = "Las observaciones son obligatorias")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observaciones;
}
