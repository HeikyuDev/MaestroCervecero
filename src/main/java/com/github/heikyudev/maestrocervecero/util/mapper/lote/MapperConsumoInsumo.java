package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperLoteInsumo;

/**
 * MapperConsumoInsumo tiene la responsabilidad de mapear la entidad ConsumoInsumoEntity a
 * ConsumoInsumoResponseDTO.
 */
public class MapperConsumoInsumo {

    /**
     * Mapea una instancia de {@link ConsumoInsumoEntity} a {@link ConsumoInsumoResponseDTO},
     * incluyendo la etapa de lote y el lote de insumo asociados.
     *
     * @param consumoInsumoEntity Entidad de consumo de insumo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ConsumoInsumoResponseDTO toDTO(ConsumoInsumoEntity consumoInsumoEntity) {
        if (consumoInsumoEntity == null) {
            return null;
        }

        return ConsumoInsumoResponseDTO.builder()
                .id(consumoInsumoEntity.getId())
                .cantidadConsumida(consumoInsumoEntity.getCantidadConsumida())
                .costoUnitarioPPP(consumoInsumoEntity.getCostoUnitarioPPP())
                .fechaAnulacion(consumoInsumoEntity.getFechaAnulacion())
                .motivoAnulacion(consumoInsumoEntity.getMotivoAnulacion())
                .estado(consumoInsumoEntity.getEstado())
                .tipoConsumo(consumoInsumoEntity.getTipoConsumo())
                .etapaLote(MapperEtapaLote.toDTO(consumoInsumoEntity.getEtapaLote()))
                .loteInsumo(MapperLoteInsumo.toDTO(consumoInsumoEntity.getLoteInsumo()))
                // === AUDITABLE ENTITY ===
                .createdBy(consumoInsumoEntity.getCreatedBy())
                .createdDate(consumoInsumoEntity.getCreatedDate())
                .lastModifiedBy(consumoInsumoEntity.getLastModifiedBy())
                .lastModifiedDate(consumoInsumoEntity.getLastModifiedDate())
                .build();
    }
}
