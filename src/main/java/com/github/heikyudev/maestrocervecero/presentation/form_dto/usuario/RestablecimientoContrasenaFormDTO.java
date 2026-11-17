package com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario;

import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Datos del formulario con el que un usuario elige su contraseña nueva desde el enlace recibido por correo. El token
 * del enlace no forma parte del formulario: el Controller lo recibe como parámetro aparte.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RestablecimientoContrasenaFormDTO {

    /**
     * Contraseña nueva. Debe cumplir las reglas de {@link MetodosPassword} (el service las vuelve a validar).
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = MetodosPassword.PATRON_OPCIONAL, message = MetodosPassword.MENSAJE_INVALIDA)
    private String password;

    /**
     * Repetición de la contraseña nueva. El service verifica que coincida con {@link #password}.
     */
    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    private String confirmacionPassword;
}
