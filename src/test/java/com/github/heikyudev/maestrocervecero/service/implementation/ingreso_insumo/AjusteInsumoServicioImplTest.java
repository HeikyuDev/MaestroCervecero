package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularAjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class AjusteInsumoServicioImplTest {

    @Mock
    private IAjusteInsumoRepository ajusteInsumoRepository;

    @Mock
    private IMotivoAjusteRepository motivoAjusteRepository;

    @Mock
    private ILoteInsumoRepository loteInsumoRepository;

    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;

    @InjectMocks
    private AjusteInsumoServicioImpl ajusteInsumoServicio;

    private static final LocalDateTime FECHA_DESDE = LocalDate.of(2026, 1, 1).atStartOfDay();
    private static final LocalDateTime FECHA_HASTA = LocalDate.of(2026, 1, 31).atTime(LocalTime.MAX);

    // ==================== filtrarAjustesInsumos ====================

    @Test
    @DisplayName("CP-FAI-01: filtrarAjustesInsumos filtra por los criterios informados")
    void filtrar_debeFiltrarPorLosCriteriosInformados() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajuste = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(1L, TipoAjuste.INGRESO, EstadoTransaccion.REGISTRADO, FECHA_DESDE, FECHA_HASTA, pageable))
                .thenReturn(new PageImpl<>(List.of(ajuste)));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(1L, TipoAjuste.INGRESO, EstadoTransaccion.REGISTRADO, FECHA_DESDE, FECHA_HASTA, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(1L, TipoAjuste.INGRESO, EstadoTransaccion.REGISTRADO, FECHA_DESDE, FECHA_HASTA, pageable);
    }

    @Test
    @DisplayName("CP-FAI-02: filtrarAjustesInsumos con todos los parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarTodosLosParametrosNulos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity registrado1 = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        AjusteInsumoEntity registrado2 = ajusteInsumoEntity(2L, EstadoTransaccion.REGISTRADO, 3.0, motivoAjusteEntity(2L, TipoAjuste.EGRESO), lote);
        AjusteInsumoEntity anulado = ajusteInsumoEntity(3L, EstadoTransaccion.ANULADO, 2.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado1, registrado2, anulado)));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(3);
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FAI-03: filtrarAjustesInsumos permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity anulado = ajusteInsumoEntity(3L, EstadoTransaccion.ANULADO, 2.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, null, EstadoTransaccion.ANULADO, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, null, EstadoTransaccion.ANULADO, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, null, EstadoTransaccion.ANULADO, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FAI-04: filtrarAjustesInsumos retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(99L, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(99L, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(99L, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FAI-05: filtrarAjustesInsumos permite acotar solo por tipo de ajuste")
    void filtrar_debeAcotarPorTipoAjuste() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity egreso = ajusteInsumoEntity(2L, EstadoTransaccion.REGISTRADO, 3.0, motivoAjusteEntity(2L, TipoAjuste.EGRESO), lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, TipoAjuste.EGRESO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(egreso)));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, TipoAjuste.EGRESO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, TipoAjuste.EGRESO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FAI-06: filtrarAjustesInsumos permite acotar por un rango de fecha de registro completo")
    void filtrar_debeAcotarPorRangoDeFechas() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajuste = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, null, null, FECHA_DESDE, FECHA_HASTA, pageable))
                .thenReturn(new PageImpl<>(List.of(ajuste)));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, null, null, FECHA_DESDE, FECHA_HASTA, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, null, null, FECHA_DESDE, FECHA_HASTA, pageable);
    }

    @Test
    @DisplayName("CP-FAI-07: filtrarAjustesInsumos permite informar solo la fecha desde")
    void filtrar_debeAcotarSoloPorFechaDesde() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, null, null, FECHA_DESDE, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, null, null, FECHA_DESDE, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, null, null, FECHA_DESDE, null, pageable);
    }

    @Test
    @DisplayName("CP-FAI-08: filtrarAjustesInsumos permite informar solo la fecha hasta")
    void filtrar_debeAcotarSoloPorFechaHasta() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.filtrarAjustesInsumo(null, null, null, null, FECHA_HASTA, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.filtrarAjustesInsumos(null, null, null, null, FECHA_HASTA, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ajusteInsumoRepository).filtrarAjustesInsumo(null, null, null, null, FECHA_HASTA, pageable);
    }

    @Test
    @DisplayName("CP-FAI-09: filtrarAjustesInsumos rechaza un rango de fechas invertido sin consultar el repositorio")
    void filtrar_debeRechazarRangoDeFechasInvertido() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ajusteInsumoServicio.filtrarAjustesInsumos(null, null, null, FECHA_HASTA, FECHA_DESDE, pageable))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha hasta no puede ser anterior a la fecha desde");
        verifyNoInteractions(ajusteInsumoRepository);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del ajuste de insumo cuando el ID existe")
    void buscarPorId_debeRetornarAjusteInsumoExistente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCantidad()).isEqualTo(5.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(ajusteInsumoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(ajusteInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ajuste de insumo con ID: 99");
        verify(ajusteInsumoRepository).findById(99L);
    }

    // ==================== registrarAjusteInsumo ====================

    @Test
    @DisplayName("CP-RA-01: registrarAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el motivo de ajuste no existe")
    void registrarAjusteInsumo_debeRechazarMotivoAjusteInexistente() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(99L, 1L, 10.0, "Observación");
        when(motivoAjusteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el motivo de ajuste con ID: 99");

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-37: registrarAjusteInsumo lanza ReglaNegocioException en el campo idMotivoAjuste y no persiste cuando el motivo de ajuste está dado de baja")
    void registrarAjusteInsumo_debeRechazarMotivoAjusteDadoDeBaja() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(7L, 1L, 10.0, "Observación");
        when(motivoAjusteRepository.findById(7L)).thenReturn(Optional.empty());
        when(motivoAjusteRepository.existsByIdAndEstado(7L, Estado.BAJA)).thenReturn(true);

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de ajuste está dado de baja")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idMotivoAjuste"));

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-02: registrarAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el lote de insumo no existe")
    void registrarAjusteInsumo_debeRechazarLoteInsumoInexistente() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 99L, 10.0, "Observación");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-03: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es nula")
    void registrarAjusteInsumo_debeRechazarCantidadNula() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, null, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidad"));

        verifyNoInteractions(ajusteInsumoRepository);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-04: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es cero (límite)")
    void registrarAjusteInsumo_debeRechazarCantidadEnCero() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 0.0, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a 0");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-05: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es negativa")
    void registrarAjusteInsumo_debeRechazarCantidadNegativa() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, -5.0, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a 0");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-06: registrarAjusteInsumo lanza ReglaNegocioException cuando la observación es nula")
    void registrarAjusteInsumo_debeRechazarObservacionNula() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, null);

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La observación es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observacion"));

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-07: registrarAjusteInsumo lanza ReglaNegocioException cuando la observación está en blanco")
    void registrarAjusteInsumo_debeRechazarObservacionEnBlanco() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "   ");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La observación es obligatoria");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-15: registrarAjusteInsumo lanza ReglaNegocioException cuando la observación tiene 256 caracteres")
    void registrarAjusteInsumo_debeRechazarObservacionDe256Caracteres() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "a".repeat(256));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La observación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observacion"));

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-08: registrarAjusteInsumo lanza ReglaNegocioException con motivo EGRESO cuando la cantidad supera la cantidad actual del lote")
    void registrarAjusteInsumo_debeRechazarEgresoQueSuperaCantidadActual() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 15.0, "Rotura de lote");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, insumoEntity(1L), 10.0, 0.0)));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a descontar no puede superar la cantidad actual del lote de insumo (10 kg)");

        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-09: registrarAjusteInsumo permite un EGRESO con cantidad igual a la disponible (límite, no debe quedar negativo)")
    void registrarAjusteInsumo_debePermitirEgresoConCantidadIgualALaDisponible() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "Rotura de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 10.0, 0.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(0.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(loteInsumoRepository).save(loteEntity);
    }

    @Test
    @DisplayName("CP-RA-10: registrarAjusteInsumo con motivo INGRESO no valida contra la cantidad disponible del lote")
    void registrarAjusteInsumo_debePermitirIngresoSinValidarDisponible() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 100.0, "Ingreso grande");
        // disponible = 5.0 - 3.0 = 2.0, pero la cantidad del ajuste (100.0) no debe compararse contra eso
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 5.0, 3.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(105.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-11: registrarAjusteInsumo persiste y retorna el DTO con motivo INGRESO (camino feliz)")
    void registrarAjusteInsumo_debePersistirConMotivoIngreso() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        MotivoAjusteEntity motivoEntity = motivoAjusteEntity(1L, TipoAjuste.INGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(25.0);

        ArgumentCaptor<AjusteInsumoEntity> captor = ArgumentCaptor.forClass(AjusteInsumoEntity.class);
        verify(ajusteInsumoRepository).save(captor.capture());
        AjusteInsumoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getCantidad()).isEqualTo(5.0);
        assertThat(entidadCapturada.getObservacion()).isEqualTo("Corrección de conteo físico");
        assertThat(entidadCapturada.getMotivoAjuste()).isEqualTo(motivoEntity);
        assertThat(entidadCapturada.getLoteInsumo()).isEqualTo(loteEntity);
        assertThat(entidadCapturada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-12: registrarAjusteInsumo persiste y retorna el DTO con motivo EGRESO (camino feliz)")
    void registrarAjusteInsumo_debePersistirConMotivoEgreso() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Rotura de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-13: registrarAjusteInsumo con EGRESO que excede la cantidad disponible reparte el excedente proporcionalmente entre las reservas activas del lote de insumo")
    void registrarAjusteInsumo_debeRepartirExcedenteProporcionalmenteEntreReservas() {
        // === PREPARACION DE DATOS ===
        // disponible = 20.0 - 12.0 = 8.0; se pide descontar 14.0 → excedente de 6.0 sobre lo reservado
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 14.0, "Rotura parcial de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 12.0);
        // reservaA tiene 2/3 de lo reservado (8.0), reservaB 1/3 (4.0)
        ReservaInsumoEntity reservaA = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteEntity).cantidadReservada(8.0).cantidadPendiente(8.0).build();
        ReservaInsumoEntity reservaB = ReservaInsumoEntity.builder().id(101L).loteInsumo(loteEntity).cantidadReservada(4.0).cantidadPendiente(4.0).build();
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(reservaA, reservaB));
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(6.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(6.0);
        // reservaA pierde 6.0 * (8/12) = 4.0 → queda en 4.0; reservaB pierde 6.0 * (4/12) = 2.0 → queda en 2.0
        assertThat(reservaA.getCantidadPendiente()).isEqualTo(4.0);
        assertThat(reservaB.getCantidadPendiente()).isEqualTo(2.0);
        // Lo quitado por la merma se suma a "reducida por ajuste" (no a "liberada") y la reserva sigue activa
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(4.0);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(2.0);
        assertThat(reservaA.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reservaB.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reservaA.getCantidadReservada()).isEqualTo(8.0);
        assertThat(reservaB.getCantidadReservada()).isEqualTo(4.0);
        assertThat(reservaA.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(reservaB.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        verify(reservaInsumoRepository).saveAll(List.of(reservaA, reservaB));
    }

    @Test
    @DisplayName("CP-RA-16: registrarAjusteInsumo con EGRESO que agota las reservas deja Liberada la que no se consumió por completo y Consumida no se toca")
    void registrarAjusteInsumo_debeLiberarLasReservasQueLaMermaAgota() {
        // === PREPARACION DE DATOS ===
        // actual = 10.0 y reservado = 10.0 → disponible 0; se pierde todo: el excedente (10.0) agota todas las reservas
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "Pérdida total del lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 10.0, 10.0);
        // reservaA: 6 reservados sin consumir; reservaB: 6 reservados, 2 consumidos y 4 pendientes
        ReservaInsumoEntity reservaA = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteEntity).cantidadReservada(6.0).cantidadPendiente(6.0).build();
        ReservaInsumoEntity reservaB = ReservaInsumoEntity.builder().id(101L).loteInsumo(loteEntity).cantidadReservada(6.0).cantidadConsumida(2.0).cantidadPendiente(4.0).build();
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(reservaA, reservaB));
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(0.0);
        assertThat(reservaA.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(6.0);
        assertThat(reservaA.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reservaA.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
        assertThat(reservaB.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(4.0);
        assertThat(reservaB.getCantidadConsumida()).isEqualTo(2.0);
        assertThat(reservaB.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }

    @Test
    @DisplayName("CP-RA-17: registrarAjusteInsumo con EGRESO reduce solo las reservas con saldo pendiente: las consumidas y las liberadas no participan")
    void registrarAjusteInsumo_noDebeTocarLasReservasCerradas() {
        // === PREPARACION DE DATOS ===
        // disponible = 20.0 - 8.0 = 12.0; se pide descontar 14.0 → excedente de 2.0 sobre lo reservado
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 14.0, "Rotura parcial de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 8.0);
        ReservaInsumoEntity activa = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteEntity).cantidadReservada(8.0).cantidadPendiente(8.0).build();
        ReservaInsumoEntity consumida = ReservaInsumoEntity.builder().id(101L).loteInsumo(loteEntity).cantidadReservada(5.0).cantidadConsumida(5.0)
                .cantidadPendiente(0.0).estado(EstadoReservaInsumo.CONSUMIDA).build();
        ReservaInsumoEntity liberada = ReservaInsumoEntity.builder().id(102L).loteInsumo(loteEntity).cantidadReservada(5.0).cantidadLiberada(5.0)
                .cantidadPendiente(0.0).estado(EstadoReservaInsumo.LIBERADA).build();
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        // La consulta del repositorio solo trae las ACTIVAS; aun si llegaran cerradas, no tienen saldo que repartir
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(activa, consumida, liberada));
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(activa.getCantidadPendiente()).isEqualTo(6.0);
        assertThat(activa.getCantidadReducidaPorAjuste()).isEqualTo(2.0);
        assertThat(activa.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(consumida.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(consumida.getEstado()).isEqualTo(EstadoReservaInsumo.CONSUMIDA);
        assertThat(liberada.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(liberada.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("CP-RA-14: registrarAjusteInsumo con EGRESO dentro de la cantidad disponible no interactúa con las reservas de insumo")
    void registrarAjusteInsumo_noDebeTocarReservasSiNoExcedeDisponible() {
        // === PREPARACION DE DATOS ===
        // disponible = 20.0 - 12.0 = 8.0; se pide descontar 5.0, no la excede
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Rotura menor");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 12.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(12.0);
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-18: registrarAjusteInsumo lanza ReglaNegocioException y no consulta nada cuando los datos del ajuste son nulos")
    void registrarAjusteInsumo_debeRechazarDtoNulo() {
        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los datos del ajuste de insumo son obligatorios");

        verifyNoInteractions(motivoAjusteRepository);
        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-19: registrarAjusteInsumo lanza ReglaNegocioException cuando el ID del lote de insumo es nulo")
    void registrarAjusteInsumo_debeRechazarIdLoteInsumoNulo() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, null, 10.0, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idLoteInsumo"));

        verifyNoInteractions(motivoAjusteRepository);
        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-20: registrarAjusteInsumo lanza ReglaNegocioException cuando el ID del motivo de ajuste es nulo")
    void registrarAjusteInsumo_debeRechazarIdMotivoAjusteNulo() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(null, 1L, 10.0, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de ajuste es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idMotivoAjuste"));

        verifyNoInteractions(motivoAjusteRepository);
        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-21: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es NaN")
    void registrarAjusteInsumo_debeRechazarCantidadNaN() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, Double.NaN, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a 0")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidad"));

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-22: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es infinita")
    void registrarAjusteInsumo_debeRechazarCantidadInfinita() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, Double.POSITIVE_INFINITY, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a 0")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidad"));

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-23: registrarAjusteInsumo acepta la cantidad máxima (límite) y rechaza una cantidad que la supera")
    void registrarAjusteInsumo_debeRechazarCantidadSobreElMaximo() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO sobreElMaximo = ajusteInsumoFormDTO(1L, 1L, 1_000_000.0001, "Observación");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(sobreElMaximo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad no puede superar 1.000.000")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidad"));

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-24: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad tiene más de 4 decimales")
    void registrarAjusteInsumo_debeRechazarCantidadConDemasiadosDecimales() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 1.23456, "Observación");

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad no puede tener más de 4 decimales")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidad"));

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-25: registrarAjusteInsumo persiste en el ajuste la misma cantidad redondeada que aplica al lote")
    void registrarAjusteInsumo_debePersistirLaCantidadRedondeadaQueAplicaAlLote() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 0.1235, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.1, 0.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<AjusteInsumoEntity> captor = ArgumentCaptor.forClass(AjusteInsumoEntity.class);
        verify(ajusteInsumoRepository).save(captor.capture());
        assertThat(loteEntity.getCantidadActual()).isEqualTo(20.2235);
        assertThat(captor.getValue().getCantidad()).isEqualTo(0.1235);
    }

    @Test
    @DisplayName("CP-RA-26: registrarAjusteInsumo incluye la unidad de medida del insumo en el mensaje cuando el EGRESO supera la cantidad actual")
    void registrarAjusteInsumo_debeIncluirLaUnidadEnElMensajeDeStockInsuficiente() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 1250.5, "Rotura de lote");
        InsumoEntity levadura = MaltaEntity.builder().id(2L).nombre("Levadura Ale").unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).build();
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, levadura, 1000.25, 0.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a descontar no puede superar la cantidad actual del lote de insumo (1.000,25 g)");
    }

    @Test
    @DisplayName("CP-RA-27: registrarAjusteInsumo con EGRESO cuando las reservas activas suman menos que el excedente deja la cantidad reservada del lote en cero, nunca negativa")
    void registrarAjusteInsumo_noDebeDejarCantidadReservadaNegativa() {
        // === PREPARACION DE DATOS ===
        // actual = 10.0, reservada = 10.0 → disponible 0; excedente = 10.0, pero las reservas activas solo tienen 3.0 pendientes
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "Pérdida total del lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 10.0, 10.0);
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteEntity).cantidadReservada(10.0).cantidadConsumida(7.0).cantidadPendiente(3.0).build();
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(reserva));
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(3.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-RA-32: registrarAjusteInsumo con EGRESO reparte un excedente que no se divide exacto entre reservas iguales conservando el total (método del mayor resto)")
    void registrarAjusteInsumo_debeConservarElTotalAlRepartirEntreTresReservasIguales() {
        // === PREPARACION DE DATOS ===
        // actual = 3.0 y reservado = 3.0 → disponible 0; excedente = 1.0 repartido entre 3 reservas de 1.0
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 3.0, 3.0);
        ReservaInsumoEntity reservaA = reservaActiva(100L, loteEntity, 1.0);
        ReservaInsumoEntity reservaB = reservaActiva(101L, loteEntity, 1.0);
        ReservaInsumoEntity reservaC = reservaActiva(102L, loteEntity, 1.0);

        // === EJECUCION ===
        registrarEgresoConReservas(loteEntity, 1.0, reservaA, reservaB, reservaC);

        // === ASSERTS ===
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(0.3334);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(0.3333);
        assertThat(reservaC.getCantidadReducidaPorAjuste()).isEqualTo(0.3333);
        assertThat(sumaReducida(reservaA, reservaB, reservaC)).isEqualTo(1.0);
        assertThat(loteEntity.getCantidadActual()).isEqualTo(2.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(2.0);
        assertThat(sumaPendiente(reservaA, reservaB, reservaC)).isEqualTo(loteEntity.getCantidadReservada());
    }

    @Test
    @DisplayName("CP-RA-33: registrarAjusteInsumo con EGRESO reparte un excedente de pocas unidades entre reservas iguales sin perder ni inventar cantidad")
    void registrarAjusteInsumo_debeRepartirUnExcedenteMuyChicoEntreReservasIguales() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 3.0, 3.0);
        ReservaInsumoEntity reservaA = reservaActiva(100L, loteEntity, 1.0);
        ReservaInsumoEntity reservaB = reservaActiva(101L, loteEntity, 1.0);
        ReservaInsumoEntity reservaC = reservaActiva(102L, loteEntity, 1.0);

        // === EJECUCION ===
        registrarEgresoConReservas(loteEntity, 0.0002, reservaA, reservaB, reservaC);

        // === ASSERTS ===
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(0.0001);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(0.0001);
        assertThat(reservaC.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(sumaReducida(reservaA, reservaB, reservaC)).isEqualTo(0.0002);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(2.9998);
        assertThat(sumaPendiente(reservaA, reservaB, reservaC)).isEqualTo(loteEntity.getCantidadReservada());
    }

    @Test
    @DisplayName("CP-RA-34: registrarAjusteInsumo con EGRESO cuyo excedente es la mínima unidad de la escala se lo asigna a una sola reserva")
    void registrarAjusteInsumo_debeAsignarLaMinimaUnidadAUnaSolaReserva() {
        // === PREPARACION DE DATOS ===
        // 0.0001 es la unidad más chica que admite la escala: una mitad de unidad (0.00005) no puede llegar como cantidad
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 3.0, 3.0);
        ReservaInsumoEntity reservaA = reservaActiva(100L, loteEntity, 1.0);
        ReservaInsumoEntity reservaB = reservaActiva(101L, loteEntity, 1.0);
        ReservaInsumoEntity reservaC = reservaActiva(102L, loteEntity, 1.0);

        // === EJECUCION ===
        registrarEgresoConReservas(loteEntity, 0.0001, reservaA, reservaB, reservaC);

        // === ASSERTS ===
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(0.0001);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(reservaC.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(2.9999);
        assertThat(sumaPendiente(reservaA, reservaB, reservaC)).isEqualTo(loteEntity.getCantidadReservada());
    }

    @Test
    @DisplayName("CP-RA-35: registrarAjusteInsumo con EGRESO reparte entre reservas de distinto saldo asignando el sobrante al mayor resto sin perder cantidad")
    void registrarAjusteInsumo_debeRepartirEntreReservasDeDistintoSaldoConservandoElTotal() {
        // === PREPARACION DE DATOS ===
        // pendientes 1.0, 2.0 y 4.0 (total 7.0); excedente = 1.0 → partes enteras 0.1428, 0.2857 y 0.5714 (suman 0.9999)
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 7.0, 7.0);
        ReservaInsumoEntity reservaA = reservaActiva(100L, loteEntity, 1.0);
        ReservaInsumoEntity reservaB = reservaActiva(101L, loteEntity, 2.0);
        ReservaInsumoEntity reservaC = reservaActiva(102L, loteEntity, 4.0);

        // === EJECUCION ===
        registrarEgresoConReservas(loteEntity, 1.0, reservaA, reservaB, reservaC);

        // === ASSERTS ===
        // la unidad que sobra va a reservaA, que tiene el mayor resto
        assertThat(reservaA.getCantidadReducidaPorAjuste()).isEqualTo(0.1429);
        assertThat(reservaB.getCantidadReducidaPorAjuste()).isEqualTo(0.2857);
        assertThat(reservaC.getCantidadReducidaPorAjuste()).isEqualTo(0.5714);
        assertThat(sumaReducida(reservaA, reservaB, reservaC)).isEqualTo(1.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(6.0);
        assertThat(sumaPendiente(reservaA, reservaB, reservaC)).isEqualTo(loteEntity.getCantidadReservada());
    }

    @Test
    @DisplayName("CP-RA-36: registrarAjusteInsumo con EGRESO igual a toda la cantidad reservada agota todas las reservas y deja la cantidad reservada del lote en cero")
    void registrarAjusteInsumo_debeAgotarTodasLasReservasSiElExcedenteEsTodoLoReservado() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 3.0, 3.0);
        ReservaInsumoEntity reservaA = reservaActiva(100L, loteEntity, 1.0);
        ReservaInsumoEntity reservaB = reservaActiva(101L, loteEntity, 1.0);
        ReservaInsumoEntity reservaC = reservaActiva(102L, loteEntity, 1.0);

        // === EJECUCION ===
        registrarEgresoConReservas(loteEntity, 3.0, reservaA, reservaB, reservaC);

        // === ASSERTS ===
        for (ReservaInsumoEntity reserva : List.of(reservaA, reservaB, reservaC)) {
            assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
            assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(1.0);
            assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
        }
        assertThat(loteEntity.getCantidadActual()).isEqualTo(0.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(0.0);
    }

    private ReservaInsumoEntity reservaActiva(Long id, LoteInsumoEntity lote, double cantidad) {
        return ReservaInsumoEntity.builder().id(id).loteInsumo(lote).cantidadReservada(cantidad).cantidadPendiente(cantidad).build();
    }

    private void registrarEgresoConReservas(LoteInsumoEntity loteEntity, double cantidad, ReservaInsumoEntity... reservas) {
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(reservas));
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ajusteInsumoServicio.registrarAjusteInsumo(ajusteInsumoFormDTO(1L, 1L, cantidad, "Rotura parcial de lote"));
    }

    private static double sumaPendiente(ReservaInsumoEntity... reservas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (ReservaInsumoEntity reserva : reservas) {
            suma = suma.add(BigDecimal.valueOf(reserva.getCantidadPendiente()));
        }
        return suma.doubleValue();
    }

    private static double sumaReducida(ReservaInsumoEntity... reservas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (ReservaInsumoEntity reserva : reservas) {
            suma = suma.add(BigDecimal.valueOf(reserva.getCantidadReducidaPorAjuste()));
        }
        return suma.doubleValue();
    }

    @Test
    @DisplayName("CP-RA-28: registrarAjusteInsumo con INGRESO lanza ReglaNegocioException y no persiste cuando el lote de insumo está vencido")
    void registrarAjusteInsumo_debeRechazarIngresoSobreLoteVencido() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        loteEntity.setFechaVencimiento(LocalDate.now().minusDays(1));
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo está vencido (venció el "
                        + LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ") y no admite ajustes de ingreso")
                .extracting(e -> ((ReglaNegocioException) e).getCampo()).isEqualTo("idLoteInsumo");
        assertThat(loteEntity.getCantidadActual()).isEqualTo(20.0);
        verify(loteInsumoRepository, never()).save(any(LoteInsumoEntity.class));
        verify(ajusteInsumoRepository, never()).save(any(AjusteInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-RA-29: registrarAjusteInsumo con INGRESO sobre un lote de insumo vigente suma la cantidad")
    void registrarAjusteInsumo_debePermitirIngresoSobreLoteVigente() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        loteEntity.setFechaVencimiento(LocalDate.now().plusDays(1));
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(25.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-30: registrarAjusteInsumo con INGRESO sobre un lote de insumo que vence hoy lo permite (límite)")
    void registrarAjusteInsumo_debePermitirIngresoSobreLoteQueVenceHoy() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        loteEntity.setFechaVencimiento(LocalDate.now());
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(25.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-31: registrarAjusteInsumo con EGRESO sobre un lote de insumo vencido lo permite (merma)")
    void registrarAjusteInsumo_debePermitirEgresoSobreLoteVencido() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Merma por vencimiento");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        loteEntity.setFechaVencimiento(LocalDate.now().minusDays(10));
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularAjusteInsumo ====================

    @Test
    @DisplayName("CP-AA-01: anularAjusteInsumo lanza ReglaNegocioException y no consulta el repositorio cuando el motivo de anulación es nulo")
    void anularAjusteInsumo_debeRechazarMotivoAnulacionNulo() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO(null);

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ajusteInsumoRepository);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-02: anularAjusteInsumo lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anularAjusteInsumo_debeRechazarMotivoAnulacionEnBlanco() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("   ");

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-09: anularAjusteInsumo lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anularAjusteInsumo_debeRechazarMotivoAnulacionDe256Caracteres() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("a".repeat(256));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-03: anularAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el ajuste no existe")
    void anularAjusteInsumo_debeLanzarExcepcionSiNoExiste() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ajuste de insumo con ID: 99");

        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-04: anularAjusteInsumo lanza ReglaNegocioException cuando el ajuste ya está anulado")
    void anularAjusteInsumo_debeRechazarAjusteYaAnulado() {
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.ANULADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular ajustes de insumo en estado Registrado");

        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-05: anularAjusteInsumo lanza ReglaNegocioException al revertir un INGRESO cuya cantidad supera la cantidad actual del lote")
    void anularAjusteInsumo_debeRechazarReversionDeIngresoQueSuperaCantidadActual() {
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 15.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a descontar no puede superar la cantidad actual del lote de insumo (15 kg)");

        verify(loteInsumoRepository, never()).save(any());
        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-06: anularAjusteInsumo revierte exitosamente un ajuste INGRESO")
    void anularAjusteInsumo_debeRevertirAjusteIngresoExitosamente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 15.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 10.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(ajusteEntity)).thenReturn(ajusteEntity);

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO);

        // === ASSERTS ===
        verify(ajusteInsumoRepository, never()).findById(any());
        assertThat(loteEntity.getCantidadActual()).isEqualTo(5.0);
        assertThat(ajusteEntity.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(ajusteEntity.getFechaAnulacion()).isNotNull();
        assertThat(ajusteEntity.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de carga");
        verify(loteInsumoRepository).save(loteEntity);
    }

    @Test
    @DisplayName("CP-AA-07: anularAjusteInsumo revierte exitosamente un ajuste EGRESO (nunca queda negativo)")
    void anularAjusteInsumo_debeRevertirAjusteEgresoExitosamente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 5.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 10.0, motivoAjusteEntity(1L, TipoAjuste.EGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(ajusteEntity)).thenReturn(ajusteEntity);

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(ajusteEntity.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        // La anulación de un ajuste no restaura reservas: no se consulta ni se modifica ninguna
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-08: anularAjusteInsumo al revertir un INGRESO que excede la cantidad disponible reparte el excedente proporcionalmente entre las reservas activas (mismo mecanismo que un EGRESO nuevo)")
    void anularAjusteInsumo_debeRepartirExcedenteProporcionalmenteAlRevertirIngreso() {
        // === PREPARACION DE DATOS ===
        // disponible = 20.0 - 12.0 = 8.0; se revierte un INGRESO de 14.0 → excedente de 6.0 sobre lo reservado
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 12.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 14.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        ReservaInsumoEntity reservaA = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteEntity).cantidadReservada(8.0).cantidadPendiente(8.0).build();
        ReservaInsumoEntity reservaB = ReservaInsumoEntity.builder().id(101L).loteInsumo(loteEntity).cantidadReservada(4.0).cantidadPendiente(4.0).build();
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.buscarPorIdParaAjustar(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(1L)).thenReturn(List.of(reservaA, reservaB));
        when(ajusteInsumoRepository.save(ajusteEntity)).thenReturn(ajusteEntity);

        // === EJECUCION ===
        ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(6.0);
        assertThat(loteEntity.getCantidadReservada()).isEqualTo(6.0);
        assertThat(reservaA.getCantidadPendiente()).isEqualTo(4.0);
        assertThat(reservaB.getCantidadPendiente()).isEqualTo(2.0);
        verify(reservaInsumoRepository).saveAll(List.of(reservaA, reservaB));
    }

    @Test
    @DisplayName("CP-AA-10: anularAjusteInsumo lanza ReglaNegocioException y no consulta el repositorio cuando el ID es nulo")
    void anularAjusteInsumo_debeRechazarIdNulo() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(null, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El ajuste de insumo es obligatorio");

        verifyNoInteractions(ajusteInsumoRepository);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-11: anularAjusteInsumo lanza ReglaNegocioException y no consulta el repositorio cuando los datos de la anulación son nulos")
    void anularAjusteInsumo_debeRechazarDtoNulo() {
        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los datos de la anulación del ajuste de insumo son obligatorios");

        verifyNoInteractions(ajusteInsumoRepository);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-12: anularAjusteInsumo indica el campo motivoAnulacion en el rechazo del motivo vacío o demasiado largo")
    void anularAjusteInsumo_debeIndicarElCampoMotivoAnulacion() {
        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, anularFormDTO("   ")))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));
        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, anularFormDTO("a".repeat(256))))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-13: anularAjusteInsumo lee el ajuste con bloqueo de escritura antes de validar su estado, sin usar la lectura sin bloqueo")
    void anularAjusteInsumo_debeLeerElAjusteConBloqueoAntesDeValidarElEstado() {
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.ANULADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        when(ajusteInsumoRepository.buscarPorIdParaAnular(1L)).thenReturn(Optional.of(ajusteEntity));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular ajustes de insumo en estado Registrado");

        verify(ajusteInsumoRepository).buscarPorIdParaAnular(1L);
        verify(ajusteInsumoRepository, never()).findById(any());
        verifyNoInteractions(loteInsumoRepository);
    }

    // ==================== helpers de construcción ====================

    private static AjusteInsumoFormDTO ajusteInsumoFormDTO(Long idMotivoAjuste, Long idLoteInsumo, Double cantidad, String observacion) {
        return AjusteInsumoFormDTO.builder()
                .idMotivoAjuste(idMotivoAjuste)
                .idLoteInsumo(idLoteInsumo)
                .cantidad(cantidad)
                .observacion(observacion)
                .build();
    }

    private static AnularAjusteInsumoFormDTO anularFormDTO(String motivoAnulacion) {
        return AnularAjusteInsumoFormDTO.builder()
                .motivoAnulacion(motivoAnulacion)
                .build();
    }

    private static InsumoEntity insumoEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
    }

    private static MotivoAjusteEntity motivoAjusteEntity(Long id, TipoAjuste tipoAjuste) {
        return MotivoAjusteEntity.builder()
                .id(id)
                .nombre(tipoAjuste == TipoAjuste.INGRESO ? "Corrección de conteo" : "Rotura de lote")
                .tipoAjuste(tipoAjuste)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, InsumoEntity insumo, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor("LOTE-" + id)
                .fechaVencimiento(java.time.LocalDate.now().plusMonths(6))
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }

    private static AjusteInsumoEntity ajusteInsumoEntity(Long id, EstadoTransaccion estado, Double cantidad, MotivoAjusteEntity motivoAjuste, LoteInsumoEntity loteInsumo) {
        return AjusteInsumoEntity.builder()
                .id(id)
                .cantidad(cantidad)
                .observacion("Observación de prueba")
                .motivoAjuste(motivoAjuste)
                .loteInsumo(loteInsumo)
                .estado(estado)
                .build();
    }
}
