package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngresoInsumoServicioImplTest {

    private static final Long DETALLE_COMPRA_ID = 1L;
    private static final Long INSUMO_ID = 1L;

    @Mock
    private IIngresoInsumoRepository ingresoInsumoRepository;
    @Mock
    private ILoteInsumoRepository loteInsumoRepository;
    @Mock
    private IDetalleCompraRepository detalleCompraRepository;
    @Mock
    private IInsumoRepository insumoRepository;
    @Mock
    private IOrdenCompraRepository ordenCompraRepository;

    @InjectMocks
    private IngresoInsumoServicioImpl ingresoInsumoServicio;

    // ==================== filtrarIngresoInsumo ====================

    @Test
    @DisplayName("CP-FII-01: filtrarIngresoInsumo filtra por los 5 criterios informados")
    void filtrar_debeFiltrarPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(6), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA, 20.0, lote);
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fechaIngreso = LocalDate.of(2026, 1, 15);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(1L, "L-1", EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA, fechaIngreso, pageable))
                .thenReturn(new PageImpl<>(List.of(ingreso)));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.filtrarIngresoInsumo(1L, "L-1", EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA, fechaIngreso, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(1L, "L-1", EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA, fechaIngreso, pageable);
    }

    @Test
    @DisplayName("CP-FII-02: filtrarIngresoInsumo con los 5 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCincoParametrosNulos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(6), 50.0, 0.0);
        IngresoInsumoEntity registrado1 = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA, 20.0, lote);
        IngresoInsumoEntity registrado2 = ingresoInsumoEntity(2L, EstadoTransaccion.REGISTRADO, TipoIngreso.DIRECTO, 10.0, lote);
        IngresoInsumoEntity anulado = ingresoInsumoEntity(3L, EstadoTransaccion.ANULADO, TipoIngreso.COMPRA, 5.0, lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado1, registrado2, anulado)));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.filtrarIngresoInsumo(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(3);
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FII-03: filtrarIngresoInsumo permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(6), 50.0, 0.0);
        IngresoInsumoEntity anulado = ingresoInsumoEntity(3L, EstadoTransaccion.ANULADO, TipoIngreso.COMPRA, 5.0, lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(null, null, EstadoTransaccion.ANULADO, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.filtrarIngresoInsumo(null, null, EstadoTransaccion.ANULADO, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(null, null, EstadoTransaccion.ANULADO, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FII-04: filtrarIngresoInsumo retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(99L, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.filtrarIngresoInsumo(99L, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(99L, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FII-05: filtrarIngresoInsumo normaliza la identificación del lote del proveedor buscada (espacios y mayúsculas) antes de consultar")
    void filtrar_debeNormalizarLaIdentificacionDelLoteBuscada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(null, "LOTE 1", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        ingresoInsumoServicio.filtrarIngresoInsumo(null, "  lote   1 ", null, null, null, pageable);

        // === ASSERTS ===
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(null, "LOTE 1", null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FII-06: filtrarIngresoInsumo trata una identificación del lote formada solo por espacios como ausencia de filtro")
    void filtrar_debeTratarIdentificacionEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.filtrarIngresoInsumo(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        ingresoInsumoServicio.filtrarIngresoInsumo(null, "    ", null, null, null, pageable);

        // === ASSERTS ===
        verify(ingresoInsumoRepository).filtrarIngresoInsumo(null, null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del ingreso de insumo cuando el ID existe")
    void buscarPorId_debeRetornarIngresoExistente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, lote);
        when(ingresoInsumoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = ingresoInsumoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(ingresoInsumoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(ingresoInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ingreso de insumo con ID: 99");

        verify(ingresoInsumoRepository).findById(99L);
    }

    // ==================== registrarIngresoInsumoPorCompra ====================

    @Test
    @DisplayName("CP-RC-01: registrarIngresoInsumoPorCompra lanza RecursoNoEncontradoException cuando el ítem de detalle de compra no existe")
    void registrarPorCompra_debeRechazarDetalleCompraInexistente() {
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(99L).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ítem de detalle de compra con ID: 99");

        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-02: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la orden de compra del ítem no está en estado PENDIENTE")
    void registrarPorCompra_debeRechazarOrdenNoPendiente() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden registrar ingresos de ítems de órdenes de compra en estado Pendiente");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-03: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la cantidad recibida es nula")
    void registrarPorCompra_debeRechazarCantidadRecibidaNula() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(null).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-04: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la cantidad recibida es menor o igual a cero")
    void registrarPorCompra_debeRechazarCantidadRecibidaNoPositiva() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(0.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-05: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la cantidad recibida supera la cantidad pendiente de entrega")
    void registrarPorCompra_debeRechazarCantidadMayorAPendiente() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(150.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (100.0)");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-06: registrarIngresoInsumoPorCompra calcula la cantidad pendiente descontando solo ingresos REGISTRADO previos")
    void registrarPorCompra_debeCalcularPendienteConsiderandoSoloRegistrados() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(65.0).build();
        IngresoInsumoEntity ingresoPrevioRegistrado = ingresoInsumoEntity(10L, EstadoTransaccion.REGISTRADO, 40.0, null);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoPrevioRegistrado));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (60.0)");
    }

    @Test
    @DisplayName("CP-RC-07: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de ingreso es nula")
    void registrarPorCompra_debeRechazarFechaIngresoNula() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaIngreso(null).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de ingreso no puede ser posterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-08: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de ingreso es posterior a la fecha actual")
    void registrarPorCompra_debeRechazarFechaIngresoFutura() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaIngreso(LocalDate.now().plusDays(1)).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de ingreso no puede ser posterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-09: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de vencimiento es nula")
    void registrarPorCompra_debeRechazarFechaVencimientoNula() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaVencimiento(null).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-10: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de vencimiento es anterior a la fecha actual")
    void registrarPorCompra_debeRechazarFechaVencimientoAnterior() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaVencimiento(LocalDate.now().minusDays(1)).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-11: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando ya existe un lote con la misma identificación de lote de proveedor pero fecha de vencimiento distinta")
    void registrarPorCompra_debeRechazarConflictoDeLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", LocalDate.now().plusMonths(3), 10.0, 0.0);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-12: registrarIngresoInsumoPorCompra crea un lote nuevo y persiste el ingreso (camino feliz)")
    void registrarPorCompra_debeCrearLoteNuevoYPersistir() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, new BigDecimal("15.50"), EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("L-2025-001")
                .cantidadRecibida(20.0)
                .build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> {
            LoteInsumoEntity lote = invocation.getArgument(0);
            lote.setId(1L);
            return lote;
        });
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> {
            IngresoInsumoEntity ingreso = invocation.getArgument(0);
            ingreso.setId(1L);
            return ingreso;
        });

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LoteInsumoEntity> loteCaptor = ArgumentCaptor.forClass(LoteInsumoEntity.class);
        verify(loteInsumoRepository).save(loteCaptor.capture());
        assertThat(loteCaptor.getValue().getCantidadActual()).isEqualTo(20.0);
        assertThat(loteCaptor.getValue().getIdentificacionLoteProveedor()).isEqualTo("L-2025-001");

        ArgumentCaptor<IngresoInsumoEntity> ingresoCaptor = ArgumentCaptor.forClass(IngresoInsumoEntity.class);
        verify(ingresoInsumoRepository).save(ingresoCaptor.capture());
        IngresoInsumoEntity ingresoGuardado = ingresoCaptor.getValue();
        assertThat(ingresoGuardado.getTipoIngreso()).isEqualTo(TipoIngreso.COMPRA);
        assertThat(ingresoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        // El costo unitario se copia del detalle de compra: no se pide en el formulario
        assertThat(ingresoGuardado.getCostoUnitario()).isEqualByComparingTo("15.50");

        assertThat(resultado.getTipoIngreso()).isEqualTo(TipoIngreso.COMPRA);
    }

    @Test
    @DisplayName("CP-RC-13: registrarIngresoInsumoPorCompra reutiliza un lote existente sumando la cantidad recibida (camino feliz)")
    void registrarPorCompra_debeReutilizarLoteExistente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        LocalDate fechaVencimiento = LocalDate.now().plusMonths(6);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(fechaVencimiento)
                .cantidadRecibida(20.0)
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", fechaVencimiento, 50.0, 0.0);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));
        when(loteInsumoRepository.save(loteExistente)).thenReturn(loteExistente);
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> {
            IngresoInsumoEntity ingreso = invocation.getArgument(0);
            ingreso.setId(1L);
            return ingreso;
        });

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        // No se crea un lote nuevo: se reutiliza y muta la misma instancia existente
        assertThat(loteExistente.getCantidadActual()).isEqualTo(70.0);
        verify(loteInsumoRepository).save(loteExistente);
    }

    @Test
    @DisplayName("CP-RC-14: registrarIngresoInsumoPorCompra multiplica las unidades pedidas por el contenido de cada unidad de la presentación comercial al calcular la cantidad pendiente")
    void registrarPorCompra_debeMultiplicarPorContenidoDeLaPresentacionAlCalcularPendiente() {
        // === PREPARACION DE DATOS ===
        // Presentación de 25 kg por bolsa: pedir cantidad=3 significa 75 kg reales pedidos, no 3 kg
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        PresentacionComercialEntity presentacion25Kg = presentacionComercialEntity(25.0, UnidadDeMedida.KILOGRAMO);
        CatalogoProveedorEntity catalogoProveedor = CatalogoProveedorEntity.builder().id(1L).insumo(insumo).presentacionComercial(presentacion25Kg).build();
        DetalleCompraEntity detalleCompra = DetalleCompraEntity.builder()
                .id(DETALLE_COMPRA_ID)
                .cantidad(3)
                .costoUnitario(BigDecimal.TEN)
                .ordenCompra(ordenCompraEntity(EstadoSolicitud.PENDIENTE))
                .catalogoProveedor(catalogoProveedor)
                .build();
        // Bajo el bug original (sin multiplicar por la presentación), 50.0 kg se hubiera rechazado
        // por superar "3" (comparando kg contra bultos); aquí está dentro de los 75.0 kg reales pedidos
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(50.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RC-15: registrarIngresoInsumoPorCompra rechaza un ingreso que supera la cantidad pendiente real una vez aplicado el contenido de la presentación comercial")
    void registrarPorCompra_debeRechazarCantidadQueSuperaElContenidoRealDeLaPresentacion() {
        // === PREPARACION DE DATOS ===
        // Presentación de 25 kg por bolsa, cantidad=3 -> 75 kg reales pedidos; se intenta recibir 80 kg
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        PresentacionComercialEntity presentacion25Kg = presentacionComercialEntity(25.0, UnidadDeMedida.KILOGRAMO);
        CatalogoProveedorEntity catalogoProveedor = CatalogoProveedorEntity.builder().id(1L).insumo(insumo).presentacionComercial(presentacion25Kg).build();
        DetalleCompraEntity detalleCompra = DetalleCompraEntity.builder()
                .id(DETALLE_COMPRA_ID)
                .cantidad(3)
                .costoUnitario(BigDecimal.TEN)
                .ordenCompra(ordenCompraEntity(EstadoSolicitud.PENDIENTE))
                .catalogoProveedor(catalogoProveedor)
                .build();
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(80.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (75.0)");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-16: registrarIngresoInsumoPorCompra normaliza la identificación del lote de proveedor (mayúsculas y espacios colapsados) para buscar y crear el lote")
    void registrarPorCompra_debeNormalizarLaIdentificacionDelLote() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("  l-2025   001 ")
                .cantidadRecibida(20.0)
                .build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025 001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LoteInsumoEntity> loteCaptor = ArgumentCaptor.forClass(LoteInsumoEntity.class);
        verify(loteInsumoRepository).save(loteCaptor.capture());
        assertThat(loteCaptor.getValue().getIdentificacionLoteProveedor()).isEqualTo("L-2025 001");
        verify(loteInsumoRepository).buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025 001");
    }

    @Test
    @DisplayName("CP-RC-17: registrarIngresoInsumoPorCompra bloquea la fila del insumo antes de buscar el lote")
    void registrarPorCompra_debeBloquearElInsumoAntesDeBuscarElLote() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(insumoRepository, loteInsumoRepository);
        orden.verify(insumoRepository).buscarPorIdParaIngresar(INSUMO_ID);
        orden.verify(loteInsumoRepository).buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001");
        orden.verify(loteInsumoRepository).save(any(LoteInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-RC-18: registrarIngresoInsumoPorCompra rechaza un lote existente con otra fecha de vencimiento aunque la identificación se escriba en minúsculas, y no lo modifica")
    void registrarPorCompra_debeRechazarConflictoDeLoteConIdentificacionEnMinusculas() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        LocalDate vencimientoDelLote = LocalDate.now().plusMonths(3);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("l-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", vencimientoDelLote, 10.0, 0.0);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        assertThat(loteExistente.getFechaVencimiento()).isEqualTo(vencimientoDelLote);
        assertThat(loteExistente.getCantidadActual()).isEqualTo(10.0);
        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-19: registrarIngresoInsumoPorCompra traduce la violación de la restricción única del lote a ReglaNegocioException (ingreso simultáneo)")
    void registrarPorCompra_debeTraducirViolacionDeLaRestriccionDelLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "uk_lote_insumo_insumo_id_identificacion_lote_proveedor"));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote del insumo con la identificación de lote de proveedor 'L-2025-001' fue registrado por otra persona al mismo tiempo, intente nuevamente");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-20: registrarIngresoInsumoPorCompra lanza RecursoNoEncontradoException cuando el insumo ya no existe al bloquearlo")
    void registrarPorCompra_debeRechazarInsumoInexistenteAlBloquear() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 1");

        verify(loteInsumoRepository, never()).buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(any(), any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-21: registrarIngresoInsumoPorCompra lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor es nula")
    void registrarPorCompra_debeRechazarIdentificacionNula() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).identificacionLoteProveedor(null).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor es obligatoria");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-22: registrarIngresoInsumoPorCompra lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor está en blanco")
    void registrarPorCompra_debeRechazarIdentificacionEnBlanco() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).identificacionLoteProveedor("   ").build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor es obligatoria");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-23: registrarIngresoInsumoPorCompra lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor tiene 256 caracteres")
    void registrarPorCompra_debeRechazarIdentificacionDe256Caracteres() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).identificacionLoteProveedor("A".repeat(256)).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor no puede superar 255 caracteres");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-24: registrarIngresoInsumoPorCompra lanza RecursoNoEncontradoException cuando la orden de compra del ítem ya no existe al bloquearla")
    void registrarPorCompra_debeRechazarOrdenInexistenteAlBloquear() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden de compra con ID: 1");

        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-25: registrarIngresoInsumoPorCompra bloquea el ítem y luego la orden de compra, y no usa findById de la orden")
    void registrarPorCompra_debeBloquearElItemYLuegoLaOrden() {
        // === PREPARACION DE DATOS ===
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        bloquearOrdenCompra(detalleCompra);

        // === EJECUCION ===
        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // === ASSERTS ===
        InOrder orden = inOrder(detalleCompraRepository, ordenCompraRepository);
        orden.verify(detalleCompraRepository).buscarPorIdParaIngresar(DETALLE_COMPRA_ID);
        orden.verify(ordenCompraRepository).buscarPorIdParaCambiarEstado(1L);
        verify(ordenCompraRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-RC-26: registrarIngresoInsumoPorCompra finaliza automáticamente la orden, sin motivo, cuando el ingreso deja todos sus ítems completamente recibidos")
    void registrarPorCompra_debeFinalizarAutomaticamenteLaOrdenCuandoSeCompleta() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(20.0).build();
        IngresoInsumoEntity ingresoPrevio = ingresoPorCompra(10L, EstadoTransaccion.REGISTRADO, 80.0, null, detalleCompra);
        IngresoInsumoEntity ingresoNuevo = ingresoPorCompra(11L, EstadoTransaccion.REGISTRADO, 20.0, null, detalleCompra);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        // Antes de guardar el ingreso queda pendiente 20; después de guardarlo, 0
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO))
                .thenReturn(List.of(ingresoPrevio))
                .thenReturn(List.of(ingresoPrevio, ingresoNuevo));
        stubearLoteNuevoYGuardado(insumo);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(orden.getFechaFinalizacion()).isNotNull();
        assertThat(orden.getMotivoFinalizacion()).isNull();
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-RC-27: registrarIngresoInsumoPorCompra deja la orden en PENDIENTE cuando el ingreso es parcial")
    void registrarPorCompra_debeDejarPendienteLaOrdenSiElIngresoEsParcial() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(20.0).build();
        IngresoInsumoEntity ingresoPrevio = ingresoPorCompra(10L, EstadoTransaccion.REGISTRADO, 50.0, null, detalleCompra);
        IngresoInsumoEntity ingresoNuevo = ingresoPorCompra(11L, EstadoTransaccion.REGISTRADO, 20.0, null, detalleCompra);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO))
                .thenReturn(List.of(ingresoPrevio))
                .thenReturn(List.of(ingresoPrevio, ingresoNuevo));
        stubearLoteNuevoYGuardado(insumo);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(orden.getFechaFinalizacion()).isNull();
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-28: registrarIngresoInsumoPorCompra deja la orden en PENDIENTE cuando este ítem queda completo pero otro ítem de la orden sigue con cantidad pendiente")
    void registrarPorCompra_debeDejarPendienteLaOrdenSiOtroItemSigueIncompleto() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        DetalleCompraEntity otroDetalle = agregarOtroDetalle(detalleCompra, 2L, 40);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(100.0).build();
        IngresoInsumoEntity ingresoNuevo = ingresoPorCompra(11L, EstadoTransaccion.REGISTRADO, 100.0, null, detalleCompra);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO))
                .thenReturn(List.of())
                .thenReturn(List.of(ingresoNuevo));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(otroDetalle.getId(), EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        stubearLoteNuevoYGuardado(insumo);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-29: registrarIngresoInsumoPorCompra finaliza automáticamente la orden cuando el último ítem pendiente de una orden de varios ítems queda completo")
    void registrarPorCompra_debeFinalizarAutomaticamenteUnaOrdenDeVariosItems() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        DetalleCompraEntity otroDetalle = agregarOtroDetalle(detalleCompra, 2L, 40);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(100.0).build();
        IngresoInsumoEntity ingresoNuevo = ingresoPorCompra(11L, EstadoTransaccion.REGISTRADO, 100.0, null, detalleCompra);
        IngresoInsumoEntity ingresoDelOtroDetalle = ingresoPorCompra(12L, EstadoTransaccion.REGISTRADO, 40.0, null, otroDetalle);
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO))
                .thenReturn(List.of())
                .thenReturn(List.of(ingresoNuevo));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(otroDetalle.getId(), EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoDelOtroDetalle));
        stubearLoteNuevoYGuardado(insumo);
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(orden.getMotivoFinalizacion()).isNull();
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-RC-30: registrarIngresoInsumoPorCompra acepta recibir lo pendiente exacto aunque la suma de los ingresos previos arrastre residuo de coma flotante")
    void registrarPorCompra_debeAceptarLoPendienteExactoConResiduoDeComaFlotante() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 3, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        bloquearOrdenCompra(detalleCompra);
        // 0.1 + 0.2 = 0.30000000000000004: sin redondear, el pendiente sería 2.6999999999999997 y 2.7 se rechazaría
        IngresoInsumoEntity previo1 = ingresoPorCompra(10L, EstadoTransaccion.REGISTRADO, 0.1, null, detalleCompra);
        IngresoInsumoEntity previo2 = ingresoPorCompra(11L, EstadoTransaccion.REGISTRADO, 0.2, null, detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(2.7).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(previo1, previo2));
        stubearLoteNuevoYGuardado(insumo);

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RC-31: registrarIngresoInsumoPorCompra informa el pendiente redondeado cuando queda menos de 0,00005 y rechaza cualquier ingreso mayor")
    void registrarPorCompra_debeRechazarConPendienteMenorALaEscala() {
        // === PREPARACION DE DATOS ===
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        bloquearOrdenCompra(detalleCompra);
        // Pendiente real 0.00003, que redondeado a 4 decimales es 0.0
        IngresoInsumoEntity previo = ingresoPorCompra(10L, EstadoTransaccion.REGISTRADO, 99.99997, null, detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(1.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(previo));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (0.0)");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-32: registrarIngresoInsumoPorCompra no finaliza una orden sin ítems cargados")
    void registrarPorCompra_noDebeFinalizarUnaOrdenSinItems() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        orden.getDetallesCompra().clear();
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(20.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        stubearLoteNuevoYGuardado(insumo);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-33: registrarIngresoInsumoPorCompra guarda el ingreso antes de recalcular la cantidad pendiente para decidir si finaliza la orden")
    void registrarPorCompra_debeGuardarElIngresoAntesDeRecalcularElPendiente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        bloquearOrdenCompra(detalleCompra);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(20.0).build();
        when(detalleCompraRepository.buscarPorIdParaIngresar(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        stubearLoteNuevoYGuardado(insumo);

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(ingresoInsumoRepository);
        orden.verify(ingresoInsumoRepository).save(any(IngresoInsumoEntity.class));
        orden.verify(ingresoInsumoRepository, atLeastOnce()).findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO);
    }

    // ==================== registrarIngresoInsumoDirecto ====================

    @Test
    @DisplayName("CP-RD-01: registrarIngresoInsumoDirecto lanza RecursoNoEncontradoException cuando el insumo no existe")
    void registrarDirecto_debeRechazarInsumoInexistente() {
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(99L).build();
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");

        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-02: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la cantidad recibida es nula")
    void registrarDirecto_debeRechazarCantidadRecibidaNula() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).cantidadRecibida(null).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-03: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la cantidad recibida es menor o igual a cero")
    void registrarDirecto_debeRechazarCantidadRecibidaNoPositiva() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).cantidadRecibida(0.0).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-04: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando el costo unitario es nulo")
    void registrarDirecto_debeRechazarCostoUnitarioNulo() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).costoUnitario(null).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-05: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando el costo unitario es menor o igual a cero")
    void registrarDirecto_debeRechazarCostoUnitarioNoPositivo() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).costoUnitario(BigDecimal.ZERO).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo unitario debe ser mayor a cero");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-06: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de ingreso es nula")
    void registrarDirecto_debeRechazarFechaIngresoNula() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaIngreso(null).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de ingreso no puede ser posterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-07: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de ingreso es posterior a la fecha actual")
    void registrarDirecto_debeRechazarFechaIngresoFutura() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaIngreso(LocalDate.now().plusDays(1)).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de ingreso no puede ser posterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-08: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de vencimiento es nula")
    void registrarDirecto_debeRechazarFechaVencimientoNula() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaVencimiento(null).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-09: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de vencimiento es anterior a la fecha actual")
    void registrarDirecto_debeRechazarFechaVencimientoAnterior() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaVencimiento(LocalDate.now().minusDays(1)).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-10: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando ya existe un lote con la misma identificación de lote de proveedor pero fecha de vencimiento distinta")
    void registrarDirecto_debeRechazarConflictoDeLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", LocalDate.now().plusMonths(3), 10.0, 0.0);
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-11: registrarIngresoInsumoDirecto crea un lote nuevo y persiste el ingreso (camino feliz)")
    void registrarDirecto_debeCrearLoteNuevoYPersistir() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("L-2025-001")
                .cantidadRecibida(20.0)
                .costoUnitario(new BigDecimal("8.75"))
                .build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> {
            LoteInsumoEntity lote = invocation.getArgument(0);
            lote.setId(1L);
            return lote;
        });
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> {
            IngresoInsumoEntity ingreso = invocation.getArgument(0);
            ingreso.setId(1L);
            return ingreso;
        });

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LoteInsumoEntity> loteCaptor = ArgumentCaptor.forClass(LoteInsumoEntity.class);
        verify(loteInsumoRepository).save(loteCaptor.capture());
        assertThat(loteCaptor.getValue().getCantidadActual()).isEqualTo(20.0);

        ArgumentCaptor<IngresoInsumoEntity> ingresoCaptor = ArgumentCaptor.forClass(IngresoInsumoEntity.class);
        verify(ingresoInsumoRepository).save(ingresoCaptor.capture());
        IngresoInsumoEntity ingresoGuardado = ingresoCaptor.getValue();
        assertThat(ingresoGuardado.getTipoIngreso()).isEqualTo(TipoIngreso.DIRECTO);
        assertThat(ingresoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(ingresoGuardado.getDetalleCompra()).isNull();
        assertThat(ingresoGuardado.getCostoUnitario()).isEqualByComparingTo("8.75");

        assertThat(resultado.getTipoIngreso()).isEqualTo(TipoIngreso.DIRECTO);
    }

    @Test
    @DisplayName("CP-RD-12: registrarIngresoInsumoDirecto reutiliza un lote existente sumando la cantidad recibida (camino feliz)")
    void registrarDirecto_debeReutilizarLoteExistente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        LocalDate fechaVencimiento = LocalDate.now().plusMonths(6);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(fechaVencimiento)
                .cantidadRecibida(20.0)
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", fechaVencimiento, 50.0, 0.0);
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));
        when(loteInsumoRepository.save(loteExistente)).thenReturn(loteExistente);
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> {
            IngresoInsumoEntity ingreso = invocation.getArgument(0);
            ingreso.setId(1L);
            return ingreso;
        });

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO);

        // === ASSERTS ===
        assertThat(loteExistente.getCantidadActual()).isEqualTo(70.0);
        verify(loteInsumoRepository).save(loteExistente);
    }

    @Test
    @DisplayName("CP-RD-13: registrarIngresoInsumoDirecto normaliza la identificación del lote de proveedor (mayúsculas y espacios colapsados) para buscar y crear el lote")
    void registrarDirecto_debeNormalizarLaIdentificacionDelLote() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("  l-2025   001 ")
                .build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025 001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LoteInsumoEntity> loteCaptor = ArgumentCaptor.forClass(LoteInsumoEntity.class);
        verify(loteInsumoRepository).save(loteCaptor.capture());
        assertThat(loteCaptor.getValue().getIdentificacionLoteProveedor()).isEqualTo("L-2025 001");
        verify(loteInsumoRepository).buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025 001");
    }

    @Test
    @DisplayName("CP-RD-14: registrarIngresoInsumoDirecto bloquea la fila del insumo antes de buscar el lote")
    void registrarDirecto_debeBloquearElInsumoAntesDeBuscarElLote() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(insumoRepository, loteInsumoRepository);
        orden.verify(insumoRepository).buscarPorIdParaIngresar(INSUMO_ID);
        orden.verify(loteInsumoRepository).buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001");
        orden.verify(loteInsumoRepository).save(any(LoteInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-RD-15: registrarIngresoInsumoDirecto rechaza un lote existente con otra fecha de vencimiento aunque la identificación se escriba en minúsculas, y no lo modifica")
    void registrarDirecto_debeRechazarConflictoDeLoteConIdentificacionEnMinusculas() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        LocalDate vencimientoDelLote = LocalDate.now().plusMonths(3);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("l-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", vencimientoDelLote, 10.0, 0.0);
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        assertThat(loteExistente.getFechaVencimiento()).isEqualTo(vencimientoDelLote);
        assertThat(loteExistente.getCantidadActual()).isEqualTo(10.0);
        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-16: registrarIngresoInsumoDirecto traduce la violación de la restricción única del lote a ReglaNegocioException (ingreso simultáneo)")
    void registrarDirecto_debeTraducirViolacionDeLaRestriccionDelLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "uk_lote_insumo_insumo_id_identificacion_lote_proveedor"));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote del insumo con la identificación de lote de proveedor 'L-2025-001' fue registrado por otra persona al mismo tiempo, intente nuevamente");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-17: registrarIngresoInsumoDirecto lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor es nula")
    void registrarDirecto_debeRechazarIdentificacionNula() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).identificacionLoteProveedor(null).build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor es obligatoria");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-18: registrarIngresoInsumoDirecto lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor está en blanco")
    void registrarDirecto_debeRechazarIdentificacionEnBlanco() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).identificacionLoteProveedor("   ").build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor es obligatoria");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-19: registrarIngresoInsumoDirecto lanza ReglaNegocioException y no bloquea el insumo cuando la identificación del lote del proveedor tiene 256 caracteres")
    void registrarDirecto_debeRechazarIdentificacionDe256Caracteres() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).identificacionLoteProveedor("A".repeat(256)).build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La identificación del lote del proveedor no puede superar 255 caracteres");

        verify(insumoRepository, never()).buscarPorIdParaIngresar(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    // ==================== anularIngresoInsumo ====================

    @Test
    @DisplayName("CP-AI-01: anularIngresoInsumo lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO(null);

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ingresoInsumoRepository, loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AI-02: anularIngresoInsumo lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("   ");

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ingresoInsumoRepository, loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AI-07: anularIngresoInsumo lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("a".repeat(256));

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");

        verifyNoInteractions(ingresoInsumoRepository, loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AI-03: anularIngresoInsumo lanza RecursoNoEncontradoException cuando el ingreso de insumo no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.buscarPorIdParaAnular(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ingreso de insumo con ID: 99");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-04: anularIngresoInsumo lanza ReglaNegocioException cuando el ingreso ya está en estado ANULADO")
    void anular_debeRechazarIngresoYaAnulado() {
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.ANULADO, 20.0, lote);
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular ingresos de insumo en estado Registrado");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-05: anularIngresoInsumo lanza ReglaNegocioException cuando la cantidad del ingreso supera la cantidad disponible del lote")
    void anular_debeRechazarCantidadMayorADisponible() {
        // Lote con cantidadActual=50 y cantidadReservada=20 -> disponible=30
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 20.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 50.0, lote);
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el ingreso: su cantidad supera la cantidad disponible del lote de insumo");

        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-06: anularIngresoInsumo marca el ingreso como ANULADO y descuenta la cantidad del lote (camino feliz)")
    void anular_debeAnularYDescontarDelLote() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, lote);
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(ingreso.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(ingreso.getFechaAnulacion()).isNotNull();
        assertThat(ingreso.getMotivoAnulacion()).isEqualTo("Error de carga");
        // La cantidad recibida (20.0) se descuenta de la cantidadActual del lote (50.0 -> 30.0)
        assertThat(lote.getCantidadActual()).isEqualTo(30.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(loteInsumoRepository).save(lote);
        verify(ingresoInsumoRepository).save(ingreso);
    }

    @Test
    @DisplayName("CP-AI-08: anularIngresoInsumo de un ingreso por compra bloquea la orden de compra antes de bloquear el lote")
    void anular_debeBloquearLaOrdenAntesDelLote() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        bloquearOrdenCompra(detalleCompra);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        InOrder orden = inOrder(ordenCompraRepository, loteInsumoRepository);
        orden.verify(ordenCompraRepository).buscarPorIdParaCambiarEstado(1L);
        orden.verify(loteInsumoRepository).buscarPorIdParaAnularIngreso(1L);
        verify(ordenCompraRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-AI-09: anularIngresoInsumo reabre la orden finalizada automáticamente cuando algún ítem vuelve a tener cantidad pendiente")
    void anular_debeReabrirLaOrdenFinalizadaAutomaticamente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        orden.setFechaFinalizacion(LocalDateTime.now());
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 100.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        IngresoInsumoEntity otroIngreso = ingresoPorCompra(2L, EstadoTransaccion.REGISTRADO, 80.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);
        // Tras anular el ingreso de 20 solo queda registrado el de 80: pendiente 20
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(otroIngreso));
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(orden.getFechaFinalizacion()).isNull();
        assertThat(orden.getMotivoFinalizacion()).isNull();
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-AI-10: anularIngresoInsumo lanza ReglaNegocioException cuando la orden fue finalizada manualmente (con motivo) y no modifica nada")
    void anular_debeRechazarSiLaOrdenFueFinalizadaManualmente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        orden.setMotivoFinalizacion("Proveedor discontinuado");
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el ingreso porque la orden de compra fue finalizada manualmente");

        assertThat(ingreso.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(lote.getCantidadActual()).isEqualTo(50.0);
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-11: anularIngresoInsumo deja FINALIZADA la orden finalizada automáticamente cuando el resto de los ingresos sigue cubriendo todos los ítems")
    void anular_noDebeReabrirLaOrdenSiTodoSigueCubierto() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 150.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        // Un ingreso extra registrado deja la cantidad recibida cubierta aun sin el que se anula
        IngresoInsumoEntity ingresoQueCubreTodo = ingresoPorCompra(2L, EstadoTransaccion.REGISTRADO, 100.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoQueCubreTodo));

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-12: anularIngresoInsumo no modifica una orden que sigue en PENDIENTE")
    void anular_noDebeModificarUnaOrdenPendiente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(ingresoInsumoRepository, never()).findByDetalleCompraIdAndEstado(any(), any());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-13: anularIngresoInsumo lanza RecursoNoEncontradoException cuando la orden de compra del ingreso ya no existe al bloquearla")
    void anular_debeRechazarOrdenInexistenteAlBloquear() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(1L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la orden de compra con ID: 1");

        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-14: anularIngresoInsumo bloquea el ingreso antes de validar su estado y antes de bloquear la orden y el lote")
    void anular_debeBloquearElIngresoPrimero() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        bloquearOrdenCompra(detalleCompra);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        InOrder orden = inOrder(ingresoInsumoRepository, ordenCompraRepository, loteInsumoRepository);
        orden.verify(ingresoInsumoRepository).buscarPorIdParaAnular(1L);
        orden.verify(ordenCompraRepository).buscarPorIdParaCambiarEstado(1L);
        orden.verify(loteInsumoRepository).buscarPorIdParaAnularIngreso(1L);
        verify(ingresoInsumoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-AI-15: anularIngresoInsumo rechaza el ingreso de una orden finalizada manualmente aunque sea el único registrado")
    void anular_debeRechazarOrdenManualAunqueSeaElUnicoIngreso() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumo);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        orden.setMotivoFinalizacion("Cantidad ingresada menor a la solicitada");
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 20.0, 0.0);
        IngresoInsumoEntity unicoIngreso = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 20.0, lote, detalleCompra);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(unicoIngreso));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el ingreso porque la orden de compra fue finalizada manualmente");

        assertThat(unicoIngreso.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verifyNoInteractions(loteInsumoRepository);
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-16: anularIngresoInsumo reabre la orden automática de dos ítems cuando solo uno de ellos vuelve a tener cantidad pendiente")
    void anular_debeReabrirUnaOrdenDeDosItemsConSoloUnoPendiente() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.FINALIZADA, insumo);
        DetalleCompraEntity otroDetalle = agregarOtroDetalle(detalleCompra, 2L, 40);
        OrdenCompraEntity orden = bloquearOrdenCompra(detalleCompra);
        orden.setFechaFinalizacion(LocalDateTime.now());
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumo, "L-1", LocalDate.now().plusMonths(1), 140.0, 0.0);
        IngresoInsumoEntity ingresoCompleto = ingresoPorCompra(2L, EstadoTransaccion.REGISTRADO, 100.0, lote, detalleCompra);
        IngresoInsumoEntity ingresoQueSeAnula = ingresoPorCompra(1L, EstadoTransaccion.REGISTRADO, 40.0, lote, otroDetalle);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingresoQueSeAnula));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingresoQueSeAnula)).thenReturn(ingresoQueSeAnula);
        // El primer ítem sigue completo; el segundo se queda sin ingresos registrados
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoCompleto));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(otroDetalle.getId(), EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        assertThat(orden.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(orden.getFechaFinalizacion()).isNull();
        verify(ordenCompraRepository).save(orden);
    }

    @Test
    @DisplayName("CP-AI-17: anularIngresoInsumo de un ingreso directo no consulta ninguna orden de compra")
    void anular_ingresoDirectoNoDebeConsultarOrdenesDeCompra() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, lote);
        when(ingresoInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ingreso));
        when(loteInsumoRepository.buscarPorIdParaAnularIngreso(1L)).thenReturn(Optional.of(lote));
        when(loteInsumoRepository.save(lote)).thenReturn(lote);
        when(ingresoInsumoRepository.save(ingreso)).thenReturn(ingreso);

        // === EJECUCION ===
        ingresoInsumoServicio.anularIngresoInsumo(1L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        verifyNoInteractions(ordenCompraRepository);
    }

    // ==================== helpers de construcción ====================

    private void stubearLoteNuevoYGuardado(InsumoEntity insumo) {
        when(insumoRepository.buscarPorIdParaIngresar(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
        when(loteInsumoRepository.save(any(LoteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingresoInsumoRepository.save(any(IngresoInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static IngresoInsumoPorCompraFormDTO.IngresoInsumoPorCompraFormDTOBuilder porCompraValidoBuilder(Long idDetalleCompra) {
        return IngresoInsumoPorCompraFormDTO.builder()
                .identificacionLoteProveedor("L-2025-001")
                .fechaIngreso(LocalDate.now())
                .cantidadRecibida(20.0)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .idDetalleCompra(idDetalleCompra);
    }

    private static IngresoInsumoDirectoFormDTO.IngresoInsumoDirectoFormDTOBuilder directoValidoBuilder(Long idInsumo) {
        return IngresoInsumoDirectoFormDTO.builder()
                .idInsumo(idInsumo)
                .identificacionLoteProveedor("L-2025-001")
                .cantidadRecibida(20.0)
                .costoUnitario(BigDecimal.TEN)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .fechaIngreso(LocalDate.now());
    }

    private static AnularIngresoInsumoFormDTO anularFormDTO(String motivoAnulacion) {
        return AnularIngresoInsumoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static InsumoEntity insumoEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }

    private static OrdenCompraEntity ordenCompraEntity(EstadoSolicitud estado) {
        return OrdenCompraEntity.builder().id(1L).estado(estado).build();
    }

    private static DetalleCompraEntity detalleCompraEntity(Long id, Integer cantidad, BigDecimal costoUnitario, EstadoSolicitud estadoOrden, InsumoEntity insumo) {
        // Presentación de 1 kg por unidad: la cantidad de unidades pedidas coincide numéricamente
        // con los kg pedidos, para no alterar los cálculos de los demás casos de prueba.
        PresentacionComercialEntity presentacionComercial = presentacionComercialEntity(1.0, UnidadDeMedida.KILOGRAMO);
        CatalogoProveedorEntity catalogoProveedor = CatalogoProveedorEntity.builder().id(1L).insumo(insumo).presentacionComercial(presentacionComercial).build();
        DetalleCompraEntity detalleCompra = DetalleCompraEntity.builder()
                .id(id)
                .cantidad(cantidad)
                .costoUnitario(costoUnitario)
                .ordenCompra(ordenCompraEntity(estadoOrden))
                .catalogoProveedor(catalogoProveedor)
                .build();
        detalleCompra.getOrdenCompra().getDetallesCompra().add(detalleCompra);
        return detalleCompra;
    }

    /**
     * Agrega a la orden del detalle indicado otro ítem de la misma presentación (1 kg por unidad) y del mismo insumo.
     */
    private static DetalleCompraEntity agregarOtroDetalle(DetalleCompraEntity detalleCompra, Long id, Integer cantidad) {
        DetalleCompraEntity otroDetalle = DetalleCompraEntity.builder()
                .id(id)
                .cantidad(cantidad)
                .costoUnitario(BigDecimal.TEN)
                .ordenCompra(detalleCompra.getOrdenCompra())
                .catalogoProveedor(detalleCompra.getCatalogoProveedor())
                .build();
        detalleCompra.getOrdenCompra().getDetallesCompra().add(otroDetalle);
        return otroDetalle;
    }

    /**
     * Simula el bloqueo de la fila de la orden del detalle indicado: devuelve la misma instancia de la orden.
     */
    private OrdenCompraEntity bloquearOrdenCompra(DetalleCompraEntity detalleCompra) {
        OrdenCompraEntity ordenCompra = detalleCompra.getOrdenCompra();
        when(ordenCompraRepository.buscarPorIdParaCambiarEstado(ordenCompra.getId())).thenReturn(Optional.of(ordenCompra));
        return ordenCompra;
    }

    private static IngresoInsumoEntity ingresoPorCompra(Long id, EstadoTransaccion estado, Double cantidadRecibida, LoteInsumoEntity lote, DetalleCompraEntity detalleCompra) {
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(id, estado, TipoIngreso.COMPRA, cantidadRecibida, lote);
        ingreso.setDetalleCompra(detalleCompra);
        return ingreso;
    }

    private static PresentacionComercialEntity presentacionComercialEntity(double cantidad, UnidadDeMedida unidadDeMedida) {
        return PresentacionComercialEntity.builder()
                .id(1L)
                .nombre("Presentación de prueba")
                .cantidad(cantidad)
                .unidadDeMedida(unidadDeMedida)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, InsumoEntity insumo, String identificacionLoteProveedor, LocalDate fechaVencimiento, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor(identificacionLoteProveedor)
                .fechaVencimiento(fechaVencimiento)
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }

    private static IngresoInsumoEntity ingresoInsumoEntity(Long id, EstadoTransaccion estado, Double cantidadRecibida, LoteInsumoEntity loteInsumo) {
        return ingresoInsumoEntity(id, estado, TipoIngreso.DIRECTO, cantidadRecibida, loteInsumo);
    }

    private static IngresoInsumoEntity ingresoInsumoEntity(Long id, EstadoTransaccion estado, TipoIngreso tipoIngreso, Double cantidadRecibida, LoteInsumoEntity loteInsumo) {
        return IngresoInsumoEntity.builder()
                .id(id)
                .fechaIngreso(LocalDate.now())
                .cantidadRecibida(cantidadRecibida)
                .costoUnitario(BigDecimal.TEN)
                .tipoIngreso(tipoIngreso)
                .estado(estado)
                .loteInsumo(loteInsumo)
                .build();
    }
}
