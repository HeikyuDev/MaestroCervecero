package com.github.heikyudev.maestrocervecero.util.mapper.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;

/**
 * MapperProvincia tiene la responsabilidad de mapear la entidad ProvinciaEntity a ProvinciaResponseDTO.
 */
public class MapperProvincia {

    /**
     * Mapea una instancia de {@link ProvinciaEntity} a {@link ProvinciaResponseDTO}.
     *
     * @param provinciaEntity Entidad de provincia a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ProvinciaResponseDTO toDTO(ProvinciaEntity provinciaEntity) {
        if (provinciaEntity == null) {
            return null;
        }

        return ProvinciaResponseDTO.builder()
                .id(provinciaEntity.getId())
                .nombre(provinciaEntity.getNombre())
                .pais(MapperPais.toDTO(provinciaEntity.getPais()))
                .estado(provinciaEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(provinciaEntity.getCreatedBy())
                .createdDate(provinciaEntity.getCreatedDate())
                .lastModifiedBy(provinciaEntity.getLastModifiedBy())
                .lastModifiedDate(provinciaEntity.getLastModifiedDate())
                .build();
    }
}
