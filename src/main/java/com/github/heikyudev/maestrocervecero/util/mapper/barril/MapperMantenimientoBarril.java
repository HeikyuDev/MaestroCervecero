package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.MantenimientoBarrilResponseDTO;

/**
 * MapperMantenimientoBarril tiene la responsabilidad de mapear la entidad
 * MantenimientoBarrilEntity a MantenimientoBarrilResponseDTO.
 */
public class MapperMantenimientoBarril {

    /**
     * Mapea una instancia de {@link MantenimientoBarrilEntity} a
     * {@link MantenimientoBarrilResponseDTO}, incluyendo el barril sobre el que se registró el
     * mantenimiento.
     *
     * @param mantenimientoBarrilEntity Entidad de mantenimiento de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MantenimientoBarrilResponseDTO toDTO(MantenimientoBarrilEntity mantenimientoBarrilEntity) {
        if (mantenimientoBarrilEntity == null) {
            return null;
        }

        return MantenimientoBarrilResponseDTO.builder()
                .id(mantenimientoBarrilEntity.getId())
                .fechaMantenimiento(mantenimientoBarrilEntity.getFechaMantenimiento())
                .estado(mantenimientoBarrilEntity.getEstado())
                .observaciones(mantenimientoBarrilEntity.getObservaciones())
                .fechaAnulacion(mantenimientoBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(mantenimientoBarrilEntity.getMotivoAnulacion())
                .barril(MapperBarril.toDTO(mantenimientoBarrilEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(mantenimientoBarrilEntity.getCreatedBy())
                .createdDate(mantenimientoBarrilEntity.getCreatedDate())
                .lastModifiedBy(mantenimientoBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(mantenimientoBarrilEntity.getLastModifiedDate())
                .build();
    }
}
