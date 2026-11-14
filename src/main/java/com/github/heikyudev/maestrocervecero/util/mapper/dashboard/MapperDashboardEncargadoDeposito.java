package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.InsumoSinStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteInsumoVencidoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;

/**
 * Mapea las órdenes de compra, los lotes de insumo y los insumos con stock a los DTO de respuesta del panel del
 * encargado de depósito. No ejecuta consultas: lee solo de las relaciones ya cargadas.
 */
public class MapperDashboardEncargadoDeposito {

    private MapperDashboardEncargadoDeposito() {
    }

    /**
     * Mapea una orden de compra (con su versión de proveedor cargada) a {@link OrdenCompraPendienteResponseDTO}.
     *
     * @param orden Orden a convertir.
     * @return El DTO, o {@code null} si la orden es nula.
     */
    public static OrdenCompraPendienteResponseDTO toOrdenCompraPendienteDTO(OrdenCompraEntity orden) {
        if (orden == null) {
            return null;
        }
        return OrdenCompraPendienteResponseDTO.builder()
                .id(orden.getId())
                .proveedor(orden.getVersionProveedor().getRazonSocial())
                .fechaEntregaEstimada(orden.getFechaEntregaEstimada())
                .build();
    }

    /**
     * Mapea un lote de insumo (con su insumo cargado) a {@link LoteInsumoVencidoResponseDTO}.
     *
     * @param lote Lote a convertir.
     * @return El DTO, o {@code null} si el lote es nulo.
     */
    public static LoteInsumoVencidoResponseDTO toLoteInsumoVencidoDTO(LoteInsumoEntity lote) {
        if (lote == null) {
            return null;
        }
        return LoteInsumoVencidoResponseDTO.builder()
                .id(lote.getId())
                .nombreInsumo(lote.getInsumo().getNombre())
                .unidadDeMedida(lote.getInsumo().getUnidadDeMedida())
                .identificacionLoteProveedor(lote.getIdentificacionLoteProveedor())
                .fechaVencimiento(lote.getFechaVencimiento())
                .cantidadActual(lote.getCantidadActual())
                .build();
    }

    /**
     * Mapea un insumo de la pantalla de stock a {@link InsumoSinStockResponseDTO}.
     *
     * @param insumo Insumo a convertir.
     * @return El DTO, o {@code null} si el insumo es nulo.
     */
    public static InsumoSinStockResponseDTO toInsumoSinStockDTO(StockInsumoResponseDTO insumo) {
        if (insumo == null) {
            return null;
        }
        return InsumoSinStockResponseDTO.builder()
                .id(insumo.getId())
                .nombre(insumo.getNombre())
                .tipo(insumo.getTipo())
                .build();
    }
}
