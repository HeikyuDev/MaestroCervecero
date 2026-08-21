package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;

/**
 * MapperPresentacionComercial tiene la responsabilidad de mapear la entidad
 * PresentacionComercialEntity a PresentacionComercialResponseDTO.
 */
public class MapperPresentacionComercial {

    /**
     * Mapea una instancia de {@link PresentacionComercialEntity} a {@link PresentacionComercialResponseDTO}.
     *
     * @param presentacionComercialEntity Entidad de presentación comercial a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static PresentacionComercialResponseDTO toDTO(PresentacionComercialEntity presentacionComercialEntity) {
        if (presentacionComercialEntity == null) {
            return null;
        }

        return PresentacionComercialResponseDTO.builder()
                .id(presentacionComercialEntity.getId())
                .nombre(presentacionComercialEntity.getNombre())
                .cantidad(presentacionComercialEntity.getCantidad())
                .unidadDeMedida(presentacionComercialEntity.getUnidadDeMedida())
                .estado(presentacionComercialEntity.getEstado())
                // === AUDITABLE ENTITY ===
                .createdBy(presentacionComercialEntity.getCreatedBy())
                .createdDate(presentacionComercialEntity.getCreatedDate())
                .lastModifiedBy(presentacionComercialEntity.getLastModifiedBy())
                .lastModifiedDate(presentacionComercialEntity.getLastModifiedDate())
                .build();
    }
}
