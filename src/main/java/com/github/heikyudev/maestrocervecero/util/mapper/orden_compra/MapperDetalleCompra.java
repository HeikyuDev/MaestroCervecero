package com.github.heikyudev.maestrocervecero.util.mapper.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperCatalogoProveedor;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;

import java.math.BigDecimal;

/**
 * MapperDetalleCompra tiene la responsabilidad de mapear la entidad DetalleCompraEntity a DetalleCompraResponseDTO.
 */
public class MapperDetalleCompra {

    /**
     * Mapea una instancia de {@link DetalleCompraEntity} a {@link DetalleCompraResponseDTO},
     * incluyendo el ítem del catálogo del proveedor solicitado y el subtotal. No informa las cantidades
     * recibida y pendiente, que requieren consultar los ingresos del ítem.
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
                .subtotal(calcularSubtotal(detalleCompraEntity))
                .catalogoProveedor(MapperCatalogoProveedor.toDTO(detalleCompraEntity.getCatalogoProveedor()))
                .build();
    }

    /**
     * Mapea una instancia de {@link DetalleCompraEntity} a {@link DetalleCompraResponseDTO} informando además las
     * cantidades recibida y pendiente, ambas redondeadas a la escala de las cantidades: en unidades
     * ({@code cantidadRecibida} y {@code cantidadPendiente}) y su equivalente en la unidad del insumo.
     *
     * @param detalleCompraEntity Entidad de detalle de compra a convertir.
     * @param cantidadRecibida Cantidad ya recibida del ítem, en la unidad de medida base del insumo.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static DetalleCompraResponseDTO toDTO(DetalleCompraEntity detalleCompraEntity, double cantidadRecibida) {
        if (detalleCompraEntity == null) {
            return null;
        }

        double cantidadPendiente = CantidadesDetalleCompra.calcularCantidadPendiente(detalleCompraEntity, cantidadRecibida);
        double presentacionesRecibidas = CantidadesDetalleCompra.convertirAPresentaciones(detalleCompraEntity, cantidadRecibida);
        double presentacionesPendientes = CantidadesDetalleCompra.calcularPresentacionesPendientes(detalleCompraEntity, cantidadRecibida);
        return DetalleCompraResponseDTO.builder()
                .id(detalleCompraEntity.getId())
                .cantidad(detalleCompraEntity.getCantidad())
                .costoUnitario(detalleCompraEntity.getCostoUnitario())
                .subtotal(calcularSubtotal(detalleCompraEntity))
                .cantidadRecibida(presentacionesRecibidas)
                .cantidadPendiente(presentacionesPendientes)
                .cantidadRecibidaEnUnidadDelInsumo(Redondeo.redondear(cantidadRecibida))
                .cantidadPendienteEnUnidadDelInsumo(Redondeo.redondear(cantidadPendiente))
                .catalogoProveedor(MapperCatalogoProveedor.toDTO(detalleCompraEntity.getCatalogoProveedor()))
                .build();
    }

    /**
     * Calcula el subtotal de un ítem: cantidad por costo unitario, sin redondear.
     *
     * @param detalleCompraEntity Ítem de detalle de compra.
     * @return El subtotal, o {@code null} si el ítem no tiene cantidad o costo unitario.
     */
    private static BigDecimal calcularSubtotal(DetalleCompraEntity detalleCompraEntity) {
        if (detalleCompraEntity.getCantidad() == null || detalleCompraEntity.getCostoUnitario() == null) {
            return null;
        }
        return detalleCompraEntity.getCostoUnitario().multiply(BigDecimal.valueOf(detalleCompraEntity.getCantidad()));
    }
}
