package com.github.heikyudev.maestrocervecero.util.mapper.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;

/**
 * MapperLocalidad tiene la responsabilidad de mapear la entidad LocalidadEntity a LocalidadResponseDTO.
 */
public class MapperLocalidad {

    /**
     * Mapea una instancia de {@link LocalidadEntity} a {@link LocalidadResponseDTO}.
     *
     * @param localidadEntity Entidad de localidad a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LocalidadResponseDTO toDTO(LocalidadEntity localidadEntity) {
        if (localidadEntity == null) {
            return null;
        }

        return LocalidadResponseDTO.builder()
                .id(localidadEntity.getId())
                .nombre(localidadEntity.getNombre())
                .codigoPostal(localidadEntity.getCodigoPostal())
                .provincia(MapperProvincia.toDTO(localidadEntity.getProvincia()))
                // === AUDITABLE ENTITY ===
                .createdBy(localidadEntity.getCreatedBy())
                .createdDate(localidadEntity.getCreatedDate())
                .lastModifiedBy(localidadEntity.getLastModifiedBy())
                .lastModifiedDate(localidadEntity.getLastModifiedDate())
                .build();
    }
}
