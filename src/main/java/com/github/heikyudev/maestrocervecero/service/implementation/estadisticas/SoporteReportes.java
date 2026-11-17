package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculos comunes de los reportes de estadísticas: redondeos, porcentajes sin dividir por cero, la página de una tabla
 * calculada en memoria, las categorías principales de un gráfico y los textos de los indicadores.
 */
final class SoporteReportes {

    /** Cantidad de decimales de los litros, horas, días y porcentajes de las estadísticas. */
    static final int DECIMALES = 2;
    /** Escala de los importes. */
    static final int ESCALA_IMPORTE = 4;
    /** Escala de los precios por unidad de insumo, para no perder precisión antes de comparar (un gramo vale fracciones de centavo). */
    static final int ESCALA_PRECIO = 10;
    /** Cantidad máxima de categorías de un gráfico; las demás se suman en una sola cuando el valor se puede sumar. */
    static final int MAXIMO_CATEGORIAS = 8;
    static final String TOTALES = "Totales del período";

    private SoporteReportes() {
    }

    static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(DECIMALES, RoundingMode.HALF_UP).doubleValue();
    }

    /** El porcentaje de una parte sobre un total, o {@code null} si el total no es mayor que cero. */
    static Double porcentaje(double parte, double total) {
        return total > 0 ? redondear(parte * 100.0 / total) : null;
    }

    /** El porcentaje de una parte sobre un total de importes, o {@code null} si el total no es mayor que cero. */
    static Double porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() > 0 ? redondear(parte.doubleValue() * 100.0 / total.doubleValue()) : null;
    }

    /** Un porcentaje para un indicador ({@code 75 %}), o un guion si no tiene valor. */
    static String textoPorcentaje(Double porcentaje) {
        return porcentaje == null ? CeldaReporteResponseDTO.SIN_DATO : FormatoCantidad.conUnidad(porcentaje, "%");
    }

    /** Una variación en porcentaje con su signo ({@code +12,5 %}), o un guion si no tiene valor. */
    static String textoVariacion(Double porcentaje) {
        if (porcentaje == null) {
            return CeldaReporteResponseDTO.SIN_DATO;
        }
        String texto = FormatoCantidad.conUnidad(porcentaje, "%");
        return porcentaje > 0 ? "+" + texto : texto;
    }

    /** Un importe con su símbolo, o un guion si no tiene valor. */
    static String textoImporte(BigDecimal importe) {
        return importe == null ? CeldaReporteResponseDTO.SIN_DATO : FormatoImporte.conSimbolo(importe);
    }

    static BigDecimal aImporte(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(ESCALA_IMPORTE);
        }
        BigDecimal importe = valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
        return importe.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }

    static double aDouble(Object valor) {
        return valor == null ? 0.0 : ((Number) valor).doubleValue();
    }

    static long aLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    /** Un importe dividido por una cantidad mayor que cero, o {@code null} si la cantidad no lo es. */
    static BigDecimal dividir(BigDecimal importe, double cantidad) {
        return cantidad > 0 ? importe.divide(BigDecimal.valueOf(cantidad), ESCALA_IMPORTE, RoundingMode.HALF_UP) : null;
    }

    /** Un importe dividido por una cantidad mayor que cero con {@link #ESCALA_PRECIO} decimales, o {@code null} si la cantidad no lo es. */
    static BigDecimal dividirConPrecision(BigDecimal importe, double cantidad) {
        return cantidad > 0 ? importe.divide(BigDecimal.valueOf(cantidad), ESCALA_PRECIO, RoundingMode.HALF_UP) : null;
    }

    /** El importe redondeado a la escala con que se muestra, o {@code null} si no tiene valor. */
    static BigDecimal paraMostrar(BigDecimal importe) {
        return importe == null ? null : importe.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }

    /**
     * La página pedida de una tabla ya calculada en memoria. Una página posterior a la última queda vacía, con el total
     * real, para que el Controller pida la última.
     */
    static <T> Page<T> paginar(List<T> filas, Pageable pageable) {
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        int desde = (int) Math.min(pagina.getOffset(), filas.size());
        int hasta = Math.min(desde + pagina.getPageSize(), filas.size());
        return new PageImpl<>(filas.subList(desde, hasta), pagina, filas.size());
    }

    /** Todas las filas en una sola página, para las tablas con título, que no se paginan. */
    static <T> Page<T> paginaUnica(List<T> filas) {
        return new PageImpl<>(filas, PageRequest.of(0, Math.max(1, filas.size())), filas.size());
    }

    /**
     * Las categorías de un gráfico, ya ordenadas de mayor a menor: las primeras {@link #MAXIMO_CATEGORIAS} y, si hay más
     * y {@code etiquetaResto} no es {@code null}, una más que suma el resto.
     */
    static List<DatoGraficoResponseDTO> principales(List<DatoGraficoResponseDTO> ordenados, String etiquetaResto) {
        if (ordenados.size() <= MAXIMO_CATEGORIAS) {
            return ordenados;
        }
        List<DatoGraficoResponseDTO> resultado = new ArrayList<>(ordenados.subList(0, MAXIMO_CATEGORIAS));
        if (etiquetaResto != null) {
            double resto = ordenados.subList(MAXIMO_CATEGORIAS, ordenados.size()).stream().mapToDouble(DatoGraficoResponseDTO::getValor).sum();
            resultado.add(new DatoGraficoResponseDTO(etiquetaResto, redondear(resto)));
        }
        return resultado;
    }

    /** Los minutos de un intervalo recortado al período {@code [inicio, finExclusivo)}; cero si no se superponen. */
    static long minutosEnElPeriodo(LocalDateTime desde, LocalDateTime hasta, LocalDateTime inicio, LocalDateTime finExclusivo) {
        LocalDateTime a = desde.isBefore(inicio) ? inicio : desde;
        LocalDateTime b = hasta.isAfter(finExclusivo) ? finExclusivo : hasta;
        return b.isAfter(a) ? java.time.Duration.between(a, b).toMinutes() : 0L;
    }
}
