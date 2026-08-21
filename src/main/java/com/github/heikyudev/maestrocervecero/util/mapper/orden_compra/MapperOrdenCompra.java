package com.github.heikyudev.maestrocervecero.util.mapper.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.orden_produccion.MapperOrdenProduccion;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperProveedor;

/**
 * MapperOrdenCompra tiene la responsabilidad de mapear la entidad OrdenCompraEntity a OrdenCompraResponseDTO.
 */
public class MapperOrdenCompra {

    /**
     * Mapea una instancia de {@link OrdenCompraEntity} a {@link OrdenCompraResponseDTO},
     * incluyendo la orden de producción, el proveedor y el detalle de la compra.
     *
     * @param ordenCompraEntity Entidad de orden de compra a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static OrdenCompraResponseDTO toDTO(OrdenCompraEntity ordenCompraEntity) {
        if (ordenCompraEntity == null) {
            return null;
        }

        return OrdenCompraResponseDTO.builder()
                .id(ordenCompraEntity.getId())
                .fechaEntregaEstimada(ordenCompraEntity.getFechaEntregaEstimada())
                .estado(ordenCompraEntity.getEstado())
                .fechaFinalizacion(ordenCompraEntity.getFechaFinalizacion())
                .motivoFinalizacion(ordenCompraEntity.getMotivoFinalizacion())
                .fechaAnulacion(ordenCompraEntity.getFechaAnulacion())
                .motivoAnulacion(ordenCompraEntity.getMotivoAnulacion())
                .ordenProduccion(MapperOrdenProduccion.toDTO(ordenCompraEntity.getOrdenProduccion()))
                .proveedor(MapperProveedor.toDTO(ordenCompraEntity.getProveedor()))
                .detallesCompra(ordenCompraEntity.getDetallesCompra().stream()
                        .map(MapperDetalleCompra::toDTO)
                        .toList())
                // === AUDITABLE ENTITY ===
                .createdBy(ordenCompraEntity.getCreatedBy())
                .createdDate(ordenCompraEntity.getCreatedDate())
                .lastModifiedBy(ordenCompraEntity.getLastModifiedBy())
                .lastModifiedDate(ordenCompraEntity.getLastModifiedDate())
                .build();
    }
}
