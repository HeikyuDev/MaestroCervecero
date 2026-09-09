package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.costo_adicional.MapperDetalleCostoDirecto;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanificacionProduccion;

/**
 * MapperLote tiene la responsabilidad de mapear la entidad LoteEntity a LoteResponseDTO.
 */
public class MapperLote {

    /**
     * Mapea una instancia de {@link LoteEntity} a {@link LoteResponseDTO}, incluyendo la planificación de
     * producción asociada, sus etapas y el detalle de costos directos adicionales aplicados.
     *
     * @param loteEntity Entidad de lote a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LoteResponseDTO toDTO(LoteEntity loteEntity) {
        if (loteEntity == null) {
            return null;
        }

        return LoteResponseDTO.builder()
                .id(loteEntity.getId())
                .identificadorInterno(loteEntity.getIdentificadorInterno())
                .volumenObjetivo(loteEntity.getVolumenObjetivo())
                .estado(loteEntity.getEstado())
                .fechaInicioEstimada(loteEntity.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(loteEntity.getFechaFinalizacionEstimada())
                .fechaInicio(loteEntity.getFechaInicio())
                .fechaFinalizacion(loteEntity.getFechaFinalizacion())
                .motivoAnulacion(loteEntity.getMotivoAnulacion())
                .planificacionProduccion(MapperPlanificacionProduccion.toDTO(loteEntity.getPlanificacionProduccion()))
                .etapas(loteEntity.getEtapas().stream()
                        .map(MapperEtapaLote::toDTO)
                        .toList())
                .detallesCostoDirecto(loteEntity.getDetallesCostoDirecto().stream()
                        .map(MapperDetalleCostoDirecto::toDTO)
                        .toList())
                // === AUDITABLE ENTITY ===
                .createdBy(loteEntity.getCreatedBy())
                .createdDate(loteEntity.getCreatedDate())
                .lastModifiedBy(loteEntity.getLastModifiedBy())
                .lastModifiedDate(loteEntity.getLastModifiedDate())
                .build();
    }
}
