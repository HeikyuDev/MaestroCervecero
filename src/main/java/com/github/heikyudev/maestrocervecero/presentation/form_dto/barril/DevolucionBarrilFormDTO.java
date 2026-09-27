package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de una devolución de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevolucionBarrilFormDTO {

    /**
     * Identificador del barril que se devuelve.
     */
    private Long idBarril;

    /**
     * Fecha y hora en la que se recibió la devolución.
     */
    private LocalDateTime fechaDevolucion;

    /**
     * Observaciones sobre la devolución.
     */
    private String observaciones;
}
