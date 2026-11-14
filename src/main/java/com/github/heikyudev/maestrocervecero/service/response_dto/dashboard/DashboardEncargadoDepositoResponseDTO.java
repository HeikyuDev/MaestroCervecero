package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con el panel del encargado de depósito: tres bloques de hasta 5 filas, cada uno con su total real, y
 * el contador de órdenes con entrega vencida. Los contadores de la pantalla salen de los {@code total} de los bloques
 * y de {@link #ordenesConEntregaVencida}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEncargadoDepositoResponseDTO {

    /** Órdenes de compra Pendiente, la de entrega más vencida primero. */
    private BloqueDashboardResponseDTO<OrdenCompraPendienteResponseDTO> ordenesPendientes;

    /** Cantidad de órdenes de compra Pendiente con fecha de entrega estimada anterior a hoy. Nunca supera al total de {@link #ordenesPendientes}. */
    private long ordenesConEntregaVencida;

    /** Lotes de insumo vencidos que conservan cantidad, el que venció antes primero. */
    private BloqueDashboardResponseDTO<LoteInsumoVencidoResponseDTO> lotesVencidos;

    /** Insumos activos sin stock disponible, por nombre. */
    private BloqueDashboardResponseDTO<InsumoSinStockResponseDTO> insumosSinStock;
}
