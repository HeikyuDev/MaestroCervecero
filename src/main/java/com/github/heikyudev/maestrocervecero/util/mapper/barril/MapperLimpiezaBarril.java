package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.LimpiezaBarrilResponseDTO;

/**
 * MapperLimpiezaBarril tiene la responsabilidad de mapear la entidad LimpiezaBarrilEntity a
 * LimpiezaBarrilResponseDTO.
 */
public class MapperLimpiezaBarril {

    /**
     * Mapea una instancia de {@link LimpiezaBarrilEntity} a {@link LimpiezaBarrilResponseDTO},
     * incluyendo el barril sobre el que se registró la limpieza.
     *
     * @param limpiezaBarrilEntity Entidad de limpieza de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static LimpiezaBarrilResponseDTO toDTO(LimpiezaBarrilEntity limpiezaBarrilEntity) {
        if (limpiezaBarrilEntity == null) {
            return null;
        }

        return LimpiezaBarrilResponseDTO.builder()
                .id(limpiezaBarrilEntity.getId())
                .fechaLimpieza(limpiezaBarrilEntity.getFechaLimpieza())
                .observaciones(limpiezaBarrilEntity.getObservaciones())
                .estado(limpiezaBarrilEntity.getEstado())
                .estadoOperativoResultante(limpiezaBarrilEntity.getEstadoOperativoResultante())
                .fechaAnulacion(limpiezaBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(limpiezaBarrilEntity.getMotivoAnulacion())
                .barril(MapperBarril.toDTO(limpiezaBarrilEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(limpiezaBarrilEntity.getCreatedBy())
                .createdDate(limpiezaBarrilEntity.getCreatedDate())
                .lastModifiedBy(limpiezaBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(limpiezaBarrilEntity.getLastModifiedDate())
                .build();
    }
}
