package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.LimpiezaEquipamientoResponseDTO;

/**
 * MapperLimpiezaEquipamiento tiene la responsabilidad de mapear la entidad
 * LimpiezaEquipamientoEntity a LimpiezaEquipamientoResponseDTO.
 */
public class MapperLimpiezaEquipamiento {

    /**
     * Mapea una instancia de {@link LimpiezaEquipamientoEntity} a
     * {@link LimpiezaEquipamientoResponseDTO}, incluyendo el equipamiento sobre el que se
     * registró la limpieza.
     *
     * @param limpiezaEquipamientoEntity Entidad de limpieza de equipamiento a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LimpiezaEquipamientoResponseDTO toDTO(LimpiezaEquipamientoEntity limpiezaEquipamientoEntity) {
        if (limpiezaEquipamientoEntity == null) {
            return null;
        }

        return LimpiezaEquipamientoResponseDTO.builder()
                .id(limpiezaEquipamientoEntity.getId())
                .fechaLimpieza(limpiezaEquipamientoEntity.getFechaLimpieza())
                .observaciones(limpiezaEquipamientoEntity.getObservaciones())
                .estado(limpiezaEquipamientoEntity.getEstado())
                .estadoOperativoResultante(limpiezaEquipamientoEntity.getEstadoOperativoResultante())
                .fechaAnulacion(limpiezaEquipamientoEntity.getFechaAnulacion())
                .motivoAnulacion(limpiezaEquipamientoEntity.getMotivoAnulacion())
                .equipamiento(MapperEquipamiento.toDTO(limpiezaEquipamientoEntity.getEquipamiento()))
                // === AUDITABLE ENTITY ===
                .createdBy(limpiezaEquipamientoEntity.getCreatedBy())
                .createdDate(limpiezaEquipamientoEntity.getCreatedDate())
                .lastModifiedBy(limpiezaEquipamientoEntity.getLastModifiedBy())
                .lastModifiedDate(limpiezaEquipamientoEntity.getLastModifiedDate())
                .build();
    }
}
