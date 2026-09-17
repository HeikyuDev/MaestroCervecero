package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para anular un envasado de lote.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularEnvasadoLoteFormDTO {

    /**
     * Motivo por el cual se anula el envasado.
     */
    private String motivoAnulacion;
}
