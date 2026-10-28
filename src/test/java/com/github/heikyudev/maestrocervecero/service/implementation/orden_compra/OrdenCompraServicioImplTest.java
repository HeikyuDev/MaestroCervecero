package com.github.heikyudev.maestrocervecero.service.implementation.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.DetalleCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServicioImplTest {

    @Mock
    private IOrdenCompraRepository ordenCompraRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IProveedorRepository proveedorRepository;
    @Mock
    private ICatalogoProveedorRepository catalogoProveedorRepository;
    @Mock
    private IIngresoInsumoRepository ingresoInsumoRepository;

    @InjectMocks
    private OrdenCompraServicioImpl ordenCompraServicio;

    // ==================== filtrarOrdenesCompra ====================

    @Test
    @DisplayName("CP-FOC-01: filtrarOrdenesCompra filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fechaEntregaEstimada = LocalDate.of(2026, 1, 15);
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, planificacion, versionProveedor);
        when(ordenCompraRepository.filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable))
                .thenReturn(new PageImpl<>(List.of(orden)));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ordenCompraRepository).filtrarOrdenesCompra(1L, 2L, EstadoSolicitud.PENDIENTE, fechaEntregaEstimada, pageable);
    }

    @Test
    @DisplayName("CP-FOC-02: filtrarOrdenesCompra con los 4 parámetros nulos no restringe la búsqueda")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity ordenPendiente = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        OrdenCompraEntity ordenFinalizada = crearOrdenCompraEntity(2L, EstadoSolicitud.FINALIZADA, LocalDate.now().plusDays(20), planificacion, versionProveedor);
        OrdenCompraEntity ordenAnulada = crearOrdenCompraEntity(3L, EstadoSolicitud.ANULADA, LocalDate.now().plusDays(30), planificacion, versionProveedor);
        when(ordenCompraRepository.filtrarOrdenesCompra(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(ordenPendiente, ordenFinalizada, ordenAnulada)));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(3);
        verify(ordenCompraRepository).filtrarOrdenesCompra(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FOC-03: filtrarOrdenesCompra retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ordenCompraRepository.filtrarOrdenesCompra(99L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(99L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ordenCompraRepository).filtrarOrdenesCompra(99L, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FOC-04: filtrarOrdenesCompra informa subtotales y total pero no consulta los ingresos ni informa las cantidades recibida y pendiente")
    void filtrar_noDebeInformarCantidadesRecibidaYPendiente() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10),
                crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE), crearVersionProveedorEntity(1L, 2L));
        agregarDetalleCompra(orden, 10L, 4, new BigDecimal("2.5000"), 1.0, UnidadDeMedida.KILOGRAMO);
        when(ordenCompraRepository.filtrarOrdenesCompra(null, null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(orden)));

        // === EJECUCION ===
        Page<OrdenCompraResponseDTO> resultado = ordenCompraServicio.filtrarOrdenesCompra(null, null, null, null, pageable);

        // === ASSERTS ===
        OrdenCompraResponseDTO ordenDTO = resultado.getContent().get(0);
        assertThat(ordenDTO.getTotal()).isEqualByComparingTo("10.0000");
        assertThat(ordenDTO.getDetallesCompra().get(0).getSubtotal()).isEqualByComparingTo("10.0000");
        assertThat(ordenDTO.getDetallesCompra().get(0).getCantidadRecibida()).isNull();
        assertThat(ordenDTO.getDetallesCompra().get(0).getCantidadPendiente()).isNull();
        verifyNoInteractions(ingresoInsumoRepository);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la orden de compra cuando el ID existe")
    void buscarPorId_debeRetornarOrdenExistente() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(ordenCompraRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-03: buscarPorId informa por ítem las presentaciones recibidas y pendientes (con su equivalente en la unidad del insumo), el subtotal, y el total de la orden")
    void buscarPorId_debeInformarCantidadesRecibidaYPendienteYTotales() {
        // === PREPARACION DE DATOS ===
        // 5 bolsas de 25.000 g para un insumo en KILOGRAMO = 125 kg solicitados
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10),
                crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE), crearVersionProveedorEntity(1L, 2L));
        agregarDetalleCompra(orden, 10L, 5, new BigDecimal("12.5000"), 25000.0, UnidadDeMedida.GRAMO);
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(10L, EstadoTransaccion.REGISTRADO))
                .thenReturn(List.of(ingresoRecibido(30.0), ingresoRecibido(20.5)));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.buscarPorId(1L);

        // === ASSERTS ===
        DetalleCompraResponseDTO detalle = resultado.getDetallesCompra().get(0);
        assertThat(detalle.getCantidadRecibida()).isEqualTo(2.02);
        assertThat(detalle.getCantidadPendiente()).isEqualTo(2.98);
        assertThat(detalle.getCantidadRecibidaEnUnidadDelInsumo()).isEqualTo(50.5);
        assertThat(detalle.getCantidadPendienteEnUnidadDelInsumo()).isEqualTo(74.5);
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("62.5000");
        assertThat(resultado.getTotal()).isEqualByComparingTo("62.5000");
        verify(ingresoInsumoRepository).findByDetalleCompraIdAndEstado(10L, EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-BI-04: buscarPorId informa cantidad recibida cero y pendiente igual a la solicitada cuando el ítem no tiene ingresos REGISTRADO")
    void buscarPorId_debeInformarTodoPendienteSinIngresos() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10),
                crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE), crearVersionProveedorEntity(1L, 2L));
        agregarDetalleCompra(orden, 10L, 5, new BigDecimal("12.5000"), 25000.0, UnidadDeMedida.GRAMO);
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(10L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getDetallesCompra().get(0).getCantidadRecibida()).isEqualTo(0.0);
        assertThat(resultado.getDetallesCompra().get(0).getCantidadPendiente()).isEqualTo(5.0);
        assertThat(resultado.getDetallesCompra().get(0).getCantidadPendienteEnUnidadDelInsumo()).isEqualTo(125.0);
    }

    @Test
    @DisplayName("CP-BI-05: buscarPorId informa pendiente cero cuando el residuo de coma flotante es menor a la escala de 4 decimales")
    void buscarPorId_debeInformarPendienteCeroConResiduoDeComaFlotante() {
        // === PREPARACION DE DATOS ===
        // 3 unidades de 0.1 kg = 0.30000000000000004 kg solicitados; se recibió 0.3 kg
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10),
                crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE), crearVersionProveedorEntity(1L, 2L));
        agregarDetalleCompra(orden, 10L, 3, new BigDecimal("1.0000"), 0.1, UnidadDeMedida.KILOGRAMO);
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(10L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoRecibido(0.3)));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getDetallesCompra().get(0).getCantidadRecibida()).isEqualTo(3.0);
        assertThat(resultado.getDetallesCompra().get(0).getCantidadPendiente()).isEqualTo(0.0);
        assertThat(resultado.getDetallesCompra().get(0).getCantidadRecibidaEnUnidadDelInsumo()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden de compra con ID: 99");

        verify(ordenCompraRepository).findById(99L);
    }

    // ==================== registrarOrdenCompra ====================

    @Test
    @DisplayName("CP-ROC-01: registrarOrdenCompra lanza ReglaNegocioException cuando la fecha de entrega estimada es nula")
    void registrar_debeRechazarFechaEntregaEstimadaNula() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).fechaEntregaEstimada(null).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de entrega estimada es obligatoria");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-02: registrarOrdenCompra lanza ReglaNegocioException cuando la fecha de entrega estimada es anterior a la fecha actual")
    void registrar_debeRechazarFechaEntregaEstimadaAnterior() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).fechaEntregaEstimada(LocalDate.now().minusDays(1)).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de entrega estimada no puede ser anterior a la fecha actual");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-03: registrarOrdenCompra lanza ReglaNegocioException cuando la cantidad de un ítem es nula")
    void registrar_debeRechazarCantidadNula() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(null, BigDecimal.TEN, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada ítem debe ser mayor a cero");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-04: registrarOrdenCompra lanza ReglaNegocioException cuando la cantidad de un ítem es menor o igual a cero")
    void registrar_debeRechazarCantidadNoPositiva() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(0, BigDecimal.TEN, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada ítem debe ser mayor a cero");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-05: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario de un ítem es nulo")
    void registrar_debeRechazarCostoUnitarioNulo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, null, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem debe ser mayor a cero");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-06: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario de un ítem es menor o igual a cero")
    void registrar_debeRechazarCostoUnitarioNoPositivo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.ZERO, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem debe ser mayor a cero");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-07: registrarOrdenCompra lanza RecursoNoEncontradoException cuando la planificación de producción no existe")
    void registrar_debeRechazarPlanificacionInexistente() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(99L, 2L).build();
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");

        verifyNoInteractions(proveedorRepository, catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-08: registrarOrdenCompra lanza ReglaNegocioException cuando la planificación de producción no está en estado PENDIENTE")
    void registrar_debeRechazarPlanificacionNoPendiente() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.FINALIZADA, 7L);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden asociar órdenes de compra a planificaciones de producción en estado Pendiente");

        verifyNoInteractions(proveedorRepository, catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-09: registrarOrdenCompra lanza RecursoNoEncontradoException cuando el proveedor no existe")
    void registrar_debeRechazarProveedorInexistente() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 99L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el proveedor con ID: 99");

        verifyNoInteractions(catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-10: registrarOrdenCompra lanza RecursoNoEncontradoException cuando el proveedor no tiene una versión activa")
    void registrar_debeRechazarProveedorSinVersionActiva() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionInactiva = crearVersionProveedorEntity(3L, 2L, false);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionInactiva);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró una versión activa para el proveedor con ID: 2");

        verifyNoInteractions(catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-11: registrarOrdenCompra lanza RecursoNoEncontradoException cuando el ítem de catálogo no existe")
    void registrar_debeRechazarCatalogoInexistente() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.TEN, 99L)))
                .build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ítem de catálogo con ID: 99");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-12: registrarOrdenCompra lanza ReglaNegocioException cuando el ítem de catálogo no pertenece al proveedor seleccionado")
    void registrar_debeRechazarCatalogoDeOtroProveedor() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.TEN, 5L)))
                .build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        VersionProveedorEntity versionDeOtroProveedor = crearVersionProveedorEntity(999L, 888L, true);
        CatalogoProveedorEntity catalogo = crearCatalogoProveedorEntity(5L, versionDeOtroProveedor, maltaEntity(7L));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(catalogo));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El ítem de catálogo con ID 5 no pertenece al proveedor seleccionado");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-13: registrarOrdenCompra lanza ReglaNegocioException cuando el insumo del ítem no forma parte de la versión de receta")
    void registrar_debeRechazarInsumoFueraDeReceta() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.TEN, 5L)))
                .build();
        // La receta solo planifica la malta 7L; el catálogo ofrece la malta 8L
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        CatalogoProveedorEntity catalogo = crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(8L));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(catalogo));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El insumo del ítem de catálogo con ID 5 no forma parte de la versión de receta de la planificación de producción seleccionada");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-14: registrarOrdenCompra registra la orden en estado PENDIENTE con su detalle de compra (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.TEN, 5L)))
                .build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        CatalogoProveedorEntity catalogo = crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(7L));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(catalogo));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> {
            OrdenCompraEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.registrarOrdenCompra(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<OrdenCompraEntity> captor = ArgumentCaptor.forClass(OrdenCompraEntity.class);
        verify(ordenCompraRepository).save(captor.capture());
        OrdenCompraEntity ordenGuardada = captor.getValue();
        assertThat(ordenGuardada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(ordenGuardada.getDetallesCompra()).hasSize(1);
        assertThat(ordenGuardada.getDetallesCompra().get(0).getCantidad()).isEqualTo(10);
        assertThat(ordenGuardada.getDetallesCompra().get(0).getCostoUnitario()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(ordenGuardada.getDetallesCompra().get(0).getOrdenCompra()).isSameAs(ordenGuardada);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("CP-ROC-15: registrarOrdenCompra lanza ReglaNegocioException y no consulta la planificación cuando la orden repite el mismo ítem del catálogo")
    void registrar_debeRechazarItemDeCatalogoRepetido() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(
                        detalleCompraFormDTO(10, BigDecimal.TEN, 5L),
                        detalleCompraFormDTO(20, BigDecimal.ONE, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra no puede repetir el mismo ítem del catálogo");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-16: registrarOrdenCompra acepta dos ítems distintos del catálogo aunque sean del mismo insumo (distinta presentación comercial)")
    void registrar_debeAceptarItemsDistintosDelMismoInsumo() {
        // === PREPARACION DE DATOS ===
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(
                        detalleCompraFormDTO(10, BigDecimal.TEN, 5L),
                        detalleCompraFormDTO(20, BigDecimal.ONE, 6L)))
                .build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(7L))));
        when(catalogoProveedorRepository.findById(6L)).thenReturn(Optional.of(crearCatalogoProveedorEntity(6L, versionActiva, maltaEntity(7L))));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ordenCompraServicio.registrarOrdenCompra(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<OrdenCompraEntity> captor = ArgumentCaptor.forClass(OrdenCompraEntity.class);
        verify(ordenCompraRepository).save(captor.capture());
        assertThat(captor.getValue().getDetallesCompra()).hasSize(2);
    }

    @Test
    @DisplayName("CP-ROC-17: registrarOrdenCompra lanza ReglaNegocioException cuando la fecha de entrega estimada supera un año desde la fecha actual")
    void registrar_debeRechazarFechaEntregaEstimadaMayorAUnAnio() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).fechaEntregaEstimada(LocalDate.now().plusDays(366)).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de entrega estimada no puede superar un año desde la fecha actual");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-18: registrarOrdenCompra lanza ReglaNegocioException cuando el formulario es nulo")
    void registrar_debeRechazarFormularioNulo() {
        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los datos de la orden de compra son obligatorios");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-19: registrarOrdenCompra lanza ReglaNegocioException cuando la planificación de producción es nula")
    void registrar_debeRechazarPlanificacionNula() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(null, 2L).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación de producción es obligatoria");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-20: registrarOrdenCompra lanza ReglaNegocioException cuando el proveedor es nulo")
    void registrar_debeRechazarProveedorNulo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, null).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El proveedor es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-21: registrarOrdenCompra lanza ReglaNegocioException cuando la lista de ítems es nula")
    void registrar_debeRechazarDetallesNulos() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).detallesCompra(null).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra debe tener al menos un ítem");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-22: registrarOrdenCompra lanza ReglaNegocioException cuando la lista de ítems está vacía")
    void registrar_debeRechazarDetallesVacios() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).detallesCompra(List.of()).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra debe tener al menos un ítem");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-23: registrarOrdenCompra lanza ReglaNegocioException cuando la orden tiene 101 ítems")
    void registrar_debeRechazarMasDeCienItems() {
        List<DetalleCompraFormDTO> detalles = new ArrayList<>();
        for (long idCatalogo = 1; idCatalogo <= 101; idCatalogo++) {
            detalles.add(detalleCompraFormDTO(10, BigDecimal.TEN, idCatalogo));
        }
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).detallesCompra(detalles).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La orden de compra no puede superar 100 ítems");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-24: registrarOrdenCompra lanza ReglaNegocioException cuando la lista de ítems contiene un ítem nulo")
    void registrar_debeRechazarItemNulo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(Arrays.asList(detalleCompraFormDTO(10, BigDecimal.TEN, 5L), null))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los ítems de la orden de compra son obligatorios");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-25: registrarOrdenCompra lanza ReglaNegocioException cuando un ítem no indica el ítem del catálogo, antes de evaluar los repetidos")
    void registrar_debeRechazarItemSinCatalogoAntesDeLosRepetidos() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(
                        detalleCompraFormDTO(10, BigDecimal.TEN, null),
                        detalleCompraFormDTO(20, BigDecimal.ONE, null)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El ítem del catálogo de cada ítem de la orden de compra es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-26: registrarOrdenCompra lanza ReglaNegocioException cuando la cantidad de un ítem supera 1.000.000")
    void registrar_debeRechazarCantidadMayorAlMaximo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(1_000_001, BigDecimal.TEN, 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada ítem no puede superar 1.000.000");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-27: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario de un ítem supera 1.000.000.000")
    void registrar_debeRechazarCostoUnitarioMayorAlMaximo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("1000000000.0001"), 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem no puede superar 1.000.000.000");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-28: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario de un ítem tiene más de 4 decimales")
    void registrar_debeRechazarCostoUnitarioConMasDeCuatroDecimales() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("10.00001"), 5L)))
                .build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem no puede tener más de 4 decimales");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-29: registrarOrdenCompra acepta los valores límite: entrega a 365 días, cantidad 1.000.000 y costo 1.000.000.000 con ceros sobrantes en los decimales")
    void registrar_debeAceptarLosValoresLimite() {
        // === PREPARACION DE DATOS ===
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .fechaEntregaEstimada(LocalDate.now().plusDays(365))
                .detallesCompra(List.of(detalleCompraFormDTO(1_000_000, new BigDecimal("1000000000.000000"), 5L)))
                .build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(7L))));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.registrarOrdenCompra(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(ordenCompraRepository).save(any(OrdenCompraEntity.class));
    }

    @Test
    @DisplayName("CP-ROC-30: registrarOrdenCompra lanza ReglaNegocioException cuando el proveedor está dado de baja")
    void registrar_debeRechazarProveedorDadoDeBaja() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        ProveedorEntity proveedorDeBaja = ProveedorEntity.builder().id(2L).estado(Estado.BAJA).build();
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedorDeBaja));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar la orden de compra para un proveedor dado de baja");

        verifyNoInteractions(catalogoProveedorRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ROC-31: registrarOrdenCompra bloquea la planificación y el proveedor con los métodos de bloqueo, sin usar findById")
    void registrar_debeBloquearPlanificacionYProveedor() {
        // === PREPARACION DE DATOS ===
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        ProveedorEntity proveedor = crearProveedorEntityConVersiones(2L, versionActiva);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedor));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(7L))));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ordenCompraServicio.registrarOrdenCompra(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(planificacionProduccionRepository, proveedorRepository, ordenCompraRepository);
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(1L);
        orden.verify(proveedorRepository).buscarPorIdParaRegistrarOrdenCompra(2L);
        orden.verify(ordenCompraRepository).save(any(OrdenCompraEntity.class));
        verify(planificacionProduccionRepository, never()).findById(any());
        verify(proveedorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-ROC-32: registrarOrdenCompra acepta un costo unitario igual a 1.000.000.000 _(Límite)_")
    void registrar_debeAceptarCostoUnitarioIgualAlMaximo() {
        stubearRegistroValido();

        OrdenCompraResponseDTO resultado = ordenCompraServicio.registrarOrdenCompra(ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("1000000000"), 5L))).build());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("CP-ROC-33: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario es 1.000.000.001 _(Límite)_")
    void registrar_debeRechazarCostoUnitarioUnoPorEncimaDelMaximo() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("1000000001"), 5L))).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem no puede superar 1.000.000.000");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-34: registrarOrdenCompra acepta un costo unitario de 4 decimales _(Límite)_")
    void registrar_debeAceptarCostoUnitarioDeCuatroDecimales() {
        stubearRegistroValido();

        OrdenCompraResponseDTO resultado = ordenCompraServicio.registrarOrdenCompra(ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("1.0001"), 5L))).build());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("CP-ROC-35: registrarOrdenCompra lanza ReglaNegocioException cuando el costo unitario es 1.00001 (5 decimales significativos) _(Límite)_")
    void registrar_debeRechazarCostoUnitarioDeCincoDecimales() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(10, new BigDecimal("1.00001"), 5L))).build();

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario de cada ítem no puede tener más de 4 decimales");

        verifyNoInteractions(planificacionProduccionRepository, proveedorRepository, catalogoProveedorRepository, ordenCompraRepository);
    }

    @Test
    @DisplayName("CP-ROC-36: registrarOrdenCompra acepta una cantidad igual a 1.000.000 _(Límite)_")
    void registrar_debeAceptarCantidadIgualAlMaximo() {
        stubearRegistroValido();

        OrdenCompraResponseDTO resultado = ordenCompraServicio.registrarOrdenCompra(ordenCompraFormDTOValidoBuilder(1L, 2L)
                .detallesCompra(List.of(detalleCompraFormDTO(1_000_000, BigDecimal.TEN, 5L))).build());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    @DisplayName("CP-ROC-37: registrarOrdenCompra acepta una orden con 100 ítems _(Límite)_")
    void registrar_debeAceptarCienItems() {
        // === PREPARACION DE DATOS ===
        List<DetalleCompraFormDTO> detalles = new ArrayList<>();
        for (long idCatalogo = 1; idCatalogo <= 100; idCatalogo++) {
            detalles.add(detalleCompraFormDTO(10, BigDecimal.TEN, idCatalogo));
        }
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(crearProveedorEntityConVersiones(2L, versionActiva)));
        when(catalogoProveedorRepository.findById(any(Long.class))).thenAnswer(invocation ->
                Optional.of(crearCatalogoProveedorEntity(invocation.getArgument(0), versionActiva, maltaEntity(7L))));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ordenCompraServicio.registrarOrdenCompra(ordenCompraFormDTOValidoBuilder(1L, 2L).detallesCompra(detalles).build());

        // === ASSERTS ===
        ArgumentCaptor<OrdenCompraEntity> captor = ArgumentCaptor.forClass(OrdenCompraEntity.class);
        verify(ordenCompraRepository).save(captor.capture());
        assertThat(captor.getValue().getDetallesCompra()).hasSize(100);
    }

    @Test
    @DisplayName("CP-ROC-38: registrarOrdenCompra rechaza un proveedor dado de baja antes de evaluar si tiene versión activa")
    void registrar_debeRechazarProveedorDeBajaAntesQueLaVersionActiva() {
        OrdenCompraFormDTO formDTO = ordenCompraFormDTOValidoBuilder(1L, 2L).build();
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        // Proveedor de baja sin ninguna versión activa: debe fallar por la baja, no por la versión
        ProveedorEntity proveedorDeBaja = ProveedorEntity.builder().id(2L).estado(Estado.BAJA).build();
        proveedorDeBaja.getVersiones().add(crearVersionProveedorEntity(3L, 2L, false));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(proveedorDeBaja));

        assertThatThrownBy(() -> ordenCompraServicio.registrarOrdenCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar la orden de compra para un proveedor dado de baja");
    }

    // ==================== anularOrdenCompra ====================

    @Test
    @DisplayName("CP-AOC-01: anularOrdenCompra lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-AOC-02: anularOrdenCompra lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-AOC-07: anularOrdenCompra lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-AOC-03: anularOrdenCompra lanza RecursoNoEncontradoException cuando la orden de compra no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden de compra con ID: 99");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AOC-04: anularOrdenCompra lanza ReglaNegocioException cuando la orden no se encuentra en estado PENDIENTE")
    void anular_debeRechazarOrdenNoPendiente() {
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.FINALIZADA, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular órdenes de compra en estado Pendiente");

        verifyNoInteractions(ingresoInsumoRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AOC-05: anularOrdenCompra lanza ReglaNegocioException cuando la orden tiene un ingreso de insumo REGISTRADO asociado")
    void anular_debeRechazarConIngresoRegistradoAsociado() {
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(true);

        assertThatThrownBy(() -> ordenCompraServicio.anularOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la orden de compra porque tiene al menos un ingreso de insumo en estado Registrado asociado");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AOC-06: anularOrdenCompra anula la orden cuando no tiene ingresos de insumo REGISTRADO asociados (camino feliz)")
    void anular_debeAnularSinIngresosRegistradosAsociados() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        AnulacionOrdenCompraFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.anularOrdenCompra(1L, formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(orden.getFechaAnulacion()).isNotNull();
        assertThat(orden.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-AOC-08: anularOrdenCompra carga la orden con el método de bloqueo, sin usar findById")
    void anular_debeBloquearLaOrden() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ordenCompraServicio.anularOrdenCompra(1L, anulacionFormDTO("Error de carga"));

        // === ASSERTS ===
        verify(ordenCompraRepository).buscarPorIdParaCambiarEstado(1L);
        verify(ordenCompraRepository, never()).findById(any());
    }

    // ==================== finalizarOrdenCompra ====================

    @Test
    @DisplayName("CP-FZOC-01: finalizarOrdenCompra lanza ReglaNegocioException cuando el motivo de finalización es nulo")
    void finalizar_debeRechazarMotivoNulo() {
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO(null);

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización es obligatorio");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-FZOC-02: finalizarOrdenCompra lanza ReglaNegocioException cuando el motivo de finalización está en blanco")
    void finalizar_debeRechazarMotivoEnBlanco() {
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("   ");

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización es obligatorio");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-FZOC-07: finalizarOrdenCompra lanza ReglaNegocioException cuando el motivo de finalización tiene 256 caracteres")
    void finalizar_debeRechazarMotivoDe256Caracteres() {
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización no puede superar 255 caracteres");

        verifyNoInteractions(ordenCompraRepository, ingresoInsumoRepository);
    }

    @Test
    @DisplayName("CP-FZOC-03: finalizarOrdenCompra lanza RecursoNoEncontradoException cuando la orden de compra no existe")
    void finalizar_debeLanzarExcepcionSiNoExiste() {
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("Proveedor discontinuado");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden de compra con ID: 99");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZOC-04: finalizarOrdenCompra lanza ReglaNegocioException cuando la orden no se encuentra en estado PENDIENTE")
    void finalizar_debeRechazarOrdenNoPendiente() {
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.ANULADA, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("Proveedor discontinuado");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden finalizar órdenes de compra en estado Pendiente");

        verifyNoInteractions(ingresoInsumoRepository);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZOC-05: finalizarOrdenCompra lanza ReglaNegocioException cuando la orden no tiene ningún ingreso de insumo REGISTRADO asociado")
    void finalizar_debeRechazarSinIngresoRegistradoAsociado() {
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("Proveedor discontinuado");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(false);

        assertThatThrownBy(() -> ordenCompraServicio.finalizarOrdenCompra(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede finalizar la orden de compra porque no tiene ningún ingreso de insumo en estado Registrado asociado: corresponde anularla en su lugar");

        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZOC-06: finalizarOrdenCompra finaliza la orden cuando tiene al menos un ingreso de insumo REGISTRADO asociado (camino feliz)")
    void finalizar_debeFinalizarConIngresoRegistradoAsociado() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        FinalizacionForzadaOrdenCompraFormDTO formDTO = finalizacionFormDTO("Cantidad ingresada menor a la solicitada");
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(true);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = ordenCompraServicio.finalizarOrdenCompra(1L, formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(orden.getFechaFinalizacion()).isNotNull();
        assertThat(orden.getMotivoFinalizacion()).isEqualTo("Cantidad ingresada menor a la solicitada");
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-FZOC-08: finalizarOrdenCompra carga la orden con el método de bloqueo, sin usar findById")
    void finalizar_debeBloquearLaOrden() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(true);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ordenCompraServicio.finalizarOrdenCompra(1L, finalizacionFormDTO("Cantidad ingresada menor a la solicitada"));

        // === ASSERTS ===
        verify(ordenCompraRepository).buscarPorIdParaCambiarEstado(1L);
        verify(ordenCompraRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-FZOC-09: finalizarOrdenCompra siempre deja un motivo de finalización no vacío, de modo que un motivo nulo identifica la finalización automática")
    void finalizar_siempreDebeDejarUnMotivoNoVacio() {
        // === PREPARACION DE DATOS ===
        VersionProveedorEntity versionProveedor = crearVersionProveedorEntity(1L, 2L);
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntity(1L, EstadoSolicitud.PENDIENTE);
        OrdenCompraEntity orden = crearOrdenCompraEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.now().plusDays(10), planificacion, versionProveedor);
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.of(orden));
        when(ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(true);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        // Un motivo en blanco se rechaza (CP-FZOC-02); uno con espacios alrededor se guarda normalizado y nunca vacío
        ordenCompraServicio.finalizarOrdenCompra(1L, finalizacionFormDTO("   Proveedor discontinuado   "));

        // === ASSERTS ===
        assertThat(orden.getMotivoFinalizacion()).isNotBlank();
    }

    // ==================== helpers de construcción ====================

    private void stubearRegistroValido() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionProduccionEntityConMalta(1L, EstadoSolicitud.PENDIENTE, 7L);
        VersionProveedorEntity versionActiva = crearVersionProveedorEntity(3L, 2L, true);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        when(proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(2L)).thenReturn(Optional.of(crearProveedorEntityConVersiones(2L, versionActiva)));
        when(catalogoProveedorRepository.findById(5L)).thenReturn(Optional.of(crearCatalogoProveedorEntity(5L, versionActiva, maltaEntity(7L))));
        when(ordenCompraRepository.save(any(OrdenCompraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Agrega a la orden un ítem de un insumo que se mide en KILOGRAMO, con una presentación comercial del contenido indicado.
     */
    private static void agregarDetalleCompra(OrdenCompraEntity orden, Long id, int cantidad, BigDecimal costoUnitario,
                                             double cantidadPresentacion, UnidadDeMedida unidadPresentacion) {
        PresentacionComercialEntity presentacion = PresentacionComercialEntity.builder()
                .id(1L).nombre("Presentación de prueba").cantidad(cantidadPresentacion).unidadDeMedida(unidadPresentacion).estado(Estado.ACTIVO).build();
        MaltaEntity insumo = MaltaEntity.builder().id(1L).nombre("Malta Pilsen").estado(Estado.ACTIVO).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        CatalogoProveedorEntity catalogo = CatalogoProveedorEntity.builder().id(id).insumo(insumo).presentacionComercial(presentacion).build();
        orden.getDetallesCompra().add(DetalleCompraEntity.builder()
                .id(id).cantidad(cantidad).costoUnitario(costoUnitario).ordenCompra(orden).catalogoProveedor(catalogo).build());
    }

    private static IngresoInsumoEntity ingresoRecibido(double cantidadRecibida) {
        return IngresoInsumoEntity.builder().cantidadRecibida(cantidadRecibida).estado(EstadoTransaccion.REGISTRADO).build();
    }

    private static AnulacionOrdenCompraFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionOrdenCompraFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO(String motivoFinalizacion) {
        return FinalizacionForzadaOrdenCompraFormDTO.builder().motivoFinalizacion(motivoFinalizacion).build();
    }

    private static OrdenCompraEntity crearOrdenCompraEntity(Long id, EstadoSolicitud estado, LocalDate fechaEntregaEstimada,
                                                              PlanificacionProduccionEntity planificacionProduccion, VersionProveedorEntity versionProveedor) {
        return OrdenCompraEntity.builder()
                .id(id)
                .fechaEntregaEstimada(fechaEntregaEstimada)
                .estado(estado)
                .planificacionProduccion(planificacionProduccion)
                .versionProveedor(versionProveedor)
                .build();
    }

    private static PlanificacionProduccionEntity crearPlanificacionProduccionEntity(Long id, EstadoSolicitud estado) {
        return PlanificacionProduccionEntity.builder()
                .id(id)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusMonths(1))
                .cantidadAProducir(100.0)
                .estado(estado)
                .versionReceta(crearVersionRecetaEntity(1L))
                .build();
    }

    private static VersionRecetaEntity crearVersionRecetaEntity(Long id) {
        return VersionRecetaEntity.builder()
                .id(id)
                .nombre("Receta Test")
                .esUltimaVersion(true)
                .build();
    }

    private static VersionProveedorEntity crearVersionProveedorEntity(Long id, Long idProveedor) {
        return crearVersionProveedorEntity(id, idProveedor, true);
    }

    private static VersionProveedorEntity crearVersionProveedorEntity(Long id, Long idProveedor, boolean esUltimaVersion) {
        return VersionProveedorEntity.builder()
                .id(id)
                .razonSocial("Proveedor Test")
                .nombreComercial("Proveedor Test")
                .cuit("30-12345678-9")
                .telefono("11-2233-4455")
                .email("contacto@proveedor.com")
                .direccion("Ruta 5 Km 120")
                .esUltimaVersion(esUltimaVersion)
                .localidad(crearLocalidadEntity(1L))
                .proveedor(ProveedorEntity.builder().id(idProveedor).estado(Estado.ACTIVO).build())
                .build();
    }

    private static ProveedorEntity crearProveedorEntityConVersiones(Long idProveedor, VersionProveedorEntity... versiones) {
        ProveedorEntity proveedorEntity = ProveedorEntity.builder().id(idProveedor).estado(Estado.ACTIVO).build();
        for (VersionProveedorEntity version : versiones) {
            version.setProveedor(proveedorEntity);
            proveedorEntity.getVersiones().add(version);
        }
        return proveedorEntity;
    }

    private static LocalidadEntity crearLocalidadEntity(Long id) {
        return LocalidadEntity.builder().id(id).nombre("San Nicolás").estado(Estado.ACTIVO).build();
    }

    private static OrdenCompraFormDTO.OrdenCompraFormDTOBuilder ordenCompraFormDTOValidoBuilder(Long idPlanificacionProduccion, Long idProveedor) {
        return OrdenCompraFormDTO.builder()
                .fechaEntregaEstimada(LocalDate.now().plusDays(10))
                .idPlanificacionProduccion(idPlanificacionProduccion)
                .idProveedor(idProveedor)
                .detallesCompra(List.of(detalleCompraFormDTO(10, BigDecimal.TEN, 5L)));
    }

    private static DetalleCompraFormDTO detalleCompraFormDTO(Integer cantidad, BigDecimal costoUnitario, Long idCatalogoProveedor) {
        return DetalleCompraFormDTO.builder()
                .cantidad(cantidad)
                .costoUnitario(costoUnitario)
                .idCatalogoProveedor(idCatalogoProveedor)
                .build();
    }

    private static PlanificacionProduccionEntity crearPlanificacionProduccionEntityConMalta(Long id, EstadoSolicitud estado, Long idMalta) {
        VersionRecetaEntity versionReceta = crearVersionRecetaEntity(1L);
        DetalleMaltaEntity detalleMalta = DetalleMaltaEntity.builder()
                .id(1L)
                .cantidad(5.0)
                .versionReceta(versionReceta)
                .malta(maltaEntity(idMalta))
                .build();
        versionReceta.getDetallesMalta().add(detalleMalta);
        return PlanificacionProduccionEntity.builder()
                .id(id)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusMonths(1))
                .cantidadAProducir(100.0)
                .estado(estado)
                .versionReceta(versionReceta)
                .build();
    }

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static CatalogoProveedorEntity crearCatalogoProveedorEntity(Long id, VersionProveedorEntity version, InsumoEntity insumo) {
        return CatalogoProveedorEntity.builder()
                .id(id)
                .version(version)
                .insumo(insumo)
                .build();
    }
}
