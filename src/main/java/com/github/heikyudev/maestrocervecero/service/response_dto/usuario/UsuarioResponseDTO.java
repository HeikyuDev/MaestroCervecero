package com.github.heikyudev.maestrocervecero.service.response_dto.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.*;


/**
 * DTO para representar un usuario.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
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
     * Nombre/s de pila del usuario.
     */
    private String nombre;

    /**
     * Apellido/s del usuario.
     */
    private String apellido;

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

    /**
     * Estado lógico del usuario (activo o dado de baja).
     */
    private Estado estado;
}
