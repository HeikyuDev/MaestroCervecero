package com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Datos del formulario con el que un usuario que olvidó su contraseña pide el enlace para restablecerla.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SolicitudRestablecimientoContrasenaFormDTO {

    /**
     * Nombre de usuario de la cuenta cuya contraseña se quiere restablecer.
     */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;
}
