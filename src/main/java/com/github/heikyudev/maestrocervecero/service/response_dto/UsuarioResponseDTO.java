package com.github.heikyudev.maestrocervecero.service.response_dto;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import lombok.*;

/**
 * DTO para representar un usuario.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class UsuarioResponseDTO {
    /**
     * Identificador único del usuario.
     */
    private Long id;

    /**
     * Nombre de usuario.
     */
    private String username;

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
