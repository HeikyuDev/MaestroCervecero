package com.github.heikyudev.maestrocervecero.util.mapper.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;

/**
 * MapperUsuario tiene la responsabilidad de mapear la entidad UsuarioEntity a UsuarioResponse DTO
 */
public class MapperUsuario {

    /**
     * Mapea una instancia de {@link UsuarioEntity} a {@link UsuarioResponseDTO}.
     *
     * @param usuarioEntity Entidad de usuario a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static UsuarioResponseDTO toDTO(UsuarioEntity usuarioEntity) {
        if (usuarioEntity == null) {
            return null;
        }

        return UsuarioResponseDTO.builder()
                .id(usuarioEntity.getId())
                .username(usuarioEntity.getUsername())
                .nombre(usuarioEntity.getNombre())
                .telefono(usuarioEntity.getTelefono())
                .correo(usuarioEntity.getCorreo())
                .rol(usuarioEntity.getRol())
                .estado(usuarioEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(usuarioEntity.getCreatedBy())
                .createdDate(usuarioEntity.getCreatedDate())
                .lastModifiedBy(usuarioEntity.getLastModifiedBy())
                .lastModifiedDate(usuarioEntity.getLastModifiedDate())
                .build();
    }
}
