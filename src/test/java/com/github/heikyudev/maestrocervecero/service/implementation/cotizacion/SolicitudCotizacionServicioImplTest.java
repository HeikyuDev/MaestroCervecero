package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.EnvioInvitacionPreparado;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoOrdenCompraPropuesta;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ItemOrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.PlanCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IConfiguracionComprasRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IInvitacionCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IOrdenCompraPropuestaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.ISolicitudCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPrearmadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudCotizacionServicioImplTest {

    @Mock private ISolicitudCotizacionRepository solicitudRepository;
    @Mock private IInvitacionCotizacionRepository invitacionRepository;
    @Mock private IOrdenCompraPropuestaRepository ordenPropuestaRepository;
    @Mock private IConfiguracionComprasRepository configuracionComprasRepository;
    @Mock private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock private IMaceradorRepository maceradorRepository;
    @Mock private com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository loteRepository;
    @Mock private ILoteInsumoRepository loteInsumoRepository;
    @Mock private IDetalleCompraRepository detalleCompraRepository;
    @Mock private IIngresoInsumoRepository ingresoInsumoRepository;
    @Mock private IVersionProveedorRepository versionProveedorRepository;
    @Mock private IOrdenCompraRepository ordenCompraRepository;
    @Mock private IEscaladoInsumoServicio escaladoInsumoServicio;
    @Mock private IOrdenCompraServicio ordenCompraServicio;
    @Mock private NotificadorCotizaciones notificador;

    @InjectMocks
    private SolicitudCotizacionServicioImpl servicio;

    private MaltaEntity malta;
    private LupuloEntity lupulo;
    private LevaduraEntity levadura;
    private VersionRecetaEntity receta;
    private PlanificacionProduccionEntity planificacion;
    private PresentacionComercialEntity bolsa25Kg;

    @BeforeEach
    void preparar() {
        malta = MaltaEntity.builder().id(1L).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
        lupulo = LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).build();
        levadura = LevaduraEntity.builder().id(3L).nombre("US-05").unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).build();
        receta = VersionRecetaEntity.builder().id(70L).nombre("Pale Ale")
                .detallesMalta(new ArrayList<>(List.of(DetalleMaltaEntity.builder().id(1L).malta(malta).cantidad(5.0).build())))
                .detallesLupulo(new ArrayList<>(List.of(DetalleLupuloEntity.builder().id(2L).lupulo(lupulo).cantidad(30.0).build())))
                .detallesLevadura(new ArrayList<>(List.of(DetalleLevaduraEntity.builder().id(3L).levadura(levadura).cantidad(11.5).build())))
                .build();
        planificacion = PlanificacionProduccionEntity.builder().id(7L).estado(EstadoSolicitud.PENDIENTE).cantidadAProducir(1000.0)
                .versionReceta(receta).build();
        bolsa25Kg = PresentacionComercialEntity.builder().id(30L).nombre("Bolsa").cantidad(25.0).unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
        lenient().when(configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)).thenReturn(Optional.of(
                ConfiguracionComprasEntity.builder().id(1L).plazoRespuestaHoras(48).cantidadPlanes(3)
                        .criterioPlanCompra(CriterioPlanCompra.MENOR_PRECIO).pesoPrecio(50.0).build()));
        lenient().when(solicitudRepository.save(any(SolicitudCotizacionEntity.class))).thenAnswer(inv -> {
            SolicitudCotizacionEntity s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(90L);
            }
            return s;
        });
    }

    // ==================== Datos de prueba ====================

    /** Planificación pendiente sin solicitud, maceradores activos de 60, 72 y 81 % y la receta escalada con las cantidades indicadas. */
    private void conPlanificacionEscalada(double kgMalta, double gLupulo, double gLevadura) {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));
        when(solicitudRepository.existsByPlanificacionProduccion_IdAndEstadoNot(7L, EstadoSolicitudCotizacion.CANCELADA)).thenReturn(false);
        lenient().when(maceradorRepository.buscarActivos()).thenReturn(List.of(macerador(60.0), macerador(72.0), macerador(81.0)));
        lenient().when(escaladoInsumoServicio.calcularRequerimientosMalta(eq(receta), eq(1000.0), any(MaceradorEntity.class)))
                .thenReturn(List.of(new RequerimientoInsumo(malta, null, kgMalta)));
        lenient().when(escaladoInsumoServicio.calcularRequerimientosLupulo(receta, 1000.0)).thenReturn(List.of(new RequerimientoInsumo(lupulo, null, gLupulo)));
        lenient().when(escaladoInsumoServicio.calcularRequerimientosLevadura(receta, 1000.0)).thenReturn(List.of(new RequerimientoInsumo(levadura, null, gLevadura)));
    }

    private static MaceradorEntity macerador(Double eficiencia) {
        return MaceradorEntity.builder().id(Math.round(eficiencia == null ? 0 : eficiencia)).eficienciaMaceracion(eficiencia).estado(Estado.ACTIVO).build();
    }

    private ProveedorEntity proveedor(long id, String razonSocial, Estado estado, InsumoEntity... insumos) {
        ProveedorEntity proveedor = ProveedorEntity.builder().id(id).estado(estado).build();
        VersionProveedorEntity version = VersionProveedorEntity.builder().id(id * 10).razonSocial(razonSocial).nombreComercial(razonSocial)
                .cuit("30" + String.format("%09d", id)).email("ventas" + id + "@proveedor.com").esUltimaVersion(true).proveedor(proveedor).build();
        long catalogo = id * 100;
        for (InsumoEntity insumo : insumos) {
            version.getCatalogoProveedor().add(CatalogoProveedorEntity.builder().id(catalogo++).version(version).insumo(insumo).presentacionComercial(bolsa25Kg).build());
        }
        proveedor.getVersiones().add(version);
        return proveedor;
    }

    private void conStock(Object[]... filas) {
        when(loteInsumoRepository.sumarStockPorInsumo(anyCollection(), any())).thenReturn(List.of(filas));
    }

    private SolicitudCotizacionEntity solicitudAbierta(LocalDateTime fechaLimite) {
        SolicitudCotizacionEntity solicitud = SolicitudCotizacionEntity.builder().id(90L).planificacionProduccion(planificacion)
                .estado(EstadoSolicitudCotizacion.ABIERTA).fechaLimiteRespuesta(fechaLimite).build();
        solicitud.getLineas().add(LineaSolicitudCotizacionEntity.builder().id(501L).solicitud(solicitud).insumo(malta)
                .cantidadRequerida(100.0).cantidadDisponible(0.0).cantidadPendienteRecepcion(0.0).cantidadFaltante(100.0).build());
        return solicitud;
    }

    private InvitacionCotizacionEntity invitar(SolicitudCotizacionEntity solicitud, ProveedorEntity proveedor, LocalDateTime respuesta) {
        InvitacionCotizacionEntity invitacion = InvitacionCotizacionEntity.builder().id(proveedor.getId() + 600).solicitud(solicitud)
                .proveedor(proveedor).tokenHash("h" + proveedor.getId()).fechaExpiracion(solicitud.getFechaLimiteRespuesta()).fechaRespuesta(respuesta)
                .estadoEnvio(EstadoEnvioInvitacion.ENVIADA).intentosEnvio(1).build();
        solicitud.getInvitaciones().add(invitacion);
        return invitacion;
    }

    private static void ofertar(InvitacionCotizacionEntity invitacion, long id, LineaSolicitudCotizacionEntity linea, String precio, int unidades) {
        CatalogoProveedorEntity catalogo = invitacion.getProveedor().getVersiones().get(0).getCatalogoProveedor().get(0);
        invitacion.getOfertas().add(OfertaCotizacionEntity.builder().id(id).invitacion(invitacion).lineaSolicitud(linea)
                .catalogoProveedor(catalogo).precioUnitario(new BigDecimal(precio)).cantidadDisponible(unidades).build());
    }

    /** Solicitud con planes generados: el plan 1 y el plan 2, cada uno con una orden propuesta pendiente. */
    private SolicitudCotizacionEntity solicitudConPlanes(ProveedorEntity proveedor1, ProveedorEntity proveedor2) {
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusHours(1));
        solicitud.setEstado(EstadoSolicitudCotizacion.PLANES_GENERADOS);
        LineaSolicitudCotizacionEntity linea = solicitud.getLineas().get(0);
        solicitud.getPlanes().add(plan(solicitud, 1, 801L, proveedor1, linea));
        solicitud.getPlanes().add(plan(solicitud, 2, 802L, proveedor2, linea));
        return solicitud;
    }

    private static PlanCompraEntity plan(SolicitudCotizacionEntity solicitud, int numero, long idOrden, ProveedorEntity proveedor, LineaSolicitudCotizacionEntity linea) {
        PlanCompraEntity plan = PlanCompraEntity.builder().id(700L + numero).solicitud(solicitud).numero(numero).estado(EstadoPlanCompra.PROPUESTO).build();
        OrdenCompraPropuestaEntity orden = OrdenCompraPropuestaEntity.builder().id(idOrden).plan(plan).proveedor(proveedor)
                .estado(EstadoOrdenCompraPropuesta.PROPUESTA).build();
        orden.getItems().add(ItemOrdenCompraPropuestaEntity.builder().id(idOrden * 10).ordenPropuesta(orden).lineaSolicitud(linea)
                .catalogoProveedor(proveedor.getVersiones().get(0).getCatalogoProveedor().get(0)).cantidad(4).precioUnitario(new BigDecimal("30000")).build());
        plan.getOrdenes().add(orden);
        return plan;
    }

    private void conOrdenPropuesta(OrdenCompraPropuestaEntity orden) {
        lenient().when(ordenPropuestaRepository.buscarIdPlanificacion(orden.getId())).thenReturn(Optional.of(7L));
        lenient().when(ordenPropuestaRepository.buscarIdSolicitud(orden.getId())).thenReturn(Optional.of(90L));
        lenient().when(ordenPropuestaRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        lenient().when(ordenPropuestaRepository.save(any(OrdenCompraPropuestaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static OrdenCompraFormDTO formOrden(Long idPlanificacion, Long idProveedor) {
        return OrdenCompraFormDTO.builder().idPlanificacionProduccion(idPlanificacion).idProveedor(idProveedor).build();
    }

    // ==================== generarSolicitud ====================

    @Test
    @DisplayName("CP-SCO-01: generarSolicitud rechaza una planificación inexistente")
    void generar_planificacionInexistente() {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.generarSolicitud(7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 7");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SCO-02: generarSolicitud no genera nada si la planificación ya no está Pendiente o ya tiene una solicitud no cancelada")
    void generar_planificacionNoPendienteOConSolicitud() {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));
        when(solicitudRepository.existsByPlanificacionProduccion_IdAndEstadoNot(7L, EstadoSolicitudCotizacion.CANCELADA)).thenReturn(true);

        assertThat(servicio.generarSolicitud(7L)).isEmpty();
        planificacion.setEstado(EstadoSolicitud.ANULADA);
        assertThat(servicio.generarSolicitud(7L)).isEmpty();

        verifyNoInteractions(escaladoInsumoServicio, notificador);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SCO-39: generarSolicitud bloquea la planificación antes de crear y no crea nada si una anulación simultánea ya la dejó Anulada")
    void generar_bloqueaLaPlanificacion() {
        // === PREPARACION DE DATOS ===
        planificacion.setEstado(EstadoSolicitud.ANULADA);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));

        // === EJECUCION ===
        assertThat(servicio.generarSolicitud(7L)).isEmpty();

        // === ASSERTS ===
        verify(planificacionProduccionRepository).buscarPorIdParaActualizar(7L);
        verify(planificacionProduccionRepository, never()).findById(any());
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(escaladoInsumoServicio, notificador);
    }

    @Test
    @DisplayName("CP-SCO-03: generarSolicitud no genera nada si el stock disponible y lo pendiente de recibir cubren lo requerido")
    void generar_sinFaltante() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        conStock(new Object[]{1L, 60.0, 5.0, 0.0, 65.0}, new Object[]{2L, 10.0, 0.0, 0.0, 10.0}, new Object[]{3L, 10.0, 0.0, 0.0, 10.0});
        // Orden pendiente: 2 bolsas de 25 kg pedidas, 10 kg ya recibidos -> 40 kg pendientes
        DetalleCompraEntity detalle = DetalleCompraEntity.builder().id(11L).cantidad(2).costoUnitario(BigDecimal.TEN)
                .catalogoProveedor(CatalogoProveedorEntity.builder().id(5L).insumo(malta).presentacionComercial(bolsa25Kg).build()).build();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of(detalle));
        when(ingresoInsumoRepository.buscarRegistradosDeDetalles(anyList()))
                .thenReturn(List.of(IngresoInsumoEntity.builder().id(1L).detalleCompra(detalle).cantidadRecibida(10.0).build()));

        // === EJECUCION Y VERIFICACION ===
        assertThat(servicio.generarSolicitud(7L)).isEmpty();
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("CP-SCO-04: generarSolicitud escala la cantidad a producir, calcula el faltante, registra los proveedores candidatos sin enlaces y pide confirmar el envío")
    void generar_conFaltante_invita() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 500.0, 0.0);
        // Malta: 20 kg disponibles; el negativo del lúpulo cuenta como 0
        conStock(new Object[]{1L, 20.0, 0.0, 0.0, 20.0}, new Object[]{2L, -3.0, 3.0, 0.0, 0.0});
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        ProveedorEntity malteria = proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta);
        ProveedorEntity lupulera = proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta, lupulo);
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(malteria.getVersiones().get(0), lupulera.getVersiones().get(0)));
        ArgumentCaptor<SolicitudCotizacionEntity> guardada = ArgumentCaptor.forClass(SolicitudCotizacionEntity.class);

        // === EJECUCION ===
        Optional<SolicitudCotizacionResponseDTO> resultado = servicio.generarSolicitud(7L);

        // === ASSERTS ===
        assertThat(resultado).isPresent();
        verify(solicitudRepository).save(guardada.capture());
        SolicitudCotizacionEntity solicitud = guardada.getValue();
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION);
        assertThat(solicitud.getPlanificacionProduccion()).isSameAs(planificacion);
        assertThat(solicitud.getFechaLimiteRespuesta()).isNull();
        // La levadura requerida es 0: no falta nada
        assertThat(solicitud.getLineas()).extracting(l -> l.getInsumo().getId(), LineaSolicitudCotizacionEntity::getCantidadRequerida,
                        LineaSolicitudCotizacionEntity::getCantidadDisponible, LineaSolicitudCotizacionEntity::getCantidadFaltante,
                        LineaSolicitudCotizacionEntity::isSinProveedor)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 100.0, 20.0, 80.0, false),
                        org.assertj.core.groups.Tuple.tuple(2L, 500.0, 0.0, 500.0, false));
        assertThat(solicitud.getInvitaciones()).hasSize(2).allSatisfy(invitacion -> {
            assertThat(invitacion.getTokenHash()).isNull();
            assertThat(invitacion.getFechaExpiracion()).isNull();
            assertThat(invitacion.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.PENDIENTE);
            assertThat(invitacion.getIntentosEnvio()).isZero();
        });
        // Ningún correo a proveedores hasta que el gerente confirme el envío
        verify(notificador, never()).invitarProveedor(any(), any(), any(), any(), any());
        verify(notificador).avisarGerentesDeCompras(90L, SolicitudCotizacionServicioImpl.ASUNTO_PENDIENTE_CONFIRMACION,
                "Se generó la solicitud de cotización N° 90 para la planificación N° 7: el stock actual no cubre Malta Malta Pilsen (faltan 80 kg) "
                        + "ni Lúpulo Cascade (faltan 500 g). Confirme el envío a los proveedores o cancele la solicitud", List.of());
    }

    @Test
    @DisplayName("CP-SCO-05: generarSolicitud resta lo pendiente de recibir de las órdenes pendientes (lo recibido de más no suma)")
    void generar_restaPendienteDeOrdenes() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        conStock();
        CatalogoProveedorEntity item = CatalogoProveedorEntity.builder().id(5L).insumo(malta).presentacionComercial(bolsa25Kg).build();
        DetalleCompraEntity pendiente = DetalleCompraEntity.builder().id(11L).cantidad(2).catalogoProveedor(item).build();
        DetalleCompraEntity recibidoDeMas = DetalleCompraEntity.builder().id(12L).cantidad(1).catalogoProveedor(item).build();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of(pendiente, recibidoDeMas));
        when(ingresoInsumoRepository.buscarRegistradosDeDetalles(anyList())).thenReturn(List.of(
                IngresoInsumoEntity.builder().id(1L).detalleCompra(pendiente).cantidadRecibida(5.0).build(),
                IngresoInsumoEntity.builder().id(2L).detalleCompra(recibidoDeMas).cantidadRecibida(30.0).build()));
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta).getVersiones().get(0)));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        // Pendiente: 50 - 5 = 45 kg del primer ítem; el segundo recibió de más y no resta nada
        assertThat(resultado.getLineas()).singleElement().satisfies(linea -> {
            assertThat(linea.getCantidadPendienteRecepcion()).isEqualTo(45.0);
            assertThat(linea.getCantidadFaltante()).isEqualTo(55.0);
            assertThat(linea.getUnidad()).isEqualTo("kg");
        });
    }

    @Test
    @DisplayName("CP-SCO-06: generarSolicitud marca los insumos que ningún proveedor ofrece y lo incluye en el pedido de confirmación a los gerentes de compras")
    void generar_insumoSinProveedor_avisa() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 500.0, 0.0);
        conStock();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta).getVersiones().get(0)));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION);
        assertThat(resultado.getLineas()).extracting(l -> l.getNombreInsumo() + ":" + l.isSinProveedor())
                .containsExactly("Malta Pilsen:false", "Cascade:true");
        assertThat(resultado.getAvisos()).contains("Ningún proveedor activo ofrece Cascade");
        assertThat(resultado.getExplicacion()).startsWith("Se generó la solicitud de cotización N° 90 para la planificación N° 7");
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_PENDIENTE_CONFIRMACION), anyString(),
                eq(List.of("Ningún proveedor activo ofrece Cascade")));
    }

    @Test
    @DisplayName("CP-SCO-07: generarSolicitud cierra de inmediato la solicitud si ningún proveedor ofrece ningún insumo faltante y avisa a los gerentes")
    void generar_sinProveedores_quedaCerrada() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        conStock();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection())).thenReturn(List.of());

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CERRADA);
        assertThat(resultado.getFechaCierre()).isNotNull();
        assertThat(resultado.getCantidadInvitaciones()).isZero();
        assertThat(resultado.getAvisos()).contains("Ningún proveedor activo ofrece Malta Pilsen", "Ningún proveedor respondió antes del cierre");
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_PROVEEDOR), anyString(), eq(List.of("Malta Pilsen")));
    }

    @Test
    @DisplayName("CP-SCO-08: generarSolicitud escala la planificación como un único lote virtual con la eficiencia promedio de los maceradores activos, aunque el volumen supere cualquier equipo")
    void generar_loteVirtual_promedioDeMaceradores() {
        // === PREPARACION DE DATOS ===
        // 1.000.000 L: muy por encima de la capacidad de cualquier equipo; no se valida ninguna capacidad
        planificacion.setCantidadAProducir(1_000_000.0);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));
        // Uno sin eficiencia (dato incompleto) no cuenta para el promedio
        when(maceradorRepository.buscarActivos()).thenReturn(List.of(macerador(60.0), macerador(72.0), macerador(81.0), macerador(null)));
        ArgumentCaptor<MaceradorEntity> promedio = ArgumentCaptor.forClass(MaceradorEntity.class);
        when(escaladoInsumoServicio.calcularRequerimientosMalta(eq(receta), eq(1_000_000.0), promedio.capture()))
                .thenReturn(List.of(new RequerimientoInsumo(malta, null, 230_000.0)));
        when(escaladoInsumoServicio.calcularRequerimientosLupulo(receta, 1_000_000.0)).thenReturn(List.of(new RequerimientoInsumo(lupulo, null, 2_500_000.0)));
        when(escaladoInsumoServicio.calcularRequerimientosLevadura(receta, 1_000_000.0)).thenReturn(List.of(new RequerimientoInsumo(levadura, null, 600_000.0)));
        conStock();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection())).thenReturn(List.of());

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        assertThat(promedio.getValue().getEficienciaMaceracion()).isEqualTo(71.0);
        assertThat(resultado.getLineas()).extracting(l -> l.getNombreInsumo() + ":" + l.getCantidadFaltante())
                .containsExactly("Malta Pilsen:230000.0", "Cascade:2500000.0", "US-05:600000.0");
        verify(escaladoInsumoServicio, never()).calcularRequerimientosTotales(any());
    }

    @Test
    @DisplayName("CP-SCO-37: generarSolicitud descuenta del stock libre lo que consumirán los lotes registrados sin iniciar de otra planificación, sin descontar los que ya reservaron")
    void generar_descuentaLotesSinIniciar() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        // 70 kg de malta libres (stock menos reservas activas, que ya incluyen al lote iniciado)
        conStock(new Object[]{1L, 70.0, 30.0, 0.0, 100.0});
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        PlanificacionProduccionEntity otra = PlanificacionProduccionEntity.builder().id(8L).versionReceta(receta).build();
        com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity sinIniciar = com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity.builder()
                .id(31L).identificadorInterno("LOT-R1-01").volumenObjetivo(300.0).planificacionProduccion(otra)
                .escaladoLote(com.github.heikyudev.maestrocervecero.persistence.entity.lote.EscaladoLoteEntity.builder().id(31L).build()).build();
        com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity conReservas = com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity.builder()
                .id(32L).identificadorInterno("LOT-R1-02").build();
        // El repositorio solo devuelve los pendientes sin reservas activas: el lote 32 ya reservó y está en las reservas activas
        when(loteRepository.buscarPendientesSinReservasActivas()).thenReturn(List.of(sinIniciar));
        when(escaladoInsumoServicio.calcularRequerimientosTotales(sinIniciar))
                .thenReturn(List.of(new RequerimientoInsumo(malta, null, 30.0), new RequerimientoInsumo(malta, null, 15.0), new RequerimientoInsumo(lupulo, null, 200.0)));
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta).getVersiones().get(0)));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        // Libre = 70 - 45 = 25 kg; faltante = 100 - 25 = 75 kg. El lúpulo del lote no cuenta: esta planificación no lo requiere
        assertThat(resultado.getLineas()).singleElement().satisfies(linea -> {
            assertThat(linea.getCantidadDisponible()).isEqualTo(70.0);
            assertThat(linea.getCantidadComprometidaLotes()).isEqualTo(45.0);
            assertThat(linea.getCantidadFaltante()).isEqualTo(75.0);
        });
        verify(escaladoInsumoServicio, never()).calcularRequerimientosTotales(conReservas);
    }

    @Test
    @DisplayName("CP-SCO-38: lo libre nunca es negativo y un lote sin iniciar cuyo consumo no se puede calcular se avisa sin descontarse")
    void generar_libreNoNegativoYLoteIncalculable() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        conStock(new Object[]{1L, 20.0, 0.0, 0.0, 20.0});
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity grande = com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity.builder()
                .id(31L).identificadorInterno("LOT-R1-01").build();
        com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity incompleto = com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity.builder()
                .id(33L).identificadorInterno("LOT-R2-01").build();
        when(loteRepository.buscarPendientesSinReservasActivas()).thenReturn(List.of(grande, incompleto));
        when(escaladoInsumoServicio.calcularRequerimientosTotales(grande)).thenReturn(List.of(new RequerimientoInsumo(malta, null, 500.0)));
        when(escaladoInsumoServicio.calcularRequerimientosTotales(incompleto)).thenThrow(new ReglaNegocioException("La etapa de Maceración del lote 33 no tiene un macerador asociado como equipamiento"));
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta).getVersiones().get(0)));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        // Libre = max(0, 20 - 500) = 0: el faltante de esta planificación es todo su requerido (el déficit del otro lote no se le suma)
        assertThat(resultado.getLineas()).singleElement().satisfies(linea -> {
            assertThat(linea.getCantidadComprometidaLotes()).isEqualTo(500.0);
            assertThat(linea.getCantidadFaltante()).isEqualTo(100.0);
        });
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_DATOS_INCOMPLETOS), anyString(),
                eq(List.of("No se pudo calcular lo que consumirá el lote LOT-R2-01 sin iniciar: no se descontó del stock disponible")));
    }

    @Test
    @DisplayName("CP-SCO-26: sin maceradores activos una receta con malta no genera solicitud y se avisa a los gerentes; una receta sin malta no los necesita")
    void generar_sinMaceradores() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));
        when(maceradorRepository.buscarActivos()).thenReturn(List.of(macerador(null)));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        // === EJECUCION ===
        assertThat(servicio.generarSolicitud(7L)).isEmpty();

        // === ASSERTS ===
        verify(notificador).avisarGerentesDeCompras(isNull(), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_SOLICITUD), mensaje.capture(), eq(List.of()));
        assertThat(mensaje.getValue()).isEqualTo("No se generó la solicitud de cotización de la planificación Plan #7 - Pale Ale porque no hay maceradores activos para estimar la malta de la receta");
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(escaladoInsumoServicio);

        // Receta sin malta: se estima sin consultar maceradores
        receta.getDetallesMalta().clear();
        when(escaladoInsumoServicio.calcularRequerimientosLupulo(receta, 1000.0)).thenReturn(List.of());
        when(escaladoInsumoServicio.calcularRequerimientosLevadura(receta, 1000.0)).thenReturn(List.of());
        assertThat(servicio.generarSolicitud(7L)).isEmpty();
        verify(maceradorRepository, org.mockito.Mockito.times(1)).buscarActivos();
    }

    @Test
    @DisplayName("CP-SCO-27: si los datos de un grupo de insumos están incompletos, se sigue con los demás y se avisa; si no se puede estimar ninguno, no se genera solicitud")
    void generar_datosIncompletos() {
        // === PREPARACION DE DATOS ===
        conPlanificacionEscalada(100.0, 0.0, 0.0);
        when(escaladoInsumoServicio.calcularRequerimientosLupulo(receta, 1000.0)).thenThrow(new NullPointerException("aa"));
        when(escaladoInsumoServicio.calcularRequerimientosLevadura(receta, 1000.0)).thenReturn(List.of(new RequerimientoInsumo(levadura, null, Double.NaN)));
        conStock();
        when(detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(anyCollection())).thenReturn(List.of());
        when(versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(anyCollection()))
                .thenReturn(List.of(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta).getVersiones().get(0)));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.generarSolicitud(7L).orElseThrow();

        // === ASSERTS ===
        assertThat(resultado.getLineas()).extracting(l -> l.getNombreInsumo()).containsExactly("Malta Pilsen");
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_DATOS_INCOMPLETOS),
                eq("La solicitud de cotización #90 de la planificación Plan #7 - Pale Ale no incluye los insumos cuyos datos están incompletos"),
                eq(List.of("No se pudo calcular la cantidad requerida de el lúpulo: revise los datos de la receta y de sus insumos",
                        "No se pudo calcular la cantidad requerida de la levadura: revise los datos de la receta y de sus insumos")));

        // Ningún grupo se puede estimar: no se genera solicitud
        when(escaladoInsumoServicio.calcularRequerimientosMalta(eq(receta), eq(1000.0), any(MaceradorEntity.class))).thenThrow(new ArithmeticException("división por cero"));
        assertThat(servicio.generarSolicitud(7L)).isEmpty();
        verify(notificador).avisarGerentesDeCompras(isNull(), eq(SolicitudCotizacionServicioImpl.ASUNTO_DATOS_INCOMPLETOS),
                eq("No se generó la solicitud de cotización de la planificación Plan #7 - Pale Ale porque faltan datos para estimar sus insumos"), anyList());
    }

    // ==================== confirmarEnvio ====================

    private SolicitudCotizacionEntity solicitudPendiente() {
        SolicitudCotizacionEntity solicitud = solicitudAbierta(null);
        solicitud.setEstado(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION);
        when(solicitudRepository.buscarIdPlanificacion(90L)).thenReturn(Optional.of(7L));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.of(planificacion));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private InvitacionCotizacionEntity candidato(SolicitudCotizacionEntity solicitud, ProveedorEntity proveedor) {
        InvitacionCotizacionEntity invitacion = InvitacionCotizacionEntity.builder().id(proveedor.getId() + 600).solicitud(solicitud)
                .proveedor(proveedor).estadoEnvio(EstadoEnvioInvitacion.PENDIENTE).intentosEnvio(0).build();
        solicitud.getInvitaciones().add(invitacion);
        return invitacion;
    }

    @Test
    @DisplayName("CP-SCO-34: confirmarEnvio abre la solicitud con el plazo desde la confirmación, quita los candidatos que ya no sirven y deja los envíos pendientes, con la planificación y la solicitud bloqueadas")
    void confirmarEnvio_abre() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudPendiente();
        InvitacionCotizacionEntity activo = candidato(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta));
        candidato(solicitud, proveedor(2L, "DE BAJA", Estado.BAJA, malta));
        candidato(solicitud, proveedor(3L, "SIN MALTA", Estado.ACTIVO, lupulo));
        LocalDateTime antes = LocalDateTime.now();

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.confirmarEnvio(90L);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitudCotizacion.ABIERTA);
        assertThat(solicitud.getFechaConfirmacionEnvio()).isNotNull();
        assertThat(solicitud.getFechaLimiteRespuesta()).isCloseTo(antes.plusHours(48), within(5, ChronoUnit.SECONDS));
        assertThat(solicitud.getInvitaciones()).containsExactly(activo);
        assertThat(activo.getFechaExpiracion()).isEqualTo(solicitud.getFechaLimiteRespuesta());
        assertThat(activo.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.PENDIENTE);
        assertThat(activo.getTokenHash()).isNull();
        assertThat(solicitud.getLineas().get(0).isSinProveedor()).isFalse();
        verify(notificador, never()).invitarProveedor(any(), any(), any(), any(), any());
        InOrder orden = inOrder(planificacionProduccionRepository, solicitudRepository);
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(7L);
        orden.verify(solicitudRepository).buscarPorIdParaActualizar(90L);
        orden.verify(solicitudRepository).save(solicitud);
    }

    @Test
    @DisplayName("CP-SCO-35: confirmarEnvio rechaza una solicitud que no está pendiente, una planificación que ya no está Pendiente y la falta de candidatos activos")
    void confirmarEnvio_reglas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudPendiente();
        candidato(solicitud, proveedor(2L, "DE BAJA", Estado.BAJA, malta));
        when(solicitudRepository.buscarIdPlanificacion(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.confirmarEnvio(99L)).isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la solicitud de cotización con ID: 99");
        assertThatThrownBy(() -> servicio.confirmarEnvio(90L)).isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede confirmar el envío porque ningún proveedor candidato sigue activo y ofrece los insumos faltantes: cancele la solicitud");
        planificacion.setEstado(EstadoSolicitud.ANULADA);
        assertThatThrownBy(() -> servicio.confirmarEnvio(90L))
                .hasMessage("No se puede confirmar el envío porque la planificación de producción ya no está Pendiente");
        solicitud.setEstado(EstadoSolicitudCotizacion.ABIERTA);
        assertThatThrownBy(() -> servicio.confirmarEnvio(90L))
                .hasMessage("Solo se puede confirmar el envío de una solicitud de cotización pendiente de confirmación");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SCO-36: una solicitud pendiente de confirmación se cancela a mano y al anular la planificación; sus candidatos quedan sin enlace vigente")
    void pendienteDeConfirmacion_seCancela() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudPendiente();
        InvitacionCotizacionEntity invitacion = candidato(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta));

        // === EJECUCION ===
        servicio.cancelarSolicitud(90L, "Se consiguió el insumo");

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
        assertThat(invitacion.getFechaExpiracion()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());

        // Anulación de la planificación con una solicitud pendiente
        SolicitudCotizacionEntity otra = solicitudAbierta(null);
        otra.setId(91L);
        otra.setEstado(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION);
        when(solicitudRepository.buscarIdsPorPlanificacionYEstados(eq(7L), anyCollection())).thenReturn(List.of(91L));
        when(solicitudRepository.buscarPorIdParaActualizar(91L)).thenReturn(Optional.of(otra));
        servicio.cancelarPorAnulacionDePlanificacion(7L);
        assertThat(otra.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
    }

    // ==================== prepararEnvios / registrarResultadosEnvio ====================

    @Test
    @DisplayName("CP-SCO-28: prepararEnvios guarda un token nuevo por invitación pendiente sin enviar nada, y registrarResultadosEnvio guarda si el correo se entregó o falló")
    void enviar_preparaYRegistraElResultado() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().plusHours(10));
        InvitacionCotizacionEntity aEnviar = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        InvitacionCotizacionEntity aFallar = invitar(solicitud, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta), null);
        InvitacionCotizacionEntity yaEnviada = invitar(solicitud, proveedor(3L, "CEBADAS", Estado.ACTIVO, malta), null);
        InvitacionCotizacionEntity enEnvio = invitar(solicitud, proveedor(5L, "EN ENVIO", Estado.ACTIVO, malta), null);
        enEnvio.setEstadoEnvio(EstadoEnvioInvitacion.EN_ENVIO);
        for (InvitacionCotizacionEntity invitacion : List.of(aEnviar, aFallar)) {
            invitacion.setEstadoEnvio(EstadoEnvioInvitacion.PENDIENTE);
            invitacion.setIntentosEnvio(0);
        }
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        List<EnvioInvitacionPreparado> preparados = servicio.prepararEnvios(90L);

        // === ASSERTS: el token queda guardado antes de enviar y no se habla con el servidor de correo ===
        assertThat(preparados).extracting(EnvioInvitacionPreparado::idInvitacion).containsExactly(601L, 602L);
        EnvioInvitacionPreparado primero = preparados.get(0);
        assertThat(primero.correo()).isEqualTo("ventas1@proveedor.com");
        assertThat(primero.razonSocial()).isEqualTo("MALTERIA SUR");
        assertThat(primero.insumos()).containsExactly("Malta Pilsen");
        assertThat(primero.fechaLimite()).isEqualTo(solicitud.getFechaLimiteRespuesta());
        assertThat(primero.token()).hasSize(43);
        assertThat(aEnviar.getTokenHash()).isEqualTo(TokenCotizacion.calcularHash(primero.token()));
        assertThat(aEnviar.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.EN_ENVIO);
        assertThat(aEnviar.getIntentosEnvio()).isEqualTo(1);
        assertThat(aEnviar.getFechaUltimoIntentoEnvio()).isNotNull();
        assertThat(yaEnviada.getIntentosEnvio()).isEqualTo(1);
        assertThat(enEnvio.getIntentosEnvio()).isEqualTo(1);
        verifyNoInteractions(notificador);
        verify(solicitudRepository).save(solicitud);

        // === EJECUCION: resultado del envío ===
        servicio.registrarResultadosEnvio(90L, java.util.Map.of(601L, true, 602L, false, 603L, false));

        // === ASSERTS ===
        assertThat(aEnviar.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.ENVIADA);
        assertThat(aFallar.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.FALLIDA);
        assertThat(aFallar.getIntentosEnvio()).isEqualTo(1);
        // Una invitación que no estaba en envío no cambia aunque venga en los resultados
        assertThat(yaEnviada.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.ENVIADA);
        verify(notificador, never()).avisarGerentesDeCompras(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CP-SCO-29: al agotar los intentos de envío se avisa que el correo no llegó; agotada o con la solicitud vencida no se prepara")
    void enviar_agotaIntentos() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().plusHours(10));
        InvitacionCotizacionEntity ultimoIntento = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        ultimoIntento.setEstadoEnvio(EstadoEnvioInvitacion.FALLIDA);
        ultimoIntento.setIntentosEnvio(2);
        InvitacionCotizacionEntity agotada = invitar(solicitud, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta), null);
        agotada.setEstadoEnvio(EstadoEnvioInvitacion.FALLIDA);
        agotada.setIntentosEnvio(3);
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        // === EJECUCION ===
        List<EnvioInvitacionPreparado> preparados = servicio.prepararEnvios(90L);
        servicio.registrarResultadosEnvio(90L, java.util.Map.of(601L, false));

        // === ASSERTS ===
        assertThat(preparados).extracting(EnvioInvitacionPreparado::idInvitacion).containsExactly(601L);
        assertThat(ultimoIntento.getIntentosEnvio()).isEqualTo(3);
        assertThat(agotada.getIntentosEnvio()).isEqualTo(3);
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_CORREO_NO_ENTREGADO), mensaje.capture(), eq(List.of("MALTERIA SUR")));
        assertThat(mensaje.getValue()).isEqualTo("No se pudo entregar por correo la solicitud de cotización #90 a los siguientes proveedores después de 3 intentos: el correo no llegó, no es que no hayan respondido");
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos()).contains("No se pudo entregar por correo la solicitud a MALTERIA SUR, LUPULOS DEL VALLE");

        // Solicitud vencida: no se prepara nada
        solicitud.setFechaLimiteRespuesta(LocalDateTime.now().minusMinutes(1));
        ultimoIntento.setIntentosEnvio(0);
        assertThat(servicio.prepararEnvios(90L)).isEmpty();
        assertThat(ultimoIntento.getIntentosEnvio()).isZero();
    }

    @Test
    @DisplayName("CP-SCO-40: la invitación de un proveedor dado de baja o sin versión vigente queda omitida (estado final) y se muestra como Dado de baja")
    void enviar_omiteProveedoresQueNoAplican() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().plusHours(10));
        InvitacionCotizacionEntity deBaja = invitar(solicitud, proveedor(4L, "DE BAJA", Estado.BAJA, malta), null);
        ProveedorEntity sinVersion = proveedor(6L, "SIN VERSION", Estado.ACTIVO, malta);
        sinVersion.getVersiones().get(0).setEsUltimaVersion(false);
        InvitacionCotizacionEntity sinVigente = invitar(solicitud, sinVersion, null);
        for (InvitacionCotizacionEntity invitacion : List.of(deBaja, sinVigente)) {
            invitacion.setEstadoEnvio(EstadoEnvioInvitacion.PENDIENTE);
            invitacion.setIntentosEnvio(0);
        }
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        List<EnvioInvitacionPreparado> preparados = servicio.prepararEnvios(90L);

        // === ASSERTS ===
        assertThat(preparados).isEmpty();
        assertThat(deBaja.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.OMITIDA);
        assertThat(sinVigente.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.OMITIDA);
        assertThat(deBaja.getIntentosEnvio()).isZero();
        assertThat(sinVigente.getTokenHash()).isEqualTo("h6");
        // Una segunda pasada no las vuelve a tomar
        assertThat(servicio.prepararEnvios(90L)).isEmpty();
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getInvitaciones())
                .extracting(com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.InvitacionCotizacionResponseDTO::getEstadoParticipacion)
                .containsOnly("Dado de baja");
    }

    @Test
    @DisplayName("CP-SCO-30: registrarFallaCierre suma intentos con un motivo genérico (el detalle solo al log) y, al llegar al máximo, marca la falla, deja de reintentar y avisa con texto genérico")
    void registrarFallaCierre_marcaYAvisaAlMaximo() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusHours(1));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        servicio.registrarFallaCierre(90L, "Bloqueo agotado");
        servicio.registrarFallaCierre(90L, null);
        assertThat(solicitud.isFallaCierre()).isFalse();
        servicio.registrarFallaCierre(90L, "org.postgresql.util.PSQLException: " + "x".repeat(300));
        servicio.registrarFallaCierre(90L, "otra");

        // === ASSERTS ===
        assertThat(solicitud.getIntentosCierre()).isEqualTo(3);
        assertThat(solicitud.isFallaCierre()).isTrue();
        // El detalle técnico no se guarda: se muestra un motivo genérico
        assertThat(solicitud.getMotivoFallaCierre()).isEqualTo(SolicitudCotizacionServicioImpl.MOTIVO_FALLA_CIERRE);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.ABIERTA);
        verify(notificador, org.mockito.Mockito.times(1)).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_FALLA_CIERRE),
                eq("No se pudo cerrar automáticamente la solicitud de cotización #90 después de 3 intentos: el proceso programado dejó de reintentarlo"), anyList());
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos()).anyMatch(aviso -> aviso.startsWith("No se pudo cerrar la solicitud automáticamente: ")).noneMatch(aviso -> aviso.contains("PSQLException"));
    }

    @Test
    @DisplayName("CP-SCO-31: el cierre sin ofertas distingue los correos que no se entregaron de los proveedores que no respondieron")
    void cerrar_distingueCorreosNoEntregados() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusMinutes(1));
        InvitacionCotizacionEntity noEntregada = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        noEntregada.setEstadoEnvio(EstadoEnvioInvitacion.FALLIDA);
        InvitacionCotizacionEntity sinRespuesta = invitar(solicitud, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta), null);
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        // === EJECUCION ===
        servicio.cerrarSolicitud(90L);

        // === ASSERTS ===
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_RESPUESTAS), mensaje.capture(), any());
        assertThat(mensaje.getValue()).isEqualTo("La solicitud de cotización #90 cerró sin que ningún proveedor respondiera; además, no se pudo entregar el correo a 1 proveedor: no se generaron planes");
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos()).contains("Ningún proveedor respondió antes del cierre y no se pudo entregar el correo a 1 proveedor");
        sinRespuesta.setEstadoEnvio(EstadoEnvioInvitacion.FALLIDA);
        assertThat(SolicitudCotizacionServicioImpl.mensajeCierreSinOfertas(solicitud))
                .isEqualTo("La solicitud de cotización #90 cerró sin ofertas porque no se pudo entregar el correo a ningún proveedor invitado: no se generaron planes");
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos()).contains("No se pudo entregar el correo a ningún proveedor invitado");
    }

    @Test
    @DisplayName("CP-SCO-41: el cierre sin ofertas distingue ningún proveedor respondió, respondieron sin ofertas y sin ofertas de proveedores activos")
    void cerrar_distingueRespuestasSinOfertas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusMinutes(1));
        InvitacionCotizacionEntity sinRespuesta = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        String prefijo = "La solicitud de cotización #90 cerró ";

        // === EJECUCION Y VERIFICACION ===
        // Nadie respondió
        assertThat(SolicitudCotizacionServicioImpl.mensajeCierreSinOfertas(solicitud)).isEqualTo(prefijo + "sin que ningún proveedor respondiera: no se generaron planes");

        // Respondió con todo en blanco: cuenta como respuesta sin ofertas
        InvitacionCotizacionEntity enBlanco = invitar(solicitud, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta), LocalDateTime.now().minusHours(2));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        servicio.cerrarSolicitud(90L);
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_RESPUESTAS), mensaje.capture(), any());
        assertThat(mensaje.getValue()).isEqualTo(prefijo + "porque los proveedores que respondieron no ofrecieron ninguno de los insumos solicitados: no se generaron planes");
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos())
                .contains("Los proveedores que respondieron no ofrecieron ninguno de los insumos solicitados");

        // Solo ofertó un proveedor que después se dio de baja
        InvitacionCotizacionEntity deBaja = invitar(solicitud, proveedor(3L, "CEBADAS", Estado.BAJA, malta), LocalDateTime.now().minusHours(1));
        ofertar(deBaja, 900L, solicitud.getLineas().get(0), "25000", 4);
        assertThat(SolicitudCotizacionServicioImpl.mensajeCierreSinOfertas(solicitud)).isEqualTo(prefijo + "sin ofertas de proveedores activos para los insumos solicitados: no se generaron planes");
        assertThat(com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion.toDTO(solicitud).getAvisos())
                .contains("Ninguna respuesta de un proveedor activo ofreció los insumos solicitados");
        assertThat(sinRespuesta.getFechaRespuesta()).isNull();
        assertThat(enBlanco.getOfertas()).isEmpty();
    }

    // ==================== cerrarSolicitud ====================

    @Test
    @DisplayName("CP-SCO-09: cerrarSolicitud no cambia nada si la solicitud no existe, no está abierta o todavía no venció")
    void cerrar_noCorresponde() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity noVencida = solicitudAbierta(LocalDateTime.now().plusHours(2));
        SolicitudCotizacionEntity cancelada = solicitudAbierta(LocalDateTime.now().minusHours(2));
        cancelada.setEstado(EstadoSolicitudCotizacion.CANCELADA);
        when(solicitudRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());
        when(solicitudRepository.buscarPorIdParaActualizar(2L)).thenReturn(Optional.of(noVencida));
        when(solicitudRepository.buscarPorIdParaActualizar(3L)).thenReturn(Optional.of(cancelada));

        // === EJECUCION ===
        servicio.cerrarSolicitud(1L);
        servicio.cerrarSolicitud(2L);
        servicio.cerrarSolicitud(3L);

        // === ASSERTS ===
        assertThat(noVencida.getEstado()).isEqualTo(EstadoSolicitudCotizacion.ABIERTA);
        assertThat(cancelada.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
        verify(solicitudRepository, never()).save(any());
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("CP-SCO-10: cerrarSolicitud sin respuestas deja la solicitud CERRADA sin planes y avisa que nadie respondió")
    void cerrar_sinRespuestas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusMinutes(1));
        invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        // === EJECUCION ===
        servicio.cerrarSolicitud(90L);

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CERRADA);
        assertThat(solicitud.getFechaCierre()).isNotNull();
        assertThat(solicitud.getPlanes()).isEmpty();
        verify(solicitudRepository).save(solicitud);
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_RESPUESTAS), mensaje.capture(), eq(List.of("Malta Pilsen")));
        assertThat(mensaje.getValue()).isEqualTo("La solicitud de cotización #90 cerró sin que ningún proveedor respondiera: no se generaron planes");
    }

    @Test
    @DisplayName("CP-SCO-11: cerrarSolicitud descarta las ofertas de proveedores dados de baja")
    void cerrar_descartaOfertasDeProveedoresInactivos() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusMinutes(1));
        InvitacionCotizacionEntity invitacion = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.BAJA, malta), LocalDateTime.now().minusHours(3));
        ofertar(invitacion, 1L, solicitud.getLineas().get(0), "25000", 10);
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        // === EJECUCION ===
        servicio.cerrarSolicitud(90L);

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CERRADA);
        assertThat(solicitud.getPlanes()).isEmpty();
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_SIN_RESPUESTAS), mensaje.capture(), any());
        assertThat(mensaje.getValue()).isEqualTo("La solicitud de cotización #90 cerró sin ofertas de proveedores activos para los insumos solicitados: no se generaron planes");
    }

    @Test
    @DisplayName("CP-SCO-12: cerrarSolicitud con ofertas genera planes propuestos, con una orden por proveedor, y avisa que están listos")
    void cerrar_conOfertas_generaPlanes() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().minusMinutes(1));
        LineaSolicitudCotizacionEntity linea = solicitud.getLineas().get(0);
        // Malteria: 2 bolsas a $25.000; Valle: 10 bolsas a $30.000. Faltan 100 kg = 4 bolsas
        ofertar(invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), LocalDateTime.now()), 1L, linea, "25000", 2);
        ofertar(invitar(solicitud, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta), LocalDateTime.now()), 2L, linea, "30000", 10);
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        servicio.cerrarSolicitud(90L);

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.PLANES_GENERADOS);
        assertThat(solicitud.getFechaCierre()).isNotNull();
        assertThat(solicitud.getFechaGeneracionPlanes()).isNotNull();
        // Plan 1: las dos bolsas baratas y dos más; plan 2: todo al segundo proveedor; plan 3: solo las baratas (no cubre)
        assertThat(solicitud.getPlanes()).extracting(PlanCompraEntity::getNumero).containsExactly(1, 2, 3);
        assertThat(solicitud.getPlanes()).allMatch(plan -> plan.getEstado() == EstadoPlanCompra.PROPUESTO);
        PlanCompraEntity mejor = solicitud.getPlanes().get(0);
        assertThat(mejor.getOrdenes()).extracting(orden -> orden.getProveedor().getId() + ":" + orden.getItems().get(0).getCantidad())
                .containsExactly("1:2", "2:2");
        assertThat(mejor.getOrdenes()).allMatch(orden -> orden.getEstado() == EstadoOrdenCompraPropuesta.PROPUESTA);
        assertThat(solicitud.getPlanes().get(1).getOrdenes()).extracting(orden -> orden.getProveedor().getId() + ":" + orden.getItems().get(0).getCantidad())
                .containsExactly("2:4");
        assertThat(solicitud.getPlanes().get(2).getOrdenes()).extracting(orden -> orden.getProveedor().getId() + ":" + orden.getItems().get(0).getCantidad())
                .containsExactly("1:2");
        verify(solicitudRepository).save(solicitud);
        verify(notificador).avisarGerentesDeCompras(eq(90L), eq(SolicitudCotizacionServicioImpl.ASUNTO_PLANES_LISTOS),
                eq("La solicitud de cotización #90 cerró y sus planes de órdenes de compra están listos para revisar"), eq(List.of("Planes generados: 3")));
    }

    // ==================== cancelarPorAnulacionDePlanificacion ====================

    @Test
    @DisplayName("CP-SCO-13: al anular la planificación, una solicitud abierta queda CANCELADA y sus enlaces vencen")
    void cancelar_solicitudAbierta() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudAbierta(LocalDateTime.now().plusHours(10));
        InvitacionCotizacionEntity invitacion = invitar(solicitud, proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), null);
        when(solicitudRepository.buscarIdsPorPlanificacionYEstados(eq(7L), anyCollection())).thenReturn(List.of(90L));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        servicio.cancelarPorAnulacionDePlanificacion(7L);

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
        assertThat(solicitud.getMotivoCancelacion()).isEqualTo("Se anuló la planificación de producción");
        assertThat(solicitud.getFechaCancelacion()).isNotNull();
        assertThat(invitacion.getFechaExpiracion()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(solicitud.getPlanes()).isEmpty();
        verify(solicitudRepository).save(solicitud);
    }

    @Test
    @DisplayName("CP-SCO-14: al anular la planificación, el plan elegido con órdenes confirmadas sigue ELEGIDO, los propuestos quedan CANCELADO, los descartados no cambian y las órdenes sin confirmar quedan DESCARTADA")
    void cancelar_solicitudConPlanes() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity p1 = proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta);
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(p1, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        PlanCompraEntity elegido = solicitud.getPlanes().get(0);
        elegido.setEstado(EstadoPlanCompra.ELEGIDO);
        OrdenCompraPropuestaEntity confirmada = elegido.getOrdenes().get(0);
        confirmada.setEstado(EstadoOrdenCompraPropuesta.CONFIRMADA);
        OrdenCompraPropuestaEntity pendiente = OrdenCompraPropuestaEntity.builder().id(803L).plan(elegido).proveedor(p1).estado(EstadoOrdenCompraPropuesta.PROPUESTA).build();
        elegido.getOrdenes().add(pendiente);
        PlanCompraEntity descartado = solicitud.getPlanes().get(1);
        descartado.setEstado(EstadoPlanCompra.DESCARTADO);
        PlanCompraEntity propuesto = plan(solicitud, 3, 804L, p1, solicitud.getLineas().get(0));
        solicitud.getPlanes().add(propuesto);
        when(solicitudRepository.buscarIdsPorPlanificacionYEstados(eq(7L), anyCollection())).thenReturn(List.of(90L));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        servicio.cancelarPorAnulacionDePlanificacion(7L);

        // === ASSERTS ===
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
        assertThat(elegido.getEstado()).isEqualTo(EstadoPlanCompra.ELEGIDO);
        assertThat(descartado.getEstado()).isEqualTo(EstadoPlanCompra.DESCARTADO);
        assertThat(propuesto.getEstado()).isEqualTo(EstadoPlanCompra.CANCELADO);
        assertThat(propuesto.getOrdenes().get(0).getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.DESCARTADA);
        assertThat(confirmada.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.CONFIRMADA);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.DESCARTADA);
    }

    @Test
    @DisplayName("CP-SCO-15: al anular la planificación sin solicitudes vigentes no se cambia nada")
    void cancelar_sinSolicitudes() {
        when(solicitudRepository.buscarIdsPorPlanificacionYEstados(eq(7L), anyCollection())).thenReturn(List.of());

        servicio.cancelarPorAnulacionDePlanificacion(7L);

        verify(solicitudRepository, never()).save(any());
    }

    // ==================== cancelarSolicitud ====================

    @Test
    @DisplayName("CP-SCO-32: cancelarSolicitud rechaza un motivo faltante o largo, una solicitud inexistente, una que no está abierta ni con planes y una con plan elegido")
    void cancelarSolicitud_reglas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        when(solicitudRepository.buscarIdPlanificacion(90L)).thenReturn(Optional.of(7L));
        when(solicitudRepository.buscarIdPlanificacion(99L)).thenReturn(Optional.empty());
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.cancelarSolicitud(90L, "   ")).isInstanceOf(ReglaNegocioException.class).hasMessage("El motivo de cancelación es obligatorio");
        assertThatThrownBy(() -> servicio.cancelarSolicitud(90L, "a".repeat(256))).hasMessage("El motivo de cancelación no puede superar 255 caracteres");
        assertThatThrownBy(() -> servicio.cancelarSolicitud(99L, "Ya no hace falta"))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró la solicitud de cotización con ID: 99");
        solicitud.getPlanes().get(0).setEstado(EstadoPlanCompra.ELEGIDO);
        assertThatThrownBy(() -> servicio.cancelarSolicitud(90L, "Ya no hace falta"))
                .hasMessage("No se puede cancelar la solicitud de cotización porque ya tiene un plan elegido");
        solicitud.setEstado(EstadoSolicitudCotizacion.CERRADA);
        assertThatThrownBy(() -> servicio.cancelarSolicitud(90L, "Ya no hace falta"))
                .hasMessage("Solo se pueden cancelar solicitudes de cotización pendientes de confirmación, abiertas o con planes generados");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SCO-33: cancelarSolicitud cancela con el motivo, vence los enlaces en el momento y cancela los planes, bloqueando la planificación y luego la solicitud")
    void cancelarSolicitud_cancela() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity conPlanes = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        InvitacionCotizacionEntity invitacion = invitar(conPlanes, proveedor(3L, "CEBADAS", Estado.ACTIVO, malta), null);
        invitacion.setFechaExpiracion(LocalDateTime.now().plusDays(1));
        when(solicitudRepository.buscarIdPlanificacion(90L)).thenReturn(Optional.of(7L));
        when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(conPlanes));

        // === EJECUCION ===
        SolicitudCotizacionResponseDTO resultado = servicio.cancelarSolicitud(90L, "  Se consiguió   el insumo  ");

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitudCotizacion.CANCELADA);
        assertThat(conPlanes.getMotivoCancelacion()).isEqualTo("Se consiguió el insumo");
        assertThat(conPlanes.getFechaCancelacion()).isNotNull();
        assertThat(invitacion.getFechaExpiracion()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(conPlanes.getPlanes()).allMatch(plan -> plan.getEstado() == EstadoPlanCompra.CANCELADO);
        assertThat(conPlanes.getPlanes()).flatExtracting(PlanCompraEntity::getOrdenes)
                .allMatch(orden -> ((OrdenCompraPropuestaEntity) orden).getEstado() == EstadoOrdenCompraPropuesta.DESCARTADA);
        InOrder orden = inOrder(planificacionProduccionRepository, solicitudRepository);
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(7L);
        orden.verify(solicitudRepository).buscarPorIdParaActualizar(90L);
        orden.verify(solicitudRepository).save(conPlanes);
    }

    // ==================== prepararOrdenCompra ====================

    @Test
    @DisplayName("CP-SCO-16: prepararOrdenCompra rechaza una orden propuesta inexistente")
    void preparar_inexistente() {
        when(ordenPropuestaRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.prepararOrdenCompra(5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden propuesta con ID: 5");
    }

    @Test
    @DisplayName("CP-SCO-17: prepararOrdenCompra rechaza una orden que ya no se puede confirmar, con el motivo")
    void preparar_estadosNoConfirmables() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity p1 = proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta);
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(p1, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        conOrdenPropuesta(orden);

        // === EJECUCION Y VERIFICACION ===
        orden.setEstado(EstadoOrdenCompraPropuesta.CONFIRMADA);
        assertThatThrownBy(() -> servicio.prepararOrdenCompra(801L)).hasMessage("La orden propuesta ya fue confirmada");
        orden.setEstado(EstadoOrdenCompraPropuesta.PROPUESTA);

        solicitud.getPlanes().get(0).setEstado(EstadoPlanCompra.DESCARTADO);
        assertThatThrownBy(() -> servicio.prepararOrdenCompra(801L)).hasMessage("No se puede confirmar la orden propuesta porque su plan está descartado");
        solicitud.getPlanes().get(0).setEstado(EstadoPlanCompra.PROPUESTO);

        solicitud.setEstado(EstadoSolicitudCotizacion.CANCELADA);
        assertThatThrownBy(() -> servicio.prepararOrdenCompra(801L)).hasMessage("No se puede confirmar la orden propuesta porque la solicitud de cotización está cancelada");
        solicitud.setEstado(EstadoSolicitudCotizacion.PLANES_GENERADOS);

        p1.setEstado(Estado.BAJA);
        assertThatThrownBy(() -> servicio.prepararOrdenCompra(801L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede confirmar la orden propuesta porque el proveedor fue dado de baja");
    }

    @Test
    @DisplayName("CP-SCO-18: prepararOrdenCompra traduce los ítems al catálogo vigente del proveedor y avisa los que ya no ofrece")
    void preparar_traduceAlCatalogoVigente() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity p1 = proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta);
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(p1, proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        // El proveedor se modificó después de cotizar: la versión vigente ofrece la malta con otro ID de catálogo y ya no el lúpulo
        VersionProveedorEntity anterior = p1.getVersiones().get(0);
        anterior.setEsUltimaVersion(false);
        VersionProveedorEntity vigente = VersionProveedorEntity.builder().id(11L).razonSocial("MALTERIA SUR").cuit("30000000001").esUltimaVersion(true).proveedor(p1).build();
        vigente.getCatalogoProveedor().add(CatalogoProveedorEntity.builder().id(999L).version(vigente).insumo(malta).presentacionComercial(bolsa25Kg).build());
        p1.getVersiones().add(vigente);
        orden.getItems().add(ItemOrdenCompraPropuestaEntity.builder().id(2L).ordenPropuesta(orden).lineaSolicitud(solicitud.getLineas().get(0))
                .catalogoProveedor(CatalogoProveedorEntity.builder().id(55L).insumo(lupulo).presentacionComercial(bolsa25Kg).build())
                .cantidad(1).precioUnitario(BigDecimal.ONE).build());
        conOrdenPropuesta(orden);

        // === EJECUCION ===
        OrdenCompraPrearmadaResponseDTO prearmada = servicio.prepararOrdenCompra(801L);

        // === ASSERTS ===
        assertThat(prearmada.getIdPlanificacion()).isEqualTo(7L);
        assertThat(prearmada.getIdProveedor()).isEqualTo(1L);
        assertThat(prearmada.getIdSolicitud()).isEqualTo(90L);
        assertThat(prearmada.getNumeroPlan()).isEqualTo(1);
        assertThat(prearmada.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getIdCatalogoProveedor()).isEqualTo(999L);
            assertThat(item.getCantidad()).isEqualTo(4);
            assertThat(item.getPrecioUnitario()).isEqualByComparingTo("30000");
        });
        assertThat(prearmada.getAvisos()).containsExactly("El proveedor ya no ofrece Cascade en Bolsa (25 kg): el ítem no se incluyó");
    }

    // ==================== confirmarOrdenPropuesta ====================

    @Test
    @DisplayName("CP-SCO-19: confirmarOrdenPropuesta rechaza los datos nulos y una orden para otro proveedor u otra planificación")
    void confirmar_rechazaDatosInvalidos() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        conOrdenPropuesta(solicitud.getPlanes().get(0).getOrdenes().get(0));

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.confirmarOrdenPropuesta(801L, null)).hasMessage("Los datos de la orden de compra son obligatorios");
        assertThatThrownBy(() -> servicio.confirmarOrdenPropuesta(801L, formOrden(7L, 2L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra debe registrarse para el proveedor de la orden propuesta");
        assertThatThrownBy(() -> servicio.confirmarOrdenPropuesta(801L, formOrden(8L, 1L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra debe registrarse para la planificación de producción de la solicitud de cotización");
        verifyNoInteractions(ordenCompraServicio);
    }

    @Test
    @DisplayName("CP-SCO-20: la primera orden confirmada registra la orden de compra, la vincula, elige su plan y descarta los demás")
    void confirmar_primeraOrden_eligePlan() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        OrdenCompraPropuestaEntity otra = solicitud.getPlanes().get(1).getOrdenes().get(0);
        conOrdenPropuesta(orden);
        OrdenCompraFormDTO form = formOrden(7L, 1L);
        when(ordenCompraServicio.registrarOrdenCompra(form)).thenReturn(OrdenCompraResponseDTO.builder().id(55L).build());
        OrdenCompraEntity referencia = OrdenCompraEntity.builder().id(55L).build();
        when(ordenCompraRepository.getReferenceById(55L)).thenReturn(referencia);

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = servicio.confirmarOrdenPropuesta(801L, form);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(55L);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.CONFIRMADA);
        assertThat(orden.getOrdenCompra()).isSameAs(referencia);
        assertThat(solicitud.getPlanes().get(0).getEstado()).isEqualTo(EstadoPlanCompra.ELEGIDO);
        assertThat(solicitud.getPlanes().get(1).getEstado()).isEqualTo(EstadoPlanCompra.DESCARTADO);
        assertThat(otra.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.DESCARTADA);
        // Orden de bloqueo: planificación, solicitud y recién después el alta de la orden de compra
        InOrder orden1 = inOrder(planificacionProduccionRepository, solicitudRepository, ordenCompraServicio);
        orden1.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(7L);
        orden1.verify(solicitudRepository).buscarPorIdParaActualizar(90L);
        orden1.verify(ordenCompraServicio).registrarOrdenCompra(form);
        verify(ordenPropuestaRepository).save(orden);
    }

    @Test
    @DisplayName("CP-SCO-21: una orden más del plan elegido se confirma sin cambiar los demás planes")
    void confirmar_planYaElegido() {
        // === PREPARACION DE DATOS ===
        ProveedorEntity p2 = proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta);
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta), p2);
        PlanCompraEntity elegido = solicitud.getPlanes().get(0);
        elegido.setEstado(EstadoPlanCompra.ELEGIDO);
        solicitud.getPlanes().get(1).setEstado(EstadoPlanCompra.DESCARTADO);
        OrdenCompraPropuestaEntity segunda = OrdenCompraPropuestaEntity.builder().id(805L).plan(elegido).proveedor(p2).estado(EstadoOrdenCompraPropuesta.PROPUESTA).build();
        elegido.getOrdenes().add(segunda);
        conOrdenPropuesta(segunda);
        OrdenCompraFormDTO form = formOrden(7L, 2L);
        when(ordenCompraServicio.registrarOrdenCompra(form)).thenReturn(OrdenCompraResponseDTO.builder().id(56L).build());

        // === EJECUCION ===
        servicio.confirmarOrdenPropuesta(805L, form);

        // === ASSERTS ===
        assertThat(segunda.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.CONFIRMADA);
        assertThat(elegido.getEstado()).isEqualTo(EstadoPlanCompra.ELEGIDO);
        assertThat(elegido.getOrdenes().get(0).getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.PROPUESTA);
        assertThat(solicitud.getPlanes().get(1).getEstado()).isEqualTo(EstadoPlanCompra.DESCARTADO);
    }

    @Test
    @DisplayName("CP-SCO-22: si el alta de la orden de compra la rechaza, la orden propuesta y los planes no cambian")
    void confirmar_rechazoDelAlta() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        conOrdenPropuesta(orden);
        OrdenCompraFormDTO form = formOrden(7L, 1L);
        when(ordenCompraServicio.registrarOrdenCompra(form)).thenThrow(new ReglaNegocioException("La fecha de entrega estimada es obligatoria"));

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.confirmarOrdenPropuesta(801L, form)).hasMessage("La fecha de entrega estimada es obligatoria");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.PROPUESTA);
        assertThat(orden.getOrdenCompra()).isNull();
        assertThat(solicitud.getPlanes()).allMatch(plan -> plan.getEstado() == EstadoPlanCompra.PROPUESTO);
        verify(ordenPropuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SCO-23: confirmarOrdenPropuesta rechaza una orden propuesta inexistente")
    void confirmar_inexistente() {
        when(ordenPropuestaRepository.buscarIdPlanificacion(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmarOrdenPropuesta(5L, formOrden(7L, 1L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden propuesta con ID: 5");
    }

    // ==================== descartarOrdenPropuesta ====================

    @Test
    @DisplayName("CP-SCO-24: descartarOrdenPropuesta solo descarta órdenes pendientes del plan elegido")
    void descartar_reglas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        conOrdenPropuesta(orden);

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.descartarOrdenPropuesta(90L, 801L)).hasMessage("Solo se pueden descartar órdenes propuestas del plan elegido");
        solicitud.getPlanes().get(0).setEstado(EstadoPlanCompra.ELEGIDO);
        orden.setEstado(EstadoOrdenCompraPropuesta.CONFIRMADA);
        assertThatThrownBy(() -> servicio.descartarOrdenPropuesta(90L, 801L)).hasMessage("Solo se pueden descartar órdenes propuestas pendientes de confirmar");
        orden.setEstado(EstadoOrdenCompraPropuesta.PROPUESTA);

        assertThat(servicio.descartarOrdenPropuesta(90L, 801L).getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.DESCARTADA);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.DESCARTADA);
        assertThat(solicitud.getPlanes().get(0).getEstado()).isEqualTo(EstadoPlanCompra.ELEGIDO);
        verify(solicitudRepository, org.mockito.Mockito.atLeastOnce()).buscarPorIdParaActualizar(90L);
    }

    @Test
    @DisplayName("CP-SCO-42: descartarOrdenPropuesta rechaza una orden propuesta que no pertenece a la solicitud indicada, sin bloquear ni cambiar nada")
    void descartar_deOtraSolicitud() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        solicitud.getPlanes().get(0).setEstado(EstadoPlanCompra.ELEGIDO);
        OrdenCompraPropuestaEntity orden = solicitud.getPlanes().get(0).getOrdenes().get(0);
        conOrdenPropuesta(orden);
        when(ordenPropuestaRepository.buscarIdSolicitud(999L)).thenReturn(Optional.empty());

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.descartarOrdenPropuesta(91L, 801L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden propuesta con ID 801 en la solicitud de cotización #91");
        assertThatThrownBy(() -> servicio.descartarOrdenPropuesta(90L, 999L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompraPropuesta.PROPUESTA);
        verify(solicitudRepository, never()).buscarPorIdParaActualizar(any());
        verify(ordenPropuestaRepository, never()).save(any());
    }

    // ==================== Consultas ====================

    @Test
    @DisplayName("CP-SCO-25: las consultas devuelven el resumen, el detalle con sus avisos y los IDs de las solicitudes vencidas y con envíos pendientes")
    void consultas() {
        // === PREPARACION DE DATOS ===
        SolicitudCotizacionEntity solicitud = solicitudConPlanes(proveedor(1L, "MALTERIA SUR", Estado.ACTIVO, malta),
                proveedor(2L, "LUPULOS DEL VALLE", Estado.ACTIVO, malta));
        PageRequest pagina = PageRequest.of(0, 20);
        when(solicitudRepository.filtrarSolicitudes(EstadoSolicitudCotizacion.PLANES_GENERADOS, null, pagina)).thenReturn(new PageImpl<>(List.of(solicitud)));
        when(solicitudRepository.findById(90L)).thenReturn(Optional.of(solicitud));
        when(solicitudRepository.findById(91L)).thenReturn(Optional.empty());
        when(solicitudRepository.buscarConPlanesPendientesDeDecision()).thenReturn(new ArrayList<>(List.of(solicitud)));
        when(solicitudRepository.buscarPendientesDeConfirmacion()).thenReturn(List.of());
        when(solicitudRepository.buscarIdsVencidas(eq(EstadoSolicitudCotizacion.ABIERTA), any())).thenReturn(List.of(3L, 4L));
        when(invitacionRepository.buscarIdsSolicitudesConEnviosPendientes(any(), eq(3))).thenReturn(List.of(5L));

        // === EJECUCION ===
        Page<SolicitudCotizacionResponseDTO> listado = servicio.filtrarSolicitudes(EstadoSolicitudCotizacion.PLANES_GENERADOS, null, pagina);
        SolicitudCotizacionResponseDTO detalle = servicio.buscarPorId(90L);

        // === ASSERTS ===
        assertThat(listado.getContent()).singleElement().satisfies(resumen -> {
            assertThat(resumen.getEtiquetaPlanificacion()).isEqualTo("Plan #7 - Pale Ale");
            assertThat(resumen.getCantidadPlanes()).isEqualTo(2);
            assertThat(resumen.getLineas()).isNull();
            assertThat(resumen.getPlanes()).isNull();
        });
        assertThat(detalle.getPlanes()).hasSize(2);
        // Cada plan propone 4 bolsas de 25 kg (100 kg) a $30.000: cubre el faltante
        assertThat(detalle.getPlanes().get(0).getCostoTotal()).isEqualByComparingTo("120000");
        assertThat(detalle.getPlanes().get(0).isCubreTodo()).isTrue();
        assertThat(detalle.getPlanes().get(0).getOrdenes().get(0).getItems().get(0).getPrecioPorUnidadDelInsumo()).isEqualByComparingTo("1200");
        assertThat(servicio.buscarPendientesDeDecision()).hasSize(1);
        assertThat(servicio.buscarPendientesDeConfirmacion()).isEmpty();
        assertThat(servicio.buscarIdsVencidas()).containsExactly(3L, 4L);
        assertThat(servicio.buscarIdsConEnviosPendientes()).containsExactly(5L);
        assertThatThrownBy(() -> servicio.buscarPorId(91L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la solicitud de cotización con ID: 91");
    }
}
