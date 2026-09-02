package com.github.heikyudev.maestrocervecero.util.mapper.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;

/**
 * MapperCostoDirectoAdicional tiene la responsabilidad de mapear la entidad
 * CostoDirectoAdicionalEntity a CostoDirectoAdicionalResponseDTO.
 */
public class MapperCostoDirectoAdicional {

    /**
     * Mapea una instancia de {@link CostoDirectoAdicionalEntity} a {@link CostoDirectoAdicionalResponseDTO}.
     *
     * @param costoDirectoAdicionalEntity Entidad de costo directo adicional a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static CostoDirectoAdicionalResponseDTO toDTO(CostoDirectoAdicionalEntity costoDirectoAdicionalEntity) {
        if (costoDirectoAdicionalEntity == null) {
            return null;
        }

        return CostoDirectoAdicionalResponseDTO.builder()
                .id(costoDirectoAdicionalEntity.getId())
                .nombre(costoDirectoAdicionalEntity.getNombre())
                .costoPorLitro(costoDirectoAdicionalEntity.getCostoPorLitro())
                .estado(costoDirectoAdicionalEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(costoDirectoAdicionalEntity.getCreatedBy())
                .createdDate(costoDirectoAdicionalEntity.getCreatedDate())
                .lastModifiedBy(costoDirectoAdicionalEntity.getLastModifiedBy())
                .lastModifiedDate(costoDirectoAdicionalEntity.getLastModifiedDate())
                .build();
    }
}
