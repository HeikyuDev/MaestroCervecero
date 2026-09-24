package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de una limpieza de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimpiezaBarrilFormDTO {

    /**
     * Identificador del barril sobre el que se registra la limpieza.
     */
    private Long idBarril;

    /**
     * Fecha y hora en la que se realizó la limpieza.
     */
    private LocalDateTime fechaLimpieza;

    /**
     * Observaciones sobre la limpieza.
     */
    private String observaciones;
}
