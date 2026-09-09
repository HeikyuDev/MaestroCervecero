package com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;

/**
 * MapperPlanificacionProduccion tiene la responsabilidad de mapear la entidad PlanificacionProduccionEntity a PlanificacionProduccionResponseDTO.
 */
public class MapperPlanificacionProduccion {

    /**
     * Mapea una instancia de {@link PlanificacionProduccionEntity} a {@link PlanificacionProduccionResponseDTO},
     * incluyendo la versión de receta utilizada.
     *
     * @param planificacionProduccionEntity Entidad de planificación de producción a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static PlanificacionProduccionResponseDTO toDTO(PlanificacionProduccionEntity planificacionProduccionEntity) {
        if (planificacionProduccionEntity == null) {
            return null;
        }

        return PlanificacionProduccionResponseDTO.builder()
                .id(planificacionProduccionEntity.getId())
                .fechaInicioEstimada(planificacionProduccionEntity.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(planificacionProduccionEntity.getFechaFinalizacionEstimada())
                .cantidadAProducir(planificacionProduccionEntity.getCantidadAProducir())
                .fechaFinalizacion(planificacionProduccionEntity.getFechaFinalizacion())
                .fechaAnulacion(planificacionProduccionEntity.getFechaAnulacion())
                .motivoFinalizacion(planificacionProduccionEntity.getMotivoFinalizacion())
                .motivoAnulacion(planificacionProduccionEntity.getMotivoAnulacion())
                .estado(planificacionProduccionEntity.getEstado())
                .versionReceta(MapperReceta.toDTO(planificacionProduccionEntity.getVersionReceta()))
                // === AUDITABLE ENTITY ===
                .createdBy(planificacionProduccionEntity.getCreatedBy())
                .createdDate(planificacionProduccionEntity.getCreatedDate())
                .lastModifiedBy(planificacionProduccionEntity.getLastModifiedBy())
                .lastModifiedDate(planificacionProduccionEntity.getLastModifiedDate())
                .build();
    }
}
