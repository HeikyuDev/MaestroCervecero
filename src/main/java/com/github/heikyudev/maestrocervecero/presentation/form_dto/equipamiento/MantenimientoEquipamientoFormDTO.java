package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de formulario para el registro de un mantenimiento de equipamiento.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoEquipamientoFormDTO {

    /**
     * Identificador del equipamiento sobre el que se registra el mantenimiento.
     */
    private Long idEquipamiento;

    /**
     * Fecha y hora en la que se realizó el mantenimiento.
     */
    private LocalDateTime fechaMantenimiento;

    /**
     * Observaciones sobre el mantenimiento.
     */
    private String observaciones;
}
