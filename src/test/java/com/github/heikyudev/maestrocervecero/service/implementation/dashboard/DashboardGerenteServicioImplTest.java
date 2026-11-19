package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ColaFermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EtapaEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteEnProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionConLitrosAPlanificarResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGerenteServicioImplTest {

    private static final Pageable TOPE = PageRequest.of(0, 5);
    private static final Pageable SOLO_EL_TOTAL = PageRequest.of(0, 1);

    @Mock
    private ILoteRepository loteRepository;

    @Mock
    private IControlEquipamientoServicio controlEquipamientoServicio;

    @Mock
    private IControlBarrilServicio controlBarrilServicio;

    @Mock
    private IPlanificacionProduccionServicio planificacionProduccionServicio;

    @Mock
    private IPlanProduccionServicio planProduccionServicio;

    @InjectMocks
    private DashboardGerenteServicioImpl dashboardGerenteServicio;

    // ==================== obtenerDashboardGerente ====================

    @Test
    @DisplayName("CP-DGE-01: obtenerDashboardGerente con el sistema vacío devuelve contadores en cero y bloques sin filas")
    void obtener_debeDevolverElPanelEnCeroConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getLotesEnEjecucion()).isZero();
        assertThat(resultado.getLotesAtrasados()).isZero();
        assertThat(resultado.getFermentadoresOcupados()).isZero();
        assertThat(resultado.getFermentadoresTotal()).isZero();
        assertThat(resultado.getFueraDeServicio()).isZero();
        assertThat(resultado.getProduccionEnCurso()).isEmpty();
        assertThat(resultado.getColaDeFermentadores().getTotal()).isZero();
        assertThat(resultado.getColaDeFermentadores().getFilas()).isEmpty();
        assertThat(resultado.getPlanificacionesPendientes().getTotal()).isZero();
        assertThat(resultado.getPlanificacionesPendientes().getFilas()).isEmpty();
        verify(loteRepository, never()).buscarParaListadoPorIds(anyCollection());
        verify(loteRepository, never()).buscarVivosDeFermentadores(anyCollection());
    }

    @Test
    @DisplayName("CP-DGE-02: los contadores informan el total real de lotes en ejecución, los atrasados, los fermentadores ocupados sobre el total y la suma de equipos y barriles en limpieza y en mantenimiento")
    void obtener_debeCalcularLosContadores() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(SOLO_EL_TOTAL)).thenReturn(new PageImpl<>(List.of(1L), SOLO_EL_TOTAL, 7));
        when(loteRepository.contarLotesEnEjecucionAtrasados(hoy)).thenReturn(2L);
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), eq(TipoEquipamiento.FERMENTADOR), isNull(), eq(TOPE)))
                .thenReturn(new PageImpl<>(List.of(), TOPE, 5));
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), eq(TipoEquipamiento.FERMENTADOR), eq(EstadoOperativo.EN_USO), eq(SOLO_EL_TOTAL)))
                .thenReturn(paginaConTotal(3));
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), isNull(), eq(EstadoOperativo.EN_LIMPIEZA), eq(SOLO_EL_TOTAL))).thenReturn(paginaConTotal(2));
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), isNull(), eq(EstadoOperativo.EN_MANTENIMIENTO), eq(SOLO_EL_TOTAL))).thenReturn(paginaConTotal(1));
        when(controlBarrilServicio.filtrarBarrilesParaControl(isNull(), isNull(), eq(EstadoOperativoBarril.EN_LIMPIEZA), eq(SOLO_EL_TOTAL))).thenReturn(paginaDeBarrilesConTotal(4));
        when(controlBarrilServicio.filtrarBarrilesParaControl(isNull(), isNull(), eq(EstadoOperativoBarril.EN_MANTENIMIENTO), eq(SOLO_EL_TOTAL))).thenReturn(paginaDeBarrilesConTotal(3));

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getLotesEnEjecucion()).isEqualTo(7);
        assertThat(resultado.getLotesAtrasados()).isEqualTo(2);
        assertThat(resultado.getFermentadoresOcupados()).isEqualTo(3);
        assertThat(resultado.getFermentadoresTotal()).isEqualTo(5);
        assertThat(resultado.getFueraDeServicio()).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-DGE-03: la producción en curso agrupa los lotes por etapa en el orden de las etapas, omite las etapas sin lotes y trae el total real, el atraso y la prioridad de cada fila")
    void obtener_debeAgruparLaProduccionEnCursoPorEtapa() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(TipoEtapa.HERVIDO, TOPE)).thenReturn(new PageImpl<>(List.of(1L), TOPE, 1));
        when(loteRepository.buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(TipoEtapa.FERMENTACION, TOPE)).thenReturn(new PageImpl<>(List.of(3L, 2L), TOPE, 8));
        when(loteRepository.buscarParaListadoPorIds(List.of(1L))).thenReturn(List.of(
                lote(1L, EstadoLote.EN_EJECUCION, hoy.minusDays(5), hoy.plusDays(2), TipoEtapa.HERVIDO, PrioridadPlanificacion.MEDIA)));
        when(loteRepository.buscarParaListadoPorIds(List.of(3L, 2L))).thenReturn(List.of(
                lote(2L, EstadoLote.EN_EJECUCION, hoy.minusDays(20), hoy.plusDays(1), TipoEtapa.FERMENTACION, PrioridadPlanificacion.BAJA),
                lote(3L, EstadoLote.EN_EJECUCION, hoy.minusDays(30), hoy.minusDays(4), TipoEtapa.FERMENTACION, PrioridadPlanificacion.ALTA)));

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getProduccionEnCurso()).extracting(EtapaEnCursoResponseDTO::getEtapa).containsExactly(TipoEtapa.HERVIDO, TipoEtapa.FERMENTACION);
        EtapaEnCursoResponseDTO hervido = resultado.getProduccionEnCurso().get(0);
        assertThat(hervido.getLotes().getTotal()).isEqualTo(1);
        assertThat(hervido.getLotes().getFilas().get(0).getDiasDeAtraso()).isZero();
        EtapaEnCursoResponseDTO fermentacion = resultado.getProduccionEnCurso().get(1);
        assertThat(fermentacion.getLotes().getTotal()).isEqualTo(8);
        assertThat(fermentacion.getLotes().getFilas()).extracting(LoteEnProduccionResponseDTO::getId).containsExactly(3L, 2L);
        LoteEnProduccionResponseDTO atrasado = fermentacion.getLotes().getFilas().get(0);
        assertThat(atrasado.getIdentificadorInterno()).isEqualTo("LOT-R1-03");
        assertThat(atrasado.getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(atrasado.getEtapaActual()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(atrasado.getDiasDeAtraso()).isEqualTo(4);
        assertThat(atrasado.getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
        assertThat(fermentacion.getLotes().getFilas().get(1).getDiasDeAtraso()).isZero();
        assertThat(fermentacion.getLotes().getFilas().get(1).getPrioridad()).isEqualTo(PrioridadPlanificacion.BAJA);
    }

    @Test
    @DisplayName("CP-DGE-04: la cola de fermentadores trae por cada fermentador su lote en ejecución con su atraso, su próximo lote pendiente y la cantidad de pendientes, con una sola consulta de lotes vivos")
    void obtener_debeArmarLaColaDeCadaFermentador() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        EquipamientoControlResponseDTO fer4 = fermentador(4L, "FER-04", EstadoOperativo.EN_USO);
        EquipamientoControlResponseDTO fer5 = fermentador(5L, "FER-05", EstadoOperativo.DISPONIBLE);
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), eq(TipoEquipamiento.FERMENTADOR), isNull(), eq(TOPE)))
                .thenReturn(new PageImpl<>(List.of(fer4, fer5), TOPE, 9));
        when(loteRepository.buscarVivosDeFermentadores(List.of(4L, 5L))).thenReturn(List.of(
                loteEnFermentador(1L, 4L, EstadoLote.EN_EJECUCION, hoy.minusDays(10), hoy.minusDays(2)),
                loteEnFermentador(2L, 4L, EstadoLote.PENDIENTE, hoy.plusDays(1), hoy.plusDays(9)),
                loteEnFermentador(3L, 4L, EstadoLote.PENDIENTE, hoy.plusDays(12), hoy.plusDays(20))));

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getColaDeFermentadores().getTotal()).isEqualTo(9);
        assertThat(resultado.getColaDeFermentadores().getFilas()).extracting(ColaFermentadorResponseDTO::getIdentificadorInterno).containsExactly("FER-04", "FER-05");
        ColaFermentadorResponseDTO cola = resultado.getColaDeFermentadores().getFilas().get(0);
        assertThat(cola.getId()).isEqualTo(4L);
        assertThat(cola.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        assertThat(cola.getLoteEnEjecucion().getId()).isEqualTo(1L);
        assertThat(cola.getLoteEnEjecucion().getDiasDeAtraso()).isEqualTo(2);
        assertThat(cola.getProximoLote().getId()).isEqualTo(2L);
        assertThat(cola.getProximoLote().getDiasDeAtraso()).isZero();
        assertThat(cola.getLotesPendientes()).isEqualTo(2);
        verify(loteRepository).buscarVivosDeFermentadores(List.of(4L, 5L));
    }

    @Test
    @DisplayName("CP-DGE-05: un fermentador sin lotes vivos se informa con lote en ejecución y próximo lote nulos y 0 pendientes")
    void obtener_debeInformarUnFermentadorSinLotes() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(controlEquipamientoServicio.filtrarEquipamientosParaControl(isNull(), eq(TipoEquipamiento.FERMENTADOR), isNull(), eq(TOPE)))
                .thenReturn(new PageImpl<>(List.of(fermentador(4L, "FER-04", EstadoOperativo.DISPONIBLE)), TOPE, 1));
        when(loteRepository.buscarVivosDeFermentadores(List.of(4L))).thenReturn(List.of());

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        ColaFermentadorResponseDTO cola = resultado.getColaDeFermentadores().getFilas().get(0);
        assertThat(cola.getLoteEnEjecucion()).isNull();
        assertThat(cola.getProximoLote()).isNull();
        assertThat(cola.getLotesPendientes()).isZero();
    }

    @Test
    @DisplayName("CP-DGE-08: las planificaciones pendientes se piden con estado Pendiente y el tope de 5 filas, y se informa el total real")
    void obtener_debeInformarLasPlanificacionesPendientes() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        PlanificacionProduccionResponseDTO plan = PlanificacionProduccionResponseDTO.builder().id(7L).estado(EstadoSolicitud.PENDIENTE)
                .prioridad(PrioridadPlanificacion.ALTA).fechaInicioEstimada(LocalDate.of(2027, 8, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 8, 20)).build();
        when(planificacionProduccionServicio.filtrarPlanificacionesProduccion(null, EstadoSolicitud.PENDIENTE, null, TOPE))
                .thenReturn(new PageImpl<>(List.of(plan), TOPE, 12));

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getPlanificacionesPendientes().getTotal()).isEqualTo(12);
        assertThat(resultado.getPlanificacionesPendientes().getFilas()).hasSize(1);
        assertThat(resultado.getPlanificacionesPendientes().getFilas().get(0).getId()).isEqualTo(7L);
        assertThat(resultado.getPlanificacionesPendientes().getFilas().get(0).getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
        assertThat(resultado.getPlanificacionesPendientes().getFilas().get(0).getFechaInicioEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
    }

    @Test
    @DisplayName("CP-DGE-11: si la carga no devuelve alguno de los IDs de la página de una etapa, ese lote no se muestra y el total sigue siendo el real")
    void obtener_noDebeMostrarUnLoteQueLaCargaNoDevuelve() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(TipoEtapa.HERVIDO, TOPE)).thenReturn(new PageImpl<>(List.of(1L, 2L), TOPE, 2));
        when(loteRepository.buscarParaListadoPorIds(List.of(1L, 2L))).thenReturn(List.of(
                lote(2L, EstadoLote.EN_EJECUCION, hoy.minusDays(5), hoy.plusDays(8), TipoEtapa.HERVIDO, PrioridadPlanificacion.MEDIA)));

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getProduccionEnCurso()).hasSize(1);
        assertThat(resultado.getProduccionEnCurso().get(0).getLotes().getTotal()).isEqualTo(2);
        assertThat(resultado.getProduccionEnCurso().get(0).getLotes().getFilas()).extracting(LoteEnProduccionResponseDTO::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("CP-DGE-12: el aviso de planificaciones con litros sin planificar trae todas las que informa el servicio de planes de producción, en su mismo orden y sin tope de filas")
    void obtener_debeInformarLasPlanificacionesConLitrosAPlanificar() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        List<PlanificacionConLitrosAPlanificarResponseDTO> planificaciones = new java.util.ArrayList<>();
        for (long id = 1; id <= 6; id++) {
            planificaciones.add(PlanificacionConLitrosAPlanificarResponseDTO.builder().id(id).etiqueta("Plan #" + id + " - Pale Ale")
                    .fechaInicioEstimada(LocalDate.of(2027, 8, (int) id)).litrosAPlanificar(100.0 * id).build());
        }
        when(planProduccionServicio.buscarPlanificacionesConLitrosAPlanificar()).thenReturn(planificaciones);

        // === EJECUCION ===
        DashboardGerenteResponseDTO resultado = dashboardGerenteServicio.obtenerDashboardGerente();

        // === ASSERTS ===
        assertThat(resultado.getPlanificacionesConLitrosAPlanificar()).hasSize(6)
                .extracting(PlanificacionConLitrosAPlanificarResponseDTO::getId).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(resultado.getPlanificacionesConLitrosAPlanificar().get(0).getLitrosAPlanificar()).isEqualTo(100.0);
    }

    // ==================== helpers ====================

    /** Deja todos los bloques y conteos en cero; cada test pisa solo lo que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), SOLO_EL_TOTAL, 0));
        lenient().when(loteRepository.contarLotesEnEjecucionAtrasados(any(LocalDate.class))).thenReturn(0L);
        lenient().when(loteRepository.buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(any(TipoEtapa.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), TOPE, 0));
        lenient().when(controlEquipamientoServicio.filtrarEquipamientosParaControl(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(controlBarrilServicio.filtrarBarrilesParaControl(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(planificacionProduccionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
    }

    private static Page<EquipamientoControlResponseDTO> paginaConTotal(long total) {
        return new PageImpl<>(List.of(), SOLO_EL_TOTAL, total);
    }

    private static Page<com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO> paginaDeBarrilesConTotal(long total) {
        return new PageImpl<>(List.of(), SOLO_EL_TOTAL, total);
    }

    private static EquipamientoControlResponseDTO fermentador(Long id, String identificador, EstadoOperativo estado) {
        return EquipamientoControlResponseDTO.builder().id(id).identificadorInterno(identificador).tipo(TipoEquipamiento.FERMENTADOR).estadoOperativo(estado).build();
    }

    private static LoteEntity loteEnFermentador(Long id, Long idFermentador, EstadoLote estado, LocalDate inicioEstimado, LocalDate finEstimado) {
        LoteEntity lote = lote(id, estado, inicioEstimado, finEstimado, null, PrioridadPlanificacion.MEDIA);
        lote.setEtapas(new ArrayList<>(List.of(EtapaLoteEntity.builder().etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote)
                .equipamiento(FermentadorEntity.builder().id(idFermentador).identificadorInterno("FER-0" + idFermentador).build()).build())));
        return lote;
    }

    private static LoteEntity lote(Long id, EstadoLote estado, LocalDate inicioEstimado, LocalDate finEstimado, TipoEtapa etapaEnCurso, PrioridadPlanificacion prioridad) {
        LoteEntity lote = LoteEntity.builder()
                .id(id)
                .identificadorInterno("LOT-R1-0" + id)
                .estado(estado)
                .fechaInicioEstimada(inicioEstimado)
                .fechaFinalizacionEstimada(finEstimado)
                .planificacionProduccion(PlanificacionProduccionEntity.builder().id(1L).prioridad(prioridad)
                        .versionReceta(VersionRecetaEntity.builder().nombre("Pale Ale").build()).build())
                .build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        if (etapaEnCurso != null) {
            etapas.add(EtapaLoteEntity.builder().etapa(etapaEnCurso).estado(EstadoEtapaLote.EN_CURSO).lote(lote).build());
        }
        lote.setEtapas(etapas);
        return lote;
    }
}
