package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperLocalidad;

/**
 * MapperProveedor tiene la responsabilidad de mapear la entidad ProveedorEntity a
 * ProveedorResponseDTO, incluyendo su localidad y su catálogo de productos.
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
                .razonSocial(proveedorEntity.getRazonSocial())
                .nombreComercial(proveedorEntity.getNombreComercial())
                .cuit(proveedorEntity.getCuit())
                .telefono(proveedorEntity.getTelefono())
                .email(proveedorEntity.getEmail())
                .direccion(proveedorEntity.getDireccion())
                .localidad(MapperLocalidad.toDTO(proveedorEntity.getLocalidad()))
                .catalogoProveedor(proveedorEntity.getCatalogoProveedor().stream()
                        .map(MapperCatalogoProveedor::toDTO)
                        .toList())
                // === AUDITABLE ENTITY ===
                .createdBy(proveedorEntity.getCreatedBy())
                .createdDate(proveedorEntity.getCreatedDate())
                .lastModifiedBy(proveedorEntity.getLastModifiedBy())
                .lastModifiedDate(proveedorEntity.getLastModifiedDate())
                .build();
    }
}
