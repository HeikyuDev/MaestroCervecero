package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para la finalización forzada de una Planificación de Producción.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente el dato ingresado por el usuario.
 * La fecha de finalización no se recibe por formulario: la calcula el service con la fecha y
 * hora actuales al momento de procesar la finalización forzada.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizacionForzadaPlanificacionProduccionFormDTO {

    /**
     * Motivo por el cual se finaliza de forma forzada la planificación de producción.
     */
    private String motivoFinalizacion;
}
