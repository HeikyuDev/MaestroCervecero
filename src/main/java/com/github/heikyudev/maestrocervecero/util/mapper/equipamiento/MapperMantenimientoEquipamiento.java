package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MantenimientoEquipamientoResponseDTO;

/**
 * MapperMantenimientoEquipamiento tiene la responsabilidad de mapear la entidad
 * MantenimientoEquipamientoEntity a MantenimientoEquipamientoResponseDTO.
 */
public class MapperMantenimientoEquipamiento {

    /**
     * Mapea una instancia de {@link MantenimientoEquipamientoEntity} a
     * {@link MantenimientoEquipamientoResponseDTO}, incluyendo el equipamiento sobre el que se
     * registró el mantenimiento.
     *
     * @param mantenimientoEquipamientoEntity Entidad de mantenimiento de equipamiento a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static MantenimientoEquipamientoResponseDTO toDTO(MantenimientoEquipamientoEntity mantenimientoEquipamientoEntity) {
        if (mantenimientoEquipamientoEntity == null) {
            return null;
        }

        return MantenimientoEquipamientoResponseDTO.builder()
                .id(mantenimientoEquipamientoEntity.getId())
                .fechaMantenimiento(mantenimientoEquipamientoEntity.getFecha())
                .estado(mantenimientoEquipamientoEntity.getEstado())
                .observaciones(mantenimientoEquipamientoEntity.getObservaciones())
                .fechaAnulacion(mantenimientoEquipamientoEntity.getFechaAnulacion())
                .motivoAnulacion(mantenimientoEquipamientoEntity.getMotivoAnulacion())
                .equipamiento(MapperEquipamiento.toDTO(mantenimientoEquipamientoEntity.getEquipamiento()))
                // === AUDITABLE ENTITY ===
                .createdBy(mantenimientoEquipamientoEntity.getCreatedBy())
                .createdDate(mantenimientoEquipamientoEntity.getCreatedDate())
                .lastModifiedBy(mantenimientoEquipamientoEntity.getLastModifiedBy())
                .lastModifiedDate(mantenimientoEquipamientoEntity.getLastModifiedDate())
                .build();
    }
}
