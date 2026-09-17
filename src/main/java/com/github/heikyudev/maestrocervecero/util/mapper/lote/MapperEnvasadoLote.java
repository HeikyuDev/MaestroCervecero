package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperBarril;

/**
 * MapperEnvasadoLote tiene la responsabilidad de mapear la entidad EnvasadoLoteEntity a
 * EnvasadoLoteResponseDTO.
 */
public class MapperEnvasadoLote {

    /**
     * Mapea una instancia de {@link EnvasadoLoteEntity} a {@link EnvasadoLoteResponseDTO},
     * incluyendo la etapa de lote y el barril asociados.
     *
     * @param envasadoLoteEntity Entidad de envasado de lote a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static EnvasadoLoteResponseDTO toDTO(EnvasadoLoteEntity envasadoLoteEntity) {
        if (envasadoLoteEntity == null) {
            return null;
        }

        return EnvasadoLoteResponseDTO.builder()
                .id(envasadoLoteEntity.getId())
                .cantidadEnvasada(envasadoLoteEntity.getCantidadEnvasada())
                .fechaAnulacion(envasadoLoteEntity.getFechaAnulacion())
                .motivoAnulacion(envasadoLoteEntity.getMotivoAnulacion())
                .estado(envasadoLoteEntity.getEstado())
                .etapaLote(MapperEtapaLote.toDTO(envasadoLoteEntity.getEtapaLote()))
                .barril(MapperBarril.toDTO(envasadoLoteEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(envasadoLoteEntity.getCreatedBy())
                .createdDate(envasadoLoteEntity.getCreatedDate())
                .lastModifiedBy(envasadoLoteEntity.getLastModifiedBy())
                .lastModifiedDate(envasadoLoteEntity.getLastModifiedDate())
                .build();
    }
}
