package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Datos del único gráfico de un reporte de estadísticas: barras horizontales simples, una por categoría, en el orden
 * recibido. La geometría del dibujo la calcula la capa de presentación.
 * <p>
 * El valor de cada barra se escribe como una cantidad (con su unidad), como un importe ({@link #moneda}) o como una
 * variación con signo ({@link #variacion}); solo una variación admite valores negativos, que se dibujan con otro color.
 * </p>
 */
@Getter
@AllArgsConstructor
public final class GraficoReporteResponseDTO {

    /** Título del gráfico (por ejemplo, "Lotes por estado"). */
    private final String titulo;

    /** Categorías del gráfico, en el orden en que se dibujan. */
    private final List<DatoGraficoResponseDTO> datos;

    /** Unidad que acompaña a cada valor escrito (por ejemplo, "L"), o {@code null} si no lleva. */
    private final String unidad;

    /** {@code true} si los valores son importes: se escriben con el símbolo de la moneda. */
    private final boolean enMoneda;

    /** {@code true} si los valores son variaciones: se escriben con su signo y pueden ser negativos. */
    private final boolean conSigno;

    /** Un gráfico de cantidades, con su unidad. */
    public GraficoReporteResponseDTO(String titulo, List<DatoGraficoResponseDTO> datos, String unidad) {
        this(titulo, datos, unidad, false, false);
    }

    /**
     * Un gráfico de importes.
     *
     * @param unidad Unidad por la que se expresa el importe (por ejemplo, "L" para "$ 15,20 / L"), o {@code null}.
     */
    public static GraficoReporteResponseDTO moneda(String titulo, List<DatoGraficoResponseDTO> datos, String unidad) {
        return new GraficoReporteResponseDTO(titulo, datos, unidad, true, false);
    }

    /** Un gráfico de variaciones en porcentaje, con signo; los valores pueden ser negativos. */
    public static GraficoReporteResponseDTO variacion(String titulo, List<DatoGraficoResponseDTO> datos) {
        return new GraficoReporteResponseDTO(titulo, datos, "%", false, true);
    }
}
