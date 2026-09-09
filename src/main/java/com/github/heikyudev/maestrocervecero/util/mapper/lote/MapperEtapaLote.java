package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperEquipamiento;

/**
 * MapperEtapaLote tiene la responsabilidad de mapear la entidad EtapaLoteEntity a EtapaLoteResponseDTO.
 */
public class MapperEtapaLote {

    /**
     * Mapea una instancia de {@link EtapaLoteEntity} a {@link EtapaLoteResponseDTO},
     * incluyendo el equipamiento asignado a la etapa.
     *
     * @param etapaLoteEntity Entidad de etapa de lote a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static EtapaLoteResponseDTO toDTO(EtapaLoteEntity etapaLoteEntity) {
        if (etapaLoteEntity == null) {
            return null;
        }

        return EtapaLoteResponseDTO.builder()
                .id(etapaLoteEntity.getId())
                .etapa(etapaLoteEntity.getEtapa())
                .estado(etapaLoteEntity.getEstado())
                .fechaInicio(etapaLoteEntity.getFechaInicio())
                .fechaFinalizacion(etapaLoteEntity.getFechaFinalizacion())
                .equipamiento(MapperEquipamiento.toDTO(etapaLoteEntity.getEquipamiento()))
                .build();
    }
}
