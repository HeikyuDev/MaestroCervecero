package com.github.heikyudev.maestrocervecero.util.mapper;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.LevaduraResponseDTO;

/**
 * MapperLevadura tiene la responsabilidad de mapear la entidad LevaduraEntity a LevaduraResponseDTO.
 */
public class MapperLevadura {

    /**
     * Mapea una instancia de {@link LevaduraEntity} a {@link LevaduraResponseDTO}.
     *
     * @param levaduraEntity Entidad de levadura a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LevaduraResponseDTO toDTO(LevaduraEntity levaduraEntity) {
        if (levaduraEntity == null) {
            return null;
        }

        return LevaduraResponseDTO.builder()
                .id(levaduraEntity.getId())
                .nombre(levaduraEntity.getNombre())
                .unidadDeMedida(levaduraEntity.getUnidadDeMedida())
                .tipo(levaduraEntity.getTipo())
                .cantidadCelulasPorGramo(levaduraEntity.getCantidadCelulasPorGramo())
                .build();
    }
}
