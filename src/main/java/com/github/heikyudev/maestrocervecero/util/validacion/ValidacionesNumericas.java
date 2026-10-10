package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Validaciones numéricas y de texto reutilizables por los services.
 * <p>
 * Todas las validaciones lanzan {@link ReglaNegocioException} con un mensaje en español. La etiqueta
 * que recibe cada método es la frase completa que encabeza el mensaje (por ejemplo
 * {@code "La capacidad total"}), de modo que la concordancia gramatical la resuelve quien llama.
 * </p>
 */
public final class ValidacionesNumericas {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-AR");
    private static final String PATRON_NUMERO = "#,##0.##";

    private ValidacionesNumericas() {
    }

    /**
     * Valida que el valor sea un número finito mayor a 0.
     *
     * @param valor    Valor a validar.
     * @param etiqueta Frase que encabeza el mensaje (por ejemplo {@code "La capacidad total"}).
     * @throws ReglaNegocioException Si el valor es nulo, NaN, infinito, cero o negativo.
     */
    public static void validarMayorACero(Double valor, String etiqueta) {
        if (!esFinito(valor) || valor <= 0) {
            throw new ReglaNegocioException(etiqueta + " debe ser un número mayor a 0");
        }
    }

    /**
     * Valida que el valor sea un número finito mayor o igual a cero.
     *
     * @param valor    Valor a validar.
     * @param etiqueta Frase que encabeza el mensaje.
     * @throws ReglaNegocioException Si el valor es nulo, NaN, infinito o negativo.
     */
    public static void validarMayorOIgualACero(Double valor, String etiqueta) {
        if (!esFinito(valor) || valor < 0) {
            throw new ReglaNegocioException(etiqueta + " debe ser un número mayor o igual a 0");
        }
    }

    /**
     * Valida que el valor no supere un máximo. Un valor nulo no se evalúa: la obligatoriedad la
     * resuelve otra validación.
     *
     * @param valor    Valor a validar.
     * @param maximo   Máximo permitido (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @param unidad   Unidad que acompaña al máximo en el mensaje (por ejemplo {@code "litros"}).
     * @throws ReglaNegocioException Si el valor es mayor al máximo.
     */
    public static void validarMaximo(Double valor, double maximo, String etiqueta, String unidad) {
        if (valor != null && valor > maximo) {
            throw mensajeMaximo(etiqueta, maximo, unidad);
        }
    }

    /**
     * Valida que el valor sea finito y mayor a 0, y luego que no supere el máximo.
     *
     * @param valor    Valor a validar.
     * @param maximo   Máximo permitido (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @param unidad   Unidad que acompaña al máximo en el mensaje.
     * @throws ReglaNegocioException Si el valor no es un número mayor a 0 o supera el máximo.
     */
    public static void validarMayorACeroConMaximo(Double valor, double maximo, String etiqueta, String unidad) {
        validarMayorACero(valor, etiqueta);
        validarMaximo(valor, maximo, etiqueta, unidad);
    }

    /**
     * Valida que el valor sea finito y mayor o igual a cero, y luego que no supere el máximo.
     *
     * @param valor    Valor a validar.
     * @param maximo   Máximo permitido (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @param unidad   Unidad que acompaña al máximo en el mensaje.
     * @throws ReglaNegocioException Si el valor no es un número mayor o igual a 0 o supera el máximo.
     */
    public static void validarMayorOIgualACeroConMaximo(Double valor, double maximo, String etiqueta, String unidad) {
        validarMayorOIgualACero(valor, etiqueta);
        validarMaximo(valor, maximo, etiqueta, unidad);
    }

    /**
     * Valida que el entero sea mayor a 0 y no supere el máximo.
     *
     * @param valor    Valor a validar.
     * @param maximo   Máximo permitido (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @param unidad   Unidad que acompaña al máximo en el mensaje (por ejemplo {@code "minutos"}).
     * @throws ReglaNegocioException Si el valor es nulo, cero, negativo o mayor al máximo.
     */
    public static void validarEnteroMayorACeroConMaximo(Integer valor, int maximo, String etiqueta, String unidad) {
        if (valor == null || valor <= 0) {
            throw new ReglaNegocioException(etiqueta + " debe ser un número entero mayor a 0");
        }
        validarMaximoEntero(valor, maximo, etiqueta, unidad);
    }

    /**
     * Valida que el entero sea mayor o igual a cero y no supere el máximo.
     *
     * @param valor    Valor a validar.
     * @param maximo   Máximo permitido (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @param unidad   Unidad que acompaña al máximo en el mensaje.
     * @throws ReglaNegocioException Si el valor es nulo, negativo o mayor al máximo.
     */
    public static void validarEnteroMayorOIgualACeroConMaximo(Integer valor, int maximo, String etiqueta, String unidad) {
        if (valor == null || valor < 0) {
            throw new ReglaNegocioException(etiqueta + " debe ser un número entero mayor o igual a 0");
        }
        validarMaximoEntero(valor, maximo, etiqueta, unidad);
    }

    /**
     * Valida que un texto no supere una longitud máxima. Un texto nulo se acepta: la obligatoriedad
     * la resuelve quien llama.
     *
     * @param valor    Texto a validar.
     * @param maximo   Cantidad máxima de caracteres (inclusive).
     * @param etiqueta Frase que encabeza el mensaje.
     * @throws ReglaNegocioException Si el texto tiene más caracteres que el máximo.
     */
    public static void validarLongitudMaxima(String valor, int maximo, String etiqueta) {
        if (valor != null && valor.length() > maximo) {
            throw new ReglaNegocioException(etiqueta + " no puede superar " + maximo + " caracteres");
        }
    }

    /**
     * Normaliza un texto: elimina los espacios de los extremos y reemplaza cada secuencia interna de
     * espacios en blanco (espacios, tabulaciones, saltos de línea) por un único espacio.
     *
     * @param valor Texto a normalizar.
     * @return El texto normalizado, o {@code null} si el valor es nulo.
     */
    public static String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static boolean esFinito(Double valor) {
        return valor != null && Double.isFinite(valor);
    }

    private static void validarMaximoEntero(int valor, int maximo, String etiqueta, String unidad) {
        if (valor > maximo) {
            throw mensajeMaximo(etiqueta, maximo, unidad);
        }
    }

    private static ReglaNegocioException mensajeMaximo(String etiqueta, double maximo, String unidad) {
        return new ReglaNegocioException(etiqueta + " no puede superar " + formatearNumero(maximo) + " " + unidad);
    }

    /**
     * Formatea un número con el estilo de la aplicación (separador de miles {@code .}, separador
     * decimal {@code ,} y hasta dos decimales), por ejemplo {@code 1.250,5}.
     *
     * @param numero Número a formatear.
     * @return El número formateado.
     */
    public static String formatearNumero(double numero) {
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(LOCALE_ES);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat(PATRON_NUMERO, simbolos).format(numero);
    }
}
