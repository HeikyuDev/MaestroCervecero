package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardOperarioResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardOperarioServicioImplTest {

    private static final Pageable TOPE = PageRequest.of(0, 5);

    @Mock
    private ILoteRepository loteRepository;

    @Mock
    private IControlEquipamientoServicio controlEquipamientoServicio;

    @Mock
    private IControlBarrilServicio controlBarrilServicio;

    @Mock
    private IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;

    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;

    @InjectMocks
    private DashboardOperarioServicioImpl dashboardOperarioServicio;

    // ==================== obtenerDashboardOperario ====================

    @Test
    @DisplayName("CP-DOP-01: obtenerDashboardOperario con el sistema vacío devuelve los seis bloques en cero y sin filas")
    void obtener_debeDevolverLosSeisBloquesVaciosConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesEnEjecucion().getTotal()).isZero();
        assertThat(resultado.getLotesEnEjecucion().getFilas()).isEmpty();
        assertThat(resultado.getLotesAIniciar().getTotal()).isZero();
        assertThat(resultado.getLotesAIniciar().getFilas()).isEmpty();
        assertThat(resultado.getEquiposEnLimpieza().getTotal()).isZero();
        assertThat(resultado.getEquiposEnLimpieza().getFilas()).isEmpty();
        assertThat(resultado.getBarrilesEnLimpieza().getTotal()).isZero();
        assertThat(resultado.getBarrilesEnLimpieza().getFilas()).isEmpty();
        assertThat(resultado.getEquiposEnMantenimiento().getTotal()).isZero();
        assertThat(resultado.getEquiposEnMantenimiento().getFilas()).isEmpty();
        assertThat(resultado.getBarrilesEnMantenimiento().getTotal()).isZero();
        assertThat(resultado.getBarrilesEnMantenimiento().getFilas()).isEmpty();
        verify(loteRepository, never()).buscarParaListadoPorIds(anyCollection());
        verify(operacionCicloVidaEquipamientoRepository, never()).buscarUltimasOperacionesRegistradas(anyCollection());
        verify(operacionCicloVidaBarrilRepository, never()).buscarUltimasOperacionesRegistradas(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-02: los lotes en ejecución con 5 o menos filas muestran todos, en el orden de los IDs de la página, con total igual a las filas")
    void obtener_debeMostrarTodosLosLotesEnEjecucionEnElOrdenDeLaPagina() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(TOPE)).thenReturn(new PageImpl<>(List.of(2L, 1L, 3L), TOPE, 3));
        when(loteRepository.buscarParaListadoPorIds(List.of(2L, 1L, 3L))).thenReturn(List.of(
                lote(1L, EstadoLote.EN_EJECUCION, hoy.minusDays(10), hoy.plusDays(2), TipoEtapa.FERMENTACION),
                lote(2L, EstadoLote.EN_EJECUCION, hoy.minusDays(20), hoy.minusDays(1), TipoEtapa.HERVIDO),
                lote(3L, EstadoLote.EN_EJECUCION, hoy.minusDays(5), hoy.plusDays(9), TipoEtapa.MACERACION)));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesEnEjecucion().getTotal()).isEqualTo(3);
        assertThat(resultado.getLotesEnEjecucion().getFilas()).extracting(LoteDelDiaResponseDTO::getId).containsExactly(2L, 1L, 3L);
        LoteDelDiaResponseDTO primera = resultado.getLotesEnEjecucion().getFilas().get(0);
        assertThat(primera.getIdentificadorInterno()).isEqualTo("LOT-R1-02");
        assertThat(primera.getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(primera.getEtapaActual()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(primera.getIdFermentador()).isEqualTo(4L);
        assertThat(primera.getIdentificadorInternoFermentador()).isEqualTo("FER-04");
        verify(loteRepository, times(1)).buscarParaListadoPorIds(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-03: los lotes en ejecución con más de 5 muestran 5 filas y el total real")
    void obtener_debeTopearLasFilasDeLotesEnEjecucionYConservarElTotal() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L);
        when(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(TOPE)).thenReturn(new PageImpl<>(ids, TOPE, 8));
        when(loteRepository.buscarParaListadoPorIds(ids)).thenReturn(lotesEnEjecucion(ids, hoy.plusDays(1)));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesEnEjecucion().getTotal()).isEqualTo(8);
        assertThat(resultado.getLotesEnEjecucion().getFilas()).hasSize(5);
        verify(loteRepository).buscarIdsDeLotesEnEjecucionParaPanel(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("CP-DOP-19: si la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y el total se conserva")
    void obtener_noDebeFallarSiLaCargaNoDevuelveAlgunLote() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesAIniciarParaPanel(hoy, TOPE)).thenReturn(new PageImpl<>(List.of(7L, 8L), TOPE, 2));
        when(loteRepository.buscarParaListadoPorIds(List.of(7L, 8L))).thenReturn(List.of(
                lote(7L, EstadoLote.PENDIENTE, hoy, hoy.plusDays(10), null)));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesAIniciar().getTotal()).isEqualTo(2);
        assertThat(resultado.getLotesAIniciar().getFilas()).extracting(LoteDelDiaResponseDTO::getId).containsExactly(7L);
    }

    @Test
    @DisplayName("CP-DOP-08: los lotes a iniciar se piden con la fecha de hoy como límite, con etapa en curso nula")
    void obtener_debeMostrarLosLotesAIniciarConLaFechaDeHoyComoLimite() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesAIniciarParaPanel(hoy, TOPE)).thenReturn(new PageImpl<>(List.of(7L, 8L), TOPE, 2));
        when(loteRepository.buscarParaListadoPorIds(List.of(7L, 8L))).thenReturn(List.of(
                lote(8L, EstadoLote.PENDIENTE, hoy, hoy.minusDays(4), null),
                lote(7L, EstadoLote.PENDIENTE, hoy.minusDays(3), hoy.minusDays(10), null)));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesAIniciar().getTotal()).isEqualTo(2);
        assertThat(resultado.getLotesAIniciar().getFilas()).extracting(LoteDelDiaResponseDTO::getId).containsExactly(7L, 8L);
        assertThat(resultado.getLotesAIniciar().getFilas().get(0).getEtapaActual()).isNull();
        verify(loteRepository).buscarIdsDeLotesAIniciarParaPanel(hoy, PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("CP-DOP-09: los lotes a iniciar con más de 5 muestran 5 filas y el total real")
    void obtener_debeTopearLasFilasDeLotesAIniciarYConservarElTotal() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L);
        when(loteRepository.buscarIdsDeLotesAIniciarParaPanel(hoy, TOPE)).thenReturn(new PageImpl<>(ids, TOPE, 9));
        when(loteRepository.buscarParaListadoPorIds(ids)).thenReturn(ids.stream()
                .map(id -> lote(id, EstadoLote.PENDIENTE, hoy, hoy.plusDays(20), null)).toList());

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getLotesAIniciar().getTotal()).isEqualTo(9);
        assertThat(resultado.getLotesAIniciar().getFilas()).hasSize(5);
    }

    @Test
    @DisplayName("CP-DOP-10: los equipos y barriles en limpieza con 5 o menos filas se muestran todos, sin causa y sin consultar operaciones")
    void obtener_debeMostrarLosEquiposYBarrilesEnLimpiezaSinCausa() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_LIMPIEZA, TOPE))
                .thenReturn(new PageImpl<>(List.of(equipo(3L, "FER-01"), equipo(5L, "FER-02")), TOPE, 2));
        when(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.EN_LIMPIEZA, TOPE))
                .thenReturn(new PageImpl<>(List.of(barril(8L, "BAR-08")), TOPE, 1));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getEquiposEnLimpieza().getTotal()).isEqualTo(2);
        assertThat(resultado.getEquiposEnLimpieza().getFilas()).extracting(EquipoPendienteResponseDTO::getIdentificadorInterno).containsExactly("FER-01", "FER-02");
        assertThat(resultado.getEquiposEnLimpieza().getFilas().get(0).getTipo()).isEqualTo(TipoEquipamiento.FERMENTADOR);
        assertThat(resultado.getEquiposEnLimpieza().getFilas()).extracting(EquipoPendienteResponseDTO::getCausaMantenimiento).containsOnlyNulls();
        assertThat(resultado.getBarrilesEnLimpieza().getTotal()).isEqualTo(1);
        assertThat(resultado.getBarrilesEnLimpieza().getFilas().get(0).getFabricante()).isEqualTo("Fabricante Sur");
        assertThat(resultado.getBarrilesEnLimpieza().getFilas().get(0).getCausaMantenimiento()).isNull();
        verify(operacionCicloVidaEquipamientoRepository, never()).buscarUltimasOperacionesRegistradas(anyCollection());
        verify(operacionCicloVidaBarrilRepository, never()).buscarUltimasOperacionesRegistradas(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-11: los equipos y barriles en limpieza con más de 5 muestran las 5 filas de la página y el total real, con el tope de 5 filas")
    void obtener_debeConservarElTotalRealDeEquiposYBarrilesEnLimpieza() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_LIMPIEZA, TOPE))
                .thenReturn(new PageImpl<>(equipos(1, 5), TOPE, 12));
        when(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.EN_LIMPIEZA, TOPE))
                .thenReturn(new PageImpl<>(barriles(1, 5), TOPE, 7));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getEquiposEnLimpieza().getTotal()).isEqualTo(12);
        assertThat(resultado.getEquiposEnLimpieza().getFilas()).hasSize(5);
        assertThat(resultado.getBarrilesEnLimpieza().getTotal()).isEqualTo(7);
        assertThat(resultado.getBarrilesEnLimpieza().getFilas()).hasSize(5);
        verify(controlEquipamientoServicio).filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_LIMPIEZA, PageRequest.of(0, 5));
        verify(controlBarrilServicio).filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.EN_LIMPIEZA, PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("CP-DOP-12: la causa del mantenimiento de un equipo sale de su última operación registrada: falla es Falla, limpieza que lo dejó en mantenimiento es Preventivo y sin operación es nula")
    void obtener_debeDerivarLaCausaDelMantenimientoDeLosEquipos() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_MANTENIMIENTO, TOPE))
                .thenReturn(new PageImpl<>(List.of(equipo(3L, "FER-01"), equipo(5L, "FER-02"), equipo(6L, "FER-03")), TOPE, 3));
        when(operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(List.of(3L, 5L, 6L))).thenReturn(List.of(
                FallaEquipamientoEntity.builder().id(30L).equipamiento(FermentadorEntity.builder().id(3L).build()).build(),
                LimpiezaEquipamientoEntity.builder().id(50L).estadoOperativoResultante(EstadoOperativo.EN_MANTENIMIENTO)
                        .equipamiento(FermentadorEntity.builder().id(5L).build()).build()));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getEquiposEnMantenimiento().getFilas()).extracting(EquipoPendienteResponseDTO::getCausaMantenimiento)
                .containsExactly(CausaMantenimiento.FALLA, CausaMantenimiento.PREVENTIVO, null);
        verify(operacionCicloVidaEquipamientoRepository, times(1)).buscarUltimasOperacionesRegistradas(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-13: la causa del mantenimiento de un barril sale de su última operación registrada: falla es Falla, limpieza que lo dejó en mantenimiento es Preventivo y sin operación es nula")
    void obtener_debeDerivarLaCausaDelMantenimientoDeLosBarriles() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.EN_MANTENIMIENTO, TOPE))
                .thenReturn(new PageImpl<>(List.of(barril(8L, "BAR-08"), barril(9L, "BAR-09"), barril(10L, "BAR-10")), TOPE, 3));
        when(operacionCicloVidaBarrilRepository.buscarUltimasOperacionesRegistradas(List.of(8L, 9L, 10L))).thenReturn(List.of(
                FallaBarrilEntity.builder().id(80L).barril(BarrilEntity.builder().id(8L).build()).build(),
                LimpiezaBarrilEntity.builder().id(90L).estadoOperativoResultante(EstadoOperativoBarril.EN_MANTENIMIENTO)
                        .barril(BarrilEntity.builder().id(9L).build()).build()));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getBarrilesEnMantenimiento().getFilas()).extracting(BarrilPendienteResponseDTO::getCausaMantenimiento)
                .containsExactly(CausaMantenimiento.FALLA, CausaMantenimiento.PREVENTIVO, null);
        verify(operacionCicloVidaBarrilRepository, times(1)).buscarUltimasOperacionesRegistradas(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-14: la causa se deriva solo de la operación que devuelve la consulta de registradas: una limpieza que no lo dejó en mantenimiento da causa nula")
    void obtener_noDebeDerivarCausaDeUnaLimpiezaQueNoDejoEnMantenimiento() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_MANTENIMIENTO, TOPE))
                .thenReturn(new PageImpl<>(List.of(equipo(3L, "FER-01")), TOPE, 1));
        when(operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(List.of(3L))).thenReturn(List.of(
                LimpiezaEquipamientoEntity.builder().id(30L).estadoOperativoResultante(EstadoOperativo.DISPONIBLE)
                        .equipamiento(FermentadorEntity.builder().id(3L).build()).build()));

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getEquiposEnMantenimiento().getFilas().get(0).getCausaMantenimiento()).isNull();
    }

    @Test
    @DisplayName("CP-DOP-15: los equipos y barriles en mantenimiento con más de 5 muestran 5 filas y el total real, y la última operación se consulta solo para las filas visibles")
    void obtener_debeTopearLasFilasDeMantenimientoYConsultarSoloLasVisibles() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, EstadoOperativo.EN_MANTENIMIENTO, TOPE))
                .thenReturn(new PageImpl<>(equipos(1, 5), TOPE, 11));
        when(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.EN_MANTENIMIENTO, TOPE))
                .thenReturn(new PageImpl<>(barriles(1, 5), TOPE, 6));
        when(operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(List.of());
        when(operacionCicloVidaBarrilRepository.buscarUltimasOperacionesRegistradas(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(List.of());

        // === EJECUCION ===
        DashboardOperarioResponseDTO resultado = dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        assertThat(resultado.getEquiposEnMantenimiento().getTotal()).isEqualTo(11);
        assertThat(resultado.getEquiposEnMantenimiento().getFilas()).hasSize(5);
        assertThat(resultado.getBarrilesEnMantenimiento().getTotal()).isEqualTo(6);
        assertThat(resultado.getBarrilesEnMantenimiento().getFilas()).hasSize(5);
        verify(operacionCicloVidaEquipamientoRepository, times(1)).buscarUltimasOperacionesRegistradas(anyCollection());
        verify(operacionCicloVidaBarrilRepository, times(1)).buscarUltimasOperacionesRegistradas(anyCollection());
    }

    @Test
    @DisplayName("CP-DOP-16: sin equipos ni barriles en mantenimiento no se consulta ninguna operación")
    void obtener_noDebeConsultarOperacionesSinEquiposNiBarrilesEnMantenimiento() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        dashboardOperarioServicio.obtenerDashboardOperario();

        // === ASSERTS ===
        verify(operacionCicloVidaEquipamientoRepository, never()).buscarUltimasOperacionesRegistradas(anyList());
        verify(operacionCicloVidaBarrilRepository, never()).buscarUltimasOperacionesRegistradas(anyList());
    }

    // ==================== helpers ====================

    /** Deja los seis bloques vacíos; cada test pisa solo el bloque que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), TOPE, 0));
        lenient().when(loteRepository.buscarIdsDeLotesAIniciarParaPanel(any(LocalDate.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), TOPE, 0));
        lenient().when(controlEquipamientoServicio.filtrarEquipamientosParaControl(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(controlBarrilServicio.filtrarBarrilesParaControl(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
    }

    private static List<LoteEntity> lotesEnEjecucion(List<Long> ids, LocalDate fin) {
        return ids.stream()
                .map(id -> lote(id, EstadoLote.EN_EJECUCION, LocalDate.now().minusDays(5), fin, TipoEtapa.HERVIDO))
                .toList();
    }

    private static LoteEntity lote(Long id, EstadoLote estado, LocalDate inicioEstimado, LocalDate finEstimado, TipoEtapa etapaEnCurso) {
        LoteEntity lote = LoteEntity.builder()
                .id(id)
                .identificadorInterno("LOT-R1-0" + id)
                .estado(estado)
                .fechaInicioEstimada(inicioEstimado)
                .fechaFinalizacionEstimada(finEstimado)
                .planificacionProduccion(PlanificacionProduccionEntity.builder().id(1L)
                        .versionReceta(VersionRecetaEntity.builder().nombre("Pale Ale").build()).build())
                .build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(EtapaLoteEntity.builder().etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote)
                .equipamiento(FermentadorEntity.builder().id(4L).identificadorInterno("FER-04").build()).build());
        if (etapaEnCurso != null && etapaEnCurso != TipoEtapa.FERMENTACION) {
            etapas.add(EtapaLoteEntity.builder().etapa(etapaEnCurso).estado(EstadoEtapaLote.EN_CURSO).lote(lote).build());
        } else if (etapaEnCurso == TipoEtapa.FERMENTACION) {
            etapas.set(0, EtapaLoteEntity.builder().etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.EN_CURSO).lote(lote)
                    .equipamiento(FermentadorEntity.builder().id(4L).identificadorInterno("FER-04").build()).build());
        }
        lote.setEtapas(etapas);
        return lote;
    }

    private static EquipamientoControlResponseDTO equipo(Long id, String identificador) {
        return EquipamientoControlResponseDTO.builder().id(id).identificadorInterno(identificador).tipo(TipoEquipamiento.FERMENTADOR).build();
    }

    private static BarrilControlResponseDTO barril(Long id, String identificador) {
        return BarrilControlResponseDTO.builder().id(id).identificadorInterno(identificador).fabricante("Fabricante Sur").build();
    }

    private static List<EquipamientoControlResponseDTO> equipos(long desde, long hasta) {
        return LongStream.rangeClosed(desde, hasta).mapToObj(id -> equipo(id, "FER-0" + id)).toList();
    }

    private static List<BarrilControlResponseDTO> barriles(long desde, long hasta) {
        return LongStream.rangeClosed(desde, hasta).mapToObj(id -> barril(id, "BAR-0" + id)).toList();
    }
}
