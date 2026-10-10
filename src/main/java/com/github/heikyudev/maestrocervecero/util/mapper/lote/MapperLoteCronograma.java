package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;

import java.util.List;

/**
 * MapperLoteCronograma tiene la responsabilidad de mapear la entidad LoteEntity a LoteCronogramaResponseDTO.
 */
public class MapperLoteCronograma {

    /**
     * Mapea una instancia de {@link LoteEntity} a {@link LoteCronogramaResponseDTO}. Lee únicamente
     * de las relaciones ya existentes de la entidad (planificación, versión de receta y etapas con su
     * equipamiento), sin ejecutar consultas propias.
     *
     * @param loteEntity Entidad de lote a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LoteCronogramaResponseDTO toDTO(LoteEntity loteEntity) {
        if (loteEntity == null) {
            return null;
        }

        EquipamientoEntity fermentador = equipamientoDeEtapa(loteEntity, TipoEtapa.FERMENTACION);

        return LoteCronogramaResponseDTO.builder()
                .id(loteEntity.getId())
                .identificadorInterno(loteEntity.getIdentificadorInterno())
                .estado(loteEntity.getEstado())
                .volumenObjetivo(loteEntity.getVolumenObjetivo())
                .fechaInicioEstimada(loteEntity.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(loteEntity.getFechaFinalizacionEstimada())
                .fechaInicio(loteEntity.getFechaInicio())
                .fechaFinalizacion(loteEntity.getFechaFinalizacion())
                .fechaCancelacion(loteEntity.getFechaCancelacion())
                .idPlanificacionProduccion(loteEntity.getPlanificacionProduccion().getId())
                .nombreReceta(loteEntity.getPlanificacionProduccion().getVersionReceta().getNombre())
                .idFermentador(fermentador != null ? fermentador.getId() : null)
                .identificadorInternoFermentador(fermentador != null ? fermentador.getIdentificadorInterno() : null)
                .identificadorInternoMolino(identificadorDeEtapa(loteEntity, TipoEtapa.MOLIENDA))
                .identificadorInternoMacerador(identificadorDeEtapa(loteEntity, TipoEtapa.MACERACION))
                .identificadorInternoOllaHervor(identificadorDeEtapa(loteEntity, TipoEtapa.HERVIDO))
                .etapaActual(loteEntity.getEtapas().stream()
                        .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.EN_CURSO)
                        .map(EtapaLoteEntity::getEtapa)
                        .findFirst()
                        .orElse(null))
                .build();
    }

    /**
     * Mapea una lista de {@link LoteEntity} a una lista de {@link LoteCronogramaResponseDTO}, conservando el orden.
     *
     * @param lotes Entidades de lote a convertir.
     * @return Lista de DTOs correspondiente.
     */
    public static List<LoteCronogramaResponseDTO> toDTOList(List<LoteEntity> lotes) {
        return lotes.stream()
                .map(MapperLoteCronograma::toDTO)
                .toList();
    }

    private static EquipamientoEntity equipamientoDeEtapa(LoteEntity lote, TipoEtapa tipo) {
        return lote.getEtapas().stream()
                .filter(etapa -> etapa.getEtapa() == tipo)
                .map(EtapaLoteEntity::getEquipamiento)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String identificadorDeEtapa(LoteEntity lote, TipoEtapa tipo) {
        EquipamientoEntity equipamiento = equipamientoDeEtapa(lote, tipo);
        return equipamiento != null ? equipamiento.getIdentificadorInterno() : null;
    }
}
