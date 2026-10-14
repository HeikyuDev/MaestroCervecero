package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;

import java.time.LocalDate;

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
        return toDTO(loteInsumoEntity, LocalDate.now());
    }

    /**
     * Mapea una instancia de {@link LoteInsumoEntity} a {@link LoteInsumoResponseDTO}, marcándola como vencida si su
     * fecha de vencimiento es anterior a la fecha indicada.
     *
     * @param loteInsumoEntity Entidad de lote de insumo a convertir.
     * @param hoy Fecha contra la que se evalúa el vencimiento.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LoteInsumoResponseDTO toDTO(LoteInsumoEntity loteInsumoEntity, LocalDate hoy) {
        if (loteInsumoEntity == null) {
            return null;
        }

        return LoteInsumoResponseDTO.builder()
                .id(loteInsumoEntity.getId())
                .cantidadActual(loteInsumoEntity.getCantidadActual())
                .cantidadReservada(loteInsumoEntity.getCantidadReservada())
                .identificacionLoteProveedor(loteInsumoEntity.getIdentificacionLoteProveedor())
                .cantidadDisponible(loteInsumoEntity.getCantidadDisponible())
                .fechaVencimiento(loteInsumoEntity.getFechaVencimiento())
                .vencido(loteInsumoEntity.getFechaVencimiento() != null && loteInsumoEntity.getFechaVencimiento().isBefore(hoy))
                .insumo(MapperInsumo.toDTO(loteInsumoEntity.getInsumo()))
                .build();
    }
}
