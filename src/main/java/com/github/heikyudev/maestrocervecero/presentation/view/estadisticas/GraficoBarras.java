package com.github.heikyudev.maestrocervecero.presentation.view.estadisticas;

import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.GraficoReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modelo del gráfico de un reporte de estadísticas, listo para dibujarse en el servidor (fragmento
 * {@code estadisticas/fragments/grafico :: grafico}) con HTML y CSS, sin JavaScript. El mismo modelo sirve para la
 * pantalla y para el reporte impreso.
 * <p>
 * Son barras horizontales simples, una por categoría y en el orden recibido: la etiqueta a la izquierda y la barra con
 * su valor escrito a continuación. El largo de cada barra es un porcentaje del ancho disponible, proporcional a su valor
 * sobre el valor más alto (que ocupa {@link #PORCENTAJE_MAXIMO} %, para dejar lugar al valor escrito). Así el gráfico
 * ocupa el ancho que tenga su contenedor, sin espacio vacío, y el texto se escribe al tamaño del texto de las tablas.
 * </p>
 * <p>
 * En un gráfico de variaciones el largo es proporcional al valor absoluto, el valor se escribe con su signo y las barras
 * negativas usan {@link #COLOR_NEGATIVO}. En los demás gráficos un valor negativo se dibuja como cero.
 * </p>
 */
@Getter
public final class GraficoBarras {

    /** Largo, en porcentaje del ancho disponible, de la barra del valor más alto. */
    public static final double PORCENTAJE_MAXIMO = 75.0;
    /** Largo mínimo visible, en porcentaje, de una barra con valor distinto de cero. */
    static final double PORCENTAJE_MINIMO = 1.0;
    /** Largo máximo de una etiqueta dentro del gráfico; las más largas se abrevian (la tabla muestra el texto completo). */
    static final int LARGO_MAXIMO_ETIQUETA = 16;
    /** Color de las barras: el ámbar de acento del sistema. */
    public static final String COLOR_BARRA = "#f59e0b";
    /** Color de las barras negativas de un gráfico de variaciones: el azul del sistema. */
    public static final String COLOR_NEGATIVO = "#0284c7";

    /** Barra ya calculada. */
    @Getter
    @AllArgsConstructor
    public static final class Barra {
        /** Etiqueta completa de la categoría. */
        private final String etiqueta;
        /** Etiqueta que se escribe en el gráfico: la completa, o abreviada con puntos suspensivos. */
        private final String etiquetaCorta;
        private final double valor;
        /** Valor con el formato del gráfico (cantidad con unidad, importe o variación con signo). */
        private final String valorTexto;
        /** Largo de la barra en porcentaje del ancho disponible; cero si el valor es cero. */
        private final double porcentaje;
        /** Color de la barra. */
        private final String color;
    }

    /** Clave única del gráfico dentro de la página. */
    private final String clave;
    private final String titulo;
    /** Texto alternativo: cada categoría con su valor. */
    private final String descripcion;
    private final List<Barra> barras;
    /** {@code true} si no hay ningún valor distinto de cero: el gráfico no se dibuja y se muestra una leyenda. */
    private final boolean vacio;

    private GraficoBarras(String clave, String titulo, String descripcion, List<Barra> barras, boolean vacio) {
        this.clave = clave;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.barras = barras;
        this.vacio = vacio;
    }

    /**
     * Calcula el gráfico de un reporte.
     *
     * @param clave Clave única del gráfico dentro de la página.
     * @param grafico Datos del gráfico del reporte.
     * @return El gráfico calculado; vacío si ninguna categoría tiene un valor que dibujar.
     */
    public static GraficoBarras de(String clave, GraficoReporteResponseDTO grafico) {
        List<DatoGraficoResponseDTO> datos = grafico.getDatos() == null ? List.of() : grafico.getDatos();
        boolean conSigno = grafico.isConSigno();
        String descripcion = datos.stream()
                .map(d -> d.getEtiqueta() + ": " + texto(valorDibujado(d.getValor(), conSigno), grafico))
                .collect(Collectors.joining("; "));
        double maximo = datos.stream().mapToDouble(d -> Math.abs(valorDibujado(d.getValor(), conSigno))).max().orElse(0.0);
        if (maximo <= 0.0) {
            return new GraficoBarras(clave, grafico.getTitulo(), descripcion, List.of(), true);
        }
        List<Barra> barras = new ArrayList<>();
        for (DatoGraficoResponseDTO dato : datos) {
            double valor = valorDibujado(dato.getValor(), conSigno);
            double absoluto = Math.abs(valor);
            double porcentaje = absoluto <= 0.0 ? 0.0 : Math.max(PORCENTAJE_MINIMO, redondear(absoluto / maximo * PORCENTAJE_MAXIMO));
            barras.add(new Barra(dato.getEtiqueta(), abreviar(dato.getEtiqueta()), valor, texto(valor, grafico), porcentaje,
                    valor < 0 ? COLOR_NEGATIVO : COLOR_BARRA));
        }
        return new GraficoBarras(clave, grafico.getTitulo(), descripcion, barras, false);
    }

    static String abreviar(String etiqueta) {
        if (etiqueta == null || etiqueta.length() <= LARGO_MAXIMO_ETIQUETA) {
            return etiqueta;
        }
        return etiqueta.substring(0, LARGO_MAXIMO_ETIQUETA - 1).stripTrailing() + "…";
    }

    /** El valor que se dibuja: los negativos solo se conservan en un gráfico de variaciones. */
    private static double valorDibujado(double valor, boolean conSigno) {
        return conSigno ? valor : Math.max(0.0, valor);
    }

    private static String texto(double valor, GraficoReporteResponseDTO grafico) {
        String unidad = grafico.getUnidad();
        if (grafico.isEnMoneda()) {
            String importe = FormatoImporte.conSimbolo(BigDecimal.valueOf(redondear(valor)));
            return unidad == null ? importe : importe + " / " + unidad;
        }
        String numero = FormatoCantidad.formatear(redondear(valor));
        if (grafico.isConSigno() && valor > 0) {
            numero = "+" + numero;
        }
        return unidad == null ? numero : numero + " " + unidad;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
