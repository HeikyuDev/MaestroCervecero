package com.github.heikyudev.maestrocervecero.service.response_dto.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.*;

import java.time.LocalDateTime;

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

    /**
     * Estado lógico del usuario (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el usuario.
     */
    private String createdBy;

    /**
     * Fecha de creación del usuario.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó al usuario por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del usuario.
     */
    private LocalDateTime lastModifiedDate;
}
