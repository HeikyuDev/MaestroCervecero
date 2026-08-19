package com.github.heikyudev.maestrocervecero.util.mapper.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;

/**
 * MapperLupulo tiene la responsabilidad de mapear la entidad LupuloEntity a LupuloResponseDTO.
 */
public class MapperLupulo {

    /**
     * Mapea una instancia de {@link LupuloEntity} a {@link LupuloResponseDTO}.
     *
     * @param lupuloEntity Entidad de lúpulo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LupuloResponseDTO toDTO(LupuloEntity lupuloEntity) {
        if (lupuloEntity == null) {
            return null;
        }

        return LupuloResponseDTO.builder()
                .id(lupuloEntity.getId())
                .nombre(lupuloEntity.getNombre())
                .unidadDeMedida(lupuloEntity.getUnidadDeMedida())
                .formato(lupuloEntity.getFormato())
                .aa(lupuloEntity.getAa())
                // === AUDITABLE ENTITY ===
                .createdBy(lupuloEntity.getCreatedBy())
                .createdDate(lupuloEntity.getCreatedDate())
                .lastModifiedBy(lupuloEntity.getLastModifiedBy())
                .lastModifiedDate(lupuloEntity.getLastModifiedDate())
                .build();
    }
}
