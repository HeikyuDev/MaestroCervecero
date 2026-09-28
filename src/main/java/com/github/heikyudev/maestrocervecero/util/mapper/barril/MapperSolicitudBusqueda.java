package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;

/**
 * MapperSolicitudBusqueda tiene la responsabilidad de mapear la entidad SolicitudBusquedaEntity
 * a SolicitudBusquedaResponseDTO.
 */
public class MapperSolicitudBusqueda {

    /**
     * Mapea una instancia de {@link SolicitudBusquedaEntity} a {@link SolicitudBusquedaResponseDTO},
     * incluyendo el despacho de barril sobre el que se solicitó la búsqueda.
     *
     * @param solicitudBusquedaEntity Entidad de solicitud de búsqueda a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static SolicitudBusquedaResponseDTO toDTO(SolicitudBusquedaEntity solicitudBusquedaEntity) {
        if (solicitudBusquedaEntity == null) {
            return null;
        }

        return SolicitudBusquedaResponseDTO.builder()
                .id(solicitudBusquedaEntity.getId())
                .fechaBusqueda(solicitudBusquedaEntity.getFechaBusqueda())
                .observaciones(solicitudBusquedaEntity.getObservaciones())
                .buscado(solicitudBusquedaEntity.isBuscado())
                .despachoBarril(MapperDespachoBarril.toDTO(solicitudBusquedaEntity.getDespachoBarril()))
                // === AUDITABLE ENTITY ===
                .createdBy(solicitudBusquedaEntity.getCreatedBy())
                .createdDate(solicitudBusquedaEntity.getCreatedDate())
                .lastModifiedBy(solicitudBusquedaEntity.getLastModifiedBy())
                .lastModifiedDate(solicitudBusquedaEntity.getLastModifiedDate())
                .build();
    }
}
