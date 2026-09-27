package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;

/**
 * MapperDevolucionBarril tiene la responsabilidad de mapear la entidad DevolucionBarrilEntity a
 * DevolucionBarrilResponseDTO.
 */
public class MapperDevolucionBarril {

    /**
     * Mapea una instancia de {@link DevolucionBarrilEntity} a {@link DevolucionBarrilResponseDTO},
     * incluyendo el barril sobre el que se registró la devolución.
     *
     * @param devolucionBarrilEntity Entidad de devolución de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static DevolucionBarrilResponseDTO toDTO(DevolucionBarrilEntity devolucionBarrilEntity) {
        if (devolucionBarrilEntity == null) {
            return null;
        }

        return DevolucionBarrilResponseDTO.builder()
                .id(devolucionBarrilEntity.getId())
                .fechaDevolucion(devolucionBarrilEntity.getFecha())
                .observaciones(devolucionBarrilEntity.getObservaciones())
                .estado(devolucionBarrilEntity.getEstado())
                .fechaAnulacion(devolucionBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(devolucionBarrilEntity.getMotivoAnulacion())
                .barril(MapperBarril.toDTO(devolucionBarrilEntity.getBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(devolucionBarrilEntity.getCreatedBy())
                .createdDate(devolucionBarrilEntity.getCreatedDate())
                .lastModifiedBy(devolucionBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(devolucionBarrilEntity.getLastModifiedDate())
                .build();
    }
}
