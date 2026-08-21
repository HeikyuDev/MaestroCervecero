package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;

/**
 * MapperCatalogoProveedor tiene la responsabilidad de mapear la entidad
 * CatalogoProveedorEntity a CatalogoProveedorResponseDTO.
 */
public class MapperCatalogoProveedor {

    /**
     * Mapea una instancia de {@link CatalogoProveedorEntity} a {@link CatalogoProveedorResponseDTO}.
     *
     * @param catalogoProveedorEntity Entidad de ítem de catálogo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static CatalogoProveedorResponseDTO toDTO(CatalogoProveedorEntity catalogoProveedorEntity) {
        if (catalogoProveedorEntity == null) {
            return null;
        }

        return CatalogoProveedorResponseDTO.builder()
                .id(catalogoProveedorEntity.getId())
                .presentacionComercial(MapperPresentacionComercial.toDTO(catalogoProveedorEntity.getPresentacionComercial()))
                .insumo(MapperInsumo.toDTO(catalogoProveedorEntity.getInsumo()))
                .seleccionado(catalogoProveedorEntity.isSeleccionado())
                .build();
    }
}
