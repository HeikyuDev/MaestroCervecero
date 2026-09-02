package com.github.heikyudev.maestrocervecero.util.mapper.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperLocalidad;

/**
 * MapperCliente tiene la responsabilidad de mapear la entidad ClienteEntity a ClienteResponseDTO.
 */
public class MapperCliente {

    /**
     * Mapea una instancia de {@link ClienteEntity} a {@link ClienteResponseDTO}.
     *
     * @param clienteEntity Entidad de cliente a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ClienteResponseDTO toDTO(ClienteEntity clienteEntity) {
        if (clienteEntity == null) {
            return null;
        }

        return ClienteResponseDTO.builder()
                .id(clienteEntity.getId())
                .nombre(clienteEntity.getNombre())
                .telefono(clienteEntity.getTelefono())
                .email(clienteEntity.getEmail())
                .direccion(clienteEntity.getDireccion())
                .localidad(MapperLocalidad.toDTO(clienteEntity.getLocalidad()))
                .estado(clienteEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(clienteEntity.getCreatedBy())
                .createdDate(clienteEntity.getCreatedDate())
                .lastModifiedBy(clienteEntity.getLastModifiedBy())
                .lastModifiedDate(clienteEntity.getLastModifiedDate())
                .build();
    }
}
