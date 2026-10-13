package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para la cancelación de un Lote.
 * <p>
 * Contiene únicamente el dato ingresado por el usuario.
 * La fecha de cancelación no se recibe por formulario: la calcula el service con la fecha y
 * hora actuales al momento de procesar la cancelación.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionLoteFormDTO {

    /**
     * Motivo por el cual se cancela el lote.
     */
    @NotBlank(message = "El motivo de cancelación es obligatorio")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "El motivo de cancelación no puede superar 255 caracteres")
    private String motivoCancelacion;
}
