package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.presentation.view.CronogramaProduccionVista.ColaFermentador;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Carril;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CronogramaProduccionBuilderTest {

    private static final LocalDate HOY = LocalDate.of(2027, 7, 3);

    // ==================== Carriles ====================

    @Test
    @DisplayName("CP-GCB-01: sin lotes cada fermentador recibido tiene su carril vacío (ordenados por identificador), el eje se arma alrededor de hoy y no hay marcas de planificación")
    void sinLotes_cadaFermentadorTieneSuCarrilVacio() {
        // Arrange
        LocalDate hoy = LocalDate.of(2027, 7, 10);

        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(List.of(), List.of("F-02", "F-01"), null, hoy);

        // Assert
        GanttPlanificacionVista gantt = vista.getGantt();
        assertThat(gantt.isVacio()).isTrue();
        assertThat(gantt.getCarriles()).extracting(Carril::getEtiqueta).containsExactly("F-01", "F-02");
        assertThat(gantt.getCarriles()).allSatisfy(c -> assertThat(c.getBarras()).isEmpty());
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 7, 9));
        assertThat(gantt.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 11));
        assertThat(gantt.getTotalDias()).isEqualTo(3);
        assertThat(gantt.getHoyPct()).isEqualTo(50.0);
        assertThat(gantt.getInicioPlanificacionPct()).isNull();
        assertThat(gantt.getFinPlanificacionPct()).isNull();
        assertThat(gantt.isPlanificacionResaltada()).isFalse();
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("F-01", "F-02");
        assertThat(vista.getColas()).allSatisfy(c -> {
            assertThat(c.getEnEjecucion()).isNull();
            assertThat(c.getPendientes()).isEmpty();
        });
    }

    @Test
    @DisplayName("CP-GCB-02: listas nulas no rompen: modelo vacío, sin carriles ni colas, con el eje alrededor de hoy")
    void listasNulas_devuelvenUnModeloVacio() {
        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(null, null, null, HOY);

        // Assert
        assertThat(vista.getGantt().isVacio()).isTrue();
        assertThat(vista.getGantt().getCarriles()).isEmpty();
        assertThat(vista.getColas()).isEmpty();
        assertThat(vista.getGantt().getTotalDias()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-GCB-03: lotes de dos planificaciones conviven intercalados en el carril de un fermentador, ordenados por inicio (eje de 12 días: 8,33 % / 41,67 % y 58,33 % / 33,33 %)")
    void lotesDeDosPlanificaciones_conviveEnUnMismoCarril() {
        // Arrange: se pasan en desorden
        LoteCronogramaResponseDTO stout = lote(2L, "STOUT-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(7), fecha(10));
        LoteCronogramaResponseDTO pale = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(List.of(stout, pale), List.of("F-01"), null, HOY);

        // Assert
        GanttPlanificacionVista gantt = vista.getGantt();
        assertThat(gantt.isVacio()).isFalse();
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(gantt.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 11));
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
        LoteCronogramaResponseDTO pale = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5));

        // Act
        Barra barra = CronogramaProduccionBuilder.construir(List.of(pale), List.of("F-01"), null, HOY)
                .getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getEtiqueta()).isEqualTo("Pale Ale - Plan #7 - PALE_ALE-1");
        assertThat(barra.getEstadoClave()).isEqualTo("pendiente");
        assertThat(barra.getTooltip()).startsWith("PALE_ALE-1 - Pendiente\nReceta: Pale Ale\nPlanificación: #7\nInicio estimado: 01/07/2027\n");
        assertThat(barra.getTooltip()).contains("Fin estimado: 05/07/2027", "Volumen: 300 L", "Molino: M-01", "Macerador: MA-01", "Olla de hervor: O-01");
    }

    @Test
    @DisplayName("CP-GCB-05: sin receta ni planificación la etiqueta se reduce al identificador del lote")
    void sinRecetaNiPlanificacion_laEtiquetaEsElIdentificador() {
        // Arrange
        LoteCronogramaResponseDTO huerfano = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", null, null, fecha(1), fecha(5));

        // Act
        Barra barra = CronogramaProduccionBuilder.construir(List.of(huerfano), List.of(), null, HOY)
                .getGantt().getCarriles().get(0).getBarras().get(0);

        // Assert
        assertThat(barra.getEtiqueta()).isEqualTo("PALE_ALE-1");
        assertThat(barra.getTooltip()).contains("Receta: -", "Planificación: -");
    }

    // ==================== Planificación destacada ====================

    @Test
    @DisplayName("CP-GCB-06: al destacar una planificación solo se atenúan las barras de las demás; sin destacar ninguna no se atenúa nada")
    void destacarUnaPlanificacion_atenuaLasDemas() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "STOUT-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(7), fecha(10)));

        // Act
        GanttPlanificacionVista destacada = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), 7L, HOY).getGantt();
        GanttPlanificacionVista sinDestacar = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), null, HOY).getGantt();

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
                lote(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(7), fecha(10)),
                lote(3L, "STOUT-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", fecha(12), fecha(15)));

        // Act
        CronogramaProduccionVista conPlan7 = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), 7L, HOY);
        CronogramaProduccionVista conPlan9 = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), 9L, HOY);

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
    @DisplayName("CP-GCB-08: el eje abarca también hoy cuando cae después del último fin (04/07 a 21/07, 18 días, hoy al 91,67 %)")
    void elEjeIncluyeHoyDespuesDeLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista gantt = CronogramaProduccionBuilder.construir(List.of(lote), List.of("F-01"), null, fecha(20)).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 7, 4));
        assertThat(gantt.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 21));
        assertThat(gantt.getTotalDias()).isEqualTo(18);
        assertThat(gantt.getHoyPct()).isEqualTo(91.67);
        assertThat(gantt.getHoyAlineacion()).isEqualTo(GanttPlanificacionVista.Alineacion.CENTRO);
    }

    @Test
    @DisplayName("CP-GCB-09: el eje abarca también hoy cuando cae antes del primer inicio (30/06 a 09/07, 10 días, hoy al 15 %)")
    void elEjeIncluyeHoyAntesDeLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO lote = lote(1L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8));

        // Act
        GanttPlanificacionVista gantt = CronogramaProduccionBuilder.construir(List.of(lote), List.of("F-01"), null, fecha(1)).getGantt();

        // Assert
        assertThat(gantt.getRangoDesde()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(gantt.getRangoHasta()).isEqualTo(LocalDate.of(2027, 7, 9));
        assertThat(gantt.getTotalDias()).isEqualTo(10);
        assertThat(gantt.getHoyPct()).isEqualTo(15.0);
        assertThat(gantt.getCarriles().get(0).getBarras().get(0).getIzquierdaPct()).isEqualTo(50.0);
        assertThat(gantt.getCarriles().get(0).getBarras().get(0).getAnchoPct()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("CP-GCB-10: un lote en ejecución conserva su progreso (inicio real 02/07, fin estimado 11/07, hoy 06/07: 50 %) y los finalizados y cancelados siguen sus reglas")
    void conservaLasReglasDeCadaEstado() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(1), fecha(11))
                .fechaInicio(fecha(2).atTime(8, 0)).build();
        LoteCronogramaResponseDTO finalizado = loteB(2L, "PALE_ALE-2", EstadoLote.FINALIZADO, "F-02", 7L, "Pale Ale", fecha(1), fecha(5))
                .fechaInicio(fecha(2).atTime(9, 0)).fechaFinalizacion(fecha(8).atTime(17, 0)).build();

        // Act
        GanttPlanificacionVista gantt = CronogramaProduccionBuilder.construir(
                List.of(enEjecucion, finalizado), List.of("F-01", "F-02"), null, fecha(6)).getGantt();

        // Assert
        Barra barraEnEjecucion = gantt.getCarriles().get(0).getBarras().get(0);
        assertThat(barraEnEjecucion.getEstadoClave()).isEqualTo("en-ejecucion");
        assertThat(barraEnEjecucion.getDesde()).isEqualTo(fecha(2));
        assertThat(barraEnEjecucion.getHasta()).isEqualTo(fecha(11));
        assertThat(barraEnEjecucion.getProgresoPct()).isEqualTo(50.0);
        Barra barraFinalizada = gantt.getCarriles().get(1).getBarras().get(0);
        assertThat(barraFinalizada.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barraFinalizada.getDesde()).isEqualTo(fecha(2));
        assertThat(barraFinalizada.getHasta()).isEqualTo(fecha(8));
    }

    // ==================== Carriles especiales ====================

    @Test
    @DisplayName("CP-GCB-11: un fermentador con lotes que no estaba en la lista recibida tiene igual su carril, ordenado sin distinguir mayúsculas; el lote sin fermentador va al final y no tiene cola")
    void carrilesExtraYLoteSinFermentador() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(2L, "A-2", EstadoLote.PENDIENTE, null, 7L, "Pale Ale", fecha(1), fecha(5)),
                lote(3L, "A-3", EstadoLote.PENDIENTE, "f-01", 7L, "Pale Ale", fecha(1), fecha(5)));

        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(lotes, List.of("F-02"), null, HOY);

        // Assert
        assertThat(vista.getGantt().getCarriles()).extracting(Carril::getEtiqueta)
                .containsExactly("f-01", "F-02", "F-03", "Sin fermentador");
        assertThat(vista.getColas()).extracting(ColaFermentador::getFermentador).containsExactly("f-01", "F-02", "F-03");
    }

    @Test
    @DisplayName("CP-GCB-12: un lote sin ninguna fecha no se dibuja pero sigue en la cola de su fermentador, al final")
    void unLoteSinFechasNoSeDibujaPeroEstaEnLaCola() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", null, null),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(5), fecha(8)));

        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), null, HOY);

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
        ColaFermentador cola = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), null, HOY).getColas().get(0);

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
        ColaFermentador cola = CronogramaProduccionBuilder.construir(List.of(unico), List.of("F-01"), null, HOY).getColas().get(0);

        // Assert
        assertThat(cola.getPendientes()).hasSize(1);
        assertThat(cola.getPendientes().get(0).isPrimero()).isTrue();
        assertThat(cola.getPendientes().get(0).isUltimo()).isTrue();
        assertThat(cola.getPendientes().get(0).getPosicion()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-GCB-15: la cola informa aparte el lote en ejecución y deja afuera los finalizados y cancelados; las colas de otros fermentadores no se mezclan")
    void laColaSoloTienePendientesYElEnEjecucionVaAparte() {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = List.of(
                lote(1L, "A-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", fecha(1), fecha(2)),
                lote(2L, "A-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(3), fecha(6)),
                lote(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(10)),
                lote(4L, "A-4", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", fecha(12), fecha(14)),
                lote(5L, "B-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", fecha(8), fecha(10)));

        // Act
        CronogramaProduccionVista vista = CronogramaProduccionBuilder.construir(lotes, List.of("F-01", "F-02"), null, HOY);

        // Assert
        ColaFermentador f01 = vista.getColas().get(0);
        assertThat(f01.getFermentador()).isEqualTo("F-01");
        assertThat(f01.getEnEjecucion().getId()).isEqualTo(2L);
        assertThat(f01.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(3L);
        ColaFermentador f02 = vista.getColas().get(1);
        assertThat(f02.getEnEjecucion()).isNull();
        assertThat(f02.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(5L);
        assertThat(vista.getGantt().getCarriles().get(0).getBarras()).hasSize(4);
    }

    // ==================== Atraso y corrimiento ====================

    @Test
    @DisplayName("CP-GCB-16: en un carril el lote en ejecución atrasado llega a hoy (02/07 al 10/07, atraso 33,33 %) y el pendiente se dibuja corrido 3 días (11/07 al 15/07) en un eje de 16 días")
    void atrasoYCorrimientoEnUnCarril_extiendenYDesplazanLasBarras() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        GanttPlanificacionVista gantt = CronogramaProduccionBuilder.construir(
                List.of(enEjecucion, pendiente), List.of("F-01"), null, fecha(10)).getGantt();

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
        assertThat(barraEnEjecucion.getAtrasoPct()).isEqualTo(33.33);
        Barra barraPendiente = barras.get(1);
        assertThat(barraPendiente.getIdLote()).isEqualTo(2L);
        assertThat(barraPendiente.getDesde()).isEqualTo(fecha(11));
        assertThat(barraPendiente.getHasta()).isEqualTo(fecha(15));
        assertThat(barraPendiente.getIzquierdaPct()).isEqualTo(62.5);
        assertThat(barraPendiente.getAnchoPct()).isEqualTo(31.25);
        assertThat(barraPendiente.getDiasDeDesplazamiento()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-GCB-17: el tooltip suma las leyendas de atraso y corrimiento después de \"Receta\" y \"Planificación\" y antes de \"Inicio estimado:\"")
    void tooltipConAtrasoYCorrimiento_ubicaLasLeyendasDespuesDeRecetaYPlanificacion() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        List<Barra> barras = CronogramaProduccionBuilder.construir(
                List.of(enEjecucion, pendiente), List.of("F-01"), null, fecha(10)).getGantt().getCarriles().get(0).getBarras();

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
                loteB(3L, "PALE_ALE-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(13), fecha(18)).diasDeDesplazamiento(3).build(),
                loteB(2L, "PALE_ALE-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12)).diasDeDesplazamiento(3).build(),
                loteB(1L, "PALE_ALE-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", fecha(2), fecha(7))
                        .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build());

        // Act
        ColaFermentador cola = CronogramaProduccionBuilder.construir(lotes, List.of("F-01"), null, fecha(10)).getColas().get(0);

        // Assert
        assertThat(cola.getEnEjecucion().getId()).isEqualTo(1L);
        assertThat(cola.getEnEjecucion().getDiasDeAtraso()).isEqualTo(3);
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getId()).containsExactly(2L, 3L);
        assertThat(cola.getPendientes()).extracting(i -> i.getPosicion()).containsExactly(1, 2);
        assertThat(cola.getPendientes()).extracting(i -> i.getLote().getDiasDeDesplazamiento()).containsExactly(3, 3);
    }

    @Test
    @DisplayName("CP-GCB-19: al destacar otra planificación el lote en ejecución atrasado se atenúa pero conserva su atraso (33,33 %, 3 días)")
    void unLoteAtenuado_conservaSuAtraso() {
        // Arrange
        LoteCronogramaResponseDTO enEjecucion = loteB(1L, "STOUT-1", EstadoLote.EN_EJECUCION, "F-01", 8L, "Stout", fecha(2), fecha(7))
                .fechaInicio(fecha(2).atTime(8, 0)).diasDeAtraso(3).build();
        LoteCronogramaResponseDTO pendiente = loteB(2L, "PALE_ALE-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", fecha(8), fecha(12))
                .diasDeDesplazamiento(3).build();

        // Act
        List<Barra> barras = CronogramaProduccionBuilder.construir(
                List.of(enEjecucion, pendiente), List.of("F-01"), 7L, fecha(10)).getGantt().getCarriles().get(0).getBarras();

        // Assert
        Barra barraAtenuada = barras.stream().filter(b -> b.getIdLote().equals(1L)).findFirst().orElseThrow();
        Barra barraDestacada = barras.stream().filter(b -> b.getIdLote().equals(2L)).findFirst().orElseThrow();
        assertThat(barraAtenuada.isAtenuada()).isTrue();
        assertThat(barraAtenuada.getAtrasoPct()).isEqualTo(33.33);
        assertThat(barraAtenuada.getDiasDeAtraso()).isEqualTo(3);
        assertThat(barraDestacada.isAtenuada()).isFalse();
    }

    // ==================== Datos de prueba ====================

    private static LocalDate fecha(int dia) {
        return LocalDate.of(2027, 7, dia);
    }

    private static LoteCronogramaResponseDTO lote(Long id, String identificador, EstadoLote estado, String fermentador,
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
