package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para anular un ingreso de insumo.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularIngresoInsumoFormDTO {

    /**
     * Motivo por el cual se anula el ingreso de insumo.
     */
    private String motivoAnulacion;
}
