package com.github.heikyudev.maestrocervecero.util.mapper.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;

/**
 * MapperEtapaControl tiene la responsabilidad de mapear la entidad EtapaControlEntity a EtapaControlResponseDTO.
 */
public class MapperEtapaControl {

    /**
     * Mapea una instancia de {@link  EtapaControlEntity} a {@link EtapaControlResponseDTO}.
     *
     * @param etapaControlEntity Entidad de etapa de control a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static EtapaControlResponseDTO toDTO(EtapaControlEntity etapaControlEntity){

        if (etapaControlEntity == null) {
            return null;
        }
        return EtapaControlResponseDTO.builder()
                .id(etapaControlEntity.getId())
                .nombre(etapaControlEntity.getNombre())
                .descripcion(etapaControlEntity.getDescripcion())
                .etapaAControlar(etapaControlEntity.getEtapaAControlar())
                .estado(etapaControlEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(etapaControlEntity.getCreatedBy())
                .createdDate(etapaControlEntity.getCreatedDate())
                .lastModifiedBy(etapaControlEntity.getLastModifiedBy())
                .lastModifiedDate(etapaControlEntity.getLastModifiedDate())
                .build();
    }
}
