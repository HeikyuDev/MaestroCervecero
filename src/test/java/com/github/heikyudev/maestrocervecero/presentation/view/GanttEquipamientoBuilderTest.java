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

    /** Semana del lunes 26/10/2026 al domingo 01/11/2026. */
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 29, 12, 0);

    private static LocalDateTime t(int dia, int hora) {
        return LocalDateTime.of(2026, 10, dia, hora, 0);
    }

    private static BloqueLineaTiempoEquipamientoResponseDTO operacion(TipoBloqueLineaTiempo tipo, LocalDateTime fecha) {
        return BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(tipo).idReferencia(7L).observaciones("Limpieza con soda")
                .fechaInicio(fecha).fechaFin(fecha).build();
    }

    private static BloqueLineaTiempoEquipamientoResponseDTO lote(String codigo, TipoEtapa etapa, EstadoEtapaLote estado, LocalDateTime inicio, LocalDateTime fin) {
        return BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE).idReferencia(3L).idLote(12L)
                .codigoLote(codigo).etapa(etapa).estadoEtapa(estado).fechaInicio(inicio).fechaFin(fin).build();
    }

    private static BloqueLineaTiempoEquipamientoResponseDTO loteEnCurso() {
        return BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE).idReferencia(3L).idLote(12L).codigoLote("LOT-R1-01")
                .etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.EN_CURSO).fechaInicio(t(26, 8)).fechaFin(t(29, 12)).finProvisorio(true).build();
    }

    private static GanttPlanificacionVista construir(BloqueLineaTiempoEquipamientoResponseDTO... bloques) {
        return GanttEquipamientoBuilder.construir(5L, "MOL-01", List.of(bloques), LUNES, AHORA);
    }

    private static List<Barra> barras(GanttPlanificacionVista vista) {
        return vista.getCarriles().get(0).getBarras();
    }

    @Test
    @DisplayName("CP-GEQ-01: sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje de una semana a 240 px por día y el título Equipo")
    void sinBloques() {
        GanttPlanificacionVista vista = construir();

        assertThat(vista.isVacio()).isTrue();
        assertThat(vista.getCarriles()).hasSize(1);
        assertThat(vista.getCarriles().get(0).getEtiqueta()).isEqualTo("MOL-01");
        assertThat(vista.getTotalDias()).isEqualTo(7);
        assertThat(vista.getAnchoMinimoPx()).isEqualTo(7 * 240);
        assertThat(vista.getTituloCarriles()).isEqualTo("Equipo");
        assertThat(vista.getLeyendaEquipamiento()).isEmpty();
    }

    @Test
    @DisplayName("CP-GEQ-02: el eje rotula cada día con su día de la semana y su fecha y cada 6 horas con la hora, y la marca de hoy está en el momento actual")
    void eje() {
        GanttPlanificacionVista vista = construir();

        assertThat(vista.getMarcasEje()).hasSize(28);
        assertThat(vista.getMarcasEje().get(0).getEtiqueta()).isEqualTo("lun 26/10");
        assertThat(vista.getMarcasEje().get(1).getEtiqueta()).isEqualTo("06:00");
        assertThat(vista.getMarcasEje().get(4).getEtiqueta()).isEqualTo("mar 27/10");
        // jueves 29/10 a las 12:00: 3 días y medio de 7
        assertThat(vista.getHoyPct()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-GEQ-03: el ancho de una etapa de lote es proporcional a su duración real, con el color de su estado, el texto dentro cuando entra completo y el enlace al lote")
    void bloqueDeLote() {
        Barra barra = barras(construir(lote("LOT-R1-01", TipoEtapa.FERMENTACION, EstadoEtapaLote.FINALIZADA, t(27, 0), t(29, 0)))).get(0);

        // 2 días de 7
        assertThat(barra.getAnchoPct()).isEqualTo(28.57);
        assertThat(barra.getIzquierdaPct()).isEqualTo(14.29);
        assertThat(barra.getEtiqueta()).isEqualTo("LOT-R1-01 · Fermentación");
        assertThat(barra.getEstadoClave()).isEqualTo("finalizado");
        assertThat(barra.getEnlace()).isEqualTo("/lotes/12/detalle");
        assertThat(barra.getTooltip()).contains("Lote LOT-R1-01").contains("Etapa: Fermentación").contains("Inicio: 27/10/2026 00:00").contains("Fin: 29/10/2026 00:00");
    }

    @Test
    @DisplayName("CP-GEQ-04: una etapa corta, de pocas horas, se dibuja proporcional y sin texto, y se identifica por su color y su tooltip")
    void etapaCortaSinTexto() {
        Barra barra = barras(construir(lote("LOT-R1-01", TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, t(28, 8), t(28, 11)))).get(0);

        // 3 horas = 30 px de 1680
        assertThat(barra.getAnchoPct()).isEqualTo(1.79);
        assertThat(barra.getEtiqueta()).isEmpty();
        assertThat(barra.getTooltip()).contains("Inicio: 28/10/2026 08:00").contains("Fin: 28/10/2026 11:00");
    }

    @Test
    @DisplayName("CP-GEQ-05: una etapa en curso muestra En curso como fin en el tooltip y una cancelada se pinta de gris")
    void etapaEnCursoYCancelada() {
        List<Barra> barras = barras(construir(
                loteEnCurso(),
                lote("LOT-R1-02", TipoEtapa.MOLIENDA, EstadoEtapaLote.CANCELADA, t(30, 8), t(30, 10))));

        assertThat(barras).extracting(Barra::getEstadoClave).containsExactly("en-ejecucion", "equipo-cancelada");
        assertThat(barras.get(0).getTooltip()).contains("Fin: En curso");
    }

    @Test
    @DisplayName("CP-GEQ-06: una operación es un rectángulo angosto de ancho fijo en su instante, sin texto, con su color sólido, su enlace y su tooltip con fecha y hora exactas")
    void bloquesDeOperacion() {
        List<Barra> barras = barras(construir(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(27, 8)),
                operacion(TipoBloqueLineaTiempo.FALLA, t(28, 8)),
                operacion(TipoBloqueLineaTiempo.MANTENIMIENTO, t(29, 8))));

        assertThat(barras).extracting(Barra::getEstadoClave).containsExactly("equipo-limpieza", "equipo-falla", "equipo-mantenimiento");
        assertThat(barras).extracting(Barra::getEnlace).containsExactly(
                "/control-equipamiento/5/operaciones/limpieza/7", "/control-equipamiento/5/operaciones/falla/7", "/control-equipamiento/5/operaciones/mantenimiento/7");
        assertThat(barras).allSatisfy(b -> {
            assertThat(b.getEtiqueta()).isEmpty();
            assertThat(b.getEstilo()).contains("width: 9px");
        });
        assertThat(barras.get(0).getTooltip()).contains("Limpieza").contains("Observaciones: Limpieza con soda").contains("Fecha y hora: 27/10/2026 08:00").doesNotContain("Responsable");
    }

    @Test
    @DisplayName("CP-GEQ-07: todo es una sola fila: sin subcarriles, sin íconos ni marcadores, y cada barra conserva el alto estándar")
    void unaSolaFila() {
        GanttPlanificacionVista vista = construir(
                operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(28, 8)), operacion(TipoBloqueLineaTiempo.FALLA, t(28, 9)),
                lote("LOT-R1-01", TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, t(28, 10), t(28, 12)));

        assertThat(vista.getCarriles()).hasSize(1);
        assertThat(barras(vista)).extracting(Barra::getEstilo).allSatisfy(e -> assertThat(e).doesNotContain("top:").doesNotContain("height:"));
    }

    @Test
    @DisplayName("CP-GEQ-08: una etapa que sale de la semana se recorta en el borde y se marca como continuación; un bloque fuera de la semana se omite")
    void recorteYFuera() {
        GanttPlanificacionVista vista = construir(
                lote("LOT-R1-01", TipoEtapa.FERMENTACION, EstadoEtapaLote.FINALIZADA, LocalDateTime.of(2026, 10, 20, 8, 0), t(27, 0)),
                operacion(TipoBloqueLineaTiempo.FALLA, LocalDateTime.of(2026, 10, 1, 8, 0)));

        List<Barra> barras = barras(vista);
        assertThat(barras).hasSize(1);
        assertThat(barras.get(0).isContinuaAntes()).isTrue();
        assertThat(barras.get(0).getIzquierdaPct()).isZero();
        assertThat(barras.get(0).getTooltip()).contains("Continúa antes de la semana").contains("Inicio: 20/10/2026 08:00");
    }

    @Test
    @DisplayName("CP-GEQ-09: la leyenda lista solo los tipos de bloque presentes, con el color de cada uno")
    void leyendaSoloConLosTiposPresentes() {
        GanttPlanificacionVista vista = construir(operacion(TipoBloqueLineaTiempo.LIMPIEZA, t(27, 8)), operacion(TipoBloqueLineaTiempo.MANTENIMIENTO, t(28, 8)));

        assertThat(vista.getLeyendaEquipamiento()).extracting(GanttPlanificacionVista.EntradaLeyenda::getClave, GanttPlanificacionVista.EntradaLeyenda::getTexto)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("equipo-limpieza", "Limpieza"), org.assertj.core.groups.Tuple.tuple("equipo-mantenimiento", "Mantenimiento"));
    }
}
