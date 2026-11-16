package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Celda de la tabla de un reporte de estadísticas, con su valor ya formateado. Las fábricas aplican el formato de la
 * aplicación según el tipo de dato; un valor sin dato se muestra con un guion.
 */
@Getter
@AllArgsConstructor
public final class CeldaReporteResponseDTO {

    /** Texto de un valor sin dato; es el único guion de los reportes. */
    public static final String SIN_DATO = "-";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Texto de la celda; en una celda de enlace, el texto accesible del botón "Abrir". */
    private final String texto;

    /** Texto secundario en letra chica debajo del texto (por ejemplo, la receta debajo del código del lote), o {@code null}. */
    private final String detalle;

    /** ID del registro al que lleva el botón "Abrir" en una celda de enlace; {@code null} en las demás. */
    private final Long id;

    /** Un texto. */
    public static CeldaReporteResponseDTO texto(String texto) {
        return new CeldaReporteResponseDTO(conGuion(texto), null, null);
    }

    /** Un texto con un texto secundario debajo. */
    public static CeldaReporteResponseDTO texto(String texto, String detalle) {
        return new CeldaReporteResponseDTO(conGuion(texto), detalle, null);
    }

    /** Un número con el formato de la aplicación (por ejemplo, {@code 1.380,5}). */
    public static CeldaReporteResponseDTO numero(Number numero) {
        return new CeldaReporteResponseDTO(FormatoCantidad.formatear(numero), null, null);
    }

    /** Un número con su unidad (por ejemplo, {@code 120 kg}). */
    public static CeldaReporteResponseDTO numero(Number numero, String unidad) {
        return new CeldaReporteResponseDTO(numero == null ? SIN_DATO : FormatoCantidad.conUnidad(numero, unidad), null, null);
    }

    /** Una variación en porcentaje con su signo (por ejemplo, {@code +12,5 %}, {@code 0 %} o {@code -3 %}). */
    public static CeldaReporteResponseDTO variacion(Number porcentaje) {
        if (porcentaje == null) {
            return new CeldaReporteResponseDTO(SIN_DATO, null, null);
        }
        String texto = FormatoCantidad.conUnidad(porcentaje, "%");
        return new CeldaReporteResponseDTO(porcentaje.doubleValue() > 0 ? "+" + texto : texto, null, null);
    }

    /** Un porcentaje (por ejemplo, {@code 75 %}). */
    public static CeldaReporteResponseDTO porcentaje(Number porcentaje) {
        return new CeldaReporteResponseDTO(porcentaje == null ? SIN_DATO : FormatoCantidad.conUnidad(porcentaje, "%"), null, null);
    }

    /** Un importe con su símbolo (por ejemplo, {@code $ 1.250,50}). */
    public static CeldaReporteResponseDTO moneda(BigDecimal importe) {
        return new CeldaReporteResponseDTO(FormatoImporte.conSimbolo(importe), null, null);
    }

    /** Una fecha en formato dd/MM/yyyy. */
    public static CeldaReporteResponseDTO fecha(LocalDate fecha) {
        return new CeldaReporteResponseDTO(fecha == null ? SIN_DATO : fecha.format(FORMATO_FECHA), null, null);
    }

    /** El día de una fecha y hora, en formato dd/MM/yyyy. */
    public static CeldaReporteResponseDTO fecha(LocalDateTime fechaHora) {
        return fecha(fechaHora == null ? null : fechaHora.toLocalDate());
    }

    /** El botón "Abrir" hacia el registro con ese ID, con su texto accesible (por ejemplo, "Abrir LOT-R7-03"). */
    public static CeldaReporteResponseDTO enlace(Long id, String textoAccesible) {
        return new CeldaReporteResponseDTO(textoAccesible, null, id);
    }

    /** Una celda vacía (por ejemplo, en la fila de totales). */
    public static CeldaReporteResponseDTO vacia() {
        return new CeldaReporteResponseDTO("", null, null);
    }

    private static String conGuion(String texto) {
        return texto == null || texto.isBlank() ? SIN_DATO : texto;
    }
}
