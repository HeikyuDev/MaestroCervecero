package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.IEstadisticaProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.LoteReporteProyeccion;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteLotes;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ColumnaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CriterioAplicadoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.IndicadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TipoColumna;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticaProduccionServicioImplTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);
    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 20);

    @Mock
    private IEstadisticaProduccionRepository repository;

    @InjectMocks
    private EstadisticaProduccionServicioImpl servicio;

    // ==================== Período ====================

    @Test
    @DisplayName("CP-ESP-01: un período inválido (sin desde, sin hasta, invertido o de más de 366 días) se rechaza con el mensaje del período y no consulta nada")
    void periodoInvalido_seRechaza() {
        assertThatThrownBy(() -> servicio.generarReporteDeLotes(criterios(null, HASTA), PRIMERA_PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_DESDE_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDeLotes(criterios(DESDE, null), PRIMERA_PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_HASTA_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDeLotes(criterios(HASTA, DESDE), PRIMERA_PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_INVERTIDO);
        assertThatThrownBy(() -> servicio.generarReporteDeLotes(criterios(DESDE, DESDE.plusDays(366)), PRIMERA_PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_EXCEDIDO);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("CP-ESP-02: los extremos del período (un solo día y 366 días) se aceptan y llegan tal cual a todas las consultas, con la receta y el estado")
    void extremosDelPeriodo_lleganALasConsultas() {
        LocalDate dia = LocalDate.of(2026, 11, 15);
        sinDatos();

        servicio.generarReporteDeLotes(new CriteriosReporteLotes(dia, dia, 7L, "Pale Ale", EstadoLote.FINALIZADO), PRIMERA_PAGINA);
        servicio.generarReporteDeLotes(criterios(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31).plusDays(1)), PRIMERA_PAGINA);

        verify(repository).contarLotesPorEstado(dia, dia, 7L, EstadoLote.FINALIZADO);
        verify(repository).sumarLitrosEnvasados(dia, dia, 7L, EstadoLote.FINALIZADO);
        verify(repository).buscarFechasDeFinalizados(dia, dia, 7L, EstadoLote.FINALIZADO);
        verify(repository).buscarLotes(eq(dia), eq(dia), eq(7L), eq(EstadoLote.FINALIZADO), any(Pageable.class));
        verify(repository).contarLotesPorEstado(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1), null, null);
    }

    // ==================== Indicadores ====================

    @Test
    @DisplayName("CP-ESP-03: los cuatro indicadores: lotes del período, finalizados con su porcentaje, litros envasados y terminados en plazo con su definición")
    void indicadores() {
        when(repository.contarLotesPorEstado(DESDE, HASTA, null, null)).thenReturn(List.of(
                new Object[]{EstadoLote.FINALIZADO, 3L}, new Object[]{EstadoLote.CANCELADO, 1L}, new Object[]{EstadoLote.PENDIENTE, 4L}));
        when(repository.sumarLitrosEnvasados(DESDE, HASTA, null, null)).thenReturn(1380.555);
        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.of(
                new Object[]{LocalDate.of(2026, 11, 20), LocalDateTime.of(2026, 11, 20, 23, 0)},
                new Object[]{LocalDate.of(2026, 11, 20), LocalDateTime.of(2026, 11, 18, 9, 0)},
                new Object[]{LocalDate.of(2026, 11, 20), LocalDateTime.of(2026, 11, 21, 0, 30)}));
        sinFilas();

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA);

        List<IndicadorResponseDTO> indicadores = reporte.getIndicadores();
        assertThat(indicadores).hasSize(4);
        assertThat(indicadores).extracting(IndicadorResponseDTO::getNombre)
                .containsExactly("Lotes del período", "Finalizados", "Litros envasados", "Terminados en plazo");
        assertThat(indicadores).extracting(IndicadorResponseDTO::getValor).containsExactly("8", "3", "1.380,56 L", "66,67 %");
        assertThat(indicadores.get(1).getAclaracion()).isEqualTo("37,5 % de los lotes");
        assertThat(indicadores.get(3).getAclaracion()).isEqualTo("2 de 3 finalizados");
        assertThat(indicadores.get(3).getAyuda()).isEqualTo(EstadisticaProduccionServicioImpl.AYUDA_EN_PLAZO);
        assertThat(reporte.getTitulo()).isEqualTo("Lotes");
        assertThat(reporte.getDescripcion()).isEqualTo("Cuántos lotes se planificaron en el período y cómo terminaron");
        assertThat(reporte.getDesde()).isEqualTo(DESDE);
        assertThat(reporte.getHasta()).isEqualTo(HASTA);
    }

    @Test
    @DisplayName("CP-ESP-04: sin lotes los indicadores quedan en cero, el porcentaje de finalizados no se muestra y terminados en plazo es un guion: no se divide por cero")
    void indicadores_sinLotes() {
        sinDatos();

        List<IndicadorResponseDTO> indicadores = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA).getIndicadores();

        assertThat(indicadores).extracting(IndicadorResponseDTO::getValor).containsExactly("0", "0", "0 L", "-");
        assertThat(indicadores.get(1).getAclaracion()).isNull();
        assertThat(indicadores.get(3).getAclaracion()).isEqualTo("Sin lotes finalizados");
    }

    @Test
    @DisplayName("CP-ESP-05: con lotes pero ninguno finalizado (o finalizados sin fechas) terminados en plazo es un guion; un único finalizado se dice en singular")
    void enPlazo_sinFinalizadosConFechas() {
        when(repository.contarLotesPorEstado(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(new Object[]{EstadoLote.PENDIENTE, 2L}));
        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(new Object[]{null, LocalDateTime.of(2026, 11, 2, 8, 0)}));
        sinFilas();

        IndicadorResponseDTO enPlazo = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA).getIndicadores().get(3);
        assertThat(enPlazo.getValor()).isEqualTo("-");
        assertThat(enPlazo.getAclaracion()).isEqualTo("Sin lotes finalizados");

        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(
                new Object[]{LocalDate.of(2026, 11, 20), LocalDateTime.of(2026, 11, 25, 8, 0)}));
        IndicadorResponseDTO uno = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA).getIndicadores().get(3);
        assertThat(uno.getValor()).isEqualTo("0 %");
        assertThat(uno.getAclaracion()).isEqualTo("0 de 1 finalizado");
    }

    // ==================== Criterios, gráfico y tabla ====================

    @Test
    @DisplayName("CP-ESP-06: los criterios aplicados son Receta y Estado, con 'Todas' y 'Todos' si no se indicaron")
    void criteriosAplicados() {
        sinDatos();

        assertThat(servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA).getCriterios())
                .extracting(CriterioAplicadoResponseDTO::getEtiqueta, CriterioAplicadoResponseDTO::getValor)
                .containsExactly(tuple("Receta", "Todas"), tuple("Estado", "Todos"));
        assertThat(servicio.generarReporteDeLotes(new CriteriosReporteLotes(DESDE, HASTA, 7L, "Pale Ale (dada de baja)", EstadoLote.CANCELADO), PRIMERA_PAGINA).getCriterios())
                .extracting(CriterioAplicadoResponseDTO::getValor).containsExactly("Pale Ale (dada de baja)", "Cancelado");
    }

    @Test
    @DisplayName("CP-ESP-07: el gráfico es 'Lotes por estado' con los cuatro estados en su orden y cero donde no hay lotes")
    void grafico() {
        when(repository.contarLotesPorEstado(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(new Object[]{EstadoLote.CANCELADO, 9L}));
        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.of());
        sinFilas();

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA);

        assertThat(reporte.getGrafico().getTitulo()).isEqualTo("Lotes por estado");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Pendiente", 0.0), tuple("En ejecución", 0.0),
                        tuple("Finalizado", 0.0), tuple("Cancelado", 9.0));
    }

    @Test
    @DisplayName("CP-ESP-08: la tabla tiene seis columnas (Lote, Estado, Litros envasados, Fin estimado, Fin real y Acciones con 'Abrir' por LOTE_CONSULTAR) y cada fila se arma con los datos del lote y sus litros de una sola consulta")
    void tabla() {
        when(repository.contarLotesPorEstado(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(new Object[]{EstadoLote.FINALIZADO, 1L}, new Object[]{EstadoLote.PENDIENTE, 1L}));
        when(repository.sumarLitrosEnvasados(DESDE, HASTA, null, null)).thenReturn(480.5);
        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.of());
        Pageable segunda = PageRequest.of(1, 1);
        when(repository.buscarLotes(DESDE, HASTA, null, null, segunda)).thenReturn(new PageImpl<>(List.of(
                new LoteReporteProyeccion(11L, "LOT-R7-01", "Ñandú Añejo", EstadoLote.FINALIZADO, LocalDate.of(2026, 11, 20), LocalDateTime.of(2026, 11, 22, 17, 0))),
                segunda, 2));
        when(repository.sumarLitrosEnvasadosPorLote(List.of(11L))).thenReturn(List.<Object[]>of(new Object[]{11L, 480.5}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), segunda);
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Se consideran los lotes con inicio estimado dentro del período, en cualquier estado");

        assertThat(reporte.getTabla().getColumnas()).extracting(ColumnaReporteResponseDTO::getTitulo, ColumnaReporteResponseDTO::getTipo)
                .containsExactly(tuple("Lote", TipoColumna.TEXTO), tuple("Estado", TipoColumna.TEXTO),
                        tuple("Litros envasados", TipoColumna.NUMERO), tuple("Fin estimado", TipoColumna.FECHA),
                        tuple("Fin real", TipoColumna.FECHA), tuple("Acciones", TipoColumna.ENLACE));
        ColumnaReporteResponseDTO acciones = reporte.getTabla().getColumnas().get(5);
        assertThat(acciones.getRuta()).isEqualTo("/lotes/{id}");
        assertThat(acciones.getPermiso()).isEqualTo("LOTE_CONSULTAR");

        List<CeldaReporteResponseDTO> celdas = reporte.getTabla().getFilas().getContent().get(0).getCeldas();
        assertThat(celdas).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("LOT-R7-01", "Finalizado", "480,5", "20/11/2026", "22/11/2026", "Abrir LOT-R7-01");
        assertThat(celdas.get(0).getDetalle()).isEqualTo("Ñandú Añejo");
        assertThat(celdas.get(5).getId()).isEqualTo(11L);
        Page<?> filas = reporte.getTabla().getFilas();
        assertThat(filas.getNumber()).isEqualTo(1);
        assertThat(filas.getTotalElements()).isEqualTo(2);
        assertThat(reporte.getTabla().getEtiquetaFilas()).isEqualTo("lote|lotes");
    }

    @Test
    @DisplayName("CP-ESP-09: un lote sin envasados registrados muestra 0 litros y sin fin real un guion; la fila de totales lleva los litros envasados del período")
    void tabla_sinEnvasadosYTotales() {
        when(repository.contarLotesPorEstado(DESDE, HASTA, null, null)).thenReturn(List.<Object[]>of(new Object[]{EstadoLote.PENDIENTE, 1L}));
        when(repository.sumarLitrosEnvasados(DESDE, HASTA, null, null)).thenReturn(null);
        when(repository.buscarFechasDeFinalizados(DESDE, HASTA, null, null)).thenReturn(List.of());
        when(repository.buscarLotes(DESDE, HASTA, null, null, PRIMERA_PAGINA)).thenReturn(new PageImpl<>(List.of(
                new LoteReporteProyeccion(12L, "LOT-R7-02", "Stout", EstadoLote.PENDIENTE, LocalDate.of(2026, 12, 1), null)), PRIMERA_PAGINA, 1));
        when(repository.sumarLitrosEnvasadosPorLote(List.of(12L))).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA);

        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("LOT-R7-02", "Pendiente", "0", "01/12/2026", "-", "Abrir LOT-R7-02");
        assertThat(reporte.getTabla().getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "", "0", "", "", "");
    }

    @Test
    @DisplayName("CP-ESP-10: sin filas en la página no se consultan los litros por lote y la tabla no lleva fila de totales")
    void tabla_sinFilas() {
        sinDatos();

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeLotes(criterios(DESDE, HASTA), PRIMERA_PAGINA);

        assertThat(reporte.getTabla().getFilas().getContent()).isEmpty();
        assertThat(reporte.getTabla().getTotales()).isNull();
        verify(repository, never()).sumarLitrosEnvasadosPorLote(anyCollection());
    }

    // ==================== Apoyo ====================

    private static CriteriosReporteLotes criterios(LocalDate desde, LocalDate hasta) {
        return new CriteriosReporteLotes(desde, hasta, null, null, null);
    }

    private void sinDatos() {
        when(repository.contarLotesPorEstado(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.buscarFechasDeFinalizados(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.sumarLitrosEnvasados(any(), any(), any(), any())).thenReturn(0.0);
        sinFilas();
    }

    private void sinFilas() {
        when(repository.buscarLotes(any(), any(), any(), any(), any(Pageable.class)))
                .thenAnswer(invocacion -> new PageImpl<LoteReporteProyeccion>(List.of(), invocacion.getArgument(4), 0));
    }
}
