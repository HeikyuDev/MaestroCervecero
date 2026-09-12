package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperLoteInsumo;

/**
 * MapperReservaInsumo tiene la responsabilidad de mapear la entidad ReservaInsumoEntity a
 * ReservaInsumoResponseDTO.
 */
public class MapperReservaInsumo {

    /**
     * Mapea una instancia de {@link ReservaInsumoEntity} a {@link ReservaInsumoResponseDTO},
     * incluyendo la etapa de lote y el lote de insumo asociados.
     *
     * @param reservaInsumoEntity Entidad de reserva de insumo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ReservaInsumoResponseDTO toDTO(ReservaInsumoEntity reservaInsumoEntity) {
        if (reservaInsumoEntity == null) {
            return null;
        }

        return ReservaInsumoResponseDTO.builder()
                .id(reservaInsumoEntity.getId())
                .cantidadReservada(reservaInsumoEntity.getCantidadReservada())
                .etapaLote(MapperEtapaLote.toDTO(reservaInsumoEntity.getEtapaLote()))
                .loteInsumo(MapperLoteInsumo.toDTO(reservaInsumoEntity.getLoteInsumo()))
                .build();
    }
}
