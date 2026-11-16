package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.IntStream;

/**
 * La única tabla de un reporte de estadísticas: hasta {@link #MAXIMO_COLUMNAS} columnas, una página de filas y, si
 * corresponde, la fila de totales del período completo (no de la página).
 */
@Getter
public final class TablaReporteResponseDTO {

    /** Cantidad máxima de columnas: la tabla tiene que entrar en una hoja A4 vertical. */
    public static final int MAXIMO_COLUMNAS = 6;

    private final List<ColumnaReporteResponseDTO> columnas;

    /** Página de filas pedida; en el reporte impreso, la primera página con el tope técnico de filas. */
    private final Page<FilaReporteResponseDTO> filas;

    /** Fila de totales del período, o {@code null} si el reporte no la lleva o si no hay filas. */
    private final FilaReporteResponseDTO totales;

    /** Qué es cada fila, en singular y plural separados por "|" (por ejemplo, {@code lote|lotes}), para la paginación. */
    private final String etiquetaFilas;

    /**
     * Arma la tabla.
     *
     * @param columnas Columnas de la tabla, como máximo {@link #MAXIMO_COLUMNAS}.
     * @param filas Página de filas, cada una con una celda por columna.
     * @param totales Fila de totales con una celda por columna, o {@code null}; sin filas no se muestra.
     * @param etiquetaFilas Qué es cada fila, en singular y plural separados por "|" (por ejemplo, {@code lote|lotes}).
     * @throws IllegalArgumentException Si hay más columnas que el máximo o si una fila no tiene una celda por columna.
     */
    public TablaReporteResponseDTO(List<ColumnaReporteResponseDTO> columnas, Page<FilaReporteResponseDTO> filas,
                                   FilaReporteResponseDTO totales, String etiquetaFilas) {
        if (columnas.size() > MAXIMO_COLUMNAS) {
            throw new IllegalArgumentException("La tabla de un reporte admite como máximo " + MAXIMO_COLUMNAS + " columnas");
        }
        for (FilaReporteResponseDTO fila : filas.getContent()) {
            validarCeldas(fila, columnas.size());
        }
        if (totales != null) {
            validarCeldas(totales, columnas.size());
        }
        this.columnas = List.copyOf(columnas);
        this.filas = filas;
        this.totales = filas.isEmpty() ? null : totales;
        this.etiquetaFilas = etiquetaFilas;
    }

    /** Qué es cada fila, en plural (por ejemplo, {@code lotes}). */
    public String getNombrePlural() {
        return etiquetaFilas.substring(etiquetaFilas.indexOf('|') + 1);
    }

    /** Las columnas que se imprimen: todas menos la de acciones. */
    public List<ColumnaReporteResponseDTO> getColumnasImprimibles() {
        return columnas.stream().filter(ColumnaReporteResponseDTO::isImprimible).toList();
    }

    /** Las celdas de una fila que se imprimen: las de las columnas imprimibles, en su orden. */
    public List<CeldaReporteResponseDTO> celdasImprimibles(FilaReporteResponseDTO fila) {
        return IntStream.range(0, columnas.size())
                .filter(i -> columnas.get(i).isImprimible())
                .mapToObj(fila.getCeldas()::get)
                .toList();
    }

    /** {@code true} si la página no trae todas las filas del período; en el reporte impreso, se alcanzó el tope técnico. */
    public boolean isTruncada() {
        return filas.getTotalElements() > filas.getNumberOfElements();
    }

    private static void validarCeldas(FilaReporteResponseDTO fila, int columnas) {
        if (fila.getCeldas().size() != columnas) {
            throw new IllegalArgumentException("Cada fila de la tabla debe tener una celda por columna");
        }
    }
}
