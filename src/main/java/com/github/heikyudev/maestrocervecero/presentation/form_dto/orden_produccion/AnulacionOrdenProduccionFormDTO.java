package com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para la anulación de una Orden de Producción.
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
public class AnulacionOrdenProduccionFormDTO {

    /**
     * Motivo por el cual se anula la orden de producción.
     */
    private String motivoAnulacion;
}
