package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Estructura única de todo reporte de estadísticas: la misma pantalla y el mismo reporte impreso la muestran para
 * cualquier reporte. Tiene un título, una línea que dice qué pregunta responde, el período y los criterios aplicados,
 * hasta {@link #MAXIMO_INDICADORES} indicadores, un gráfico de barras y una tabla o, si el reporte lo necesita, varias
 * tablas con título, una debajo de la otra.
 */
@Getter
public final class ReporteEstadisticoResponseDTO {

    /** Cantidad máxima de indicadores de un reporte. */
    public static final int MAXIMO_INDICADORES = 4;

    /** Título del reporte (por ejemplo, "Lotes"). */
    private final String titulo;

    /** La pregunta que responde el reporte, en palabras simples y sin punto final. */
    private final String descripcion;

    /**
     * Qué fecha decide qué registros entran en el período (por ejemplo, "Se consideran los lotes con inicio estimado dentro del período, en cualquier estado"),
     * sin punto final; {@code null} si el reporte no la aclara.
     */
    private final String criterioDeFecha;

    /** Primer día del período. */
    private final LocalDate desde;

    /** Último día del período. */
    private final LocalDate hasta;

    /** Todos los criterios de filtro propios del reporte, se hayan indicado o no ("Todos" o "Todas"). */
    private final List<CriterioAplicadoResponseDTO> criterios;

    private final List<IndicadorResponseDTO> indicadores;

    private final GraficoReporteResponseDTO grafico;

    /** Las tablas del reporte, en el orden en que se muestran; siempre hay al menos una. */
    private final List<TablaReporteResponseDTO> tablas;

    /**
     * Arma el reporte.
     *
     * @param tabla La única tabla del reporte; {@code null} si se indican {@code tablas}.
     * @param tablas Las tablas con título del reporte, una debajo de la otra; {@code null} o vacía si se indica {@code tabla}.
     * @throws IllegalArgumentException Si tiene más indicadores que {@link #MAXIMO_INDICADORES} o si no se indica
     * exactamente una de {@code tabla} y {@code tablas}.
     */
    @Builder
    public ReporteEstadisticoResponseDTO(String titulo, String descripcion, String criterioDeFecha, LocalDate desde, LocalDate hasta,
                                         List<CriterioAplicadoResponseDTO> criterios, List<IndicadorResponseDTO> indicadores,
                                         GraficoReporteResponseDTO grafico, TablaReporteResponseDTO tabla,
                                         List<TablaReporteResponseDTO> tablas) {
        if (indicadores.size() > MAXIMO_INDICADORES) {
            throw new IllegalArgumentException("Un reporte admite como máximo " + MAXIMO_INDICADORES + " indicadores");
        }
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.criterioDeFecha = criterioDeFecha;
        this.desde = desde;
        this.hasta = hasta;
        this.criterios = List.copyOf(criterios);
        this.indicadores = List.copyOf(indicadores);
        this.grafico = grafico;
        boolean variasTablas = tablas != null && !tablas.isEmpty();
        if ((tabla == null) == !variasTablas) {
            throw new IllegalArgumentException("Un reporte lleva una tabla o una lista de tablas, no ambas ni ninguna");
        }
        this.tablas = variasTablas ? List.copyOf(tablas) : List.of(tabla);
    }

    /** La primera tabla del reporte; la única en los reportes de una tabla. */
    public TablaReporteResponseDTO getTabla() {
        return tablas.get(0);
    }
}
