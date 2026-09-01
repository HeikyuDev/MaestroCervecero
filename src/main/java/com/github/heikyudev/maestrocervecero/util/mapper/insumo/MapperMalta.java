package com.github.heikyudev.maestrocervecero.util.mapper.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;

/**
 * MapperMalta tiene la responsabilidad de mapear la entidad MaltaEntity a MaltaResponseDTO.
 */
public class MapperMalta {

    /**
     * Mapea una instancia de {@link MaltaEntity} a {@link MaltaResponseDTO}.
     *
     * @param maltaEntity Entidad de malta a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MaltaResponseDTO toDTO(MaltaEntity maltaEntity) {
        if (maltaEntity == null) {
            return null;
        }

        return MaltaResponseDTO.builder()
                .id(maltaEntity.getId())
                .nombre(maltaEntity.getNombre())
                .unidadDeMedida(maltaEntity.getUnidadDeMedida())
                .tipo(maltaEntity.getTipo())
                .potencialExtracto(maltaEntity.getPotencialExtracto())
                .estado(maltaEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(maltaEntity.getCreatedBy())
                .createdDate(maltaEntity.getCreatedDate())
                .lastModifiedBy(maltaEntity.getLastModifiedBy())
                .lastModifiedDate(maltaEntity.getLastModifiedDate())
                .build();
    }
}
