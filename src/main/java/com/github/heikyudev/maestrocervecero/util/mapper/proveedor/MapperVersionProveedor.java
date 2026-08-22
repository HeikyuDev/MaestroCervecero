package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperLocalidad;

/**
 * MapperVersionProveedor tiene la responsabilidad de mapear la entidad VersionProveedorEntity a
 * VersionProveedorResponseDTO, incluyendo su localidad y su catálogo de productos.
 */
public class MapperVersionProveedor {

    /**
     * Mapea una instancia de {@link VersionProveedorEntity} a {@link VersionProveedorResponseDTO}.
     *
     * @param versionProveedorEntity Entidad de proveedor a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static VersionProveedorResponseDTO toDTO(VersionProveedorEntity versionProveedorEntity) {
        if (versionProveedorEntity == null) {
            return null;
        }

        return VersionProveedorResponseDTO.builder()
                .id(versionProveedorEntity.getId())
                .razonSocial(versionProveedorEntity.getRazonSocial())
                .nombreComercial(versionProveedorEntity.getNombreComercial())
                .cuit(versionProveedorEntity.getCuit())
                .telefono(versionProveedorEntity.getTelefono())
                .email(versionProveedorEntity.getEmail())
                .direccion(versionProveedorEntity.getDireccion())
                .esUltimaVersion(versionProveedorEntity.isEsUltimaVersion())
                .localidad(MapperLocalidad.toDTO(versionProveedorEntity.getLocalidad()))
                // Convierto la lista de entidades CatalogoProveedorEntity a una lista de DTOs CatalogoProveedorResponseDTO
                .catalogoProveedor(versionProveedorEntity.getCatalogoProveedor().stream()
                        .map(MapperCatalogoProveedor::toDTO)
                        .toList())
                .build();
    }
}
