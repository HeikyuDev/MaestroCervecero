package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Estructura única de todo reporte de estadísticas: la misma pantalla y el mismo reporte impreso la muestran para
 * cualquier reporte. Tiene un título, una línea que dice qué pregunta responde, el período y los criterios aplicados,
 * hasta {@link #MAXIMO_INDICADORES} indicadores, un gráfico de barras y una tabla.
 */
@Getter
public final class ReporteEstadisticoResponseDTO {

    /** Cantidad máxima de indicadores de un reporte. */
    public static final int MAXIMO_INDICADORES = 4;

    /** Título del reporte (por ejemplo, "Lotes"). */
    private final String titulo;

    /** La pregunta que responde el reporte, en palabras simples y sin punto final. */
    private final String descripcion;

    /** Primer día del período. */
    private final LocalDate desde;

    /** Último día del período. */
    private final LocalDate hasta;

    /** Todos los criterios de filtro propios del reporte, se hayan indicado o no ("Todos" o "Todas"). */
    private final List<CriterioAplicadoResponseDTO> criterios;

    private final List<IndicadorResponseDTO> indicadores;

    private final GraficoReporteResponseDTO grafico;

    private final TablaReporteResponseDTO tabla;

    /**
     * Arma el reporte.
     *
     * @throws IllegalArgumentException Si tiene más indicadores que {@link #MAXIMO_INDICADORES}.
     */
    @Builder
    public ReporteEstadisticoResponseDTO(String titulo, String descripcion, LocalDate desde, LocalDate hasta,
                                         List<CriterioAplicadoResponseDTO> criterios, List<IndicadorResponseDTO> indicadores,
                                         GraficoReporteResponseDTO grafico, TablaReporteResponseDTO tabla) {
        if (indicadores.size() > MAXIMO_INDICADORES) {
            throw new IllegalArgumentException("Un reporte admite como máximo " + MAXIMO_INDICADORES + " indicadores");
        }
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.desde = desde;
        this.hasta = hasta;
        this.criterios = List.copyOf(criterios);
        this.indicadores = List.copyOf(indicadores);
        this.grafico = grafico;
        this.tabla = tabla;
    }
}
