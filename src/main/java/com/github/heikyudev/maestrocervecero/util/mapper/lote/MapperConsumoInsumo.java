package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperLoteInsumo;

import java.util.List;

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
     * @return Objeto DTO correspondiente, sin advertencias, o {@code null} si la entidad de entrada es nula.
     */
    public static ConsumoInsumoResponseDTO toDTO(ConsumoInsumoEntity consumoInsumoEntity) {
        return toDTO(consumoInsumoEntity, List.of());
    }

    /**
     * Mapea una instancia de {@link ConsumoInsumoEntity} a {@link ConsumoInsumoResponseDTO},
     * incluyendo la etapa de lote y el lote de insumo asociados y las advertencias del registro.
     *
     * @param consumoInsumoEntity Entidad de consumo de insumo a convertir.
     * @param advertencias Avisos que no impidieron el consumo; {@code null} equivale a ninguno.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ConsumoInsumoResponseDTO toDTO(ConsumoInsumoEntity consumoInsumoEntity, List<String> advertencias) {
        if (consumoInsumoEntity == null) {
            return null;
        }

        return ConsumoInsumoResponseDTO.builder()
                .id(consumoInsumoEntity.getId())
                .cantidadConsumida(consumoInsumoEntity.getCantidadConsumida())
                .costoUnitarioPPP(consumoInsumoEntity.getCostoUnitarioPPP())
                .fechaRegistro(consumoInsumoEntity.getCreatedDate())
                .fechaAnulacion(consumoInsumoEntity.getFechaAnulacion())
                .motivoAnulacion(consumoInsumoEntity.getMotivoAnulacion())
                .usuarioAnulacion(consumoInsumoEntity.getEstado() == EstadoTransaccion.ANULADO ? consumoInsumoEntity.getLastModifiedBy() : null)
                .estado(consumoInsumoEntity.getEstado())
                .tipoConsumo(consumoInsumoEntity.getTipoConsumo())
                .etapaLote(MapperEtapaLote.toDTO(consumoInsumoEntity.getEtapaLote()))
                .loteInsumo(MapperLoteInsumo.toDTO(consumoInsumoEntity.getLoteInsumo()))
                .advertencias(advertencias == null ? List.of() : List.copyOf(advertencias))
                .build();
    }
}
