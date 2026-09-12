package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.MedicionLoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.MedicionLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;

/**
 * MapperMedicionLote tiene la responsabilidad de mapear la entidad MedicionLoteEntity a MedicionLoteResponseDTO.
 */
public class MapperMedicionLote {

    /**
     * Mapea una instancia de {@link MedicionLoteEntity} a {@link MedicionLoteResponseDTO}, incluyendo
     * el detalle de parámetro de control medido y la etapa del lote sobre la que se registró.
     *
     * @param medicionLoteEntity Entidad de medición de lote a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MedicionLoteResponseDTO toDTO(MedicionLoteEntity medicionLoteEntity) {
        if (medicionLoteEntity == null) {
            return null;
        }

        return MedicionLoteResponseDTO.builder()
                .id(medicionLoteEntity.getId())
                .valorMedido(medicionLoteEntity.getValorMedido())
                .fechaMedicion(medicionLoteEntity.getFechaMedicion())
                .fechaAnulacion(medicionLoteEntity.getFechaAnulacion())
                .motivoAnulacion(medicionLoteEntity.getMotivoAnulacion())
                .estado(medicionLoteEntity.getEstado())
                .hayAlerta(medicionLoteEntity.isHayAlerta())
                .detalleParametroControl(MapperReceta.mapDetalleParametroControl(medicionLoteEntity.getDetalleParametroControl()))
                .etapaLote(MapperEtapaLote.toDTO(medicionLoteEntity.getEtapaLote()))
                // === AUDITABLE ENTITY ===
                .createdBy(medicionLoteEntity.getCreatedBy())
                .createdDate(medicionLoteEntity.getCreatedDate())
                .lastModifiedBy(medicionLoteEntity.getLastModifiedBy())
                .lastModifiedDate(medicionLoteEntity.getLastModifiedDate())
                .build();
    }
}
