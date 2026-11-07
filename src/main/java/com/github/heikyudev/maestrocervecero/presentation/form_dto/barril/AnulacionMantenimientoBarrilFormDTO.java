package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para la anulación de el mantenimiento de barril.
 * <p>
 * Contiene únicamente el dato ingresado por el usuario. La fecha de anulación no se recibe por formulario: la
 * calcula el service con la fecha y hora actuales al momento de procesar la anulación.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnulacionMantenimientoBarrilFormDTO {

    /**
     * Motivo por el cual se anula el mantenimiento de barril.
     */
    @NotBlank(message = "El motivo de anulación es obligatorio")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "El motivo de anulación no puede superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String motivoAnulacion;
}
