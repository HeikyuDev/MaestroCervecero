package com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UsuarioFormDTO {
    /**
     * Nombre de usuario.
     */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    /**
     * Password de usuario.
     * <p>
     * Sin anotación de obligatoriedad a propósito: el mismo DTO se usa en la modificación, donde
     * dejarla vacía significa "conservar la actual". El alta la exige en el service. Si se informa,
     * debe cumplir las reglas de {@link MetodosPassword} (el service las vuelve a validar).
     * </p>
     */
    @Pattern(regexp = MetodosPassword.PATRON_OPCIONAL, message = MetodosPassword.MENSAJE_INVALIDA)
    private String password;

    /**
     * Nombre/s de pila del usuario.
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Apellido/s del usuario.
     */
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    /**
     * Correo electrónico del usuario.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String correo;

    /**
     * Teléfono del usuario.
     */
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    /**
     * Rol del usuario.
     */
    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
