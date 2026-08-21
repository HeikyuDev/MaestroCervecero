package com.github.heikyudev.maestrocervecero.util.mapper.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperCatalogoProveedor;

/**
 * MapperDetalleCompra tiene la responsabilidad de mapear la entidad DetalleCompraEntity a DetalleCompraResponseDTO.
 */
public class MapperDetalleCompra {

    /**
     * Mapea una instancia de {@link DetalleCompraEntity} a {@link DetalleCompraResponseDTO},
     * incluyendo el ítem del catálogo del proveedor solicitado.
     *
     * @param detalleCompraEntity Entidad de detalle de compra a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static DetalleCompraResponseDTO toDTO(DetalleCompraEntity detalleCompraEntity) {
        if (detalleCompraEntity == null) {
            return null;
        }

        return DetalleCompraResponseDTO.builder()
                .id(detalleCompraEntity.getId())
                .cantidad(detalleCompraEntity.getCantidad())
                .costoUnitario(detalleCompraEntity.getCostoUnitario())
                .catalogoProveedor(MapperCatalogoProveedor.toDTO(detalleCompraEntity.getCatalogoProveedor()))
                .build();
    }
}
