package com.github.heikyudev.maestrocervecero.util.mapper.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_produccion.OrdenProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;

/**
 * MapperOrdenProduccion tiene la responsabilidad de mapear la entidad OrdenProduccionEntity a OrdenProduccionResponseDTO.
 */
public class MapperOrdenProduccion {

    /**
     * Mapea una instancia de {@link OrdenProduccionEntity} a {@link OrdenProduccionResponseDTO},
     * incluyendo la versión de receta utilizada.
     *
     * @param ordenProduccionEntity Entidad de orden de producción a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static OrdenProduccionResponseDTO toDTO(OrdenProduccionEntity ordenProduccionEntity) {
        if (ordenProduccionEntity == null) {
            return null;
        }

        return OrdenProduccionResponseDTO.builder()
                .id(ordenProduccionEntity.getId())
                .fechaInicioEstimada(ordenProduccionEntity.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(ordenProduccionEntity.getFechaFinalizacionEstimada())
                .cantidadAProducir(ordenProduccionEntity.getCantidadAProducir())
                .fechaFinalizacion(ordenProduccionEntity.getFechaFinalizacion())
                .fechaAnulacion(ordenProduccionEntity.getFechaAnulacion())
                .motivoFinalizacion(ordenProduccionEntity.getMotivoFinalizacion())
                .motivoAnulacion(ordenProduccionEntity.getMotivoAnulacion())
                .estado(ordenProduccionEntity.getEstado())
                .versionReceta(MapperReceta.toDTO(ordenProduccionEntity.getVersionReceta()))
                // === AUDITABLE ENTITY ===
                .createdBy(ordenProduccionEntity.getCreatedBy())
                .createdDate(ordenProduccionEntity.getCreatedDate())
                .lastModifiedBy(ordenProduccionEntity.getLastModifiedBy())
                .lastModifiedDate(ordenProduccionEntity.getLastModifiedDate())
                .build();
    }
}
