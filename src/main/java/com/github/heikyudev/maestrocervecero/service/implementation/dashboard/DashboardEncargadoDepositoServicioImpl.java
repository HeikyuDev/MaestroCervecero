package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDepositoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDepositoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardEncargadoDeposito;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación de {@link IDashboardEncargadoDepositoServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Las órdenes y los lotes salen de las consultas del
 * panel (con su proveedor o su insumo cargados en la misma consulta, de modo que el mapeo no consulta) y los insumos sin
 * stock salen del servicio de stock, que ya devuelve las filas y el total en una sola llamada.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DashboardEncargadoDepositoServicioImpl implements IDashboardEncargadoDepositoServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_ENCARGADO_DEPOSITO_FILAS_POR_BLOQUE);

    private final IOrdenCompraRepository ordenCompraRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IStockInsumoServicio stockInsumoServicio;

    @Override
    @Transactional(readOnly = true)
    public DashboardEncargadoDepositoResponseDTO obtenerDashboardEncargadoDeposito() {
        LocalDate hoy = LocalDate.now();

        return DashboardEncargadoDepositoResponseDTO.builder()
                .ordenesPendientes(BloqueDashboardResponseDTO.de(ordenCompraRepository.buscarPendientesParaPanel(TOPE_DE_FILAS),
                        MapperDashboardEncargadoDeposito::toOrdenCompraPendienteDTO))
                .ordenesConEntregaVencida(ordenCompraRepository.contarPendientesConEntregaVencida(hoy))
                .lotesVencidos(BloqueDashboardResponseDTO.de(loteInsumoRepository.buscarVencidosConCantidadParaPanel(hoy, TOPE_DE_FILAS),
                        MapperDashboardEncargadoDeposito::toLoteInsumoVencidoDTO))
                .insumosSinStock(BloqueDashboardResponseDTO.de(stockInsumoServicio.filtrarInsumos(null, null, SituacionStockInsumo.SIN_STOCK_DISPONIBLE, TOPE_DE_FILAS),
                        MapperDashboardEncargadoDeposito::toInsumoSinStockDTO))
                .build();
    }
}
