package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;

/**
 * MapperFallaEquipamiento tiene la responsabilidad de mapear la entidad FallaEquipamientoEntity
 * a FallaEquipamientoResponseDTO.
 */
public class MapperFallaEquipamiento {

    /**
     * Mapea una instancia de {@link FallaEquipamientoEntity} a {@link FallaEquipamientoResponseDTO},
     * incluyendo el equipamiento sobre el que se registró la falla.
     *
     * @param fallaEquipamientoEntity Entidad de falla de equipamiento a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static FallaEquipamientoResponseDTO toDTO(FallaEquipamientoEntity fallaEquipamientoEntity) {
        if (fallaEquipamientoEntity == null) {
            return null;
        }

        return FallaEquipamientoResponseDTO.builder()
                .id(fallaEquipamientoEntity.getId())
                .fechaFalla(fallaEquipamientoEntity.getFechaFalla())
                .estado(fallaEquipamientoEntity.getEstado())
                .observaciones(fallaEquipamientoEntity.getObservaciones())
                .fechaAnulacion(fallaEquipamientoEntity.getFechaAnulacion())
                .motivoAnulacion(fallaEquipamientoEntity.getMotivoAnulacion())
                .equipamiento(MapperEquipamiento.toDTO(fallaEquipamientoEntity.getEquipamiento()))
                // === AUDITABLE ENTITY ===
                .createdBy(fallaEquipamientoEntity.getCreatedBy())
                .createdDate(fallaEquipamientoEntity.getCreatedDate())
                .lastModifiedBy(fallaEquipamientoEntity.getLastModifiedBy())
                .lastModifiedDate(fallaEquipamientoEntity.getLastModifiedDate())
                .build();
    }
}
