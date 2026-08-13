package com.github.heikyudev.maestrocervecero.presentation.form_dto;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
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
    private String username;

    /**
     * Password de usuario.
     */
    private String password;

    /**
     * Nombre completo del usuario.
     */
    private String nombre;

    /**
     * Correo electrónico del usuario.
     */
    private String correo;

    /**
     * Teléfono del usuario.
     */
    private String telefono;

    /**
     * Rol del usuario.
     */
    private Rol rol;
}
