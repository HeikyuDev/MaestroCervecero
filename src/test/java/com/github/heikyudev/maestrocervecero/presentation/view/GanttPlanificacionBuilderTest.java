package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
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
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8))
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
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-1 - Pendiente");
        assertThat(barra.getDesde()).isEqualTo(fecha(5));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getIzquierdaPct()).isEqualTo(16.67);
        assertThat(barra.getAnchoPct()).isEqualTo(66.67);
        assertThat(barra.getProgresoPct()).isEqualTo(0.0);
        assertThat(barra.getTooltip()).isEqualTo(String.join("\n",
                "PALE_ALE-1 - Pendiente",
                "Inicio estimado: 05/07/2027",
                "Fin estimado: 08/07/2027",
                "Inicio real: -",
                "Fin real: -",
                "Volumen: 300 L",
                "Molino: M-01",
                "Macerador: MA-01",
                "Olla de hervor: O-01"));
    }

    @Test
    @DisplayName("CP-GPB-04: dos lotes simultáneos en distintos fermentadores van en dos carriles, ordenados por identificador, con la misma geometría")
    void dosLotesConcurrentes_vanEnDosCarriles() {
        // Arrange
        LoteCronogramaResponseDTO enF02 = lote(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-02", fecha(5), fecha(8));
        LoteCronogramaResponseDTO enF01 = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));

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
        LoteCronogramaResponseDTO segundo = lote(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-01", fecha(6), fecha(10));
        LoteCronogramaResponseDTO primero = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(5));

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
    @DisplayName("CP-GPB-06: un lote en ejecución va del inicio real al fin estimado, con 5 de sus 10 días ya transcurridos (progreso 50 %)")
    void enEjecucion_vaDelInicioRealAlFinEstimadoConProgreso() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(14);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("en-ejecucion");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-1 - En ejecución");
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(11));
        assertThat(barra.getIzquierdaPct()).isEqualTo(14.29);
        assertThat(barra.getAnchoPct()).isEqualTo(71.43);
        assertThat(barra.getProgresoPct()).isEqualTo(50.0);
        assertThat(barra.getTooltip()).contains("Inicio real: 02/07/2027 08:00");
    }

    @Test
    @DisplayName("CP-GPB-07: el progreso de un lote en ejecución es 0 % antes de su inicio y 100 % después de su fin estimado")
    void enEjecucion_elProgresoQuedaAcotado() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();

        // Act
        Barra antes = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), LocalDate.of(2027, 6, 20))
                .getCarriles().get(0).getBarras().get(0);
        Barra despues = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), LocalDate.of(2027, 8, 1))
                .getCarriles().get(0).getBarras().get(0);
        Barra elUltimoDia = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(12), fecha(11))
                .getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(antes.getProgresoPct()).isEqualTo(0.0);
        assertThat(despues.getProgresoPct()).isEqualTo(100.0);
        assertThat(elUltimoDia.getProgresoPct()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("CP-GPB-08: un lote finalizado va del inicio real al fin real (7 días de un eje de 10) y sin progreso")
    void finalizado_vaDelInicioRealAlFinReal() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(5), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(10);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-1 - Finalizado");
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(8));
        assertThat(barra.getIzquierdaPct()).isEqualTo(20.0);
        assertThat(barra.getAnchoPct()).isEqualTo(70.0);
        assertThat(barra.getProgresoPct()).isEqualTo(0.0);
        assertThat(barra.getTooltip()).contains("Fin real: 08/07/2027 17:00");
    }

    @Test
    @DisplayName("CP-GPB-09: un lote cancelado que había arrancado va del inicio real a la cancelación (3 días de un eje de 12)")
    void canceladoConInicioReal_vaDelInicioALaCancelacion() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.CANCELADO, "F-01", fecha(1), fecha(10))
                .fechaInicio(fecha(3).atTime(10, 0)).fechaCancelacion(fecha(5).atTime(15, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(10), fecha(6));

        // Assert
        assertThat(vista.getTotalDias()).isEqualTo(12);
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("cancelado");
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-1 - Cancelado");
        assertThat(barra.getDesde()).isEqualTo(fecha(3));
        assertThat(barra.getHasta()).isEqualTo(fecha(5));
        assertThat(barra.getIzquierdaPct()).isEqualTo(25.0);
        assertThat(barra.getAnchoPct()).isEqualTo(25.0);
        assertThat(barra.getTooltip()).contains("Cancelación: 05/07/2027 15:00");
    }

    @Test
    @DisplayName("CP-GPB-10: un lote cancelado que nunca arrancó usa sus fechas estimadas (3 días de un eje de 12)")
    void canceladoSinInicioReal_usaLasFechasEstimadas() {
        // Arrange
        LoteCronogramaResponseDTO lote = loteB(1L, "PALE_ALE-1", EstadoLote.CANCELADO, "F-01", fecha(2), fecha(4))
                .fechaCancelacion(fecha(1).atTime(12, 0)).build();

        // Act
        GanttPlanificacionVista vista = GanttPlanificacionBuilder.construir(List.of(lote), fecha(1), fecha(10), fecha(6));

        // Assert
        Barra barra = vista.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getDesde()).isEqualTo(fecha(2));
        assertThat(barra.getHasta()).isEqualTo(fecha(4));
        assertThat(barra.getIzquierdaPct()).isEqualTo(16.67);
        assertThat(barra.getAnchoPct()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("CP-GPB-11: si faltan fechas reales se usan las estimadas, y un fin anterior al inicio se acota a un solo día")
    void fechasFaltantesOInvertidas_seAcotan() {
        // Arrange
        LoteCronogramaResponseDTO finalizadoSinFechasReales = lote(1L, "PALE_ALE-1", EstadoLote.FINALIZADO, "F-01", fecha(1), fecha(5));
        LoteCronogramaResponseDTO invertido = lote(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-02", fecha(10), fecha(5));

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
                .id(1L).identificadorInterno("PALE_ALE-1").estado(EstadoLote.PENDIENTE).identificadorInternoFermentador("F-01").build();

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
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", fecha(1), fecha(12));

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
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", fecha(5), fecha(8));

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
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01",
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
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01",
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

    // ==================== Datos de prueba ====================

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
