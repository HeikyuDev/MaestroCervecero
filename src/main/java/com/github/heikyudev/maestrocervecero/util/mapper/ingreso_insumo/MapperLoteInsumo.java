package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;

/**
 * MapperLoteInsumo tiene la responsabilidad de mapear la entidad LoteInsumoEntity a
 * LoteInsumoResponseDTO.
 */
public class MapperLoteInsumo {

    /**
     * Mapea una instancia de {@link LoteInsumoEntity} a {@link LoteInsumoResponseDTO}.
     *
     * @param loteInsumoEntity Entidad de lote de insumo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LoteInsumoResponseDTO toDTO(LoteInsumoEntity loteInsumoEntity) {
        if (loteInsumoEntity == null) {
            return null;
        }

        return LoteInsumoResponseDTO.builder()
                .id(loteInsumoEntity.getId())
                .cantidadActual(loteInsumoEntity.getCantidadActual())
                .cantidadReservada(loteInsumoEntity.getCantidadReservada())
                .identificacionLoteProveedor(loteInsumoEntity.getIdentificacionLoteProveedor())
                .fechaVencimiento(loteInsumoEntity.getFechaVencimiento())
                .insumo(MapperInsumo.toDTO(loteInsumoEntity.getInsumo()))
                .build();
    }
}
