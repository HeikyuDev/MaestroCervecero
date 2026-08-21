package com.github.heikyudev.maestrocervecero.util.mapper.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;

/**
 * MapperPais tiene la responsabilidad de mapear la entidad PaisEntity a PaisResponseDTO.
 */
public class MapperPais {

    /**
     * Mapea una instancia de {@link PaisEntity} a {@link PaisResponseDTO}.
     *
     * @param paisEntity Entidad de país a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static PaisResponseDTO toDTO(PaisEntity paisEntity) {
        if (paisEntity == null) {
            return null;
        }

        return PaisResponseDTO.builder()
                .id(paisEntity.getId())
                .nombre(paisEntity.getNombre())
                .estado(paisEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(paisEntity.getCreatedBy())
                .createdDate(paisEntity.getCreatedDate())
                .lastModifiedBy(paisEntity.getLastModifiedBy())
                .lastModifiedDate(paisEntity.getLastModifiedDate())
                .build();
    }
}
