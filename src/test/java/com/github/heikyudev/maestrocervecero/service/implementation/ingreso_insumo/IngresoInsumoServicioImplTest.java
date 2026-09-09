package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private IngresoInsumoServicioImpl ingresoInsumoServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de ingresos de insumo incluyendo tanto REGISTRADO como ANULADO")
    void buscarTodos_debeRetornarPaginaConTodosLosEstados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 0.0);
        IngresoInsumoEntity ingreso1 = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, lote);
        IngresoInsumoEntity ingreso2 = ingresoInsumoEntity(2L, EstadoTransaccion.REGISTRADO, 15.0, lote);
        IngresoInsumoEntity ingreso3 = ingresoInsumoEntity(3L, EstadoTransaccion.ANULADO, 10.0, lote);
        when(ingresoInsumoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(ingreso1, ingreso2, ingreso3), pageable, 3));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).hasSize(3);
        verify(ingresoInsumoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay ingresos de insumo registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ingresoInsumoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<IngresoInsumoResponseDTO> resultado = ingresoInsumoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ingresoInsumoRepository).findAll(pageable);
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
        when(detalleCompraRepository.findById(99L)).thenReturn(Optional.empty());

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
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden registrar ingresos de ítems de órdenes de compra en estado PENDIENTE");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-03: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la cantidad recibida es nula")
    void registrarPorCompra_debeRechazarCantidadRecibidaNula() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).cantidadRecibida(null).build();
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));

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
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));

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
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
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
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(ingresoPrevioRegistrado));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (60.0)");
    }

    @Test
    @DisplayName("CP-RC-07: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de vencimiento es nula")
    void registrarPorCompra_debeRechazarFechaVencimientoNula() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaVencimiento(null).build();
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-08: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando la fecha de vencimiento es anterior a la fecha actual")
    void registrarPorCompra_debeRechazarFechaVencimientoAnterior() {
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumoEntity(INSUMO_ID));
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID).fechaVencimiento(LocalDate.now().minusDays(1)).build();
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-09: registrarIngresoInsumoPorCompra lanza ReglaNegocioException cuando ya existe un lote con la misma identificación de lote de proveedor pero fecha de vencimiento distinta")
    void registrarPorCompra_debeRechazarConflictoDeLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, BigDecimal.TEN, EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", LocalDate.now().plusMonths(3), 10.0, 0.0);
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoPorCompra(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RC-10: registrarIngresoInsumoPorCompra crea un lote nuevo y persiste el ingreso (camino feliz)")
    void registrarPorCompra_debeCrearLoteNuevoYPersistir() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        DetalleCompraEntity detalleCompra = detalleCompraEntity(DETALLE_COMPRA_ID, 100, new BigDecimal("15.50"), EstadoSolicitud.PENDIENTE, insumo);
        IngresoInsumoPorCompraFormDTO formDTO = porCompraValidoBuilder(DETALLE_COMPRA_ID)
                .identificacionLoteProveedor("L-2025-001")
                .cantidadRecibida(20.0)
                .build();
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
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
    @DisplayName("CP-RC-11: registrarIngresoInsumoPorCompra reutiliza un lote existente sumando la cantidad recibida (camino feliz)")
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
        when(detalleCompraRepository.findById(DETALLE_COMPRA_ID)).thenReturn(Optional.of(detalleCompra));
        when(ingresoInsumoRepository.findByDetalleCompraIdAndEstado(DETALLE_COMPRA_ID, EstadoTransaccion.REGISTRADO)).thenReturn(List.of());
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));
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
    @DisplayName("CP-RD-06: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de vencimiento es nula")
    void registrarDirecto_debeRechazarFechaVencimientoNula() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaVencimiento(null).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-07: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando la fecha de vencimiento es anterior a la fecha actual")
    void registrarDirecto_debeRechazarFechaVencimientoAnterior() {
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumoEntity(INSUMO_ID)));
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID).fechaVencimiento(LocalDate.now().minusDays(1)).build();

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de vencimiento no puede ser anterior a la fecha actual");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-08: registrarIngresoInsumoDirecto lanza ReglaNegocioException cuando ya existe un lote con la misma identificación de lote de proveedor pero fecha de vencimiento distinta")
    void registrarDirecto_debeRechazarConflictoDeLote() {
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("L-2025-001")
                .fechaVencimiento(LocalDate.now().plusMonths(9))
                .build();
        LoteInsumoEntity loteExistente = loteInsumoEntity(1L, insumo, "L-2025-001", LocalDate.now().plusMonths(3), 10.0, 0.0);
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));

        assertThatThrownBy(() -> ingresoInsumoServicio.registrarIngresoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta");

        verify(loteInsumoRepository, never()).save(any());
        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RD-09: registrarIngresoInsumoDirecto crea un lote nuevo y persiste el ingreso (camino feliz)")
    void registrarDirecto_debeCrearLoteNuevoYPersistir() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumo = insumoEntity(INSUMO_ID);
        IngresoInsumoDirectoFormDTO formDTO = directoValidoBuilder(INSUMO_ID)
                .identificacionLoteProveedor("L-2025-001")
                .cantidadRecibida(20.0)
                .costoUnitario(new BigDecimal("8.75"))
                .build();
        when(insumoRepository.findById(INSUMO_ID)).thenReturn(Optional.of(insumo));
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.empty());
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
    @DisplayName("CP-RD-10: registrarIngresoInsumoDirecto reutiliza un lote existente sumando la cantidad recibida (camino feliz)")
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
        when(loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(INSUMO_ID, "L-2025-001")).thenReturn(Optional.of(loteExistente));
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
    @DisplayName("CP-AI-03: anularIngresoInsumo lanza RecursoNoEncontradoException cuando el ingreso de insumo no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.findById(99L)).thenReturn(Optional.empty());

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
        when(ingresoInsumoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

        assertThatThrownBy(() -> ingresoInsumoServicio.anularIngresoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular ingresos de insumo en estado REGISTRADO");

        verify(ingresoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AI-05: anularIngresoInsumo lanza ReglaNegocioException cuando la cantidad del ingreso supera la cantidad disponible del lote")
    void anular_debeRechazarCantidadMayorADisponible() {
        // Lote con cantidadActual=50 y cantidadReservada=20 -> disponible=30
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(INSUMO_ID), "L-1", LocalDate.now().plusMonths(1), 50.0, 20.0);
        IngresoInsumoEntity ingreso = ingresoInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 50.0, lote);
        AnularIngresoInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ingresoInsumoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

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
        when(ingresoInsumoRepository.findById(1L)).thenReturn(Optional.of(ingreso));
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

    // ==================== helpers de construcción ====================

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
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static OrdenCompraEntity ordenCompraEntity(EstadoSolicitud estado) {
        return OrdenCompraEntity.builder().id(1L).estado(estado).build();
    }

    private static DetalleCompraEntity detalleCompraEntity(Long id, Integer cantidad, BigDecimal costoUnitario, EstadoSolicitud estadoOrden, InsumoEntity insumo) {
        CatalogoProveedorEntity catalogoProveedor = CatalogoProveedorEntity.builder().id(1L).insumo(insumo).build();
        return DetalleCompraEntity.builder()
                .id(id)
                .cantidad(cantidad)
                .costoUnitario(costoUnitario)
                .ordenCompra(ordenCompraEntity(estadoOrden))
                .catalogoProveedor(catalogoProveedor)
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
        return IngresoInsumoEntity.builder()
                .id(id)
                .fechaIngreso(LocalDate.now())
                .cantidadRecibida(cantidadRecibida)
                .costoUnitario(BigDecimal.TEN)
                .tipoIngreso(TipoIngreso.DIRECTO)
                .estado(estado)
                .loteInsumo(loteInsumo)
                .build();
    }
}
