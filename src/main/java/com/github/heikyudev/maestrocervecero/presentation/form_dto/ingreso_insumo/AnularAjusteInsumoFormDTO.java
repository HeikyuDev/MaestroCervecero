package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import lombok.*;

/**
 * DTO de formulario para la anulación de un ajuste de insumo.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularAjusteInsumoFormDTO {

    /**
     * Motivo por el cual se anula el ajuste de insumo.
     */
    private String motivoAnulacion;
}
