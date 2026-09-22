package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de una falla de equipamiento.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallaEquipamientoFormDTO {

    /**
     * Identificador del equipamiento sobre el que se registra la falla.
     */
    private Long idEquipamiento;

    /**
     * Fecha y hora en la que ocurrió la falla.
     */
    private LocalDateTime fechaFalla;

    /**
     * Observaciones sobre la falla.
     */
    private String observaciones;
}
