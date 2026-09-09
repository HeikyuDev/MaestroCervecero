package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;

/**
 * MapperEquipamiento tiene la responsabilidad de mapear la entidad base EquipamientoEntity
 * a EquipamientoResponseDTO, sin los datos técnicos propios de cada tipo concreto.
 */
public class MapperEquipamiento {

    /**
     * Mapea una instancia de {@link EquipamientoEntity} a {@link EquipamientoResponseDTO}.
     *
     * @param equipamientoEntity Entidad de equipamiento a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static EquipamientoResponseDTO toDTO(EquipamientoEntity equipamientoEntity) {
        if (equipamientoEntity == null) {
            return null;
        }

        return EquipamientoResponseDTO.builder()
                .id(equipamientoEntity.getId())
                .identificadorInterno(equipamientoEntity.getIdentificadorInterno())
                .descripcion(equipamientoEntity.getDescripcion())
                .estadoOperativo(equipamientoEntity.getEstadoOperativo())
                .estado(equipamientoEntity.getEstado())
                .build();
    }
}
