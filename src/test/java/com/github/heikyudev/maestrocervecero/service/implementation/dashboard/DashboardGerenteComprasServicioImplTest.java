package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteComprasResponseDTO;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGerenteComprasServicioImplTest {

    private static final Pageable TOPE = PageRequest.of(0, 5);

    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;

    @Mock
    private IOrdenCompraRepository ordenCompraRepository;

    @Mock
    private IDetalleCompraRepository detalleCompraRepository;

    @Mock
    private IIngresoInsumoRepository ingresoInsumoRepository;

    @InjectMocks
    private DashboardGerenteComprasServicioImpl dashboardServicio;

    @Test
    @DisplayName("CP-DGC-01: obtenerDashboardGerenteCompras con el sistema vacío devuelve los tres bloques en cero, sin filas, sin entregas vencidas y con la ventana de 30 días, sin consultar ítems ni ingresos")
    void obtener_debeDevolverElPanelEnCeroConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardGerenteComprasResponseDTO resultado = dashboardServicio.obtenerDashboardGerenteCompras();

        // === ASSERTS ===
        assertThat(resultado.getPlanificacionesSinOrden().getTotal()).isZero();
        assertThat(resultado.getPlanificacionesSinOrden().getFilas()).isEmpty();
        assertThat(resultado.getOrdenesPendientes().getTotal()).isZero();
        assertThat(resultado.getOrdenesPendientes().getFilas()).isEmpty();
        assertThat(resultado.getOrdenesConEntregaVencida()).isZero();
        assertThat(resultado.getOrdenesFinalizadas().getTotal()).isZero();
        assertThat(resultado.getOrdenesFinalizadas().getFilas()).isEmpty();
        assertThat(resultado.getDiasDeOrdenesFinalizadas()).isEqualTo(30);
        verify(detalleCompraRepository, never()).buscarConPresentacionDeOrdenes(anyCollection());
        verify(ingresoInsumoRepository, never()).buscarRegistradosDeDetalles(anyCollection());
    }

    @Test
    @DisplayName("CP-DGC-02: las planificaciones sin orden se piden con el tope de 5 filas, con el total real, la etiqueta, la fecha de inicio y la prioridad, en el orden de la página")
    void obtener_debeInformarLasPlanificacionesSinOrden() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(planificacionProduccionRepository.buscarPendientesSinOrdenDeCompraParaPanel(TOPE)).thenReturn(new PageImpl<>(List.of(
                planificacion(7L, "Pale Ale", LocalDate.of(2027, 8, 1), PrioridadPlanificacion.ALTA),
                planificacion(9L, "Stout", LocalDate.of(2027, 8, 5), PrioridadPlanificacion.BAJA)), TOPE, 6));

        // === EJECUCION ===
        DashboardGerenteComprasResponseDTO resultado = dashboardServicio.obtenerDashboardGerenteCompras();

        // === ASSERTS ===
        assertThat(resultado.getPlanificacionesSinOrden().getTotal()).isEqualTo(6);
        assertThat(resultado.getPlanificacionesSinOrden().getFilas()).hasSize(2);
        assertThat(resultado.getPlanificacionesSinOrden().getFilas().get(0).getId()).isEqualTo(7L);
        assertThat(resultado.getPlanificacionesSinOrden().getFilas().get(0).getEtiqueta()).contains("7").contains("Pale Ale");
        assertThat(resultado.getPlanificacionesSinOrden().getFilas().get(0).getFechaInicioEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
        assertThat(resultado.getPlanificacionesSinOrden().getFilas().get(0).getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
        assertThat(resultado.getPlanificacionesSinOrden().getFilas().get(1).getId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("CP-DGC-03: las órdenes pendientes informan el total real, el proveedor, la entrega estimada y el avance de recepción: las presentaciones recibidas sobre las pedidas en todos los ítems, sin pasar de lo pedido")
    void obtener_debeInformarLasOrdenesPendientesConSuAvance() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        OrdenCompraEntity orden = orden(7L, "Malterías del Sur S.A.", hoy.minusDays(3), EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity sinIngresos = orden(8L, "Lúpulos SA", hoy.plusDays(4), EstadoSolicitud.PENDIENTE);
        when(ordenCompraRepository.buscarPendientesParaPanel(TOPE)).thenReturn(new PageImpl<>(List.of(orden, sinIngresos), TOPE, 9));
        // Orden 7: ítem 70 de 4 presentaciones de 25 kg y ítem 71 de 2 presentaciones de 25 kg (6 pedidas en total)
        DetalleCompraEntity item70 = detalle(70L, orden, 4);
        DetalleCompraEntity item71 = detalle(71L, orden, 2);
        DetalleCompraEntity item80 = detalle(80L, sinIngresos, 3);
        when(detalleCompraRepository.buscarConPresentacionDeOrdenes(anyCollection())).thenReturn(List.of(item70, item71, item80));
        // 50 kg recibidos del ítem 70 (2 presentaciones) y 200 kg del ítem 71 (8 presentaciones, pero solo 2 pedidas)
        when(ingresoInsumoRepository.buscarRegistradosDeDetalles(anyCollection())).thenReturn(List.of(
                ingreso(item70, 30.0), ingreso(item70, 20.0), ingreso(item71, 200.0)));

        // === EJECUCION ===
        DashboardGerenteComprasResponseDTO resultado = dashboardServicio.obtenerDashboardGerenteCompras();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesPendientes().getTotal()).isEqualTo(9);
        assertThat(resultado.getOrdenesPendientes().getFilas()).hasSize(2);
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getId()).isEqualTo(7L);
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getProveedor()).isEqualTo("Malterías del Sur S.A.");
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getFechaEntregaEstimada()).isEqualTo(hoy.minusDays(3));
        // (2 + 2) recibidas de 6 pedidas = 66 %
        assertThat(resultado.getOrdenesPendientes().getFilas().get(0).getAvanceRecepcion()).isEqualTo(66);
        assertThat(resultado.getOrdenesPendientes().getFilas().get(1).getAvanceRecepcion()).isZero();
    }

    @Test
    @DisplayName("CP-DGC-04: las órdenes con entrega vencida se cuentan con la fecha de hoy")
    void obtener_debeContarLasOrdenesConEntregaVencida() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(ordenCompraRepository.contarPendientesConEntregaVencida(LocalDate.now())).thenReturn(3L);

        // === EJECUCION ===
        DashboardGerenteComprasResponseDTO resultado = dashboardServicio.obtenerDashboardGerenteCompras();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesConEntregaVencida()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-DGC-05: las órdenes finalizadas se piden desde el inicio del día de hace 30 días con el tope de 5 filas, e indican si el cierre fue manual (con motivo) o automático")
    void obtener_debeInformarLasOrdenesFinalizadas() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDateTime desde = LocalDate.now().minusDays(30).atStartOfDay();
        OrdenCompraEntity automatica = orden(5L, "Malterías del Sur S.A.", LocalDate.now(), EstadoSolicitud.FINALIZADA);
        automatica.setFechaFinalizacion(LocalDateTime.of(2027, 7, 1, 9, 0));
        OrdenCompraEntity manual = orden(4L, "Lúpulos SA", LocalDate.now(), EstadoSolicitud.FINALIZADA);
        manual.setFechaFinalizacion(LocalDateTime.of(2027, 6, 28, 15, 30));
        manual.setMotivoFinalizacion("El proveedor no entregará el resto");
        when(ordenCompraRepository.buscarFinalizadasDesdeParaPanel(eq(desde), eq(TOPE))).thenReturn(new PageImpl<>(List.of(automatica, manual), TOPE, 14));

        // === EJECUCION ===
        DashboardGerenteComprasResponseDTO resultado = dashboardServicio.obtenerDashboardGerenteCompras();

        // === ASSERTS ===
        assertThat(resultado.getOrdenesFinalizadas().getTotal()).isEqualTo(14);
        assertThat(resultado.getOrdenesFinalizadas().getFilas().get(0).getId()).isEqualTo(5L);
        assertThat(resultado.getOrdenesFinalizadas().getFilas().get(0).getFechaFinalizacion()).isEqualTo(LocalDateTime.of(2027, 7, 1, 9, 0));
        assertThat(resultado.getOrdenesFinalizadas().getFilas().get(0).isCierreManual()).isFalse();
        assertThat(resultado.getOrdenesFinalizadas().getFilas().get(1).isCierreManual()).isTrue();
        assertThat(resultado.getOrdenesFinalizadas().getFilas().get(1).getProveedor()).isEqualTo("Lúpulos SA");
    }

    // ==================== helpers ====================

    /** Deja los bloques vacíos; cada test pisa solo el bloque que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(planificacionProduccionRepository.buscarPendientesSinOrdenDeCompraParaPanel(any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(ordenCompraRepository.buscarPendientesParaPanel(any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(ordenCompraRepository.contarPendientesConEntregaVencida(any(LocalDate.class))).thenReturn(0L);
        lenient().when(ordenCompraRepository.buscarFinalizadasDesdeParaPanel(any(LocalDateTime.class), any(Pageable.class))).thenReturn(Page.empty(TOPE));
    }

    private static PlanificacionProduccionEntity planificacion(Long id, String receta, LocalDate inicio, PrioridadPlanificacion prioridad) {
        return PlanificacionProduccionEntity.builder().id(id).estado(EstadoSolicitud.PENDIENTE).fechaInicioEstimada(inicio).prioridad(prioridad)
                .versionReceta(VersionRecetaEntity.builder().nombre(receta).build()).build();
    }

    private static OrdenCompraEntity orden(Long id, String proveedor, LocalDate entrega, EstadoSolicitud estado) {
        return OrdenCompraEntity.builder().id(id).estado(estado).fechaEntregaEstimada(entrega)
                .versionProveedor(VersionProveedorEntity.builder().razonSocial(proveedor).build()).build();
    }

    /** Un ítem de {@code cantidad} presentaciones de 25 kg de una malta que se mide en kilogramos. */
    private static DetalleCompraEntity detalle(Long id, OrdenCompraEntity orden, int cantidad) {
        CatalogoProveedorEntity catalogo = CatalogoProveedorEntity.builder()
                .presentacionComercial(PresentacionComercialEntity.builder().cantidad(25.0).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build())
                .insumo(MaltaEntity.builder().nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build())
                .build();
        return DetalleCompraEntity.builder().id(id).cantidad(cantidad).ordenCompra(orden).catalogoProveedor(catalogo).build();
    }

    private static IngresoInsumoEntity ingreso(DetalleCompraEntity detalle, double cantidadRecibida) {
        return IngresoInsumoEntity.builder().detalleCompra(detalle).cantidadRecibida(cantidadRecibida).build();
    }
}
