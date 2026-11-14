package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un usuario activo modificado recientemente, en el panel del administrador.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRecienteResponseDTO {

    /** Identificador único del usuario. */
    private Long id;

    /** Nombre de usuario con el que inicia sesión; con él se arma el enlace a la lista de usuarios filtrada. */
    private String username;

    /** Nombre y apellido del usuario. */
    private String nombreCompleto;

    /** Rol del usuario. */
    private Rol rol;
}
