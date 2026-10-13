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
 * DTO de formulario para anular un envasado de lote.
 * <p>
 * Contiene únicamente el dato ingresado por el usuario.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnularEnvasadoLoteFormDTO {

    /**
     * Motivo por el cual se anula el envasado.
     */
    @NotBlank(message = "El motivo de anulación es obligatorio")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "El motivo de anulación no puede superar 255 caracteres")
    private String motivoAnulacion;
}
