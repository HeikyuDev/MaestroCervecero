package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BusquedaBarrilEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BusquedaBarrilResponseDTO;

/**
 * MapperBusquedaBarril tiene la responsabilidad de mapear la entidad BusquedaBarrilEntity a
 * BusquedaBarrilResponseDTO.
 */
public class MapperBusquedaBarril {

    /**
     * Mapea una instancia de {@link BusquedaBarrilEntity} a {@link BusquedaBarrilResponseDTO},
     * incluyendo la solicitud de búsqueda sobre la que se registró.
     *
     * @param busquedaBarrilEntity Entidad de búsqueda de barril a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static BusquedaBarrilResponseDTO toDTO(BusquedaBarrilEntity busquedaBarrilEntity) {
        if (busquedaBarrilEntity == null) {
            return null;
        }

        return BusquedaBarrilResponseDTO.builder()
                .id(busquedaBarrilEntity.getId())
                .estado(busquedaBarrilEntity.getEstado())
                .fechaAnulacion(busquedaBarrilEntity.getFechaAnulacion())
                .motivoAnulacion(busquedaBarrilEntity.getMotivoAnulacion())
                .solicitudBusqueda(MapperSolicitudBusqueda.toDTO(busquedaBarrilEntity.getSolicitudBusqueda()))
                // === AUDITABLE ENTITY ===
                .createdBy(busquedaBarrilEntity.getCreatedBy())
                .createdDate(busquedaBarrilEntity.getCreatedDate())
                .lastModifiedBy(busquedaBarrilEntity.getLastModifiedBy())
                .lastModifiedDate(busquedaBarrilEntity.getLastModifiedDate())
                .build();
    }
}
