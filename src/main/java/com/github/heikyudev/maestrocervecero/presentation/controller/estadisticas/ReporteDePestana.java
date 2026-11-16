package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.CampoFiltro;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * El reporte de una pestaña de la pantalla "Estadísticas". Cada implementación es un componente de Spring que interpreta
 * los criterios propios de su pestaña (como máximo {@link #MAXIMO_CAMPOS}) y llama al método del Service que devuelve el
 * reporte. El período, los permisos, la pantalla y el reporte impreso son comunes y los resuelve
 * {@link EstadisticasController}.
 */
public interface ReporteDePestana {

    /** Cantidad máxima de criterios propios de una pestaña, además de "Desde" y "Hasta". */
    int MAXIMO_CAMPOS = 2;

    /**
     * Criterios ya interpretados de una petición, listos para generar el reporte.
     *
     * @param campos Los criterios propios de la pestaña con su valor aplicado, para la barra de filtros.
     * @param avisos Avisos de interpretación (por ejemplo, un estado inexistente que no filtra); vacío si no hay.
     * @param generador Genera el reporte para una página de la tabla; lanza ReglaNegocioException si el período no es válido.
     */
    record Solicitud(List<CampoFiltro> campos, List<String> avisos, Function<Pageable, ReporteEstadisticoResponseDTO> generador) {
    }

    /** La pestaña que arma este reporte. */
    PestanaEstadistica pestana();

    /**
     * Interpreta los criterios propios de la pestaña. Un valor que no existe o que el buscador no ofrece no filtra.
     *
     * @param desde Primer día del período pedido, o {@code null} si no se indicó o no es una fecha.
     * @param hasta Último día del período pedido, o {@code null} si no se indicó o no es una fecha.
     * @param parametros Todos los parámetros de la petición.
     * @return La solicitud del reporte.
     */
    Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros);
}
