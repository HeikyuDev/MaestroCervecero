package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para la cancelación de un Lote.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * La fecha de cancelación no se recibe por formulario: la calcula el service con la fecha y
 * hora actuales al momento de procesar la cancelación.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionLoteFormDTO {

    /**
     * Motivo por el cual se cancela el lote.
     */
    private String motivoCancelacion;
}
