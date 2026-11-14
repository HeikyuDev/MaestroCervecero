package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDepositoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.InsumoSinStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteInsumoVencidoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardEncargadoDepositoServicioImplTest {

    private static final Pageable TOPE = PageRequest.of(0, 5);

    @Mock
    private IOrdenCompraRepository ordenCompraRepository;

    @Mock
    private ILoteInsumoRepository loteInsumoRepository;

    @Mock
    private IStockInsumoServicio stockInsumoServicio;

    @InjectMocks
    private DashboardEncargadoDepositoServicioImpl dashboardServicio;

    @Test
    @DisplayName("CP-DED-01: obtenerDashboardEncargadoDeposito con el sistema vacío devuelve los tres bloques en cero, sin filas, y sin órdenes con entrega vencida")
    void obtener_debeDevolverElPanelEnCeroConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardEncargadoDepositoResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDeposito();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesPendientes().getTotal()).isZero();
        assertThat(resultado.getOrdenesPendientes().getFilas()).isEmpty();
        assertThat(resultado.getOrdenesConEntregaVencida()).isZero();
        assertThat(resultado.getLotesVencidos().getTotal()).isZero();
        assertThat(resultado.getLotesVencidos().getFilas()).isEmpty();
        assertThat(resultado.getInsumosSinStock().getTotal()).isZero();
        assertThat(resultado.getInsumosSinStock().getFilas()).isEmpty();
    }

    @Test
    @DisplayName("CP-DED-02: las órdenes pendientes se piden con el tope de 5 filas, se informa el total real y el proveedor y la fecha de cada fila, en el orden de la página")
    void obtener_debeInformarLasOrdenesPendientes() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(ordenCompraRepository.buscarPendientesParaPanel(TOPE)).thenReturn(new PageImpl<>(List.of(
                orden(3L, "Malterías del Sur S.A.", hoy.minusDays(4)), orden(1L, "Lúpulos SA", hoy.plusDays(2))), TOPE, 9));

        // === EJECUCION ===
        DashboardEncargadoDepositoResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDeposito();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesPendientes().getTotal()).isEqualTo(9);
        assertThat(resultado.getOrdenesPendientes().getFilas()).extracting(OrdenCompraPendienteResponseDTO::getId).containsExactly(3L, 1L);
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getProveedor()).isEqualTo("Malterías del Sur S.A.");
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getFechaEntregaEstimada()).isEqualTo(hoy.minusDays(4));
    }

    @Test
    @DisplayName("CP-DED-03: las órdenes con entrega vencida se cuentan con la fecha de hoy")
    void obtener_debeContarLasOrdenesConEntregaVencida() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(ordenCompraRepository.contarPendientesConEntregaVencida(LocalDate.now())).thenReturn(4L);

        // === EJECUCION ===
        DashboardEncargadoDepositoResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDeposito();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesConEntregaVencida()).isEqualTo(4);
        verify(ordenCompraRepository).contarPendientesConEntregaVencida(LocalDate.now());
    }

    @Test
    @DisplayName("CP-DED-04: los lotes vencidos se piden con la fecha de hoy y el tope de 5 filas, con el insumo, la unidad, el lote del proveedor, el vencimiento y la cantidad actual")
    void obtener_debeInformarLosLotesVencidos() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteInsumoRepository.buscarVencidosConCantidadParaPanel(hoy, TOPE)).thenReturn(new PageImpl<>(List.of(
                lote(10L, "Malta Pilsen", "L-125", hoy.minusDays(30), 7.5)), TOPE, 12));

        // === EJECUCION ===
        DashboardEncargadoDepositoResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDeposito();

        // === ASSERTS ===
        assertThat(resultado.getLotesVencidos().getTotal()).isEqualTo(12);
        LoteInsumoVencidoResponseDTO fila = resultado.getLotesVencidos().getFilas().get(0);
        assertThat(fila.getId()).isEqualTo(10L);
        assertThat(fila.getNombreInsumo()).isEqualTo("Malta Pilsen");
        assertThat(fila.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        assertThat(fila.getIdentificacionLoteProveedor()).isEqualTo("L-125");
        assertThat(fila.getFechaVencimiento()).isEqualTo(hoy.minusDays(30));
        assertThat(fila.getCantidadActual()).isEqualTo(7.5);
        verify(loteInsumoRepository).buscarVencidosConCantidadParaPanel(hoy, TOPE);
    }

    @Test
    @DisplayName("CP-DED-05: los insumos sin stock se piden con la situación sin stock disponible y el tope de 5 filas, con el total real")
    void obtener_debeInformarLosInsumosSinStock() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        StockInsumoResponseDTO insumo = StockInsumoResponseDTO.builder().id(4L).nombre("Lúpulo Cascade").tipo(TipoInsumo.LUPULO).build();
        when(stockInsumoServicio.filtrarInsumos(null, null, SituacionStockInsumo.SIN_STOCK_DISPONIBLE, TOPE))
                .thenReturn(new PageImpl<>(List.of(insumo), TOPE, 6));

        // === EJECUCION ===
        DashboardEncargadoDepositoResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDeposito();

        // === ASSERTS ===
        assertThat(resultado.getInsumosSinStock().getTotal()).isEqualTo(6);
        InsumoSinStockResponseDTO fila = resultado.getInsumosSinStock().getFilas().get(0);
        assertThat(fila.getId()).isEqualTo(4L);
        assertThat(fila.getNombre()).isEqualTo("Lúpulo Cascade");
        assertThat(fila.getTipo()).isEqualTo(TipoInsumo.LUPULO);
    }

    // ==================== helpers ====================

    /** Deja los bloques vacíos; cada test pisa solo el bloque que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(ordenCompraRepository.buscarPendientesParaPanel(any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(ordenCompraRepository.contarPendientesConEntregaVencida(any(LocalDate.class))).thenReturn(0L);
        lenient().when(loteInsumoRepository.buscarVencidosConCantidadParaPanel(any(LocalDate.class), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(stockInsumoServicio.filtrarInsumos(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
    }

    private static OrdenCompraEntity orden(Long id, String proveedor, LocalDate fechaEntrega) {
        return OrdenCompraEntity.builder().id(id).estado(EstadoSolicitud.PENDIENTE).fechaEntregaEstimada(fechaEntrega)
                .versionProveedor(VersionProveedorEntity.builder().razonSocial(proveedor).build()).build();
    }

    private static LoteInsumoEntity lote(Long id, String insumo, String identificacion, LocalDate vencimiento, double cantidad) {
        return LoteInsumoEntity.builder().id(id).identificacionLoteProveedor(identificacion).fechaVencimiento(vencimiento).cantidadActual(cantidad)
                .insumo(MaltaEntity.builder().nombre(insumo).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build()).build();
    }
}
