package com.github.heikyudev.maestrocervecero.util.mapper.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanificacionProduccion;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperVersionProveedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * MapperOrdenCompra tiene la responsabilidad de mapear la entidad OrdenCompraEntity a OrdenCompraResponseDTO.
 */
public class MapperOrdenCompra {

    /**
     * Mapea una instancia de {@link OrdenCompraEntity} a {@link OrdenCompraResponseDTO},
     * incluyendo la planificación de producción, el proveedor, el detalle de la compra y el total. Los ítems no
     * informan las cantidades recibida y pendiente.
     *
     * @param ordenCompraEntity Entidad de orden de compra a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static OrdenCompraResponseDTO toDTO(OrdenCompraEntity ordenCompraEntity) {
        return toDTO(ordenCompraEntity, MapperDetalleCompra::toDTO);
    }

    /**
     * Mapea una instancia de {@link OrdenCompraEntity} a {@link OrdenCompraResponseDTO} informando en cada ítem
     * las cantidades recibida y pendiente.
     *
     * @param ordenCompraEntity Entidad de orden de compra a convertir.
     * @param cantidadRecibidaPorDetalle Resuelve la cantidad ya recibida de cada ítem, en la unidad de medida base del insumo; el
     *                                   mapper la informa en presentaciones y en esa unidad.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static OrdenCompraResponseDTO toDTOConCantidades(OrdenCompraEntity ordenCompraEntity, ToDoubleFunction<DetalleCompraEntity> cantidadRecibidaPorDetalle) {
        return toDTO(ordenCompraEntity, detalle -> MapperDetalleCompra.toDTO(detalle, cantidadRecibidaPorDetalle.applyAsDouble(detalle)));
    }

    private static OrdenCompraResponseDTO toDTO(OrdenCompraEntity ordenCompraEntity, Function<DetalleCompraEntity, DetalleCompraResponseDTO> mapeadorDetalle) {
        if (ordenCompraEntity == null) {
            return null;
        }

        List<DetalleCompraResponseDTO> detallesCompra = ordenCompraEntity.getDetallesCompra().stream()
                .map(mapeadorDetalle)
                .toList();

        int totalPedidas = detallesCompra.stream()
                .map(DetalleCompraResponseDTO::getCantidad)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        boolean conCantidades = !detallesCompra.isEmpty()
                && detallesCompra.stream().allMatch(detalle -> detalle.getCantidadRecibida() != null && detalle.getCantidadPendiente() != null);
        Double totalRecibidas = conCantidades
                ? detallesCompra.stream().mapToDouble(DetalleCompraResponseDTO::getCantidadRecibida).sum() : null;
        Double totalPendientes = conCantidades
                ? detallesCompra.stream().mapToDouble(detalle -> Math.max(0.0, detalle.getCantidadPendiente())).sum() : null;
        Integer porcentajeRecibido = null;
        if (conCantidades) {
            porcentajeRecibido = totalPedidas > 0 ? (int) Math.min(100, Math.round(totalRecibidas * 100.0 / totalPedidas)) : 0;
        }

        return OrdenCompraResponseDTO.builder()
                .totalItems(detallesCompra.size())
                .totalPedidas(totalPedidas)
                .totalRecibidas(totalRecibidas)
                .totalPendientes(totalPendientes)
                .porcentajeRecibido(porcentajeRecibido)
                .id(ordenCompraEntity.getId())
                .fechaEntregaEstimada(ordenCompraEntity.getFechaEntregaEstimada())
                .estado(ordenCompraEntity.getEstado())
                .fechaFinalizacion(ordenCompraEntity.getFechaFinalizacion())
                .motivoFinalizacion(ordenCompraEntity.getMotivoFinalizacion())
                .usuarioFinalizacion(ordenCompraEntity.getEstado() == EstadoSolicitud.FINALIZADA ? ordenCompraEntity.getLastModifiedBy() : null)
                .fechaAnulacion(ordenCompraEntity.getFechaAnulacion())
                .motivoAnulacion(ordenCompraEntity.getMotivoAnulacion())
                .usuarioAnulacion(ordenCompraEntity.getEstado() == EstadoSolicitud.ANULADA ? ordenCompraEntity.getLastModifiedBy() : null)
                .planificacionProduccion(MapperPlanificacionProduccion.toDTO(ordenCompraEntity.getPlanificacionProduccion()))
                .proveedor(MapperVersionProveedor.toDTO(ordenCompraEntity.getVersionProveedor()))
                .detallesCompra(detallesCompra)
                .total(detallesCompra.stream()
                        .map(DetalleCompraResponseDTO::getSubtotal)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }
}
