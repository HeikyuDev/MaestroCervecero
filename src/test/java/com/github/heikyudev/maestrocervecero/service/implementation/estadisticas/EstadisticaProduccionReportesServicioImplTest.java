package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.IEstadisticaProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCostos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteInsumos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteTiempos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.IndicadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TablaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reportes "Costos", "Tiempos", "Insumos" y "Equipamiento" de las estadísticas de producción.
 */
@ExtendWith(MockitoExtension.class)
class EstadisticaProduccionReportesServicioImplTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);
    private static final LocalDateTime INICIO = DESDE.atStartOfDay();
    private static final LocalDateTime FIN = HASTA.plusDays(1).atStartOfDay();
    private static final Pageable PAGINA = PageRequest.of(0, 20);

    @Mock
    private IEstadisticaProduccionRepository repository;

    @Mock
    private IConfiguracionProduccionRepository configuracionRepository;

    @InjectMocks
    private EstadisticaProduccionServicioImpl servicio;

    private static String pesos(String importe) {
        return FormatoImporte.conSimbolo(new BigDecimal(importe));
    }

    private static List<String> valores(ReporteEstadisticoResponseDTO reporte) {
        return reporte.getIndicadores().stream().map(IndicadorResponseDTO::getValor).toList();
    }

    // ==================== Período de los cuatro reportes ====================

    @Test
    @DisplayName("CP-ESP-11: un período inválido se rechaza en los cuatro reportes con el mensaje del período y sin consultar nada")
    void periodoInvalido() {
        assertThatThrownBy(() -> servicio.generarReporteDeCostos(new CriteriosReporteCostos(null, HASTA, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_DESDE_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, null, null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_HASTA_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDeInsumos(new CriteriosReporteInsumos(HASTA, DESDE, null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_INVERTIDO);
        assertThatThrownBy(() -> servicio.generarReporteDeEquipamiento(new CriteriosReporteEquipamiento(DESDE, DESDE.plusDays(366), null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_EXCEDIDO);
        verifyNoInteractions(repository);
    }

    // ==================== Costos ====================

    private static Object[] lote(long id, String codigo, String receta, long idReceta, LocalDateTime fin, double litros) {
        return new Object[]{id, codigo, receta, idReceta, fin, litros};
    }

    @Test
    @DisplayName("CP-ESP-12: Costos suma el costo de insumos y los costos adicionales de cada lote y calcula el costo total, los litros y el peso de los insumos, sin un costo por litro global")
    void costos_indicadoresYTabla() {
        when(repository.buscarLotesConEnvasadoFinalizado(INICIO, FIN, null)).thenReturn(List.of(
                lote(1, "LOT-A-01", "Pale Ale", 7, LocalDateTime.of(2026, 11, 5, 10, 0), 100.0),
                lote(2, "LOT-B-01", "Stout", 8, LocalDateTime.of(2026, 11, 9, 10, 0), 50.0)));
        when(repository.sumarCostoDeInsumosPorLote(List.of(1L, 2L))).thenReturn(List.of(
                new Object[]{1L, new BigDecimal("150.0000")}, new Object[]{2L, new BigDecimal("50.0000")}));
        when(repository.sumarCostosAdicionalesPorLote(List.of(1L, 2L))).thenReturn(List.<Object[]>of(new Object[]{1L, new BigDecimal("50.0000")}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCostos(new CriteriosReporteCostos(DESDE, HASTA, null, null), PAGINA);

        assertThat(reporte.getDescripcion()).isEqualTo("Cuánto costó producir cada litro en el período");
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Se consideran los lotes con envasado finalizado dentro del período y litros envasados registrados");
        assertThat(valores(reporte)).containsExactly(pesos("250"), "150 L", "80 %");
        assertThat(reporte.getIndicadores()).extracting(IndicadorResponseDTO::getNombre)
                .containsExactly("Costo total", "Litros envasados", "Peso de los insumos");
        assertThat(reporte.getCriterios()).extracting("etiqueta", "valor").containsExactly(tuple("Receta", "Todas"));
        // Una tabla por receta, en el orden del gráfico: Pale Ale cuesta $ 2 por litro y Stout $ 1
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Pale Ale", 2.0), tuple("Stout", 1.0));
        assertThat(reporte.getTablas()).extracting(TablaReporteResponseDTO::getTitulo).containsExactly("Pale Ale", "Stout");
        assertThat(reporte.getTablas()).allSatisfy(t -> {
            assertThat(t.isPaginada()).isFalse();
            assertThat(t.getColumnas()).extracting(c -> c.getTitulo())
                    .containsExactly("Lote", "Fin de envasado", "Litros", "Costo de insumos", "Costo adicional", "Costo total", "Costo por litro");
        });
        TablaReporteResponseDTO stout = reporte.getTablas().get(1);
        List<CeldaReporteResponseDTO> primera = stout.getFilas().getContent().get(0).getCeldas();
        assertThat(primera).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("LOT-B-01", "09/11/2026", "50", pesos("50"), pesos("0"), pesos("50"), pesos("1"));
        assertThat(primera.get(0).getDetalle()).isNull();
        assertThat(stout.getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "", "50", pesos("50"), pesos("0"), pesos("50"), pesos("1"));
        assertThat(reporte.getTablas().get(0).getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "", "100", pesos("150"), pesos("50"), pesos("200"), pesos("2"));
    }

    @Test
    @DisplayName("CP-ESP-13: Costos sin lotes muestra ceros y guiones: no divide por cero, lleva una tabla vacía sin título y no lleva totales")
    void costos_sinLotes() {
        when(repository.buscarLotesConEnvasadoFinalizado(INICIO, FIN, 7L)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCostos(new CriteriosReporteCostos(DESDE, HASTA, 7L, "Pale Ale"), PAGINA);

        assertThat(valores(reporte)).containsExactly(pesos("0"), "0 L", "-");
        assertThat(reporte.getTablas()).hasSize(1);
        assertThat(reporte.getTabla().getTitulo()).isNull();
        assertThat(reporte.getTabla().getColumnas()).hasSize(7);
        assertThat(reporte.getTabla().getFilas().getContent()).isEmpty();
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getGrafico().getDatos()).isEmpty();
        assertThat(reporte.getCriterios().get(0).getValor()).isEqualTo("Pale Ale");
    }

    @Test
    @DisplayName("CP-ESP-14: el gráfico de Costos muestra el costo por litro de las ocho recetas más caras sin sumar el resto en 'Otras'; hay una tabla por cada receta, la más cara primero")
    void costos_graficoSinOtras() {
        List<Object[]> lotes = new ArrayList<>();
        List<Object[]> insumos = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            lotes.add(lote(i, "LOT-" + i, "Receta " + i, i, LocalDateTime.of(2026, 11, 2, 8, 0).plusHours(i), 10.0));
            insumos.add(new Object[]{i, new BigDecimal(i * 10)});
            ids.add(i);
        }
        when(repository.buscarLotesConEnvasadoFinalizado(INICIO, FIN, null)).thenReturn(lotes);
        when(repository.sumarCostoDeInsumosPorLote(ids)).thenReturn(insumos);
        when(repository.sumarCostosAdicionalesPorLote(ids)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCostos(new CriteriosReporteCostos(DESDE, HASTA, null, null), PAGINA);
        List<DatoGraficoResponseDTO> datos = reporte.getGrafico().getDatos();

        assertThat(datos).hasSize(8);
        assertThat(datos.get(0).getEtiqueta()).isEqualTo("Receta 10");
        assertThat(datos.get(0).getValor()).isEqualTo(10.0);
        assertThat(datos).extracting(DatoGraficoResponseDTO::getEtiqueta).doesNotContain("Otras", "Otros");
        assertThat(reporte.getTablas()).hasSize(10);
        assertThat(reporte.getTablas().get(0).getTitulo()).isEqualTo("Receta 10");
        assertThat(reporte.getTablas().get(0).getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .contains(pesos("100"), pesos("0"));
        assertThat(reporte.getTablas().subList(0, 8)).extracting(TablaReporteResponseDTO::getTitulo)
                .isEqualTo(datos.stream().map(DatoGraficoResponseDTO::getEtiqueta).toList());
    }

    @Test
    @DisplayName("CP-ESP-27: en Costos una receta con varios lotes los lista del fin de envasado más reciente al más antiguo y sus totales usan solo sus lotes; ante igual costo por litro, las tablas siguen el orden por nombre")
    void costos_variosLotesPorRecetaYEmpate() {
        when(repository.buscarLotesConEnvasadoFinalizado(INICIO, FIN, null)).thenReturn(List.of(
                lote(1, "LOT-Z-01", "Zeta", 9, LocalDateTime.of(2026, 11, 20, 10, 0), 10.0),
                lote(2, "LOT-A-01", "Alfa", 7, LocalDateTime.of(2026, 11, 3, 10, 0), 10.0),
                lote(3, "LOT-A-02", "Alfa", 7, LocalDateTime.of(2026, 11, 15, 10, 0), 30.0)));
        when(repository.sumarCostoDeInsumosPorLote(List.of(1L, 2L, 3L))).thenReturn(List.of(
                new Object[]{1L, new BigDecimal("20")}, new Object[]{2L, new BigDecimal("20")}, new Object[]{3L, new BigDecimal("60")}));
        when(repository.sumarCostosAdicionalesPorLote(List.of(1L, 2L, 3L))).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCostos(new CriteriosReporteCostos(DESDE, HASTA, null, null), PAGINA);

        assertThat(reporte.getTablas()).extracting(TablaReporteResponseDTO::getTitulo).containsExactly("Alfa", "Zeta");
        TablaReporteResponseDTO alfa = reporte.getTablas().get(0);
        assertThat(alfa.getFilas().getContent()).extracting(f -> f.getCeldas().get(0).getTexto()).containsExactly("LOT-A-02", "LOT-A-01");
        assertThat(alfa.getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "", "40", pesos("80"), pesos("0"), pesos("80"), pesos("2"));
        assertThat(valores(reporte)).containsExactly(pesos("100"), "50 L", "100 %");
    }

    // ==================== Tiempos ====================

    private static Object[] etapa(long id, String lote, TipoEtapa tipo, long idEquipo, LocalDateTime inicio, long minutosReales,
                                  Integer maceracion, Integer hervido, Integer fermentacion, Integer maduracion, Double rendimiento, Double capacidad) {
        return new Object[]{id, lote, "Pale Ale", tipo, idEquipo, "EQ-" + idEquipo, inicio, inicio.plusMinutes(minutosReales),
                maceracion, hervido, fermentacion, maduracion, rendimiento, capacidad};
    }

    @Test
    @DisplayName("CP-ESP-15: Tiempos estima Maceración, Fermentación, Molienda y Envasado, cuenta las etapas dentro de lo estimado, el desvío promedio y la etapa de mayor desvío")
    void tiempos_indicadores() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 8, 0);
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, null, null)).thenReturn(List.of(
                etapa(1, "LOT-1", TipoEtapa.FERMENTACION, 10, t, 12 * 1440, null, null, 10, null, null, null),
                etapa(2, "LOT-1", TipoEtapa.MACERACION, 11, t, 45, 60, null, null, null, null, null),
                etapa(3, "LOT-1", TipoEtapa.HERVIDO, 12, t, 70, null, null, null, null, null, null),
                etapa(4, "LOT-1", TipoEtapa.MOLIENDA, 13, t, 150, null, null, null, null, 50.0, null),
                etapa(5, "LOT-1", TipoEtapa.ENVASADO, 14, t, 120, null, null, null, null, null, 200.0)));
        when(repository.buscarRendimientosDeMolinos(any())).thenReturn(List.of());
        when(repository.buscarCapacidadesDeFermentadores(any())).thenReturn(List.<Object[]>of(new Object[]{14L, 200.0}));
        when(repository.sumarMaltaConsumidaPorEtapa(any())).thenReturn(List.<Object[]>of(new Object[]{4L, UnidadDeMedida.KILOGRAMO, 100.0}));
        when(configuracionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID))
                .thenReturn(Optional.of(ConfiguracionProduccionEntity.builder().velocidadEstandarEnvasado(100.0).build()));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, null, null, null), PAGINA);

        // Desvíos: Fermentación +20 %, Maceración -25 %, Hervido sin estimar, Molienda +25 % (150 sobre 120), Envasado 0 % (120 sobre 120)
        assertThat(valores(reporte)).containsExactly("5", "50 %", "+5 %", "Molienda");
        assertThat(reporte.getIndicadores().get(0).getAclaracion()).isEqualTo("1 sin estimar");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("2 de 4 estimadas");
        assertThat(reporte.getIndicadores().get(1).getAyuda()).isNotBlank();
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta)
                .containsExactly("Molienda", "Maceración", "Fermentación", "Envasado");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getValor).containsExactly(125.0, 75.0, 120.0, 100.0);
        assertThat(reporte.getTabla().getColumnas()).hasSize(6);
        List<List<String>> filas = reporte.getTabla().getFilas().getContent().stream()
                .map(f -> f.getCeldas().stream().map(CeldaReporteResponseDTO::getTexto).toList()).toList();
        assertThat(filas).contains(
                List.of("LOT-1", "Fermentación", "EQ-10", "12 días", "10 días", "+20 %"),
                List.of("LOT-1", "Hervido", "EQ-12", "1 h 10 min", "Sin estimar", "-"),
                List.of("LOT-1", "Maceración", "EQ-11", "45 min", "1 h", "-25 %"));
        assertThat(reporte.getTabla().getTotales()).isNull();
    }

    @Test
    @DisplayName("CP-ESP-16: Tiempos sin etapas estimables muestra guiones en los porcentajes y 'Sin etapas estimadas'; el rendimiento del molino vigente se usa si el lote no tiene escalado")
    void tiempos_sinEstimables() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 8, 0);
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, TipoEtapa.MOLIENDA, 7L)).thenReturn(List.<Object[]>of(
                etapa(4, "LOT-1", TipoEtapa.MOLIENDA, 13, t, 150, null, null, null, null, null, null)));
        when(repository.buscarRendimientosDeMolinos(any())).thenReturn(List.<Object[]>of(new Object[]{13L, 50.0}));
        when(repository.sumarMaltaConsumidaPorEtapa(any())).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, TipoEtapa.MOLIENDA, 7L, "Pale Ale"), PAGINA);

        assertThat(valores(reporte)).containsExactly("1", "-", "-", "-");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("Sin etapas estimadas");
        assertThat(reporte.getGrafico().getDatos()).isEmpty();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Molienda", "Pale Ale");
    }

    @Test
    @DisplayName("CP-ESP-17: Tiempos sin etapas finalizadas muestra ceros y guiones")
    void tiempos_vacio() {
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, null, null)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte)).containsExactly("0", "-", "-", "-");
        assertThat(reporte.getTabla().getFilas().getContent()).isEmpty();
    }

    @Test
    @DisplayName("CP-ESP-23: Tiempos incluye una etapa que empezó antes del período y terminó dentro de él: el universo son las etapas finalizadas cuya fecha de finalización cae en el período")
    void tiempos_etapaQueEmpezoAntesDelPeriodo() {
        LocalDateTime inicioEtapa = LocalDateTime.of(2026, 10, 31, 22, 0);
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, null, null)).thenReturn(List.<Object[]>of(
                etapa(1, "LOT-1", TipoEtapa.MACERACION, 11, inicioEtapa, 240, 120, null, null, null, null, null)));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte).get(0)).isEqualTo("1");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("LOT-1", "Maceración", "EQ-11", "4 h", "2 h", "+100 %");
    }

    @Test
    @DisplayName("CP-ESP-24: Tiempos calcula 'dentro de lo estimado' y el desvío promedio con los mismos valores sin redondear: +0,0099 % y 0 % promedian 0 %, no +0,01 %")
    void tiempos_desvioPromedioSinRedondearAntes() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 8, 0);
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, null, null)).thenReturn(List.of(
                // 14 días son 20.160 minutos: 2 minutos más son +0,00992 % (se muestra +0,01 %) y 0 minutos más son 0 %
                etapa(1, "LOT-1", TipoEtapa.FERMENTACION, 10, t, 20162, null, null, 14, null, null, null),
                etapa(2, "LOT-2", TipoEtapa.FERMENTACION, 10, t, 20160, null, null, 14, null, null, null)));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte)).containsExactly("2", "50 %", "0 %", "Fermentación");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("1 de 2 estimadas");
        assertThat(reporte.getIndicadores().get(3).getAclaracion()).isEqualTo("0 % en promedio");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas().get(5).getTexto()).isEqualTo("+0,01 %");
    }

    @Test
    @DisplayName("CP-ESP-25: ante igual desvío promedio, la 'Etapa con mayor desvío' es la primera en el orden del proceso (Molienda, Maceración, Hervido, Fermentación, Maduración, Envasado)")
    void tiempos_empateDeMayorDesvio() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 8, 0);
        when(repository.buscarEtapasFinalizadas(INICIO, FIN, null, null)).thenReturn(List.of(
                etapa(1, "LOT-1", TipoEtapa.HERVIDO, 12, t, 90, null, 60, null, null, null, null),
                etapa(2, "LOT-1", TipoEtapa.MACERACION, 11, t, 90, 60, null, null, null, null, null)));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeTiempos(new CriteriosReporteTiempos(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte).get(3)).isEqualTo("Maceración");
        assertThat(reporte.getIndicadores().get(3).getAclaracion()).isEqualTo("+50 % en promedio");
    }

    // ==================== Insumos ====================

    private static Object[] consumo(long id, String nombre, double cantidad, String valor, long lotes, String directo) {
        return new Object[]{id, nombre, UnidadDeMedida.KILOGRAMO, cantidad, new BigDecimal(valor), lotes, new BigDecimal(directo)};
    }

    @Test
    @DisplayName("CP-ESP-18: Insumos arma el ranking por valor con tipo, cantidad con su unidad, porcentaje del total y lotes, y calcula el valor consumido, el insumo de mayor valor y el consumo sin reserva")
    void insumos_indicadoresYTabla() {
        when(repository.resumirConsumosPorInsumo(INICIO, FIN, null, null)).thenReturn(List.of(
                consumo(2, "Cascade", 5, "400", 3, "100"), consumo(1, "Pilsen", 120, "600", 2, "0")));
        when(repository.buscarTiposDeInsumos(List.of(1L, 2L))).thenReturn(List.of(new Object[]{1L, MaltaEntity.class}, new Object[]{2L, LupuloEntity.class}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeInsumos(new CriteriosReporteInsumos(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte)).containsExactly(pesos("1000"), "2", "Pilsen", "10 %");
        assertThat(reporte.getIndicadores().get(2).getAclaracion()).isEqualTo(pesos("600"));
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Se consideran los consumos registrados dentro del período, sin contar los anulados");
        assertThat(reporte.getIndicadores().get(0).getAyuda()).isNull();
        assertThat(reporte.getIndicadores().get(0).getAclaracion()).isEqualTo("Al costo de cada consumo");
        assertThat(reporte.getTabla().getColumnas()).extracting(c -> c.getTitulo()).containsExactly("Insumo", "Tipo", "Cantidad", "Valor", "% del total", "Lotes de producción");
        assertThat(reporte.getTabla().getColumnas().get(3).getAyuda()).isNull();
        assertThat(reporte.getTabla().getColumnas().get(5).getAyuda()).isEqualTo("Cantidad de lotes de producción en los que se consumió el insumo en el período");
        assertThat(reporte.getDescripcion()).isEqualTo("Qué insumos se consumieron y cuánto se gastó en ellos");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Pilsen", "Malta", "120 kg", pesos("600"), "60 %", "2");
        assertThat(reporte.getTabla().getFilas().getContent().get(1).getCeldas().get(1).getTexto()).isEqualTo("Lúpulo");
        assertThat(reporte.getTabla().getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "", "", pesos("1000"), "100 %", "");
    }

    @Test
    @DisplayName("CP-ESP-19: Insumos con el tipo de insumo elegido consulta por su clase y, sin consumos, muestra ceros y guiones")
    void insumos_vacio() {
        when(repository.resumirConsumosPorInsumo(INICIO, FIN, MaltaEntity.class, 7L)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeInsumos(new CriteriosReporteInsumos(DESDE, HASTA, TipoInsumo.MALTA, 7L, "Pale Ale"), PAGINA);

        assertThat(valores(reporte)).containsExactly(pesos("0"), "0", "-", "-");
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Malta", "Pale Ale");
    }

    @Test
    @DisplayName("CP-ESP-20: el gráfico de Insumos muestra los ocho de mayor valor y suma el resto en 'Otros'")
    void insumos_graficoConOtros() {
        List<Object[]> filas = new ArrayList<>();
        List<Object[]> tipos = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            filas.add(consumo(i, "Insumo " + i, 1, String.valueOf(i * 10), 1, "0"));
            tipos.add(new Object[]{i, MaltaEntity.class});
            ids.add(i);
        }
        when(repository.resumirConsumosPorInsumo(INICIO, FIN, null, null)).thenReturn(filas);
        when(repository.buscarTiposDeInsumos(any())).thenReturn(tipos);

        List<DatoGraficoResponseDTO> datos = servicio.generarReporteDeInsumos(new CriteriosReporteInsumos(DESDE, HASTA, null, null, null), PAGINA).getGrafico().getDatos();

        assertThat(datos).hasSize(9);
        assertThat(datos.get(0).getEtiqueta()).isEqualTo("Insumo 10");
        assertThat(datos.get(8).getEtiqueta()).isEqualTo("Otros");
        assertThat(datos.get(8).getValor()).isEqualTo(30.0);
    }

    // ==================== Equipamiento ====================

    @Test
    @DisplayName("CP-ESP-21: Equipamiento cubre los cuatro tipos de equipo, recorta las etapas al período, cuenta las en curso hasta el fin del período y suma limpiezas, fallas y mantenimientos")
    void equipamiento_indicadoresYTabla() {
        LocalDate desde = LocalDate.of(2020, 1, 1);
        LocalDate hasta = LocalDate.of(2020, 1, 2);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
        when(repository.buscarEquipos(null, null)).thenReturn(List.of(
                new Object[]{4L, "FER-01", FermentadorEntity.class, Estado.ACTIVO},
                new Object[]{1L, "MOL-01", MolinoEntity.class, Estado.ACTIVO},
                new Object[]{2L, "MAC-01", MaceradorEntity.class, Estado.ACTIVO},
                new Object[]{3L, "OLL-01", OllaHervorEntity.class, Estado.ACTIVO},
                new Object[]{5L, "FER-02", FermentadorEntity.class, Estado.ACTIVO},
                new Object[]{6L, "MOL-02", MolinoEntity.class, Estado.BAJA},
                new Object[]{7L, "MOL-03", MolinoEntity.class, Estado.BAJA}));
        when(repository.buscarEtapasDelPeriodo(inicio, fin, null)).thenReturn(List.of(
                // Empezó antes del período y terminó el 1/1 a las 12: cuenta 12 h
                new Object[]{4L, LocalDateTime.of(2019, 12, 31, 20, 0), LocalDateTime.of(2020, 1, 1, 12, 0)},
                // En curso desde el 1/1 a las 10: cuenta hasta el fin del período (38 h)
                new Object[]{1L, LocalDateTime.of(2020, 1, 1, 10, 0), null},
                // Del equipo dado de baja con actividad
                new Object[]{6L, LocalDateTime.of(2020, 1, 2, 0, 0), LocalDateTime.of(2020, 1, 2, 6, 0)}));
        when(repository.contarLimpiezasPorEquipo(inicio, fin)).thenReturn(List.<Object[]>of(new Object[]{4L, 2L}, new Object[]{2L, 1L}));
        when(repository.contarFallasPorEquipo(inicio, fin)).thenReturn(List.<Object[]>of(new Object[]{4L, 1L}));
        when(repository.contarMantenimientosPorEquipo(inicio, fin)).thenReturn(List.<Object[]>of(new Object[]{3L, 1L}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeEquipamiento(new CriteriosReporteEquipamiento(desde, hasta, null, null, null), PAGINA);

        // MOL-03 está dado de baja sin actividad: no figura. Con actividad: FER-01, MOL-01, MAC-01, OLL-01 y MOL-02 (de baja)
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Se consideran las etapas superpuestas con el período y las intervenciones de equipamiento registradas dentro de él");
        assertThat(reporte.getIndicadores()).extracting(IndicadorResponseDTO::getNombre).containsExactly("Limpiezas", "Fallas", "Mantenimientos");
        assertThat(valores(reporte)).containsExactly("3", "1", "1");
        assertThat(reporte.getIndicadores()).extracting(IndicadorResponseDTO::getAclaracion).containsExactly("Registradas", "Registradas", "Registrados");
        assertThat(reporte.getTablas()).hasSize(1);
        assertThat(reporte.getTabla().getTitulo()).isNull();
        assertThat(reporte.getTabla().getColumnas()).extracting(c -> c.getTitulo())
                .containsExactly("Equipo", "Etapas atendidas", "Horas en uso", "Limpiezas", "Fallas", "Mantenimientos");
        assertThat(reporte.getTabla().getFilas().getContent()).extracting(f -> f.getCeldas().get(0).getTexto())
                .containsExactly("MOL-01", "MOL-02 (dado de baja)", "MAC-01", "OLL-01", "FER-01", "FER-02");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("MOL-01", "1", "38 h", "0", "0", "0");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas().get(0).getDetalle()).isEqualTo(TipoEquipamiento.MOLINO.getEtiqueta());
        assertThat(reporte.getTabla().getTotales().getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("Totales del período", "3", "56 h", "3", "1", "1");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("MOL-01", 38.0), tuple("FER-01", 12.0), tuple("MOL-02", 6.0));
    }

    @Test
    @DisplayName("CP-ESP-22: Equipamiento filtra por tipo de equipo y por equipo y, sin equipos, muestra ceros y no lleva totales")
    void equipamiento_vacio() {
        when(repository.buscarEquipos(FermentadorEntity.class, 4L)).thenReturn(List.of());
        when(repository.buscarEtapasDelPeriodo(INICIO, FIN, 4L)).thenReturn(List.of());
        when(repository.contarLimpiezasPorEquipo(INICIO, FIN)).thenReturn(List.of());
        when(repository.contarFallasPorEquipo(INICIO, FIN)).thenReturn(List.of());
        when(repository.contarMantenimientosPorEquipo(INICIO, FIN)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeEquipamiento(
                new CriteriosReporteEquipamiento(DESDE, HASTA, TipoEquipamiento.FERMENTADOR, 4L, "FER-01"), PAGINA);

        assertThat(valores(reporte)).containsExactly("0", "0", "0");
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getGrafico().getDatos()).isEmpty();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Fermentador", "FER-01");
    }

    @Test
    @DisplayName("CP-ESP-26: Equipamiento cuenta como uso real la etapa de un lote cancelado que ya había iniciado, hasta el momento de la cancelación: 30 h y 1 etapa")
    void equipamiento_etapaDeLoteCancelado() {
        LocalDate desde = LocalDate.of(2020, 1, 1);
        LocalDate hasta = LocalDate.of(2020, 1, 3);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
        when(repository.buscarEquipos(null, null)).thenReturn(List.<Object[]>of(new Object[]{4L, "FER-01", FermentadorEntity.class, Estado.ACTIVO}));
        // La etapa inició el 1/1 a las 06:00 y el lote se canceló el 2/1 a las 12:00, que quedó como su fecha de finalización
        when(repository.buscarEtapasDelPeriodo(inicio, fin, null)).thenReturn(List.<Object[]>of(
                new Object[]{4L, LocalDateTime.of(2020, 1, 1, 6, 0), LocalDateTime.of(2020, 1, 2, 12, 0)}));
        when(repository.contarLimpiezasPorEquipo(inicio, fin)).thenReturn(List.of());
        when(repository.contarFallasPorEquipo(inicio, fin)).thenReturn(List.of());
        when(repository.contarMantenimientosPorEquipo(inicio, fin)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeEquipamiento(new CriteriosReporteEquipamiento(desde, hasta, null, null, null), PAGINA);

        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas()).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("FER-01", "1", "30 h", "0", "0", "0");
    }
}
