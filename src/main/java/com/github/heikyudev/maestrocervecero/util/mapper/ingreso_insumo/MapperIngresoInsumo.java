package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.orden_compra.MapperDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;

/**
 * MapperIngresoInsumo tiene la responsabilidad de mapear la entidad IngresoInsumoEntity a
 * IngresoInsumoResponseDTO.
 */
public class MapperIngresoInsumo {

    /**
     * Mapea una instancia de {@link IngresoInsumoEntity} a {@link IngresoInsumoResponseDTO},
     * incluyendo el detalle de compra recibido (si lo hay), el identificador de su orden de compra, el lote de insumo
     * asociado y, si el ingreso está anulado, el usuario que lo anuló (la última modificación de la auditoría).
     *
     * @param ingresoInsumoEntity Entidad de ingreso de insumo a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static IngresoInsumoResponseDTO toDTO(IngresoInsumoEntity ingresoInsumoEntity) {
        if (ingresoInsumoEntity == null) {
            return null;
        }

        return IngresoInsumoResponseDTO.builder()
                .id(ingresoInsumoEntity.getId())
                .fechaIngreso(ingresoInsumoEntity.getFechaIngreso())
                .cantidadRecibida(ingresoInsumoEntity.getCantidadRecibida())
                .cantidadPresentaciones(calcularPresentaciones(ingresoInsumoEntity))
                .costoUnitario(ingresoInsumoEntity.getCostoUnitario())
                .fechaAnulacion(ingresoInsumoEntity.getFechaAnulacion())
                .motivoAnulacion(ingresoInsumoEntity.getMotivoAnulacion())
                .usuarioAnulacion(ingresoInsumoEntity.getEstado() == EstadoTransaccion.ANULADO ? ingresoInsumoEntity.getLastModifiedBy() : null)
                .tipoIngreso(ingresoInsumoEntity.getTipoIngreso())
                .estado(ingresoInsumoEntity.getEstado())
                .detalleCompra(MapperDetalleCompra.toDTO(ingresoInsumoEntity.getDetalleCompra()))
                .idOrdenCompra(ingresoInsumoEntity.getDetalleCompra() != null && ingresoInsumoEntity.getDetalleCompra().getOrdenCompra() != null
                        ? ingresoInsumoEntity.getDetalleCompra().getOrdenCompra().getId() : null)
                .loteInsumo(MapperLoteInsumo.toDTO(ingresoInsumoEntity.getLoteInsumo()))
                .build();
    }

    /**
     * Calcula a cuántas presentaciones del ítem equivale la cantidad recibida de un ingreso por compra.
     *
     * @param ingresoInsumoEntity Ingreso de insumo a evaluar.
     * @return Las presentaciones, o {@code null} si el ingreso no es de un ítem de compra o el ítem no informa su presentación.
     */
    private static Double calcularPresentaciones(IngresoInsumoEntity ingresoInsumoEntity) {
        DetalleCompraEntity detalle = ingresoInsumoEntity.getDetalleCompra();
        if (detalle == null || ingresoInsumoEntity.getCantidadRecibida() == null || detalle.getCatalogoProveedor() == null
                || detalle.getCatalogoProveedor().getInsumo() == null || detalle.getCatalogoProveedor().getPresentacionComercial() == null) {
            return null;
        }
        return CantidadesDetalleCompra.convertirAPresentaciones(detalle, ingresoInsumoEntity.getCantidadRecibida());
    }
}
