package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteComprasServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteComprasResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardGerenteCompras;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IDashboardGerenteComprasServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Las planificaciones y las órdenes salen de las consultas
 * del panel (con su versión de receta o de proveedor cargada en la misma consulta). El avance de recepción de las órdenes
 * visibles se calcula con una sola consulta de ítems y una sola de ingresos registrados, con la misma conversión a
 * presentaciones que usa el ingreso de insumos.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DashboardGerenteComprasServicioImpl implements IDashboardGerenteComprasServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_GERENTE_COMPRAS_FILAS_POR_BLOQUE);

    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final IDetalleCompraRepository detalleCompraRepository;
    private final IIngresoInsumoRepository ingresoInsumoRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardGerenteComprasResponseDTO obtenerDashboardGerenteCompras() {
        LocalDate hoy = LocalDate.now();
        int dias = LimitesTecnicos.PANEL_GERENTE_COMPRAS_DIAS_ORDENES_FINALIZADAS;

        Page<OrdenCompraEntity> pendientes = ordenCompraRepository.buscarPendientesParaPanel(TOPE_DE_FILAS);
        Map<Long, Integer> avances = calcularAvances(pendientes.getContent());

        return DashboardGerenteComprasResponseDTO.builder()
                .planificacionesSinOrden(BloqueDashboardResponseDTO.de(planificacionProduccionRepository.buscarPendientesSinOrdenDeCompraParaPanel(TOPE_DE_FILAS),
                        MapperDashboardGerenteCompras::toPlanificacionSinOrdenDTO))
                .ordenesPendientes(BloqueDashboardResponseDTO.de(pendientes, orden -> MapperDashboardGerenteCompras.toOrdenCompraEnCursoDTO(orden, avances.getOrDefault(orden.getId(), 0))))
                .ordenesConEntregaVencida(ordenCompraRepository.contarPendientesConEntregaVencida(hoy))
                .ordenesFinalizadas(BloqueDashboardResponseDTO.de(ordenCompraRepository.buscarFinalizadasDesdeParaPanel(hoy.minusDays(dias).atStartOfDay(), TOPE_DE_FILAS),
                        MapperDashboardGerenteCompras::toOrdenCompraFinalizadaDTO))
                .diasDeOrdenesFinalizadas(dias)
                .build();
    }

    /**
     * Calcula el avance de recepción (0 a 100) de cada orden con una consulta de ítems y una de ingresos registrados: las
     * presentaciones recibidas de cada ítem (sin pasar de lo pedido) sobre las pedidas en total, redondeado hacia abajo.
     */
    private Map<Long, Integer> calcularAvances(List<OrdenCompraEntity> ordenes) {
        if (ordenes.isEmpty()) {
            return Map.of();
        }
        List<DetalleCompraEntity> detalles = detalleCompraRepository.buscarConPresentacionDeOrdenes(ordenes.stream().map(OrdenCompraEntity::getId).toList());
        if (detalles.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> recibidoPorDetalle = ingresoInsumoRepository
                .buscarRegistradosDeDetalles(detalles.stream().map(DetalleCompraEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getDetalleCompra().getId(), Collectors.summingDouble(IngresoInsumoEntity::getCantidadRecibida)));

        Map<Long, List<DetalleCompraEntity>> detallesPorOrden = detalles.stream().collect(Collectors.groupingBy(d -> d.getOrdenCompra().getId()));
        return detallesPorOrden.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> avance(e.getValue(), recibidoPorDetalle)));
    }

    private static int avance(Collection<DetalleCompraEntity> detalles, Map<Long, Double> recibidoPorDetalle) {
        double pedidas = 0;
        double recibidas = 0;
        for (DetalleCompraEntity detalle : detalles) {
            double recibidasDelItem = CantidadesDetalleCompra.convertirAPresentaciones(detalle, recibidoPorDetalle.getOrDefault(detalle.getId(), 0.0));
            pedidas += detalle.getCantidad();
            recibidas += Math.min(recibidasDelItem, detalle.getCantidad());
        }
        return pedidas > 0 ? (int) Math.min(100, Math.floor(recibidas * 100 / pedidas)) : 0;
    }
}
