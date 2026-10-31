package com.github.heikyudev.maestrocervecero.presentation.view;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.view.GanttPlanificacionVista.Barra;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GanttEquipamientoBuilderTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 30);
    private static final LocalDate HOY = LocalDate.of(2026, 10, 15);

    private static LocalDateTime t(int dia, int hora) {
        return LocalDateTime.of(2026, 10, dia, hora, 0);
    }

    private static BloqueLineaTiempoEquipamientoResponseDTO operacion(TipoBloqueLineaTiempo tipo, LocalDateTime fecha) {
        return BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(tipo).idReferencia(7L).observaciones("Limpieza con soda")
                .fechaInicio(fecha).fechaFin(fecha).build();
    }

    @Test
    @DisplayName("CP-GEQ-01: sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje del período y el título Equipo")
    void sinBloques() {
        GanttPlanificacionVista vista = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(), DESDE, HASTA, HOY);

        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).hasSize(1);
        assertThat(vista.getCarriles().get(0).getEtiqueta()).isEqualTo("FER-01");
        assertThat(vista.getTotalDias()).isEqualTo(30);
        assertThat(vista.getTituloCarriles()).isEqualTo("Equipo");
        assertThat(vista.getLeyendaEquipamiento()).isEmpty();
        assertThat(vista.getHoyPct()).isNotNull();
    }

    @Test
    @DisplayName("CP-GEQ-02: un bloque de lote lleva código y etapa, color por estado de la etapa, enlace al detalle del lote y un tooltip con inicio, fin y estado")
    void bloqueDeLote() {
        BloqueLineaTiempoEquipamientoResponseDTO lote = BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE)
                .idReferencia(3L).idLote(12L).codigoLote("LOT-R1-01").etapa(TipoEtapa.MOLIENDA).estadoEtapa(EstadoEtapaLote.FINALIZADA)
                .fechaInicio(t(3, 8)).fechaFin(t(5, 8)).build();

        Barra barra = GanttEquipamientoBuilder.construir(5L, "M-01", List.of(lote), DESDE, HASTA, HOY).getCarriles().get(0).getBarras().get(0);

        assertThat(barra.getEtiqueta()).isEqualTo("LOT-R1-01 · Molienda");
        assertThat(barra.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barra.getEnlace()).isEqualTo("/lotes/12/detalle");
        assertThat(barra.getTooltip()).contains("Lote LOT-R1-01").contains("Etapa: Molienda").contains("Inicio: 03/10/2026 08:00").contains("Fin: 05/10/2026 08:00");
        assertThat(barra.getIzquierdaPct()).isGreaterThan(0).isLessThan(20);
    }

    @Test
    @DisplayName("CP-GEQ-03: una etapa en curso muestra En curso como fin provisorio en el tooltip")
    void etapaEnCurso() {
        BloqueLineaTiempoEquipamientoResponseDTO lote = BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE)
                .idLote(12L).codigoLote("LOT-R1-01").etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.EN_CURSO)
                .fechaInicio(t(10, 8)).fechaFin(t(15, 8)).finProvisorio(true).build();

        Barra barra = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(lote), DESDE, HASTA, HOY).getCarriles().get(0).getBarras().get(0);

        assertThat(barra.getTooltip()).contains("Fin: En curso");
    }

    @Test
    @DisplayName("CP-GEQ-04: cada operación tiene su color (limpieza, falla y mantenimiento), enlaza a su detalle y su tooltip lleva la fecha y hora y las observaciones")
    void bloquesDeOperacion() {
        List<Barra> barras = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(2, 8)),
                operacion(TipoBloqueLineaTiempo.FALLA, t(4, 8)),
                operacion(TipoBloqueLineaTiempo.MANTENIMIENTO, t(6, 8))), DESDE, HASTA, HOY).getCarriles().get(0).getBarras();

        assertThat(barras).extracting(Barra::getEstadoClave).containsExactly("limpieza", "equipo-falla", "pendiente");
        assertThat(barras).extracting(Barra::getEnlace).containsExactly(
                "/control-equipamiento/5/operaciones/limpieza/7", "/control-equipamiento/5/operaciones/falla/7", "/control-equipamiento/5/operaciones/mantenimiento/7");
        assertThat(barras.get(0).getTooltip()).contains("Observaciones: Limpieza con soda").contains("Fecha y hora: 02/10/2026 08:00").doesNotContain("Responsable");
    }

    @Test
    @DisplayName("CP-GEQ-05: toda barra tiene un ancho mínimo para leer su texto: 120 px las operaciones y 170 px los lotes, aunque duren minutos, y empieza en su posición real del eje")
    void anchoMinimoPorTipo() {
        BloqueLineaTiempoEquipamientoResponseDTO lote = BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE)
                .idLote(12L).codigoLote("LOT-R1-01").etapa(TipoEtapa.MOLIENDA).estadoEtapa(EstadoEtapaLote.FINALIZADA)
                .fechaInicio(t(20, 8)).fechaFin(t(20, 9)).build();

        List<Barra> barras = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(2, 8)), lote), DESDE, HASTA, HOY).getCarriles().get(0).getBarras();

        assertThat(barras.get(0).getEstilo()).contains("min-width: 120px").contains("left: min(" + barras.get(0).getIzquierdaPct() + "%");
        assertThat(barras.get(1).getEstilo()).contains("min-width: 170px");
        assertThat(barras.get(0).getEtiqueta()).isEqualTo("Limpieza");
        assertThat(barras.get(0).getTooltip()).contains("Fecha y hora: 02/10/2026 08:00").doesNotContain("Fin:");
    }

    @Test
    @DisplayName("CP-GEQ-07: las barras no llevan ícono ni marcador: los lotes se colorean por el estado de su etapa y las operaciones por su tipo")
    void coloresSinIconos() {
        List<Barra> barras = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(3, 8)),
                operacion(TipoBloqueLineaTiempo.FALLA, t(6, 8)),
                BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE).idLote(12L).codigoLote("LOT-R1-01")
                        .etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.EN_CURSO).fechaInicio(t(10, 8)).fechaFin(t(15, 8)).finProvisorio(true).build(),
                BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE).idLote(13L).codigoLote("LOT-R1-02")
                        .etapa(TipoEtapa.MOLIENDA).estadoEtapa(EstadoEtapaLote.CANCELADA).fechaInicio(t(20, 8)).fechaFin(t(20, 10)).build()),
                DESDE, HASTA, HOY).getCarriles().get(0).getBarras();

        assertThat(barras).extracting(Barra::getEstadoClave).containsExactly("limpieza", "equipo-falla", "en-ejecucion", "equipo-cancelada");
        assertThat(barras.get(2).getTooltip()).contains("Fin: En curso");
    }

    @Test
    @DisplayName("CP-GEQ-08: las barras que se pisan por su ancho mínimo se reparten en subcarriles sin solaparse y la fila crece; sin superposición la fila conserva su alto estándar")
    void subcarriles() {
        GanttPlanificacionVista juntos = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(2, 8)),
                operacion(TipoBloqueLineaTiempo.FALLA, t(2, 9)),
                operacion(TipoBloqueLineaTiempo.MANTENIMIENTO, t(2, 10))), DESDE, HASTA, HOY);
        GanttPlanificacionVista separados = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(2, 8)),
                operacion(TipoBloqueLineaTiempo.FALLA, t(20, 9))), DESDE, HASTA, HOY);

        assertThat(juntos.getCarriles().get(0).getAlturaRem()).isEqualTo(3.0 * 3);
        assertThat(juntos.getCarriles().get(0).getBarras()).extracting(Barra::getEstilo).allSatisfy(e -> assertThat(e).contains("height: 2rem"))
                .extracting(e -> e.replaceAll(".*top: ([0-9.]+)rem.*", "$1")).doesNotHaveDuplicates();
        assertThat(separados.getCarriles().get(0).getAlturaRem()).isNull();
    }

    @Test
    @DisplayName("CP-GEQ-06: una etapa que sale del período se recorta en el borde y se marca como continuación; un bloque fuera del período se omite")
    void recorteYFuera() {
        BloqueLineaTiempoEquipamientoResponseDTO largo = BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE)
                .idLote(12L).codigoLote("LOT-R1-01").etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.FINALIZADA)
                .fechaInicio(LocalDateTime.of(2026, 9, 28, 8, 0)).fechaFin(t(3, 8)).build();
        GanttPlanificacionVista vista = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(largo,
                operacion(TipoBloqueLineaTiempo.FALLA, LocalDateTime.of(2026, 8, 1, 8, 0))), DESDE, HASTA, HOY);

        List<Barra> barras = vista.getCarriles().get(0).getBarras();
        assertThat(barras).hasSize(1);
        assertThat(barras.get(0).isContinuaAntes()).isTrue();
        assertThat(barras.get(0).getIzquierdaPct()).isZero();
        assertThat(barras.get(0).getTooltip()).contains("Continúa antes del período").contains("Inicio: 28/09/2026 08:00");
    }

    @Test
    @DisplayName("CP-GEQ-09: la leyenda lista solo los tipos de bloque presentes, con el color de cada uno")
    void leyendaSoloConLosTiposPresentes() {
        GanttPlanificacionVista vista = GanttEquipamientoBuilder.construir(5L, "FER-01", List.of(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(2, 8)), operacion(TipoBloqueLineaTiempo.MANTENIMIENTO, t(9, 8))), DESDE, HASTA, HOY);

        assertThat(vista.getLeyendaEquipamiento()).extracting(GanttPlanificacionVista.EntradaLeyenda::getClave, GanttPlanificacionVista.EntradaLeyenda::getTexto)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("limpieza", "Limpieza"), org.assertj.core.groups.Tuple.tuple("pendiente", "Mantenimiento"));
    }
}
