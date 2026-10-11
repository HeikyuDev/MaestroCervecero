package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ColaFermentador;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Alineacion;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Limpieza;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.MarcaEje;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CronogramaProduccionBuilderTest {

    private static final LocalDate HOY = LocalDate.of(2027, 7, 3);

    /** Período de los casos que no nombran otro: del 01/07 al 20/07/2027 (20 días). */
    private static final LocalDate DESDE = fecha(1);
    private static final LocalDate HASTA = fecha(20);

    // ==================== Carriles ====================

    @Test
    @DisplayName("CP-GCB-01: sin lotes cada fermentador recibido tiene su carril vacío (ordenados por identificador), el eje es el período y no hay marcas de planificación")
    void sinLotes_cadaFermentadorTieneSuCarrilVacio() {
        // Act
        CronogramaProduccionVista vista = construir(List.of(), List.of("F-02", "F-01"), null, fecha(10), fecha(9), fecha(11));

        // Assert
        GanttPlanificacionVista gantt = vista.getGantt();
        assertThat(gantt.isVacio()).isTrue();
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(gantt.getCarriles()).allSatisfy(c -> assertThat(c.getBarras()).isEmpty());
        assertThat(gantt.getRangoDesde()).isEqualTo(fecha(9));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(11));
        assertThat(gantt.getTotalDias()).isEqualTo(3);
        assertThat(gantt.getHoyPct()).isEqualTo(50.0);
        assertThat(gantt.getInicioPlanificacionPct()).isNull();
        assertThat(gantt.getFinPlanificacionPct()).isNull();
        assertThat(gantt.isPlanificacionResaltada()).isFalse();
        assertThat(vista.getPagina()).isEqualTo(1);
        assertThat(vista.getTotalPaginas()).isEqualTo(1);
        assertThat(vista.getTotalFermentadores()).isEqualTo(2);
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("F-01", "F-02");
        assertThat(vista.getColas()).allSatisfy(c -> {
            assertThat(c.getEnEjecucion()).isNull();
            assertThat(c.getPendientes()).isEmpty();
        });
    }

    @Test
    @DisplayName("CP-GCB-02: listas nulas no rompen: modelo vacío, sin carriles ni colas, con el eje del período")
    void listasNulas_devuelvenUnModeloVacio() {
        // Act
        CronogramaProduccionVista vista = construir(null, null, null, fecha(10), fecha(9), fecha(11));

        // Assert
        assertThat(vista.getGantt().isVacio()).isTrue();
        assertThat(vista.getGantt().getCarriles()).isEmpty();
        assertThat(vista.getColas()).isEmpty();
        assertThat(vista.getGantt().getTotalDias()).isEqualTo(3);
        assertThat(vista.getPagina()).isEqualTo(1);
        assertThat(vista.getTotalPaginas()).isEqualTo(1);
        assertThat(vista.getTotalFermentadores()).isZero();
    }

    @Test
    @DisplayName("CP-GCB-03: lotes de dos planificaciones conviven intercalados en el carril de un fermentador, ordenados por inicio (eje de 12 días: 8,33 % / 41,67 % y 58,33 % / 33,33 %)")
    void lotesDeDosPlanificaciones_conviveEnUnMismoCarril() {
        // Arrange: se pasan en desorden
        LoteCronogramaResponseDTO stout = lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(7), fecha(10));
        LoteCronogramaResponseDTO pale = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        CronogramaProduccionVista vista = construir(List.of(stout, pale), List.of(), null, HOY, LocalDate.of(2027, 6, 30), fecha(11));

        // Assert
        GanttPlanificacionVista gantt = vista.getGantt();
        assertThat(gantt.isVacio()).isFalse();
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(11));
        assertThat(gantt.getTotalDias()).isEqualTo(12);
        assertThat(gantt.getCarriles()).hasSize(1);
        List<Barra> barras = gantt.getCarriles().get(0).getBarras();
        assertThat(barras).extracting(Barra::getIdLote).containsExactly(1L, 2L);
        assertThat(barras.get(0).getIzquierdaPct()).isEqualTo(8.33);
        assertThat(barras.get(0).getAnchoPct()).isEqualTo(41.67);
        assertThat(barras.get(1).getIzquierdaPct()).isEqualTo(58.33);
        assertThat(barras.get(1).getAnchoPct()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("CP-GCB-04: la etiqueta de la barra dice receta, planificación y lote; el tooltip suma la receta y la planificación a los datos del lote")
    void laBarraSeIdentificaConRecetaPlanificacionYLote() {
        // Arrange
        LoteCronogramaResponseDTO pale = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        Barra barra = construir(List.of(pale), List.of("F-01"), null).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getEtiqueta()).isEqualTo("Pale Ale - Plan #7 - PALE_ALE-R7-1");
        assertThat(barra.getEstadoClave()).isEqualTo("pendiente");
        assertThat(barra.getTooltip()).startsWith("PALE_ALE-R7-1 - Pendiente\nReceta: Pale Ale\nPlanificación: #7\nInicio estimado: 01/07/2027\n");
        assertThat(barra.getTooltip()).contains("Fin estimado: 05/07/2027", "Volumen: 300 L", "Molino: M-01", "Macerador: MA-01", "Olla de hervor: O-01", "Fermentador: F-01");
    }

    @Test
    @DisplayName("CP-GCB-05: sin receta ni planificación la etiqueta se reduce al identificador del lote")
    void sinRecetaNiPlanificacion_laEtiquetaEsElIdentificador() {
        // Arrange
        LoteCronogramaResponseDTO huerfano = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", null, null, fecha(1), fecha(5));

        // Act
        Barra barra = construir(List.of(huerfano), List.of(), null).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-R7-1");
        assertThat(barra.getTooltip()).contains("Receta: -", "Planificación: -");
    }

    // ==================== Planificación destacada ====================

    @Test
    @DisplayName("CP-GCB-06: al destacar una planificación solo se atenúan las barras de las demás; sin destacar ninguna no se atenúa nada")
    void destacarUnaPlanificacion_atenuaLasDemas() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(7), fecha(10)));

        // Act
        GanttPlanificacionVista destacada = construir(lotes, List.of("F-01"), 7L).getGantt();
        GanttPlanificacionVista sinDestacar = construir(lotes, List.of("F-01"), null).getGantt();

        // Assert
        assertThat(destacada.isPlanificacionResaltada()).isTrue();
        assertThat(destacada.getCarriles().get(0).getBarras()).extracting(Barra::isAtenuada).containsExactly(false, true);
        assertThat(sinDestacar.isPlanificacionResaltada()).isFalse();
        assertThat(sinDestacar.getCarriles().get(0).getBarras()).extracting(Barra::isAtenuada).containsExactly(false, false);
    }

    @Test
    @DisplayName("CP-GCB-07: destacar una planificación sin lotes atenúa todas las barras; las colas marcan como atenuados los lotes de las demás, también el que está en ejecución")
    void destacar_tambienAtenuaLasFilasDeLaCola() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(7), fecha(10)),
                lote(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(12), fecha(15)));

        // Act
        CronogramaProduccionVista conPlan7 = construir(lotes, List.of("F-01"), 7L);
        CronogramaProduccionVista conPlan9 = construir(lotes, List.of("F-01"), 9L);

        // Assert
        ColaFermentador cola7 = conPlan7.getColas().get(0);
        assertThat(cola7.isEnEjecucionAtenuado()).isFalse();
        assertThat(cola7.getPendientes()).extracting(i -> i.isAtenuado()).containsExactly(false, true);
        ColaFermentador cola9 = conPlan9.getColas().get(0);
        assertThat(cola9.isEnEjecucionAtenuado()).isTrue();
        assertThat(cola9.getPendientes()).extracting(i -> i.isAtenuado()).containsExactly(true, true);
        assertThat(conPlan9.getGantt().getCarriles().get(0).getBarras()).extracting(Barra::isAtenuada).containsExactly(true, true, true);
    }

    // ==================== Eje ====================

    @Test
    @DisplayName("CP-GCB-08: hoy dentro del período, después de las barras (04/07 a 21/07, 18 días, hoy al 91,67 %)")
    void hoyDentroDelPeriodoDespuesDeLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista gantt = construir(List.of(lote), List.of("F-01"), null, fecha(20), fecha(4), fecha(21)).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(fecha(4));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(21));
        assertThat(gantt.getTotalDias()).isEqualTo(18);
        assertThat(gantt.getHoyPct()).isEqualTo(91.67);
        assertThat(gantt.getHoyAlineacion()).isEqualTo(Alineacion.CENTRO);
    }

    @Test
    @DisplayName("CP-GCB-09: hoy dentro del período, antes de las barras (30/06 a 09/07, 10 días, hoy al 15 %)")
    void hoyDentroDelPeriodoAntesDeLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista gantt = construir(List.of(lote), List.of("F-01"), null, fecha(1), LocalDate.of(2027, 6, 30), fecha(9)).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(9));
        assertThat(gantt.getTotalDias()).isEqualTo(10);
        assertThat(gantt.getHoyPct()).isEqualTo(15.0);
        assertThat(gantt.getCarriles().get(0).getBarras().get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(gantt.getCarriles().get(0).getBarras().get(0).getAnchoPct()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("CP-GCB-10: un lote en ejecución va del inicio real (02/07) al fin estimado (11/07) y los finalizados siguen sus reglas")
    void conservaLasReglasDeCadaEstado() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();
        LoteCronogramaResponseDTO finalizado = loteB(2L, "PALE_ALE-R7-2", EstadoLote.FINALIZADO, "F-02", 7L, "Pale Ale", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0)).build();

        // Act
        GanttPlanificacionVista gantt = construir(List.of(enEjecucion, finalizado), List.of("F-01", "F-02"), null,
                fecha(6), LocalDate.of(2027, 6, 30), fecha(12)).getGantt();

        // Assert
        Barra barraEnEjecucion = gantt.getCarriles().get(0).getBarras().get(0);
        assertThat(barraEnEjecucion.getEstadoClave()).isEqualTo("en-ejecucion");
        assertThat(barraEnEjecucion.getDesde()).isEqualTo(fecha(2));
        assertThat(barraEnEjecucion.getHasta()).isEqualTo(fecha(11));
        Barra barraFinalizada = gantt.getCarriles().get(1).getBarras().get(0);
        assertThat(barraFinalizada.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barraFinalizada.getDesde()).isEqualTo(fecha(2));
        assertThat(barraFinalizada.getHasta()).isEqualTo(fecha(8));
    }

    // ==================== Carriles especiales ====================

    @Test
    @DisplayName("CP-GCB-11: un fermentador con lotes y uno recibido como carril fijo tienen su carril, ordenado sin distinguir mayúsculas, y su cola")
    void carrilesFijosYCarrilesDeLosLotes() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(3L, "A-3", EstadoLote.PENDIENTE, "f-01", 7L, "Pale Ale", fecha(1), fecha(5)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-02"), null);

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta).containsExactly("f-01", "F-02", "F-03");
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("f-01", "F-02", "F-03");
        assertThat(vista.getTotalFermentadores()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-GCB-12: un lote sin ninguna fecha no se dibuja pero sigue en la cola de su fermentador, al final")
    void unLoteSinFechasNoSeDibujaPeroEstaEnLaCola() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", null, null),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-01"), null);

        // Assert
        assertThat(vista.getGantt().getCarriles().get(0).getBarras()).extracting(Barra::getIdLote).containsExactly(2L);
        assertThat(vista.getColas().get(0).getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(2L, 1L);
    }

    // ==================== Colas ====================

    @Test
    @DisplayName("CP-GCB-13: la cola ordena los pendientes por inicio estimado y, ante igual fecha, por id; numera desde 1 y marca el primero y el último")
    void laColaSeOrdenaYSeNumera() {
        // Arrange: en desorden
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(15), fecha(18)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(5), fecha(8)));

        // Act
        ColaFermentador cola = construir(lotes, List.of("F-01"), null).getColas().get(0);

        // Assert
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(1L, 2L, 3L);
        assertThat(cola.getPendientes()).extracting(i -> i.getPosicion()).containsExactly(1, 2, 3);
        assertThat(cola.getPendientes()).extracting(i -> i.isPrimero()).containsExactly(true, false, false);
        assertThat(cola.getPendientes()).extracting(i -> i.isUltimo()).containsExactly(false, false, true);
    }

    @Test
    @DisplayName("CP-GCB-14: con un solo pendiente es a la vez el primero y el último (no puede subir ni bajar)")
    void conUnSoloPendiente_esPrimeroYUltimo() {
        // Arrange
        LoteCronogramaResponseDTO unico = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        ColaFermentador cola = construir(List.of(unico), List.of("F-01"), null).getColas().get(0);

        // Assert
        assertThat(cola.getPendientes()).hasSize(1);
        assertThat(cola.getPendientes().get(0).isPrimero()).isTrue();
        assertThat(cola.getPendientes().get(0).isUltimo()).isTrue();
        assertThat(cola.getPendientes().get(0).getPosicion()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-GCB-15: la cola informa aparte el lote en ejecución y deja afuera los finalizados y cancelados (que además no se dibujan); las colas de otros fermentadores no se mezclan")
    void laColaSoloTienePendientesYElEnEjecucionVaAparte() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", fecha(1), fecha(2)),
                lote(2L, "A-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(3), fecha(6)),
                lote(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(10)),
                lote(4L, "A-4", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", fecha(12), fecha(14)),
                lote(5L, "B-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", fecha(8), fecha(10)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-01", "F-02"), null);

        // Assert
        ColaFermentador f01 = vista.getColas().get(0);
        assertThat(f01.getFermentador()).isEqualTo("F-01");
        assertThat(f01.getEnEjecucion().getId()).isEqualTo(2L);
        assertThat(f01.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(3L);
        ColaFermentador f02 = vista.getColas().get(1);
        assertThat(f02.getEnEjecucion()).isNull();
        assertThat(f02.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(5L);
        assertThat(vista.getGantt().getCarriles().get(0).getBarras()).hasSize(3);
    }

    // ==================== Atraso y corrimiento ====================

    @Test
    @DisplayName("CP-GCB-16: en un carril el lote en ejecución atrasado llega a hoy (02/07 al 10/07, con la etiqueta \"Atrasado 3 días\") y el pendiente se dibuja corrido 3 días (11/07 al 15/07) en un eje de 16 días, sin limpiezas")
    void atrasoYCorrimientoEnUnCarril_extiendenYDesplazanLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        GanttPlanificacionVista gantt = construir(List.of(enEjecucion, pendiente), List.of("F-01"), null, fecha(10), fecha(1), fecha(16)).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(fecha(1));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(16));
        assertThat(gantt.getTotalDias()).isEqualTo(16);
        List<Barra> barras = gantt.getCarriles().get(0).getBarras();
        Barra barraEnEjecucion = barras.get(0);
        assertThat(barraEnEjecucion.getIdLote()).isEqualTo(1L);
        assertThat(barraEnEjecucion.getDesde()).isEqualTo(fecha(2));
        assertThat(barraEnEjecucion.getHasta()).isEqualTo(fecha(10));
        assertThat(barraEnEjecucion.getIzquierdaPct()).isEqualTo(6.25);
        assertThat(barraEnEjecucion.getAnchoPct()).isEqualTo(56.25);
        assertThat(barraEnEjecucion.getDiasDeAtraso()).isEqualTo(3);
        assertThat(barraEnEjecucion.getEtiquetaAtraso()).isEqualTo("Atrasado 3 días");
        Barra barraPendiente = barras.get(1);
        assertThat(barraPendiente.getIdLote()).isEqualTo(2L);
        assertThat(barraPendiente.getDesde()).isEqualTo(fecha(11));
        assertThat(barraPendiente.getHasta()).isEqualTo(fecha(15));
        assertThat(barraPendiente.getIzquierdaPct()).isEqualTo(62.5);
        assertThat(barraPendiente.getAnchoPct()).isEqualTo(31.25);
        assertThat(barraPendiente.getEtiquetaAtraso()).isNull();
        assertThat(gantt.getCarriles().get(0).getLimpiezas()).isEmpty();
    }

    @Test
    @DisplayName("CP-GCB-17: el tooltip suma las leyendas de atraso y corrimiento después de \"Receta\" y \"Planificación\" y antes de \"Inicio estimado:\"")
    void tooltipConAtrasoYCorrimiento_ubicaLasLeyendasDespuesDeRecetaYPlanificacion() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        List<Barra> barras = construir(List.of(enEjecucion, pendiente), List.of("F-01"), null, fecha(10), fecha(1), fecha(16))
                .getGantt().getCarriles().get(0).getBarras();

        // Assert
        String tooltipEnEjecucion = barras.get(0).getTooltip();
        assertThat(tooltipEnEjecucion).contains("Receta: Pale Ale", "Planificación: #7", "Atrasado 3 días respecto de la fecha estimada (07/07)");
        assertThat(tooltipEnEjecucion.indexOf("Receta: Pale Ale")).isLessThan(tooltipEnEjecucion.indexOf("Planificación: #7"));
        assertThat(tooltipEnEjecucion.indexOf("Planificación: #7")).isLessThan(tooltipEnEjecucion.indexOf("Atrasado"));
        assertThat(tooltipEnEjecucion.indexOf("Atrasado")).isLessThan(tooltipEnEjecucion.indexOf("Inicio estimado:"));
        String tooltipPendiente = barras.get(1).getTooltip();
        assertThat(tooltipPendiente).contains("Planificado para el 08/07, corrido 3 días por atraso");
        assertThat(tooltipPendiente.indexOf("Planificación: #7")).isLessThan(tooltipPendiente.indexOf("Planificado para"));
        assertThat(tooltipPendiente.indexOf("Planificado para")).isLessThan(tooltipPendiente.indexOf("Inicio estimado:"));
    }

    @Test
    @DisplayName("CP-GCB-18: la cola ordena los pendientes por fechas estimadas guardadas y conserva el corrimiento de cada lote; el lote en ejecución conserva su atraso")
    void laColaConservaElAtrasoYElCorrimientoSinAlterarElOrden() {
        // Arrange: se pasan en desorden
        List<LoteCronogramaResponseDTO> lotes = List.of(
                loteB(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(13), fecha(18)).diasDeDesplazamiento(3).build(),
                loteB(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12)).diasDeDesplazamiento(3).build(),
                loteB(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                        .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build());

        // Act
        ColaFermentador cola = construir(lotes, List.of("F-01"), null, fecha(10), DESDE, HASTA).getColas().get(0);

        // Assert
        assertThat(cola.getEnEjecucion().getId()).isEqualTo(1L);
        assertThat(cola.getEnEjecucion().getDiasDeAtraso()).isEqualTo(3);
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(2L, 3L);
        assertThat(cola.getPendientes()).extracting(i -> i.getPosicion()).containsExactly(1, 2);
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getDiasDeDesplazamiento()).containsExactly(3, 3);
    }

    @Test
    @DisplayName("CP-GCB-19: al destacar otra planificación el lote en ejecución atrasado se atenúa pero conserva su atraso (3 días) y su etiqueta")
    void unLoteAtenuado_conservaSuAtraso() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "STOUT-R8-1", EstadoLote.EN_EJECUCION, "F-01", 8L, "Stout", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        List<Barra> barras = construir(List.of(enEjecucion, pendiente), List.of("F-01"), 7L, fecha(10), fecha(1), fecha(16))
                .getGantt().getCarriles().get(0).getBarras();

        // Assert
        Barra barraAtenuada = barras.stream().filter(b -> b.getIdLote().equals(1L)).findFirst().orElseThrow();
        Barra barraDestacada = barras.stream().filter(b -> b.getIdLote().equals(2L)).findFirst().orElseThrow();
        assertThat(barraAtenuada.isAtenuada()).isTrue();
        assertThat(barraAtenuada.getEtiquetaAtraso()).isEqualTo("Atrasado 3 días");
        assertThat(barraAtenuada.getDiasDeAtraso()).isEqualTo(3);
        assertThat(barraDestacada.isAtenuada()).isFalse();
    }

    // ==================== Cancelados y limpieza ====================

    @Test
    @DisplayName("CP-GCB-20: los lotes cancelados no se dibujan, no generan carril ni cuentan para la limpieza; un finalizado no origina limpieza")
    void losCanceladosNoSeDibujanYUnFinalizadoNoOriginaLimpieza() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", fecha(1), fecha(3))
                        .fechaInicio(fecha(1).atTime(8, 0)).fechaFinalizacion(fecha(3).atTime(17, 0)).build(),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(10), fecha(12)),
                lote(4L, "STOUT-R8-1", EstadoLote.CANCELADO, "F-02", 8L, "Stout", fecha(2), fecha(4)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-02"), null, fecha(10), LocalDate.of(2027, 6, 30), fecha(13));

        // Assert
        GanttPlanificacionVista gantt = vista.getGantt();
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(13));
        assertThat(gantt.getTotalDias()).isEqualTo(14);
        assertThat(gantt.isVacio()).isFalse();
        assertThat(gantt.getCarriles().get(0).getEtiqueta()).isEqualTo("F-01");
        assertThat(gantt.getCarriles().get(0).getBarras()).extracting(Barra::getIdLote).containsExactly(1L, 3L);
        assertThat(gantt.getCarriles().get(0).getLimpiezas()).isEmpty();
        assertThat(gantt.getCarriles().get(1).getEtiqueta()).isEqualTo("F-02");
        assertThat(gantt.getCarriles().get(1).getBarras()).isEmpty();
        assertThat(vista.getColas().get(0).getEnEjecucion()).isNull();
        assertThat(vista.getColas().get(0).getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(3L);
        assertThat(vista.getColas().get(1).getPendientes()).isEmpty();
    }

    @Test
    @DisplayName("CP-GCB-21: la limpieza entre lotes de planificaciones distintas se dibuja el 06/07 (eje de 12 días), sin atenuar si no se destaca ninguna planificación")
    void limpiezaEntreLotesDePlanificacionesDistintas() {
        // Act
        GanttPlanificacionVista gantt = construir(lotesDeDosPlanificaciones(), List.of("F-01"), null, fecha(2), LocalDate.of(2027, 6, 30), fecha(11)).getGantt();

        // Assert
        assertThat(gantt.getTotalDias()).isEqualTo(12);
        List<Limpieza> limpiezas = gantt.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(6));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(8.33);
        assertThat(limpiezas.get(0).getTooltip()).isEqualTo("Limpieza del fermentador y demás equipamiento");
        assertThat(limpiezas.get(0).isAtenuada()).isFalse();
    }

    @Test
    @DisplayName("CP-GCB-22: la limpieza se atenúa según el lote que la origina (el lote 1, del plan 7): atenuada al destacar el plan 8 o el 9, y no al destacar el 7")
    void laLimpiezaSeAtenuaSegunElLoteQueLaOrigina() {
        for (Long destacada : new Long[]{8L, 7L, 9L, null}) {
            // Act
            GanttPlanificacionVista gantt = construir(lotesDeDosPlanificaciones(), List.of("F-01"), destacada, fecha(2),
                    LocalDate.of(2027, 6, 30), fecha(11)).getGantt();

            // Assert
            boolean esperada = destacada != null && !destacada.equals(7L);
            assertThat(gantt.getCarriles().get(0).getLimpiezas()).hasSize(1);
            assertThat(gantt.getCarriles().get(0).getLimpiezas().get(0).isAtenuada()).as("plan destacado %s", destacada).isEqualTo(esperada);
        }
    }

    @Test
    @DisplayName("CP-GCB-23: los carriles sin lotes o con un único lote no tienen limpieza (lista vacía, no null)")
    void losCarrilesSinLimpieza() {
        // Arrange
        LoteCronogramaResponseDTO unico = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        GanttPlanificacionVista gantt = construir(List.of(unico), List.of("F-01"), null, fecha(1), DESDE, HASTA).getGantt();

        // Assert
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(gantt.getCarriles()).allSatisfy(carril -> assertThat(carril.getLimpiezas()).isNotNull().isEmpty());
    }

    @Test
    @DisplayName("CP-GCB-24: los fermentadores no se mezclan para la limpieza: solo hay limpieza donde queda un día libre futuro entre lotes vivos del mismo fermentador")
    void losFermentadoresNoSeMezclanParaLaLimpieza() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(7), fecha(10)),
                lote(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", fecha(1), fecha(5)),
                lote(4L, "STOUT-R8-2", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", fecha(6), fecha(10)));

        // Act
        GanttPlanificacionVista gantt = construir(lotes, List.of("F-01", "F-02"), null, fecha(2), LocalDate.of(2027, 6, 30), fecha(11)).getGantt();

        // Assert
        assertThat(gantt.getCarriles().get(0).getLimpiezas()).extracting(Limpieza::getFecha).containsExactly(fecha(6));
        assertThat(gantt.getCarriles().get(1).getLimpiezas()).isEmpty();
    }

    @Test
    @DisplayName("CP-GCB-25: una limpieza anterior a hoy no se dibuja (hoy 07/07) y la de hoy sí (hoy 06/07)")
    void laLimpiezaSoloSeDibujaDesdeHoy() {
        // Act
        GanttPlanificacionVista conHoy07 = construir(lotesDeDosPlanificaciones(), List.of("F-01"), null, fecha(7), LocalDate.of(2027, 6, 30), fecha(11)).getGantt();
        GanttPlanificacionVista conHoy06 = construir(lotesDeDosPlanificaciones(), List.of("F-01"), null, fecha(6), LocalDate.of(2027, 6, 30), fecha(11)).getGantt();

        // Assert
        assertThat(conHoy07.getTotalDias()).isEqualTo(12);
        assertThat(conHoy06.getTotalDias()).isEqualTo(12);
        assertThat(conHoy07.getCarriles().get(0).getLimpiezas()).isEmpty();
        assertThat(conHoy06.getCarriles().get(0).getLimpiezas()).hasSize(1);
        assertThat(conHoy06.getCarriles().get(0).getLimpiezas().get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(conHoy06.getCarriles().get(0).getLimpiezas().get(0).getAnchoPct()).isEqualTo(8.33);
    }

    @Test
    @DisplayName("CP-GCB-26: la limpieza sigue a un lote en ejecución y no a uno finalizado (solo el 09/07, no el 04/07)")
    void laLimpiezaSigueAUnLoteEnEjecucion() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                loteB(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", fecha(1), fecha(3))
                        .fechaInicio(fecha(1).atTime(8, 0)).fechaFinalizacion(fecha(3).atTime(17, 0)).build(),
                loteB(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(5), fecha(8))
                        .fechaInicio(fecha(5).atTime(8, 0)).build(),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(10), fecha(12)));

        // Act
        GanttPlanificacionVista gantt = construir(lotes, List.of("F-01"), null, fecha(6), LocalDate.of(2027, 6, 30), fecha(13)).getGantt();

        // Assert
        assertThat(gantt.getTotalDias()).isEqualTo(14);
        List<Limpieza> limpiezas = gantt.getCarriles().get(0).getLimpiezas();
        assertThat(limpiezas).hasSize(1);
        assertThat(limpiezas.get(0).getFecha()).isEqualTo(fecha(9));
        assertThat(limpiezas.get(0).getIzquierdaPct()).isEqualTo(64.29);
        assertThat(limpiezas.get(0).getAnchoPct()).isEqualTo(7.14);
        assertThat(limpiezas.get(0).isAtenuada()).isFalse();
    }

    @Test
    @DisplayName("CP-GCB-27: el tooltip de la barra lista los equipos del lote (molino, macerador, olla de hervor y fermentador) con sus identificadores")
    void elTooltipListaLosEquiposDelLote() {
        // Arrange
        LoteCronogramaResponseDTO pale = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        Barra barra = construir(List.of(pale), List.of("F-01"), null).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getTooltip()).endsWith("Molino: M-01\nMacerador: MA-01\nOlla de hervor: O-01\nFermentador: F-01");
    }

    @Test
    @DisplayName("CP-GCB-28: cada carril se identifica con el identificador de su fermentador y tiene su cola; un carril fijo no duplica el carril de sus lotes")
    void cadaCarrilSeIdentificaConSuFermentador() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", fecha(1), fecha(5)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-01"), null);

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("F-01", "F-02");
    }

    // ==================== Período, eje y recorte de las barras ====================

    @Test
    @DisplayName("CP-GCB-29: el eje es el período, sin márgenes ni ajustes a las barras (01/07 al 20/07, 20 días, marcas cada 7 días, hoy al 47,5 %)")
    void elEjeEsElPeriodo() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista gantt = construir(List.of(lote), List.of(), null, fecha(10), DESDE, HASTA).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(fecha(1));
        assertThat(gantt.getRangoHasta()).isEqualTo(fecha(20));
        assertThat(gantt.getTotalDias()).isEqualTo(20);
        assertThat(gantt.getAnchoMinimoPx()).isEqualTo(720);
        assertThat(gantt.getHoyPct()).isEqualTo(47.5);
        assertThat(gantt.getMarcasEje()).extracting(MarcaEje::getEtiqueta).containsExactly("01/07", "08/07", "15/07");
        assertThat(gantt.getMarcasEje()).extracting(MarcaEje::getPosicionPct).containsExactly(0.0, 35.0, 70.0);
        assertThat(gantt.getMarcasEje()).extracting(MarcaEje::getAlineacion).containsExactly(Alineacion.IZQUIERDA, Alineacion.CENTRO, Alineacion.CENTRO);
        Barra barra = gantt.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getIzquierdaPct()).isEqualTo(20.0);
        assertThat(barra.getAnchoPct()).isEqualTo(20.0);
        assertThat(barra.isContinuaAntes()).isFalse();
        assertThat(barra.isContinuaDespues()).isFalse();
    }

    @Test
    @DisplayName("CP-GCB-30: las marcas y el ancho dependen de la longitud del período (262 días: 19 marcas cada 14; 76 días: 11 marcas cada 7; 1826 días: 66 marcas cada 28 con año)")
    void marcasYAnchoSegunLaLongitudDelPeriodo() {
        // Arrange
        LocalDate hoy = LocalDate.of(2027, 7, 3);
        LoteCronogramaResponseDTO loteA = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2018, 5, 2), LocalDate.of(2018, 5, 10));
        LoteCronogramaResponseDTO loteB = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.minusDays(5), hoy.minusDays(1));
        LoteCronogramaResponseDTO loteC = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2020, 6, 15), LocalDate.of(2020, 6, 20));

        // Act
        GanttPlanificacionVista ejeA = construir(List.of(loteA), List.of(), null, hoy, LocalDate.of(2018, 4, 21), LocalDate.of(2019, 1, 7)).getGantt();
        GanttPlanificacionVista ejeB = construir(List.of(loteB), List.of(), null, hoy, hoy.minusDays(15), hoy.plusDays(60)).getGantt();
        GanttPlanificacionVista ejeC = construir(List.of(loteC), List.of(), null, hoy, LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31)).getGantt();

        // Assert
        assertThat(ejeA.getTotalDias()).isEqualTo(262);
        assertThat(ejeA.getMarcasEje()).hasSize(19);
        assertThat(ejeA.getMarcasEje().get(0).getEtiqueta()).isEqualTo("21/04");
        assertThat(ejeA.getMarcasEje().get(1).getEtiqueta()).isEqualTo("05/05");
        assertThat(ejeA.getMarcasEje().get(18).getEtiqueta()).isEqualTo("29/12");
        assertThat(ejeA.getAnchoMinimoPx()).isEqualTo(3600);
        assertThat(ejeB.getTotalDias()).isEqualTo(76);
        assertThat(ejeB.getMarcasEje()).hasSize(11);
        assertThat(ejeB.getAnchoMinimoPx()).isEqualTo(1824);
        assertThat(ejeC.getTotalDias()).isEqualTo(1826);
        assertThat(ejeC.getMarcasEje()).hasSize(66).hasSizeLessThanOrEqualTo(150);
        assertThat(ejeC.getMarcasEje().get(0).getEtiqueta()).isEqualTo("01/01/18");
        assertThat(ejeC.getMarcasEje().get(1).getEtiqueta()).isEqualTo("29/01/18");
        assertThat(ejeC.getAnchoMinimoPx()).isEqualTo(3600);
    }

    @Test
    @DisplayName("CP-GCB-31: una barra que empieza antes del período se recorta por la izquierda y lo indica (25/06 al 05/07: 0 % / 25 %, continúa antes)")
    void barraRecortadaPorLaIzquierda() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(-5), fecha(5));

        // Act
        Barra barra = construir(List.of(lote), List.of(), null, HOY, DESDE, HASTA).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getIzquierdaPct()).isEqualTo(0.0);
        assertThat(barra.getAnchoPct()).isEqualTo(25.0);
        assertThat(barra.isContinuaAntes()).isTrue();
        assertThat(barra.isContinuaDespues()).isFalse();
        assertThat(barra.getDesde()).isEqualTo(LocalDate.of(2027, 6, 25));
        assertThat(barra.getHasta()).isEqualTo(fecha(5));
    }

    @Test
    @DisplayName("CP-GCB-32: una barra que termina después del período se recorta por la derecha y lo indica (15/07 al 25/07: 70 % / 30 %, continúa después)")
    void barraRecortadaPorLaDerecha() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(15), fecha(25));

        // Act
        Barra barra = construir(List.of(lote), List.of(), null, HOY, DESDE, HASTA).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getIzquierdaPct()).isEqualTo(70.0);
        assertThat(barra.getAnchoPct()).isEqualTo(30.0);
        assertThat(barra.isContinuaAntes()).isFalse();
        assertThat(barra.isContinuaDespues()).isTrue();
        assertThat(barra.getHasta()).isEqualTo(fecha(25));
    }

    @Test
    @DisplayName("CP-GCB-33: una barra que cubre todo el período se recorta por ambos lados (25/06 al 25/07: 0 % / 100 %)")
    void barraRecortadaPorAmbosLados() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(-5), fecha(25));

        // Act
        Barra barra = construir(List.of(lote), List.of(), null, HOY, DESDE, HASTA).getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getIzquierdaPct()).isEqualTo(0.0);
        assertThat(barra.getAnchoPct()).isEqualTo(100.0);
        assertThat(barra.isContinuaAntes()).isTrue();
        assertThat(barra.isContinuaDespues()).isTrue();
    }

    @Test
    @DisplayName("CP-GCB-34: el primer y el último día del período cuentan y el día siguiente no (a y b sin recorte; c y d recortados a un día; e y f sin barra)")
    void bordesDelPeriodo() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(3)),
                lote(2L, "B-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", fecha(18), fecha(20)),
                lote(3L, "C-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", fecha(-5), fecha(1)),
                lote(4L, "D-1", EstadoLote.PENDIENTE, "F-04", 7L, "Pale Ale", fecha(20), fecha(30)),
                lote(5L, "E-1", EstadoLote.PENDIENTE, "F-05", 7L, "Pale Ale", fecha(21), fecha(25)),
                lote(6L, "F-1", EstadoLote.PENDIENTE, "F-06", 7L, "Pale Ale", LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 30)));

        // Act
        GanttPlanificacionVista gantt = construir(lotes, List.of(), null, HOY, DESDE, HASTA).getGantt();

        // Assert
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02", "F-03", "F-04");
        Barra a = gantt.getCarriles().get(0).getBarras().get(0);
        Barra b = gantt.getCarriles().get(1).getBarras().get(0);
        Barra c = gantt.getCarriles().get(2).getBarras().get(0);
        Barra d = gantt.getCarriles().get(3).getBarras().get(0);
        assertThat(a.isContinuaAntes() || a.isContinuaDespues()).isFalse();
        assertThat(b.isContinuaAntes() || b.isContinuaDespues()).isFalse();
        assertThat(c.getIzquierdaPct()).isEqualTo(0.0);
        assertThat(c.getAnchoPct()).isEqualTo(5.0);
        assertThat(c.isContinuaAntes()).isTrue();
        assertThat(d.getIzquierdaPct()).isEqualTo(95.0);
        assertThat(d.getAnchoPct()).isEqualTo(5.0);
        assertThat(d.isContinuaDespues()).isTrue();
    }

    @Test
    @DisplayName("CP-GCB-35: el tooltip indica la continuación con las fechas reales del lote, debajo de \"Receta\", \"Planificación\" y las leyendas de atraso o corrimiento y antes de \"Inicio estimado:\"")
    void elTooltipIndicaLaContinuacion() {
        // Arrange
        LoteCronogramaResponseDTO izquierda = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(-5), fecha(5));
        LoteCronogramaResponseDTO derecha = lote(2L, "B-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", fecha(15), fecha(25));
        LoteCronogramaResponseDTO ambos = loteB(3L, "C-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", fecha(-5), fecha(25))
                .diasDeDesplazamiento(2).build();
        LoteCronogramaResponseDTO sinRecorte = lote(4L, "D-1", EstadoLote.PENDIENTE, "F-04", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        List<Carril> carriles = construir(List.of(izquierda, derecha, ambos, sinRecorte), List.of(), null, HOY, DESDE, HASTA).getGantt().getCarriles();

        // Assert
        String tooltipIzquierda = carriles.get(0).getBarras().get(0).getTooltip();
        assertThat(tooltipIzquierda).contains("Continúa antes del período").doesNotContain("Continúa después del período");
        assertThat(tooltipIzquierda).contains("Inicio estimado: 25/06/2027", "Fin estimado: 05/07/2027");
        String tooltipDerecha = carriles.get(1).getBarras().get(0).getTooltip();
        assertThat(tooltipDerecha).contains("Continúa después del período").doesNotContain("Continúa antes del período");
        String tooltipAmbos = carriles.get(2).getBarras().get(0).getTooltip();
        assertThat(tooltipAmbos).contains("Continúa antes del período", "Continúa después del período");
        assertThat(tooltipAmbos.indexOf("Planificación: #7")).isLessThan(tooltipAmbos.indexOf("Planificado para"));
        assertThat(tooltipAmbos.indexOf("Planificado para")).isLessThan(tooltipAmbos.indexOf("Continúa antes del período"));
        assertThat(tooltipAmbos.indexOf("Continúa antes del período")).isLessThan(tooltipAmbos.indexOf("Continúa después del período"));
        assertThat(tooltipAmbos.indexOf("Continúa después del período")).isLessThan(tooltipAmbos.indexOf("Inicio estimado:"));
        assertThat(carriles.get(3).getBarras().get(0).getTooltip()).doesNotContain("Continúa");
    }

    @Test
    @DisplayName("CP-GCB-36: en un período de 1826 días un lote de un solo día ocupa el ancho mínimo de 1 % y no se sale del eje")
    void anchoMinimoEnUnPeriodoLargo() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2020, 6, 15), LocalDate.of(2020, 6, 15));

        // Act
        Barra barra = construir(List.of(lote), List.of(), null, HOY, LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31))
                .getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getAnchoPct()).isEqualTo(1.0);
        assertThat(barra.getIzquierdaPct()).isEqualTo(49.07);
    }

    @Test
    @DisplayName("CP-GCB-37: la marca de hoy solo se dibuja si hoy cae dentro del período (hoy 25/07 y hoy 28/06: sin marca; el eje y la barra no cambian)")
    void hoyFueraDelPeriodo() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        for (LocalDate hoy : List.of(fecha(25), LocalDate.of(2027, 6, 28))) {
            // Act
            GanttPlanificacionVista gantt = construir(List.of(lote), List.of(), null, hoy, DESDE, HASTA).getGantt();

            // Assert
            assertThat(gantt.getHoyPct()).isNull();
            assertThat(gantt.getHoyAlineacion()).isNull();
            assertThat(gantt.getTotalDias()).isEqualTo(20);
            assertThat(gantt.getCarriles().get(0).getBarras().get(0).getIzquierdaPct()).isEqualTo(20.0);
            assertThat(gantt.getCarriles().get(0).getBarras().get(0).getAnchoPct()).isEqualTo(20.0);
        }
    }

    // ==================== Qué lotes se dibujan en el período ====================

    @Test
    @DisplayName("CP-GCB-38: un pendiente se solapa con el período según sus fechas corridas, no las guardadas (guardado fuera pero corrido adentro se dibuja; guardado adentro pero corrido afuera no)")
    void pendientes_fechasEstimadasMasCorrimiento() {
        // Arrange
        LoteCronogramaResponseDTO a = lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5));
        LoteCronogramaResponseDTO b = loteB(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 25))
                .diasDeDesplazamiento(15).build();
        LoteCronogramaResponseDTO c = loteB(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(15), fecha(19))
                .diasDeDesplazamiento(10).build();

        // Act
        List<Barra> barras = construir(List.of(a, b, c), List.of(), null, HOY, DESDE, HASTA).getGantt().getCarriles().get(0).getBarras();

        // Assert
        assertThat(barras).extracting(Barra::getIdLote).containsExactly(2L);
        assertThat(barras.get(0).getDesde()).isEqualTo(fecha(5));
        assertThat(barras.get(0).getHasta()).isEqualTo(fecha(10));
        assertThat(barras.get(0).getIzquierdaPct()).isEqualTo(20.0);
        assertThat(barras.get(0).getAnchoPct()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("CP-GCB-39: un lote en ejecución va del inicio real al fin estimado más el atraso: sin atraso y fuera no se dibuja; atrasado llega a hoy y entra; el que empieza adentro y termina afuera se recorta")
    void enEjecucion_delInicioRealAlFinEstimadoMasElAtraso() {
        // Arrange
        LoteCronogramaResponseDTO a = loteB(1L, "A-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 30))
                .fechaInicio(LocalDate.of(2027, 6, 20).atTime(8, 0)).build();
        LoteCronogramaResponseDTO b = loteB(2L, "B-1", EstadoLote.EN_EJECUCION, "F-02", 7L, "Pale Ale", LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 30))
                .fechaInicio(LocalDate.of(2027, 6, 20).atTime(8, 0)).diasDeAtraso(15).build();
        LoteCronogramaResponseDTO c = loteB(3L, "C-1", EstadoLote.EN_EJECUCION, "F-03", 7L, "Pale Ale", fecha(18), fecha(30))
                .fechaInicio(fecha(18).atTime(8, 0)).build();

        // Act
        GanttPlanificacionVista gantt = construir(List.of(a, b, c), List.of(), null, fecha(15), DESDE, HASTA).getGantt();

        // Assert
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-02", "F-03");
        Barra barraB = gantt.getCarriles().get(0).getBarras().get(0);
        assertThat(barraB.getHasta()).isEqualTo(fecha(15));
        assertThat(barraB.getIzquierdaPct()).isEqualTo(0.0);
        assertThat(barraB.getAnchoPct()).isEqualTo(75.0);
        assertThat(barraB.getEtiquetaAtraso()).isEqualTo("Atrasado 15 días");
        assertThat(barraB.isContinuaAntes()).isTrue();
        assertThat(gantt.getHoyPct()).isEqualTo(72.5);
        Barra barraC = gantt.getCarriles().get(1).getBarras().get(0);
        assertThat(barraC.getIzquierdaPct()).isEqualTo(85.0);
        assertThat(barraC.getAnchoPct()).isEqualTo(15.0);
        assertThat(barraC.isContinuaDespues()).isTrue();
    }

    @Test
    @DisplayName("CP-GCB-40: un finalizado va del inicio real a la finalización real: solo se dibuja el que se solapa con el período (28/06 al 03/07, recortado)")
    void finalizados_delInicioRealALaFinalizacionReal() {
        // Arrange
        LoteCronogramaResponseDTO a = finalizado(1L, "F-01", LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 20));
        LoteCronogramaResponseDTO b = finalizado(2L, "F-02", LocalDate.of(2027, 6, 28), fecha(3));
        LoteCronogramaResponseDTO c = finalizado(3L, "F-03", fecha(21), fecha(30));

        // Act
        GanttPlanificacionVista gantt = construir(List.of(a, b, c), List.of(), null, HOY, DESDE, HASTA).getGantt();

        // Assert
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-02");
        Barra barra = gantt.getCarriles().get(0).getBarras().get(0);
        assertThat(barra.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barra.getIzquierdaPct()).isEqualTo(0.0);
        assertThat(barra.getAnchoPct()).isEqualTo(15.0);
        assertThat(barra.isContinuaAntes()).isTrue();
        assertThat(barra.getDesde()).isEqualTo(LocalDate.of(2027, 6, 28));
    }

    @Test
    @DisplayName("CP-GCB-41: un fermentador sin barras en el período no tiene carril (solo F-01, aunque F-02 tenga un pendiente fuera del período)")
    void unFermentadorSinBarrasEnElPeriodoNoTieneCarril() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(2L, "B-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 8)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of(), null);

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01");
        assertThat(vista.getTotalFermentadores()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-GCB-42: un carril fijo se conserva aunque no tenga barras en el período (F-02 vacío)")
    void unCarrilFijoSeConservaAunqueNoTengaBarras() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(2L, "B-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 8)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of("F-02"), null);

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(vista.getGantt().getCarriles().get(1).getBarras()).isEmpty();
        assertThat(vista.getTotalFermentadores()).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-GCB-43: si nada se solapa con el período no hay carriles, ni colas, ni diagrama (vacío)")
    void sinBarrasEnElPeriodo_noHayCarrilesNiDiagrama() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 5)),
                finalizado(2L, "F-02", LocalDate.of(2027, 6, 10), LocalDate.of(2027, 6, 20)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of(), null);

        // Assert
        assertThat(vista.getGantt().isVacio()).isTrue();
        assertThat(vista.getGantt().getCarriles()).isEmpty();
        assertThat(vista.getColas()).isEmpty();
        assertThat(vista.getPagina()).isEqualTo(1);
        assertThat(vista.getTotalPaginas()).isEqualTo(1);
        assertThat(vista.getTotalFermentadores()).isZero();
    }

    // ==================== Colas y limpiezas independientes del período ====================

    @Test
    @DisplayName("CP-GCB-44: la cola incluye lo que está fuera del período: el carril tiene 3 barras y la cola lista el lote en ejecución y los dos pendientes (uno fuera del período)")
    void laColaIncluyeLoQueEstaFueraDelPeriodo() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                finalizado(1L, "F-01", fecha(2), fecha(4)),
                loteB(2L, "A-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 6, 20), LocalDate.of(2027, 6, 30))
                        .fechaInicio(LocalDate.of(2027, 6, 20).atTime(8, 0)).diasDeAtraso(15).build(),
                lote(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(9)),
                lote(4L, "A-4", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 8, 10), LocalDate.of(2027, 8, 14)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of(), null, fecha(15), DESDE, HASTA);

        // Assert
        assertThat(vista.getGantt().getCarriles().get(0).getBarras()).extracting(Barra::getIdLote).containsExactly(2L, 1L, 3L);
        ColaFermentador cola = vista.getColas().get(0);
        assertThat(cola.getEnEjecucion().getId()).isEqualTo(2L);
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(3L, 4L);
        assertThat(cola.getPendientes()).extracting(i -> i.getPosicion()).containsExactly(1, 2);
        assertThat(cola.getPendientes()).extracting(i -> i.isPrimero()).containsExactly(true, false);
        assertThat(cola.getPendientes()).extracting(i -> i.isUltimo()).containsExactly(false, true);
    }

    @Test
    @DisplayName("CP-GCB-45: solo hay cola para los fermentadores con carril (F-02, con lotes solo fuera del período, no tiene carril ni cola)")
    void sinCarrilNoHayCola() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(5L, "B-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", LocalDate.of(2027, 8, 10), LocalDate.of(2027, 8, 14)),
                loteB(6L, "B-2", EstadoLote.EN_EJECUCION, "F-02", 7L, "Pale Ale", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 20))
                        .fechaInicio(LocalDate.of(2027, 6, 1).atTime(8, 0)).build());

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of(), null);

        // Assert
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("F-01");
    }

    @Test
    @DisplayName("CP-GCB-46: la limpieza usa los lotes vivos completos y se dibuja solo si su día cae en el período (a: el 16/07 aunque el siguiente caiga fuera; b: ninguna, el 21/07 está fuera; c: el 01/07 aunque el lote que la origina caiga fuera)")
    void laLimpiezaUsaLaColaCompletaYSeDibujaSoloDentroDelPeriodo() {
        // Arrange
        List<LoteCronogramaResponseDTO> casoA = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(10), fecha(15)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(22), fecha(25)));
        List<LoteCronogramaResponseDTO> casoB = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(17), fecha(20)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(25), fecha(28)));
        List<LoteCronogramaResponseDTO> casoC = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", LocalDate.of(2027, 6, 25), LocalDate.of(2027, 6, 30)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)));

        // Act
        GanttPlanificacionVista ganttA = construir(casoA, List.of(), null, fecha(5), DESDE, HASTA).getGantt();
        GanttPlanificacionVista ganttB = construir(casoB, List.of(), null, fecha(5), DESDE, HASTA).getGantt();
        GanttPlanificacionVista ganttC = construir(casoC, List.of(), null, LocalDate.of(2027, 6, 29), DESDE, HASTA).getGantt();

        // Assert
        assertThat(ganttA.getCarriles().get(0).getBarras()).hasSize(1);
        assertThat(ganttA.getCarriles().get(0).getLimpiezas()).hasSize(1);
        assertThat(ganttA.getCarriles().get(0).getLimpiezas().get(0).getFecha()).isEqualTo(fecha(16));
        assertThat(ganttA.getCarriles().get(0).getLimpiezas().get(0).getIzquierdaPct()).isEqualTo(75.0);
        assertThat(ganttA.getCarriles().get(0).getLimpiezas().get(0).getAnchoPct()).isEqualTo(5.0);
        assertThat(ganttB.getCarriles().get(0).getLimpiezas()).isEmpty();
        assertThat(ganttC.getCarriles().get(0).getBarras()).extracting(Barra::getIdLote).containsExactly(2L);
        assertThat(ganttC.getCarriles().get(0).getLimpiezas()).hasSize(1);
        assertThat(ganttC.getCarriles().get(0).getLimpiezas().get(0).getFecha()).isEqualTo(fecha(1));
        assertThat(ganttC.getCarriles().get(0).getLimpiezas().get(0).getIzquierdaPct()).isEqualTo(0.0);
        assertThat(ganttC.getCarriles().get(0).getLimpiezas().get(0).getAnchoPct()).isEqualTo(5.0);
        assertThat(ganttC.getHoyPct()).isNull();
    }

    // ==================== Paginación de carriles ====================

    @Test
    @DisplayName("CP-GCB-47: los carriles se reparten de a 10 en orden de identificador (25 fermentadores: páginas de 10, 10 y 5)")
    void variasPaginas() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = lotesDeFermentadores(25);

        // Act
        CronogramaProduccionVista pagina1 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 1, 10);
        CronogramaProduccionVista pagina2 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 2, 10);
        CronogramaProduccionVista pagina3 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 3, 10);

        // Assert
        assertThat(etiquetas(pagina1)).containsExactlyElementsOf(fermentadores(1, 10));
        assertThat(etiquetas(pagina2)).containsExactlyElementsOf(fermentadores(11, 20));
        assertThat(etiquetas(pagina3)).containsExactlyElementsOf(fermentadores(21, 25));
        assertThat(List.of(pagina1, pagina2, pagina3)).allSatisfy(v -> {
            assertThat(v.getTotalPaginas()).isEqualTo(3);
            assertThat(v.getTotalFermentadores()).isEqualTo(25);
        });
        assertThat(List.of(pagina1.getPagina(), pagina2.getPagina(), pagina3.getPagina())).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("CP-GCB-48: menos de una página completa: 3 fermentadores, una sola página")
    void unaSolaPagina() {
        // Act
        CronogramaProduccionVista vista = construir(lotesDeFermentadores(3), List.of(), null, HOY, DESDE, HASTA, 1, 10);

        // Assert
        assertThat(vista.getGantt().getCarriles()).hasSize(3);
        assertThat(vista.getPagina()).isEqualTo(1);
        assertThat(vista.getTotalPaginas()).isEqualTo(1);
        assertThat(vista.getTotalFermentadores()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-GCB-49: exactamente una página llena (10 fermentadores) no crea una página vacía: la página 2 se ajusta a la 1")
    void exactamenteUnaPaginaLlena() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = lotesDeFermentadores(10);

        // Act
        CronogramaProduccionVista pagina1 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 1, 10);
        CronogramaProduccionVista pagina2 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 2, 10);

        // Assert
        assertThat(pagina1.getGantt().getCarriles()).hasSize(10);
        assertThat(pagina1.getTotalPaginas()).isEqualTo(1);
        assertThat(pagina2.getPagina()).isEqualTo(1);
        assertThat(pagina2.getGantt().getCarriles()).hasSize(10);
    }

    @Test
    @DisplayName("CP-GCB-50: una página fuera de rango se ajusta a la última (9 → 3) o a la primera (0 y -4 → 1) y el modelo informa la efectiva")
    void paginaFueraDeRango() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = lotesDeFermentadores(25);

        // Act
        CronogramaProduccionVista pasada = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 9, 10);
        CronogramaProduccionVista cero = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 0, 10);
        CronogramaProduccionVista negativa = construir(lotes, List.of(), null, HOY, DESDE, HASTA, -4, 10);

        // Assert
        assertThat(pasada.getPagina()).isEqualTo(3);
        assertThat(etiquetas(pasada)).containsExactlyElementsOf(fermentadores(21, 25));
        assertThat(cero.getPagina()).isEqualTo(1);
        assertThat(etiquetas(cero)).containsExactlyElementsOf(fermentadores(1, 10));
        assertThat(negativa.getPagina()).isEqualTo(1);
        assertThat(etiquetas(negativa)).containsExactlyElementsOf(fermentadores(1, 10));
    }

    @Test
    @DisplayName("CP-GCB-51: orden estable y páginas contiguas: alfabético sin distinguir mayúsculas, sin repetir ni omitir fermentadores, igual para cualquier orden de los lotes; sin orden natural (\"F-10\" antes que \"F-2\")")
    void ordenEstableYPaginasContiguas() {
        // Arrange: 12 fermentadores con identificadores mezclados en mayúsculas
        List<String> identificadores = new ArrayList<>(fermentadores(1, 12));
        identificadores.set(1, "f-02");
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>();
        for (int i = 0; i < identificadores.size(); i++) {
            lotes.add(lote((long) i + 1, "A-" + i, EstadoLote.PENDIENTE, identificadores.get(i), 7L, "Pale Ale", fecha(5), fecha(8)));
        }
        Collections.shuffle(lotes, new Random(1));
        List<LoteCronogramaResponseDTO> otroOrden = new ArrayList<>(lotes);
        Collections.shuffle(otroOrden, new Random(2));

        // Act
        List<List<String>> paginas = new ArrayList<>();
        List<List<String>> paginasOtroOrden = new ArrayList<>();
        for (int pagina = 1; pagina <= 3; pagina++) {
            paginas.add(etiquetas(construir(lotes, List.of(), null, HOY, DESDE, HASTA, pagina, 5)));
            paginasOtroOrden.add(etiquetas(construir(otroOrden, List.of(), null, HOY, DESDE, HASTA, pagina, 5)));
        }
        CronogramaProduccionVista longitudes = construir(List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-2", 7L, "Pale Ale", fecha(5), fecha(8)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-10", 7L, "Pale Ale", fecha(5), fecha(8))), List.of(), null);

        // Assert
        assertThat(paginas.get(0)).containsExactly("F-01", "f-02", "F-03", "F-04", "F-05");
        assertThat(paginas.get(1)).containsExactly("F-06", "F-07", "F-08", "F-09", "F-10");
        assertThat(paginas.get(2)).containsExactly("F-11", "F-12");
        Set<String> union = new LinkedHashSet<>();
        paginas.forEach(union::addAll);
        assertThat(union).hasSize(12);
        assertThat(paginasOtroOrden).isEqualTo(paginas);
        assertThat(etiquetas(longitudes)).containsExactly("F-10", "F-2");
    }

    @Test
    @DisplayName("CP-GCB-52: las colas siguen la página: solo hay cola para los carriles de la página pedida (página 2: F-11 a F-20)")
    void lasColasSiguenLaPagina() {
        // Act
        CronogramaProduccionVista vista = construir(lotesDeFermentadores(25), List.of(), null, HOY, DESDE, HASTA, 2, 10);

        // Assert
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactlyElementsOf(fermentadores(11, 20));
    }

    @Test
    @DisplayName("CP-GCB-53: un carril fijo cuenta como un carril más para la paginación y no se duplica con el de sus lotes (F-07 fijo y lotes de F-01 a F-12: 12 carriles)")
    void losCarrilesFijosCuentanParaLaPaginacion() {
        // Act
        CronogramaProduccionVista pagina2 = construir(lotesDeFermentadores(12), List.of("F-07"), null, HOY, DESDE, HASTA, 2, 10);
        CronogramaProduccionVista pagina1 = construir(lotesDeFermentadores(12), List.of("F-07"), null, HOY, DESDE, HASTA, 1, 10);

        // Assert
        assertThat(pagina2.getTotalFermentadores()).isEqualTo(12);
        assertThat(etiquetas(pagina2)).containsExactly("F-11", "F-12");
        assertThat(etiquetas(pagina1).stream().filter("F-07"::equals)).hasSize(1);
    }

    @Test
    @DisplayName("CP-GCB-54: con 50 fermentadores el diagrama no crece sin límite: 5 páginas, a lo sumo 10 carriles y 10 colas por página, y el mismo eje en todas")
    void cincuentaFermentadores() {
        // Arrange: pendiente, en ejecución y finalizado dentro del período en cada fermentador
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String fermentador = String.format("F-%02d", i);
            lotes.add(lote(i * 10L, "P-" + i, EstadoLote.PENDIENTE, fermentador, 7L, "Pale Ale", LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 5)));
            lotes.add(loteB(i * 10L + 1, "E-" + i, EstadoLote.EN_EJECUCION, fermentador, 7L, "Pale Ale", LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 10))
                    .fechaInicio(LocalDate.of(2020, 6, 1).atTime(8, 0)).build());
            lotes.add(finalizado(i * 10L + 2, fermentador, LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 10)));
        }
        LocalDate desde = LocalDate.of(2018, 1, 1);
        LocalDate hasta = LocalDate.of(2022, 12, 31);

        // Act
        CronogramaProduccionVista pagina1 = construir(lotes, List.of(), null, HOY, desde, hasta, 1, 10);
        CronogramaProduccionVista pagina5 = construir(lotes, List.of(), null, HOY, desde, hasta, 5, 10);

        // Assert
        for (CronogramaProduccionVista vista : List.of(pagina1, pagina5)) {
            assertThat(vista.getTotalPaginas()).isEqualTo(5);
            assertThat(vista.getTotalFermentadores()).isEqualTo(50);
            assertThat(vista.getGantt().getCarriles()).hasSize(10);
            assertThat(vista.getColas()).hasSize(10);
            assertThat(vista.getGantt().getTotalDias()).isEqualTo(1826);
            assertThat(vista.getGantt().getAnchoMinimoPx()).isEqualTo(3600);
            assertThat(vista.getGantt().getMarcasEje()).hasSize(66);
            assertThat(vista.getGantt().getCarriles()).allSatisfy(c -> assertThat(c.getBarras()).hasSize(3));
        }
        assertThat(etiquetas(pagina5)).containsExactlyElementsOf(fermentadores(41, 50));
    }

    @Test
    @DisplayName("CP-GCB-55: un lote sin fermentador no se dibuja ni genera carril ni cola (12 fermentadores: página 2 con F-11 y F-12)")
    void unLoteSinFermentadorNoSeDibuja() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>(lotesDeFermentadores(12));
        lotes.add(lote(99L, "SIN-FERMENTADOR", EstadoLote.PENDIENTE, null, 7L, "Pale Ale", fecha(5), fecha(8)));

        // Act
        CronogramaProduccionVista pagina1 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 1, 10);
        CronogramaProduccionVista pagina2 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 2, 10);

        // Assert
        assertThat(pagina1.getGantt().getCarriles()).hasSize(10);
        assertThat(etiquetas(pagina2)).containsExactly("F-11", "F-12");
        assertThat(pagina2.getTotalFermentadores()).isEqualTo(12);
        assertThat(pagina2.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("F-11", "F-12");
    }

    @Test
    @DisplayName("CP-GCB-56: vacío describe todo el período y no la página: con 25 fermentadores con barras vale false también en la última página")
    void vacioNoDependeDeLaPagina() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = lotesDeFermentadores(25);

        // Act
        CronogramaProduccionVista pagina1 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 1, 10);
        CronogramaProduccionVista pagina3 = construir(lotes, List.of(), null, HOY, DESDE, HASTA, 3, 10);

        // Assert
        assertThat(pagina1.getGantt().isVacio()).isFalse();
        assertThat(pagina3.getGantt().isVacio()).isFalse();
        assertThat(pagina3.getGantt().getCarriles()).hasSize(5);
    }

    // ==================== Fermentadores dados de baja ====================

    @Test
    @DisplayName("CP-GCB-57: los carriles se derivan de los lotes sin mirar el estado del fermentador: un período histórico (2012) muestra el fermentador activo y el dado de baja")
    void losFermentadoresDadosDeBajaTienenCarril() {
        // Arrange: el builder solo recibe lotes; "F-02" podría estar dado de baja y no cambia nada
        List<LoteCronogramaResponseDTO> lotes = List.of(
                finalizado(1L, "F-01", LocalDate.of(2012, 3, 1), LocalDate.of(2012, 3, 10)),
                finalizado(2L, "F-02", LocalDate.of(2012, 5, 1), LocalDate.of(2012, 5, 10)));

        // Act
        CronogramaProduccionVista vista = construir(lotes, List.of(), null, HOY, LocalDate.of(2012, 1, 1), LocalDate.of(2012, 12, 31));

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(vista.getGantt().getCarriles()).allSatisfy(c -> {
            assertThat(c.getBarras()).hasSize(1);
            assertThat(c.getBarras().get(0).getEstadoClave()).isEqualTo("finalizado");
        });
        assertThat(vista.getTotalFermentadores()).isEqualTo(2);
        assertThat(vista.getGantt().isVacio()).isFalse();
        assertThat(vista.getGantt().getHoyPct()).isNull();
        assertThat(vista.getColas()).allSatisfy(c -> {
            assertThat(c.getEnEjecucion()).isNull();
            assertThat(c.getPendientes()).isEmpty();
        });
    }

    // ==================== Datos de prueba ====================

    /** Lote 1 pendiente del plan 7 (01/07 al 05/07) y lote 2 pendiente del plan 8 (07/07 al 10/07) en "F-01". */
    private static List<LoteCronogramaResponseDTO> lotesDeDosPlanificaciones() {
        return List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(7), fecha(10)));
    }

    /** Un pendiente del 05/07 al 08/07 por cada fermentador "F-01" a "F-nn". */
    private static List<LoteCronogramaResponseDTO> lotesDeFermentadores(int cantidad) {
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            lotes.add(lote(i, "A-" + i, EstadoLote.PENDIENTE, String.format("F-%02d", i), 7L, "Pale Ale", fecha(5), fecha(8)));
        }
        return lotes;
    }

    /** Los identificadores "F-dd" de {@code primero} a {@code ultimo}. */
    private static List<String> fermentadores(int primero, int ultimo) {
        List<String> identificadores = new ArrayList<>();
        for (int i = primero; i <= ultimo; i++) {
            identificadores.add(String.format("F-%02d", i));
        }
        return identificadores;
    }

    private static List<String> etiquetas(CronogramaProduccionVista vista) {
        return vista.getGantt().getCarriles().stream().map(Carril::getEtiqueta).toList();
    }

    private static CronogramaProduccionVista construir(List<LoteCronogramaResponseDTO> lotes, List<String> carrilesFijos, Long idPlanificacionDestacada) {
        return construir(lotes, carrilesFijos, idPlanificacionDestacada, HOY, DESDE, HASTA);
    }

    private static CronogramaProduccionVista construir(List<LoteCronogramaResponseDTO> lotes, List<String> carrilesFijos, Long idPlanificacionDestacada,
                                                       LocalDate hoy, LocalDate desde, LocalDate hasta) {
        return construir(lotes, carrilesFijos, idPlanificacionDestacada, hoy, desde, hasta, 1, 10);
    }

    private static CronogramaProduccionVista construir(List<LoteCronogramaResponseDTO> lotes, List<String> carrilesFijos, Long idPlanificacionDestacada,
                                                       LocalDate hoy, LocalDate desde, LocalDate hasta, int pagina, int tamanioPagina) {
        return CronogramaProduccionBuilder.construir(lotes, carrilesFijos, idPlanificacionDestacada, hoy, desde, hasta, pagina, tamanioPagina);
    }

    /** El día {@code dia} de julio de 2027; un día cero o negativo cae en junio (0 = 30/06, -5 = 25/06). */
    private static LocalDate fecha(int dia) {
        return LocalDate.of(2027, 6, 30).plusDays(dia);
    }

    private static LoteCronogramaResponseDTO finalizado(Long id, String fermentador, LocalDate inicioReal, LocalDate finReal) {
        return loteB(id, "FIN-" + id, EstadoLote.FINALIZADO, fermentador, 7L, "Pale Ale", inicioReal, finReal)
                .fechaInicio(inicioReal.atTime(8, 0)).fechaFinalizacion(finReal.atTime(17, 0)).build();
    }

    private static LoteCronogramaResponseDTO lote(long id, String identificador, EstadoLote estado, String fermentador,
                                                  Long idPlanificacion, String receta, LocalDate inicio, LocalDate fin) {
        return loteB(id, identificador, estado, fermentador, idPlanificacion, receta, inicio, fin).build();
    }

    private static LoteCronogramaResponseDTO.LoteCronogramaResponseDTOBuilder loteB(Long id, String identificador, EstadoLote estado,
                                                                                    String fermentador, Long idPlanificacion, String receta,
                                                                                    LocalDate inicio, LocalDate fin) {
        return LoteCronogramaResponseDTO.builder()
                .id(id)
                .identificadorInterno(identificador)
                .estado(estado)
                .volumenObjetivo(300.0)
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(fin)
                .idPlanificacionProduccion(idPlanificacion)
                .nombreReceta(receta)
                .identificadorInternoFermentador(fermentador)
                .identificadorInternoMolino("M-01")
                .identificadorInternoMacerador("MA-01")
                .identificadorInternoOllaHervor("O-01");
    }
}
