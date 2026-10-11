package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Limpieza;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GanttPlanificacionBuilderTest {

    private static final LocalDate HOY_LEJANO = LocalDate.of(2027, 6, 1);

    // ==================== Sin lotes ====================

    @Test
    @DisplayName("CP-GPB-01: sin lotes el modelo está vacío y el eje va de las fechas de la planificación con un día de margen (27 días), con sus marcas y la de hoy")
    void sinLotes_dibujaSoloElEjeYLasMarcasDeLaPlanificacion() {
        // Arrange
        LocalDate hoy = LocalDate.of(2027, 7, 10);

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 25), hoy);

        // Assert
        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).isEmpty();
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(vista.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 26));
        assertThat(vista.getTotalDias()).isEqualTo(27);
        assertThat(vista.getInicioPlanificacionPct()).isEqualTo(3.7);
        assertThat(vista.getFinPlanificacionPct()).isEqualTo(96.3);
        assertThat(vista.getHoyPct()).isEqualTo(38.89);
        assertThat(vista.getAnchoMinimoPx()).isEqualTo(720);
    }

    @Test
    @DisplayName("CP-GPB-02: sin lotes ni fechas de planificación el eje se arma alrededor de hoy (3 días), con marcas diarias y sin marcas de planificación")
    void sinLotesNiFechas_elEjeSeArmaAlrededorDeHoy() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(null, null, null, LocalDate.of(2027, 7, 10));

        // Assert
        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 7, 9));
        assertThat(vista.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 11));
        assertThat(vista.getTotalDias()).isEqualTo(3);
        assertThat(vista.getInicioPlanificacionPct()).isNull();
        assertThat(vista.getFinPlanificacionPct()).isNull();
        assertThat(vista.getHoyPct()).isEqualTo(50.0);
        assertThat(vista.getMarcasEje()).extracting(m -> m.getEtiqueta()).containsExactly("09/07", "10/07", "11/07");
        assertThat(vista.getMarcasEje()).extracting(m -> m.getPosicionPct()).containsExactly(0.0, 33.33, 66.67);
    }

    // ==================== Lotes pendientes y carriles ====================

    @Test
    @DisplayName("CP-GPB-03: un lote pendiente ocupa de su inicio a su fin estimados (inclusive): 4 días de un eje de 6 (izquierda 16,67 %, ancho 66,67 %), con etiqueta y tooltip")
    void unLotePendiente_ocupaSusFechasEstimadas() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8))
                .identificadorInternoMolino("M-01").identificadorInternoMacerador("MA-01").identificadorInternoOllaHervor("O-01").build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(5), fecha(8), HOY_LEJANO);

        // Assert
        assertThat(vista.isVacio()).isFalse();
        assertThat(vista.getRangoDesde()).isEqualTo(fecha(4));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(9));
        assertThat(vista.getTotalDias()).isEqualTo(6);
        assertThat(vista.getCarriles()).hasSize(1);
        Carril carril = vista.getCarriles().get(0);
        assertThat(carril.getEtiqueta()).isEqualTo("F-01");
        Barra barra = carril.getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("pendiente");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-R7-1 - Pendiente");
        assertThat(barra.getDesde()).isEqualTo(fecha(5));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getIzquierdaPct()).isEqualTo(16.67);
        assertThat(barra.getAnchoPct()).isEqualTo(66.67);
        assertThat(barra.getTooltip()).isEqualTo(String.join("\n",
                "PALE_ALE-R7-1 - Pendiente",
                "Inicio estimado: 05/07/2027",
                "Fin estimado: 08/07/2027",
                "Inicio real: -",
                "Fin real: -",
                "Volumen: 300 L",
                "Molino: M-01",
                "Macerador: MA-01",
                "Olla de hervor: O-01",
                "Fermentador: F-01"));
    }

    @Test
    @DisplayName("CP-GPB-04: dos lotes simultáneos en distintos fermentadores van en dos carriles, ordenados por identificador, con la misma geometría")
    void dosLotesConcurrentes_vanEnDosCarriles() {
        // Arrange
        LoteCronogramaResponseDTO enF02 = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(5), fecha(8));
        LoteCronogramaResponseDTO enF01 = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enF02, enF01), fecha(5), fecha(8), HOY_LEJANO);

        // Assert
        assertThat(vista.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        for (Carril carril : vista.getCarriles()) {
            assertThat(carril.getBarras()).hasSize(1);
            assertThat(carril.getBarras().get(0).getIzquierdaPct()).isEqualTo(16.67);
            assertThat(carril.getBarras().get(0).getAnchoPct()).isEqualTo(66.67);
        }
        assertThat(vista.getCarriles().get(0).getBarras().get(0).getIdLote()).isEqualTo(1L);
        assertThat(vista.getCarriles().get(1).getBarras().get(0).getIdLote()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-GPB-05: lotes consecutivos en un mismo fermentador comparten carril, ordenados por inicio, sin superponerse (eje de 12 días)")
    void lotesSecuencialesEnUnCarril_seOrdenanPorInicio() {
        // Arrange (se pasan en orden inverso)
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(6), fecha(10));
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(segundo, primero), fecha(1), fecha(10), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(12);
        assertThat(vista.getCarriles()).hasSize(1);
        List<Barra> barras = vista.getCarriles().get(0).getBarras();
        assertThat(barras).extracting(Barra::getIdLote).containsExactly(1L, 2L);
        assertThat(barras.get(0).getIzquierdaPct()).isEqualTo(8.33);
        assertThat(barras.get(0).getAnchoPct()).isEqualTo(41.67);
        assertThat(barras.get(1).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(barras.get(1).getAnchoPct()).isEqualTo(41.67);
        assertThat(barras.get(0).getIzquierdaPct() + barras.get(0).getAnchoPct()).isLessThanOrEqualTo(barras.get(1).getIzquierdaPct());
    }

    // ==================== Reglas por estado ====================

    @Test
    @DisplayName("CP-GPB-06: un lote en ejecución va del inicio real al fin estimado, en un solo tramo (izquierda 14,29 %, ancho 71,43 %)")
    void enEjecucion_vaDelInicioRealAlFinEstimado() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(14);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("en-ejecucion");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-R7-1 - En ejecución");
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(11));
        assertThat(barra.getIzquierdaPct()).isEqualTo(14.29);
        assertThat(barra.getAnchoPct()).isEqualTo(71.43);
        assertThat(barra.getTooltip()).contains("Inicio real: 02/07/2027 08:00");
    }

    @Test
    @DisplayName("CP-GPB-07: la barra de un lote en ejecución no cambia con la fecha de hoy: antes de su inicio, después de su fin estimado y en su último día es la misma")
    void enEjecucion_laBarraNoDependeDeHoy() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();

        // Act
        Barra antes = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), LocalDate.of(2027, 6, 20))
                .getCarriles().get(0).getBarras().get(0);
        Barra despues = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), LocalDate.of(2027, 8, 1))
                .getCarriles().get(0).getBarras().get(0);
        Barra elUltimoDia = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), fecha(11))
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(antes).isEqualTo(despues).isEqualTo(elUltimoDia);
        assertThat(antes.getDesde()).isEqualTo(fecha(2));
        assertThat(antes.getHasta()).isEqualTo(fecha(11));
    }

    @Test
    @DisplayName("CP-GPB-08: un lote finalizado va del inicio real al fin real (7 días de un eje de 10)")
    void finalizado_vaDelInicioRealAlFinReal() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(5), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(10);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-R7-1 - Finalizado");
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getIzquierdaPct()).isEqualTo(20.0);
        assertThat(barra.getAnchoPct()).isEqualTo(70.0);
        assertThat(barra.getTooltip()).contains("Fin real: 08/07/2027 17:00");
    }

    @Test
    @DisplayName("CP-GPB-09: un lote cancelado que había arrancado no se dibuja: el modelo queda vacío y el eje lo determina solo la planificación (12 días)")
    void canceladoConInicioReal_noSeDibuja() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.CANCELADO, "F-01", fecha(1), fecha(10))
                .fechaInicio(fecha(3).atTime(10, 0)).fechaCancelacion(fecha(5).atTime(15, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(10), fecha(6));

        // Assert
        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).isEmpty();
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(11));
        assertThat(vista.getTotalDias()).isEqualTo(12);
    }

    @Test
    @DisplayName("CP-GPB-10: un lote cancelado que nunca arrancó tampoco se dibuja (modelo vacío, eje de 12 días)")
    void canceladoSinInicioReal_noSeDibuja() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.CANCELADO, "F-01", fecha(2), fecha(4))
                .fechaCancelacion(fecha(1).atTime(12, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(10), fecha(6));

        // Assert
        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).isEmpty();
        assertThat(vista.getTotalDias()).isEqualTo(12);
    }

    @Test
    @DisplayName("CP-GPB-11: si faltan fechas reales se usan las estimadas, y un fin anterior al inicio se acota a un solo día")
    void fechasFaltantesOInvertidas_seAcotan() {
        // Arrange
        LoteCronogramaResponseDTO finalizadoSinFechasReales = lote(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO invertido = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(10), fecha(5));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(finalizadoSinFechasReales, invertido), fecha(1), fecha(10), HOY_LEJANO);

        // Assert
        Barra sinReales = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(sinReales.getDesde()).isEqualTo(fecha(1));
        assertThat(sinReales.getHasta()).isEqualTo(fecha(5));
        Barra acotada = vista.getCarriles().get(1).getBarras().get(0);
        assertThat(acotada.getDesde()).isEqualTo(fecha(10));
        assertThat(acotada.getHasta()).isEqualTo(fecha(10));
    }

    @Test
    @DisplayName("CP-GPB-12: un lote sin ninguna fecha no se dibuja; si es el único, el modelo queda vacío")
    void loteSinFechas_seIgnora() {
        // Arrange
        LoteCronogramaResponseDTO sinFechas = LoteCronogramaResponseDTO.builder()
                .id(1L).identificadorInterno("PALE_ALE-R7-1").estado(EstadoLote.PENDIENTE).identificadorInternoFermentador("F-01").build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(sinFechas), fecha(1), fecha(10), HOY_LEJANO);

        // Assert
        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).isEmpty();
    }

    // ==================== Eje y marcas ====================

    @Test
    @DisplayName("CP-GPB-13: el eje abarca el menor inicio y el mayor fin entre las barras y la planificación, con un día de margen")
    void elEjeAbarcaBarrasYPlanificacion() {
        // Arrange: el lote empieza antes y termina después de las fechas de la planificación
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(5), fecha(8), HOY_LEJANO);

        // Assert
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(13));
        assertThat(vista.getTotalDias()).isEqualTo(14);
        assertThat(vista.getInicioPlanificacionPct()).isEqualTo(35.71);
        assertThat(vista.getFinPlanificacionPct()).isEqualTo(64.29);
    }

    @Test
    @DisplayName("CP-GPB-14: la marca de cada paso depende del largo del eje: cada día hasta 14 días, de a 7 hasta 120, de a 14 hasta 365 y de a 28 después")
    void elPasoDeLasMarcasDependeDelLargoDelEje() {
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(3)).isEqualTo(1);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(14)).isEqualTo(1);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(15)).isEqualTo(7);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(120)).isEqualTo(7);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(121)).isEqualTo(14);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(365)).isEqualTo(14);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(366)).isEqualTo(28);
    }

    @Test
    @DisplayName("CP-GPB-15: un eje de 62 días lleva 9 marcas semanales dd/MM desde el primer día, con sus posiciones en %")
    void lasMarcasSemanalesTienenFechaYPosicion() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 7, 1), LocalDate.of(2027, 8, 29), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(62);
        assertThat(vista.getMarcasEje()).hasSize(9);
        assertThat(vista.getMarcasEje().get(0).getEtiqueta()).isEqualTo("30/06");
        assertThat(vista.getMarcasEje().get(0).getPosicionPct()).isEqualTo(0.0);
        assertThat(vista.getMarcasEje().get(1).getEtiqueta()).isEqualTo("07/07");
        assertThat(vista.getMarcasEje().get(1).getPosicionPct()).isEqualTo(11.29);
        assertThat(vista.getMarcasEje().get(8).getEtiqueta()).isEqualTo("25/08");
        assertThat(vista.getMarcasEje().get(8).getPosicionPct()).isEqualTo(90.32);
    }

    @Test
    @DisplayName("CP-GPB-16: un eje corto lleva una marca por día y las de 27 días, cuatro marcas semanales")
    void lasMarcasDeUnEjeCortoSonDiariasYLasDe27DiasSemanales() {
        // Act
        GanttPlanificacionVista corto = GanttPlanificacionBuilder.construir(List.of(), fecha(5), fecha(8), HOY_LEJANO);
        GanttPlanificacionVista medio = GanttPlanificacionBuilder.construir(List.of(), LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 25), HOY_LEJANO);

        // Assert
        assertThat(corto.getMarcasEje()).extracting(m -> m.getEtiqueta())
                .containsExactly("04/07", "05/07", "06/07", "07/07", "08/07", "09/07");
        assertThat(medio.getMarcasEje()).extracting(m -> m.getEtiqueta())
                .containsExactly("30/06", "07/07", "14/07", "21/07");
        assertThat(medio.getMarcasEje()).extracting(m -> m.getPosicionPct())
                .containsExactly(0.0, 25.93, 51.85, 77.78);
    }

    @Test
    @DisplayName("CP-GPB-17: la marca de hoy va al centro de su día cuando cae dentro del eje (incluidos los extremos) y no existe fuera de él")
    void laMarcaDeHoyEstaSoloDentroDelEje() {
        // Act
        GanttPlanificacionVista primerDia = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), LocalDate.of(2027, 6, 30));
        GanttPlanificacionVista ultimoDia = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), fecha(26));
        GanttPlanificacionVista antes = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), LocalDate.of(2027, 6, 29));
        GanttPlanificacionVista despues = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), fecha(27));
        GanttPlanificacionVista sinFecha = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), null);

        // Assert
        assertThat(primerDia.getHoyPct()).isEqualTo(1.85);
        assertThat(ultimoDia.getHoyPct()).isEqualTo(98.15);
        assertThat(antes.getHoyPct()).isNull();
        assertThat(despues.getHoyPct()).isNull();
        assertThat(sinFecha.getHoyPct()).isNull();
    }

    @Test
    @DisplayName("CP-GPB-18: las marcas de la planificación coinciden con los bordes de una barra que cubre sus mismas fechas (inicio al comienzo del día, fin al cierre del último)")
    void lasMarcasDeLaPlanificacionCoincidenConLosBordesDeLaBarra() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(5), fecha(8), HOY_LEJANO);

        // Assert
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(vista.getInicioPlanificacionPct()).isEqualTo(16.67).isEqualTo(barra.getIzquierdaPct());
        assertThat(vista.getFinPlanificacionPct()).isEqualTo(83.33);
        assertThat(barra.getIzquierdaPct() + barra.getAnchoPct()).isCloseTo(vista.getFinPlanificacionPct(), org.assertj.core.data.Offset.offset(0.011));
    }

    // ==================== Ancho mínimo y límites ====================

    @Test
    @DisplayName("CP-GPB-19: una barra de un solo día en un eje larguísimo conserva un ancho mínimo visible de 1 %")
    void unaBarraMuyAngostaTieneUnAnchoMinimo() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01",
                LocalDate.of(2027, 6, 15), LocalDate.of(2027, 6, 15));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(lote), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(367);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getAnchoPct()).isEqualTo(1.0);
        assertThat(barra.getIzquierdaPct()).isEqualTo(45.23);
    }

    @Test
    @DisplayName("CP-GPB-20: una barra mínima en el último día se corre a la izquierda para no salirse del eje")
    void unaBarraMinimaAlFinalNoSeSaleDelEje() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01",
                LocalDate.of(2027, 12, 31), LocalDate.of(2027, 12, 31));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(lote), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), HOY_LEJANO);

        // Assert
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getAnchoPct()).isEqualTo(1.0);
        assertThat(barra.getIzquierdaPct()).isEqualTo(99.0);
    }

    @Test
    @DisplayName("CP-GPB-21: el ancho mínimo del área de tiempo crece con los días (24 px por día) entre 720 y 3600 px")
    void elAnchoMinimoDelAreaDeTiempoEstaAcotado() {
        // Act
        GanttPlanificacionVista corto = GanttPlanificacionBuilder.construir(List.of(), fecha(5), fecha(8), HOY_LEJANO);
        GanttPlanificacionVista medio = GanttPlanificacionBuilder.construir(List.of(), LocalDate.of(2027, 7, 1), LocalDate.of(2027, 10, 8), HOY_LEJANO);
        GanttPlanificacionVista largo = GanttPlanificacionBuilder.construir(List.of(), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), HOY_LEJANO);

        // Assert
        assertThat(corto.getAnchoMinimoPx()).isEqualTo(720);
        assertThat(medio.getTotalDias()).isEqualTo(102);
        assertThat(medio.getAnchoMinimoPx()).isEqualTo(2448);
        assertThat(largo.getAnchoMinimoPx()).isEqualTo(3600);
    }

    // ==================== Orden ====================

    @Test
    @DisplayName("CP-GPB-22: los carriles se ordenan por identificador sin distinguir mayúsculas y el lote sin fermentador va al final en el carril \"Sin fermentador\"")
    void losCarrilesSeOrdenanPorIdentificador() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "L-1", EstadoLote.PENDIENTE, "F-10", fecha(1), fecha(2)),
                lote(2L, "L-2", EstadoLote.PENDIENTE, null, fecha(1), fecha(2)),
                lote(3L, "L-3", EstadoLote.PENDIENTE, "F-02", fecha(1), fecha(2)),
                lote(4L, "L-4", EstadoLote.PENDIENTE, "f-01", fecha(1), fecha(2)));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotes, fecha(1), fecha(2), HOY_LEJANO);

        // Assert
        assertThat(vista.getCarriles()).extracting(Carril::getEtiqueta)
                .containsExactly("f-01", "F-02", "F-10", "Sin fermentador");
    }

    @Test
    @DisplayName("CP-GPB-23: lotes con el mismo inicio en un carril se ordenan por id")
    void loteConIgualInicioSeOrdenanPorId() {
        // Arrange
        LoteCronogramaResponseDTO cinco = lote(5L, "L-5", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(3));
        LoteCronogramaResponseDTO tres = lote(3L, "L-3", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(3));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(cinco, tres), fecha(1), fecha(3), HOY_LEJANO);

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras()).extracting(Barra::getIdLote).containsExactly(3L, 5L);
    }

    // ==================== Alineación de etiquetas ====================

    @Test
    @DisplayName("CP-GPB-24: la etiqueta de la marca del borde izquierdo se alinea a la izquierda, la del borde derecho a la derecha y las demás al centro")
    void lasEtiquetasDeLosBordesSeAlineanHaciaAdentro() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 27), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(29);
        assertThat(vista.getMarcasEje()).extracting(m -> m.getEtiqueta())
                .containsExactly("30/06", "07/07", "14/07", "21/07", "28/07");
        assertThat(vista.getMarcasEje()).extracting(m -> m.getAlineacion())
                .containsExactly(Alineacion.IZQUIERDA, Alineacion.CENTRO, Alineacion.CENTRO, Alineacion.CENTRO, Alineacion.DERECHA);
    }

    @Test
    @DisplayName("CP-GPB-25: el límite de borde es del 4 % del eje: hasta 4 % se alinea a la izquierda, desde 96 % a la derecha y entre ambos al centro")
    void elLimiteDeBordeEsDelCuatroPorCiento() {
        assertThat(GanttPlanificacionBuilder.alineacionPara(0.0)).isEqualTo(Alineacion.IZQUIERDA);
        assertThat(GanttPlanificacionBuilder.alineacionPara(4.0)).isEqualTo(Alineacion.IZQUIERDA);
        assertThat(GanttPlanificacionBuilder.alineacionPara(4.01)).isEqualTo(Alineacion.CENTRO);
        assertThat(GanttPlanificacionBuilder.alineacionPara(95.99)).isEqualTo(Alineacion.CENTRO);
        assertThat(GanttPlanificacionBuilder.alineacionPara(96.0)).isEqualTo(Alineacion.DERECHA);
        assertThat(GanttPlanificacionBuilder.alineacionPara(100.0)).isEqualTo(Alineacion.DERECHA);
    }

    @Test
    @DisplayName("CP-GPB-26: la bandera de hoy se alinea según su cercanía a los bordes y no existe si hoy queda fuera del eje")
    void laBanderaDeHoySeAlineaSegunElBorde() {
        // Act
        GanttPlanificacionVista primerDia = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), LocalDate.of(2027, 6, 30));
        GanttPlanificacionVista medio = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), fecha(10));
        GanttPlanificacionVista ultimoDia = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), fecha(26));
        GanttPlanificacionVista fuera = GanttPlanificacionBuilder.construir(List.of(), fecha(1), fecha(25), fecha(27));

        // Assert
        assertThat(primerDia.getHoyAlineacion()).isEqualTo(Alineacion.IZQUIERDA);
        assertThat(medio.getHoyAlineacion()).isEqualTo(Alineacion.CENTRO);
        assertThat(ultimoDia.getHoyAlineacion()).isEqualTo(Alineacion.DERECHA);
        assertThat(fuera.getHoyAlineacion()).isNull();
    }

    // ==================== Tope de marcas del eje ====================

    @Test
    @DisplayName("CP-GPB-27: el paso de las marcas crece (56, 91, 182, 365 y múltiplos de 365 días) cuando con 28 días habría más de 150 marcas")
    void elPasoDeLasMarcasCreceParaNoSuperarElMaximo() {
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(4200)).isEqualTo(28);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(4201)).isEqualTo(56);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(8400)).isEqualTo(56);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(8401)).isEqualTo(91);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(13650)).isEqualTo(91);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(13651)).isEqualTo(182);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(27300)).isEqualTo(182);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(27301)).isEqualTo(365);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(54750)).isEqualTo(365);
        assertThat(GanttPlanificacionBuilder.pasoDeMarcas(54751)).isEqualTo(730);
    }

    @Test
    @DisplayName("CP-GPB-28: un eje de 10 años (3.655 días) lleva 131 marcas de a 28 días, con el año en la etiqueta (dd/MM/yy)")
    void unEjeDeDiezAniosNoSuperaElMaximoDeMarcas() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 1, 1), LocalDate.of(2036, 12, 31), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(3655);
        assertThat(vista.getMarcasEje()).hasSize(131);
        assertThat(vista.getMarcasEje().get(0).getEtiqueta()).isEqualTo("31/12/26");
        assertThat(vista.getMarcasEje().get(0).getPosicionPct()).isEqualTo(0.0);
        assertThat(vista.getMarcasEje().get(0).getAlineacion()).isEqualTo(Alineacion.IZQUIERDA);
        assertThat(vista.getMarcasEje().get(130).getEtiqueta()).isEqualTo("18/12/36");
    }

    @Test
    @DisplayName("CP-GPB-29: un eje de 30 años (10.960 días) lleva 121 marcas de a 91 días, con posiciones crecientes dentro del eje")
    void unEjeDeTreintaAniosAmpliaElPasoDeLasMarcas() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 1, 1), LocalDate.of(2056, 12, 31), HOY_LEJANO);

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(10960);
        assertThat(vista.getMarcasEje()).hasSize(121);
        assertThat(vista.getMarcasEje().get(0).getEtiqueta()).isEqualTo("31/12/26");
        assertThat(vista.getMarcasEje().get(120).getEtiqueta()).isEqualTo("23/11/56");
        assertThat(vista.getMarcasEje().get(120).getPosicionPct()).isLessThanOrEqualTo(100.0);
        assertThat(vista.getMarcasEje()).extracting(m -> m.getPosicionPct()).isSorted();
    }

    @Test
    @DisplayName("CP-GPB-30: el año aparece en la etiqueta solo cuando el eje supera los 365 días: 365 días dd/MM y 366 días dd/MM/yy")
    void laEtiquetaLlevaElAnioSoloEnEjesDeMasDeUnAnio() {
        // Act
        GanttPlanificacionVista deUnAnio = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 1, 2), LocalDate.of(2027, 12, 30), HOY_LEJANO);
        GanttPlanificacionVista deMasDeUnAnio = GanttPlanificacionBuilder.construir(
                List.of(), LocalDate.of(2027, 1, 2), LocalDate.of(2027, 12, 31), HOY_LEJANO);

        // Assert
        assertThat(deUnAnio.getTotalDias()).isEqualTo(365);
        assertThat(deUnAnio.getMarcasEje().get(0).getEtiqueta()).isEqualTo("01/01");
        assertThat(deMasDeUnAnio.getTotalDias()).isEqualTo(366);
        assertThat(deMasDeUnAnio.getMarcasEje().get(0).getEtiqueta()).isEqualTo("01/01/27");
    }

    // ==================== Atraso y corrimiento ====================

    @Test
    @DisplayName("CP-GPB-31: un lote en ejecución atrasado extiende su barra hasta hoy (02/07 al 10/07, 9 días de un eje de 12), con la etiqueta \"Atrasado 3 días\"")
    void enEjecucionAtrasado_extiendeLaBarraHastaHoyYLlevaLaEtiquetaDeAtraso() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(8), fecha(10));

        // Assert
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(11));
        assertThat(vista.getTotalDias()).isEqualTo(12);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("en-ejecucion");
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(10));
        assertThat(barra.getIzquierdaPct()).isEqualTo(16.67);
        assertThat(barra.getAnchoPct()).isEqualTo(75.0);
        assertThat(barra.getDiasDeAtraso()).isEqualTo(3);
        assertThat(barra.getEtiquetaAtraso()).isEqualTo("Atrasado 3 días");
    }

    @Test
    @DisplayName("CP-GPB-32: con un atraso mayor que la duración estimada la barra va del 02/07 al 10/07 (9 días), con la etiqueta \"Atrasado 7 días\"")
    void enEjecucionConAtrasoMayorQueLaDuracion_llegaHastaHoy() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(3))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(7).build();

        // Act
        Barra barra = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(3), fecha(10))
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(10));
        assertThat(barra.getDiasDeAtraso()).isEqualTo(7);
        assertThat(barra.getEtiquetaAtraso()).isEqualTo("Atrasado 7 días");
    }

    @Test
    @DisplayName("CP-GPB-33: un lote en ejecución sin atraso no extiende su barra (02/07 al 11/07), no lleva etiqueta de atraso y su tooltip no tiene línea de atraso")
    void enEjecucionSinAtraso_noExtiendeNiLlevaEtiqueta() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(0).build();

        // Act
        Barra barra = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), fecha(6))
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(11));
        assertThat(barra.getIzquierdaPct()).isEqualTo(14.29);
        assertThat(barra.getAnchoPct()).isEqualTo(71.43);
        assertThat(barra.getDiasDeAtraso()).isEqualTo(0);
        assertThat(barra.getEtiquetaAtraso()).isNull();
        assertThat(barra.getTooltip()).doesNotContain("Atrasado");
    }

    @Test
    @DisplayName("CP-GPB-34: un lote pendiente corrido 3 días se dibuja del 11/07 al 15/07 (eje de 10 días), sin etiqueta de atraso, y la planificación sigue marcada en sus fechas")
    void pendienteCorrido_desplazaLasFechasDeLaBarra() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(8), fecha(12), fecha(10));

        // Assert
        assertThat(vista.getRangoDesde()).isEqualTo(fecha(7));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(16));
        assertThat(vista.getTotalDias()).isEqualTo(10);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("pendiente");
        assertThat(barra.getDesde()).isEqualTo(fecha(11));
        assertThat(barra.getHasta()).isEqualTo(fecha(15));
        assertThat(barra.getIzquierdaPct()).isEqualTo(40.0);
        assertThat(barra.getAnchoPct()).isEqualTo(50.0);
        assertThat(barra.getDiasDeAtraso()).isEqualTo(0);
        assertThat(barra.getEtiquetaAtraso()).isNull();
        assertThat(vista.getInicioPlanificacionPct()).isEqualTo(10.0);
        assertThat(vista.getFinPlanificacionPct()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("CP-GPB-35: un lote pendiente sin corrimiento usa sus fechas estimadas (05/07 al 08/07) y su tooltip no tiene línea de corrimiento")
    void pendienteSinCorrimiento_usaLasFechasEstimadas() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8))
                .diasDeDesplazamiento(0).build();

        // Act
        Barra barra = GanttPlanificacionBuilder.construir(List.of(lote), fecha(5), fecha(8), HOY_LEJANO)
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getDesde()).isEqualTo(fecha(5));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getIzquierdaPct()).isEqualTo(16.67);
        assertThat(barra.getAnchoPct()).isEqualTo(66.67);
        assertThat(barra.getTooltip()).doesNotContain("corrido");
    }

    @Test
    @DisplayName("CP-GPB-36: el tooltip del lote en ejecución atrasado dice cuánto se atrasó respecto de la fecha estimada (3 días y 1 día), antes de \"Inicio estimado:\"")
    void tooltipDelEnEjecucionAtrasado_dicePorCuantoSeAtraso() {
        // Arrange
        LoteCronogramaResponseDTO tresDias = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO unDia = loteB(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-02", fecha(1), fecha(9))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(1).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(tresDias, unDia), fecha(1), fecha(9), fecha(10));

        // Assert
        String tooltipTresDias = barraDeLote(vista, 1L).getTooltip();
        String tooltipUnDia = barraDeLote(vista, 2L).getTooltip();
        assertThat(tooltipTresDias).contains("Atrasado 3 días respecto de la fecha estimada (07/07)");
        assertThat(tooltipUnDia).contains("Atrasado 1 día respecto de la fecha estimada (09/07)");
        assertThat(tooltipTresDias.indexOf("Atrasado")).isGreaterThan(tooltipTresDias.indexOf("PALE_ALE-R7-1 - En ejecución"));
        assertThat(tooltipTresDias.indexOf("Atrasado")).isLessThan(tooltipTresDias.indexOf("Inicio estimado:"));
        assertThat(tooltipUnDia.indexOf("Atrasado")).isLessThan(tooltipUnDia.indexOf("Inicio estimado:"));
    }

    @Test
    @DisplayName("CP-GPB-37: el tooltip del lote pendiente corrido dice la fecha planificada y cuánto se corrió (3 días y 1 día), antes de \"Inicio estimado:\" que muestra la fecha guardada")
    void tooltipDelPendienteCorrido_diceLaFechaPlanificadaYElCorrimiento() {
        // Arrange
        LoteCronogramaResponseDTO tresDias = loteB(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();
        LoteCronogramaResponseDTO unDia = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(8), fecha(12))
                .diasDeDesplazamiento(1).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(tresDias, unDia), fecha(8), fecha(12), fecha(10));

        // Assert
        String tooltipTresDias = barraDeLote(vista, 1L).getTooltip();
        String tooltipUnDia = barraDeLote(vista, 2L).getTooltip();
        assertThat(tooltipTresDias).contains("Planificado para el 08/07, corrido 3 días por atraso");
        assertThat(tooltipUnDia).contains("Planificado para el 08/07, corrido 1 día por atraso");
        assertThat(tooltipTresDias.indexOf("Planificado para")).isLessThan(tooltipTresDias.indexOf("Inicio estimado:"));
        assertThat(tooltipUnDia.indexOf("Planificado para")).isLessThan(tooltipUnDia.indexOf("Inicio estimado:"));
        assertThat(tooltipTresDias).contains("Inicio estimado: 08/07/2027");
    }

    @Test
    @DisplayName("CP-GPB-38: sin atraso, sin corrimiento o sin fecha de finalización estimada que mostrar el tooltip no agrega ninguna leyenda")
    void tooltipSinAtrasoNiCorrimiento_noAgregaLeyendas() {
        // Arrange
        LoteCronogramaResponseDTO sinAtraso = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(0).build();
        LoteCronogramaResponseDTO sinCorrimiento = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(8), fecha(12))
                .diasDeDesplazamiento(0).build();
        LoteCronogramaResponseDTO sinFechaDeFin = loteB(3L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, "F-03", fecha(1), null)
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(sinAtraso, sinCorrimiento, sinFechaDeFin), fecha(1), fecha(12), fecha(10));

        // Assert
        for (Long id : List.of(1L, 2L, 3L)) {
            assertThat(barraDeLote(vista, id).getTooltip()).doesNotContain("Atrasado").doesNotContain("corrido");
        }
    }

    @Test
    @DisplayName("CP-GPB-39: el eje abarca las barras extendidas y corridas (30/06 al 16/07, 17 días) y las barras del carril no se superponen")
    void elEjeAbarcaLasBarrasExtendidasYCorridas() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enEjecucion, pendiente), fecha(1), fecha(5), fecha(10));

        // Assert
        assertThat(vista.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(16));
        assertThat(vista.getTotalDias()).isEqualTo(17);
        assertThat(vista.getCarriles()).hasSize(1);
        Barra barraEnEjecucion = barraDeLote(vista, 1L);
        Barra barraPendiente = barraDeLote(vista, 2L);
        assertThat(barraEnEjecucion.getIzquierdaPct()).isEqualTo(11.76);
        assertThat(barraEnEjecucion.getAnchoPct()).isEqualTo(52.94);
        assertThat(barraPendiente.getDesde()).isEqualTo(fecha(11));
        assertThat(barraPendiente.getHasta()).isEqualTo(fecha(15));
        assertThat(barraPendiente.getIzquierdaPct()).isEqualTo(64.71);
        assertThat(barraPendiente.getAnchoPct()).isEqualTo(29.41);
        assertThat(barraEnEjecucion.getHasta()).isBefore(barraPendiente.getDesde());
    }

    @Test
    @DisplayName("CP-GPB-40: un lote finalizado conserva sus fechas aunque llegue con atraso y corrimiento (sin etiqueta de atraso ni leyendas)")
    void finalizado_noSeVeAfectadoPorElAtrasoNiElCorrimiento() {
        // Arrange
        LoteCronogramaResponseDTO finalizado = loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0))
                .diasDeAtraso(3).diasDeDesplazamiento(3).build();

        // Act
        Barra barra = GanttPlanificacionBuilder.construir(List.of(finalizado), fecha(1), fecha(5), fecha(6))
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getDiasDeAtraso()).isEqualTo(0);
        assertThat(barra.getEtiquetaAtraso()).isNull();
        assertThat(barra.getTooltip()).doesNotContain("Atrasado").doesNotContain("corrido");
    }

    @Test
    @DisplayName("CP-GPB-41: el corrimiento no mueve un lote en ejecución y el atraso no extiende un lote pendiente: cada valor aplica solo a su estado")
    void cadaValorAplicaSoloASuEstado() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeDesplazamiento(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(8), fecha(12))
                .diasDeAtraso(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enEjecucion, pendiente), fecha(1), fecha(12), fecha(6));

        // Assert
        Barra barraEnEjecucion = barraDeLote(vista, 1L);
        assertThat(barraEnEjecucion.getDesde()).isEqualTo(fecha(2));
        assertThat(barraEnEjecucion.getHasta()).isEqualTo(fecha(11));
        Barra barraPendiente = barraDeLote(vista, 2L);
        assertThat(barraPendiente.getDesde()).isEqualTo(fecha(8));
        assertThat(barraPendiente.getHasta()).isEqualTo(fecha(12));
        assertThat(barraPendiente.getDiasDeAtraso()).isEqualTo(0);
        assertThat(barraPendiente.getEtiquetaAtraso()).isNull();
    }

    @Test
    @DisplayName("CP-GPB-42: un lote cancelado no amplía el eje ni genera carril: solo se dibuja el carril del lote pendiente (eje de 6 días)")
    void canceladoNoOcupaEspacioNiCarril() {
        // Arrange
        LoteCronogramaResponseDTO pendiente = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));
        LoteCronogramaResponseDTO cancelado = lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-02", fecha(1), fecha(20));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(pendiente, cancelado), fecha(5), fecha(8), HOY_LEJANO);

        // Assert
        assertThat(vista.getRangoDesde()).isEqualTo(fecha(4));
        assertThat(vista.getRangoHasta()).isEqualTo(fecha(9));
        assertThat(vista.getTotalDias()).isEqualTo(6);
        assertThat(vista.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01");
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(1);
        assertThat(vista.getCarriles().get(0).getBarras().get(0).getIzquierdaPct()).isEqualTo(16.67);
        assertThat(vista.getCarriles().get(0).getBarras().get(0).getAnchoPct()).isEqualTo(66.67);
    }

    @Test
    @DisplayName("CP-GPB-43: la etiqueta de atraso solo existe en el lote en ejecución atrasado y distingue singular y plural")
    void etiquetaDeAtraso_soloEnElLoteEnEjecucionAtrasado() {
        // Arrange
        LoteCronogramaResponseDTO tresDias = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO unDia = loteB(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-02", fecha(1), fecha(9))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(1).build();
        LoteCronogramaResponseDTO sinAtraso = loteB(3L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, "F-03", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(0).build();
        LoteCronogramaResponseDTO pendiente = loteB(4L, "PALE_ALE-R7-4", EstadoLote.PENDIENTE, "F-04", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();
        LoteCronogramaResponseDTO finalizado = loteB(5L, "PALE_ALE-R7-5", EstadoLote.FINALIZADO, "F-05", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0)).diasDeAtraso(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(tresDias, unDia, sinAtraso, pendiente, finalizado), fecha(1), fecha(12), fecha(10));

        // Assert
        assertThat(barraDeLote(vista, 1L).getEtiquetaAtraso()).isEqualTo("Atrasado 3 días");
        assertThat(barraDeLote(vista, 2L).getEtiquetaAtraso()).isEqualTo("Atrasado 1 día");
        assertThat(barraDeLote(vista, 3L).getEtiquetaAtraso()).isNull();
        assertThat(barraDeLote(vista, 4L).getEtiquetaAtraso()).isNull();
        assertThat(barraDeLote(vista, 5L).getEtiquetaAtraso()).isNull();
    }

    // ==================== Equipos del lote y carriles ====================

    @Test
    @DisplayName("CP-GPB-64: el tooltip del lote lista los equipos que usa (molino, macerador, olla de hervor y fermentador) y muestra \"-\" en los que no tiene")
    void elTooltipListaLosEquiposDelLote() {
        // Arrange
        LoteCronogramaResponseDTO completo = loteB(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(3))
                .identificadorInternoMolino("M-01").identificadorInternoMacerador("MA-01").identificadorInternoOllaHervor("O-01").build();
        LoteCronogramaResponseDTO sinEquipos = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, null, fecha(5), fecha(7)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(completo, sinEquipos), fecha(1), fecha(7), HOY_LEJANO);

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras().get(0).getTooltip())
                .endsWith("Molino: M-01\nMacerador: MA-01\nOlla de hervor: O-01\nFermentador: F-01");
        assertThat(vista.getCarriles().get(1).getBarras().get(0).getTooltip())
                .endsWith("Molino: -\nMacerador: -\nOlla de hervor: -\nFermentador: -");
    }

    @Test
    @DisplayName("CP-GPB-65: cada carril se identifica con el identificador de su fermentador y el de los lotes sin fermentador es \"Sin fermentador\"")
    void cadaCarrilSeIdentificaConSuFermentador() {
        // Arrange
        LoteCronogramaResponseDTO enF01 = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(3));
        LoteCronogramaResponseDTO sinFermentador = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, null, fecha(5), fecha(7));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enF01, sinFermentador), fecha(1), fecha(7), HOY_LEJANO);

        // Assert
        assertThat(vista.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "Sin fermentador");
    }

    // ==================== Limpieza del fermentador y demás equipamiento ====================

    @Test
    @DisplayName("CP-GPB-44: entre dos lotes vivos con un día libre en el futuro se dibuja una limpieza de un día (el 06/07, eje de 12 días)")
    void entreDosLotesVivos_conUnDiaLibreEnElFuturo_dibujaLaLimpieza() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeLimpieza(), fecha(1), fecha(10), fecha(1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(12);
        List<Barra> barras = vista.getCarriles().get(0).getBarras();
        assertThat(barras.get(0).getIzquierdaPct()).isEqualTo(8.33);
        assertThat(barras.get(0).getAnchoPct()).isEqualTo(41.67);
        assertThat(barras.get(1).getIzquierdaPct()).isEqualTo(58.33);
        assertThat(barras.get(1).getAnchoPct()).isEqualTo(33.33);
        assertThat(vista.getCarriles().get(0).getLimpiezas()).hasSize(1);
        Limpieza limpieza = vista.getCarriles().get(0).getLimpiezas().get(0);
        assertThat(limpieza.getFecha()).isEqualTo(fecha(6));
        assertThat(limpieza.getIzquierdaPct()).isEqualTo(50.0);
        assertThat(limpieza.getAnchoPct()).isEqualTo(8.33);
        assertThat(limpieza.getTooltip()).isEqualTo("Limpieza del fermentador y demás equipamiento");
        assertThat(limpieza.isAtenuada()).isFalse();
    }

    @Test
    @DisplayName("CP-GPB-45: dos lotes consecutivos (el segundo empieza el día siguiente al primero) no tienen limpieza")
    void lotesConsecutivos_noTienenLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(6), fecha(10));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, segundo), fecha(1), fecha(10), fecha(1));

        // Assert
        assertThat(vista.getCarriles().get(0).getLimpiezas()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("CP-GPB-46: con un hueco mayor a un día se dibuja igualmente solo el día siguiente al lote que termina (eje de 14 días)")
    void conUnHuecoMayorAUnDia_dibujaSoloElDiaSiguiente() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(10), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, segundo), fecha(1), fecha(12), fecha(1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(14);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(42.86);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.14);
    }

    @Test
    @DisplayName("CP-GPB-47: el último lote y el lote único no tienen limpieza")
    void elUltimoLoteYElLoteUnico_noTienenLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO unico = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista soloUno = GanttPlanificacionBuilder.construir(List.of(unico), fecha(5), fecha(8), fecha(1));
        GanttPlanificacionVista dos = GanttPlanificacionBuilder.construir(lotesDeLimpieza(), fecha(1), fecha(10), fecha(1));

        // Assert
        assertThat(soloUno.getCarriles().get(0).getLimpiezas()).isEmpty();
        assertThat(dos.getCarriles().get(0).getLimpiezas()).extracting(Limpieza::getFecha).containsExactly(fecha(6));
    }

    @Test
    @DisplayName("CP-GPB-48: una limpieza por cada día libre en el futuro antes de un lote posterior (04/07 y 08/07, eje de 13 días)")
    void unaLimpiezaPorCadaDiaLibreAntesDeUnLotePosterior() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeTresPendientes(), fecha(1), fecha(11), fecha(1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(13);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).extracting(Limpieza::getFecha).containsExactly(fecha(4), fecha(8));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(30.77);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.69);
        assertThat(limpiezas.get(1).getIzquierdaPct()).isEqualTo(61.54);
        assertThat(limpiezas.get(1).getAnchoPct()).isEqualTo(7.69);
    }

    @Test
    @DisplayName("CP-GPB-49: un lote cancelado no cubre el día de limpieza ni cuenta como lote siguiente")
    void unCanceladoNoCuentaParaLaLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO cancelado = lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-01", fecha(6), fecha(8));
        LoteCronogramaResponseDTO tercero = lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(10), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, cancelado, tercero), fecha(1), fecha(12), fecha(1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(14);
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(42.86);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.14);
    }

    @Test
    @DisplayName("CP-GPB-50: un lote cancelado posterior no es un lote siguiente: sin lote vivo posterior no hay limpieza")
    void unCanceladoPosterior_noEsUnLoteSiguiente() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO cancelado = lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-01", fecha(8), fecha(10));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, cancelado), fecha(1), fecha(10), fecha(1));

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(1);
        assertThat(vista.getCarriles().get(0).getLimpiezas()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPB-51: los carriles son independientes: la limpieza solo se calcula entre lotes del mismo fermentador")
    void losCarrilesSonIndependientesParaLaLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO enF01 = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO enF02 = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-02", fecha(10), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enF01, enF02), fecha(1), fecha(12), fecha(1));

        // Assert
        assertThat(vista.getCarriles()).hasSize(2);
        assertThat(vista.getCarriles()).allSatisfy(carril -> assertThat(carril.getLimpiezas()).isEmpty());
    }

    @Test
    @DisplayName("CP-GPB-52: el carril \"Sin fermentador\" no tiene limpieza")
    void elCarrilSinFermentador_noTieneLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, null, fecha(1), fecha(5));
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, null, fecha(10), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, segundo), fecha(1), fecha(12), fecha(1));

        // Assert
        assertThat(vista.getCarriles()).hasSize(1);
        assertThat(vista.getCarriles().get(0).getEtiqueta()).isEqualTo("Sin fermentador");
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
        assertThat(vista.getCarriles().get(0).getLimpiezas()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPB-53: con lotes solapados una barra que cubre el día de limpieza lo anula, y dos lotes que terminan el mismo día producen un único bloque")
    void lotesSolapados_unaBarraQueCubreElDiaLoAnula() {
        // Arrange
        List<LoteCronogramaResponseDTO> casoA = List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(10)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8)),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(12), fecha(14)));
        List<LoteCronogramaResponseDTO> casoB = List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(3), fecha(5)),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(8), fecha(9)));

        // Act
        GanttPlanificacionVista vistaA = GanttPlanificacionBuilder.construir(casoA, fecha(1), fecha(14), fecha(1));
        GanttPlanificacionVista vistaB = GanttPlanificacionBuilder.construir(casoB, fecha(1), fecha(9), fecha(1));

        // Assert
        assertThat(vistaA.getTotalDias()).isEqualTo(16);
        List<Limpieza> limpiezasA = vistaA.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezasA).hasSize(1);
        assertThat(limpiezasA.get(0).getFecha()).isEqualTo(fecha(11));
        assertThat(limpiezasA.get(0).getIzquierdaPct()).isEqualTo(68.75);
        assertThat(limpiezasA.get(0).getAnchoPct()).isEqualTo(6.25);
        assertThat(vistaB.getTotalDias()).isEqualTo(11);
        List<Limpieza> limpiezasB = vistaB.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezasB).hasSize(1);
        assertThat(limpiezasB.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezasB.get(0).getIzquierdaPct()).isEqualTo(54.55);
        assertThat(limpiezasB.get(0).getAnchoPct()).isEqualTo(9.09);
    }

    @Test
    @DisplayName("CP-GPB-54: la limpieza sigue a un lote en ejecución y no a uno finalizado (solo el 09/07, no el 04/07)")
    void laLimpiezaSigueAUnLoteEnEjecucionYNoAUnFinalizado() {
        // Arrange
        LoteCronogramaResponseDTO finalizado = loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(3))
                .fechaInicio(fecha(1).atTime(8, 0)).fechaFinalizacion(fecha(3).atTime(17, 0)).build();
        LoteCronogramaResponseDTO enEjecucion = loteB(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01", fecha(5), fecha(8))
                .fechaInicio(fecha(5).atTime(8, 0)).build();
        LoteCronogramaResponseDTO pendiente = lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(10), fecha(12));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(finalizado, enEjecucion, pendiente), fecha(1), fecha(12), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(14);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(9));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(64.29);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.14);
    }

    @Test
    @DisplayName("CP-GPB-55: la limpieza va después del tramo extendido por el atraso (el 11/07, eje de 18 días)")
    void laLimpiezaVaDespuesDelTramoExtendidoPorElAtraso() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(12), fecha(16));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(enEjecucion, pendiente), fecha(1), fecha(16), fecha(10));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(18);
        List<Barra> barras = vista.getCarriles().get(0).getBarras();
        assertThat(barras.get(0).getIzquierdaPct()).isEqualTo(11.11);
        assertThat(barras.get(0).getAnchoPct()).isEqualTo(50.0);
        assertThat(barras.get(1).getIzquierdaPct()).isEqualTo(66.67);
        assertThat(barras.get(1).getAnchoPct()).isEqualTo(27.78);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(11));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(61.11);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(5.56);
    }

    @Test
    @DisplayName("CP-GPB-56: la limpieza usa la posición corrida del lote siguiente: el corrimiento deja libres el 06/07 y el 07/07")
    void laLimpiezaUsaLaPosicionCorridaDelLoteSiguiente() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO corrido = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(6), fecha(9))
                .diasDeDesplazamiento(2).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, corrido), fecha(1), fecha(9), fecha(1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(13);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(46.15);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.69);
        Barra barraCorrida = barraDeLote(vista, 2L);
        assertThat(barraCorrida.getIzquierdaPct()).isEqualTo(61.54);
        assertThat(barraCorrida.getAnchoPct()).isEqualTo(30.77);
    }

    @Test
    @DisplayName("CP-GPB-57: el bloque de limpieza respeta el ancho mínimo de 1 % cuando un día ocupa menos")
    void laLimpiezaRespetaElAnchoMinimo() {
        // Arrange
        LocalDate primerDia = LocalDate.of(2027, 6, 10);
        LocalDate segundoDia = LocalDate.of(2027, 6, 20);
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", primerDia, primerDia);
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", segundoDia, segundoDia);

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(
                List.of(primero, segundo), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), LocalDate.of(2027, 6, 1));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(367);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(LocalDate.of(2027, 6, 11));
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(1.0);
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(44.14);
    }

    @Test
    @DisplayName("CP-GPB-58: un lote sin fechas no participa de la limpieza: ni se dibuja ni da paso")
    void unLoteSinFechas_noParticipaDeLaLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO sinFechas = LoteCronogramaResponseDTO.builder()
                .id(2L).identificadorInterno("PALE_ALE-R7-2").estado(EstadoLote.PENDIENTE).identificadorInternoFermentador("F-01").build();
        LoteCronogramaResponseDTO tercero = lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(7), fecha(10));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(primero, sinFechas, tercero), fecha(1), fecha(10), fecha(1));

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(8.33);
    }

    @Test
    @DisplayName("CP-GPB-59: una limpieza anterior a hoy no se dibuja (la limpieza real se registra fuera del diagrama)")
    void unaLimpiezaAnteriorAHoy_noSeDibuja() {
        for (LocalDate hoy : List.of(fecha(7), fecha(10))) {
            // Act
            GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeLimpieza(), fecha(1), fecha(10), hoy);

            // Assert
            assertThat(vista.getTotalDias()).isEqualTo(12);
            assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
            assertThat(vista.getCarriles().get(0).getLimpiezas()).as("hoy %s", hoy).isEmpty();
        }
    }

    @Test
    @DisplayName("CP-GPB-60: la limpieza del día de hoy sí se dibuja")
    void laLimpiezaDeHoy_seDibuja() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeLimpieza(), fecha(1), fecha(10), fecha(6));

        // Assert
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(8.33);
    }

    @Test
    @DisplayName("CP-GPB-61: un lote finalizado no origina limpieza aunque el día siguiente sea hoy")
    void unLoteFinalizado_noOriginaLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO finalizado = loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(3))
                .fechaInicio(fecha(1).atTime(8, 0)).fechaFinalizacion(fecha(3).atTime(17, 0)).build();
        LoteCronogramaResponseDTO pendiente = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(6), fecha(9));

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(finalizado, pendiente), fecha(1), fecha(9), fecha(4));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(11);
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
        assertThat(vista.getCarriles().get(0).getLimpiezas()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPB-62: solo se dibujan las limpiezas que todavía no pasaron (con hoy 06/07 solo la del 08/07)")
    void soloSeDibujanLasLimpiezasQueTodaviaNoPasaron() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeTresPendientes(), fecha(1), fecha(11), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(13);
        List<Limpieza> limpiezas = vista.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(8));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(61.54);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.69);
    }

    @Test
    @DisplayName("CP-GPB-63: sin la fecha de hoy no se puede saber qué limpiezas son futuras y no se dibuja ninguna")
    void sinHoy_noSeDibujaNingunaLimpieza() {
        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(lotesDeLimpieza(), fecha(1), fecha(10), null);

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2);
        assertThat(vista.getCarriles().get(0).getLimpiezas()).isNotNull().isEmpty();
        assertThat(vista.getHoyPct()).isNull();
    }

    @Test
    @DisplayName("CP-GPB-66: el eje de una planificación abarca todas sus barras, así que ninguna se recorta ni dice que continúa fuera del período")
    void elDiagramaDeUnaPlanificacion_noRecortaBarras() {
        // Arrange
        LoteCronogramaResponseDTO pendiente = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO enEjecucion = loteB(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(pendiente, enEjecucion), fecha(1), fecha(10), fecha(10));

        // Assert
        assertThat(vista.getCarriles().get(0).getBarras()).hasSize(2).allSatisfy(barra -> {
            assertThat(barra.isContinuaAntes()).isFalse();
            assertThat(barra.isContinuaDespues()).isFalse();
            assertThat(barra.getTooltip()).doesNotContain("Continúa antes del período").doesNotContain("Continúa después del período");
        });
    }

    // ==================== Datos de prueba ====================

    /** Dos lotes pendientes de "F-01" con un día libre entre ellos: del 01/07 al 05/07 y del 07/07 al 10/07. */
    private static List<LoteCronogramaResponseDTO> lotesDeLimpieza() {
        return List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(7), fecha(10)));
    }

    /** Tres lotes pendientes de "F-01" con un día libre entre cada par: 01/07 al 03/07, 05/07 al 07/07 y 09/07 al 11/07. */
    private static List<LoteCronogramaResponseDTO> lotesDeTresPendientes() {
        return List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(3)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(7)),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", fecha(9), fecha(11)));
    }

    private static Barra barraDeLote(GanttPlanificacionVista vista, Long idLote) {
        return vista.getCarriles().stream()
                .flatMap(carril -> carril.getBarras().stream())
                .filter(barra -> idLote.equals(barra.getIdLote()))
                .findFirst()
                .orElseThrow();
    }

    private static LocalDate fecha(int dia) {
        return LocalDate.of(2027, 7, dia);
    }

    private static LoteCronogramaResponseDTO lote(Long id, String identificador, EstadoLote estado,
                                                  String fermentador, LocalDate inicioEstimado, LocalDate finEstimado) {
        return loteB(id, identificador, estado, fermentador, inicioEstimado, finEstimado).build();
    }

    /**
     * Arma un lote con lo común y deja el builder abierto para ajustar campos sueltos.
     */
    private static LoteCronogramaResponseDTO.LoteCronogramaResponseDTOBuilder loteB(Long id, String identificador, EstadoLote estado,
                                                                                    String fermentador, LocalDate inicioEstimado, LocalDate finEstimado) {
        return LoteCronogramaResponseDTO.builder()
                .id(id)
                .identificadorInterno(identificador)
                .estado(estado)
                .volumenObjetivo(300.0)
                .fechaInicioEstimada(inicioEstimado)
                .fechaFinalizacionEstimada(finEstimado)
                .identificadorInternoFermentador(fermentador);
    }
}
