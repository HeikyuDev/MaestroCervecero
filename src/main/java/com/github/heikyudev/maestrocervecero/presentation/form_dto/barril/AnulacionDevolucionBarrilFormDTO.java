package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para la anulación de una devolución de barril.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * La fecha de anulación no se recibe por formulario: la calcula el service con la fecha y
 * hora actuales al momento de procesar la anulación.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnulacionDevolucionBarrilFormDTO {

    /**
     * Motivo por el cual se anula la devolución de barril.
     */
    private String motivoAnulacion;
}
