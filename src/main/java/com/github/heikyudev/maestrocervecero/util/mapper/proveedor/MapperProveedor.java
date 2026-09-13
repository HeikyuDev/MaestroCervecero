package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;

/**
 * MapperProveedor tiene la responsabilidad de mapear la entidad {@link ProveedorEntity}
 * a {@link ProveedorResponseDTO}.
 */
public class MapperProveedor {
    /**
     * Mapea una instancia de {@link ProveedorEntity} a {@link ProveedorResponseDTO}.
     *
     * @param proveedorEntity Entidad de proveedor a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static ProveedorResponseDTO toDTO(ProveedorEntity proveedorEntity) {
        if (proveedorEntity == null) {
            return null;
        }
        return ProveedorResponseDTO.builder()
                .id(proveedorEntity.getId())
                .version(mapVersionActual(proveedorEntity))
                .estado(proveedorEntity.getEstado())
                // AUDITABLE ENTITY
                .createdBy(proveedorEntity.getCreatedBy())
                .createdDate(proveedorEntity.getCreatedDate())
                .lastModifiedBy(proveedorEntity.getLastModifiedBy())
                .lastModifiedDate(proveedorEntity.getLastModifiedDate())
                .build();
    }


    /**
     * Mapea la versión actual del proveedor a {@link VersionProveedorResponseDTO}.
     *
     * @param proveedorEntity Entidad de proveedor que contiene las versiones.
     * @return Objeto DTO correspondiente a la versión actual o {@code null} si no hay versiones.
     */
    private static VersionProveedorResponseDTO mapVersionActual(ProveedorEntity proveedorEntity) {
        return MetodosVersionado.buscarVersionActiva(proveedorEntity.getVersiones())
                .map(MapperVersionProveedor::toDTO)
                .orElse(null);
    }
}
