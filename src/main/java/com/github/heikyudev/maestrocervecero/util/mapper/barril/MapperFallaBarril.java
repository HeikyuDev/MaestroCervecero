package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FallaBarrilResponseDTO;

/**
 * MapperFallaBarril tiene la responsabilidad de mapear la entidad FallaBarrilEntity a
 * FallaBarrilResponseDTO.
 */
public class MapperFallaBarril {

    /**
     * Mapea una instancia de {@link FallaBarrilEntity} a {@link FallaBarrilResponseDTO},
     * incluyendo el barril sobre el que se registró la falla.
     *
     * @param fallaBarrilEntity Entidad de falla de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static FallaBarrilResponseDTO toDTO(FallaBarrilEntity fallaBarrilEntity) {
        if (fallaBarrilEntity == null) {
            return null;
        }

        return FallaBarrilResponseDTO.builder()
                .id(fallaBarrilEntity.getId())
                .fechaFalla(fallaBarrilEntity.getFecha())
                .estado(fallaBarrilEntity.getEstado())
                .observaciones(fallaBarrilEntity.getObservaciones())
                .fechaAnulacion(fallaBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(fallaBarrilEntity.getMotivoAnulacion())
                .barril(MapperBarril.toDTO(fallaBarrilEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(fallaBarrilEntity.getCreatedBy())
                .createdDate(fallaBarrilEntity.getCreatedDate())
                .lastModifiedBy(fallaBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(fallaBarrilEntity.getLastModifiedDate())
                .build();
    }
}
