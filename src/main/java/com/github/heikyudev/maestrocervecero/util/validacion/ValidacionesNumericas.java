package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Validaciones numéricas y de longitud de texto reutilizables por los services.
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
            throw new ReglaNegocioException(etiqueta + " debe ser mayor a 0");
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
            throw new ReglaNegocioException(etiqueta + " debe ser mayor o igual a 0");
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
     * Valida que el valor no tenga más decimales que los permitidos. Un valor nulo, NaN o infinito no
     * se evalúa: su validez la resuelven otras validaciones. La comparación se hace con
     * {@link BigDecimal} sobre la representación decimal del valor, ignorando los ceros finales
     * (35,50 tiene un decimal).
     *
     * @param valor        Valor a validar.
     * @param maxDecimales Cantidad máxima de decimales (inclusive).
     * @param etiqueta     Frase que encabeza el mensaje (por ejemplo {@code "El IBU objetivo"}).
     * @throws ReglaNegocioException Si el valor tiene más decimales que el máximo.
     */
    public static void validarMaximoDecimales(Double valor, int maxDecimales, String etiqueta) {
        if (esFinito(valor) && BigDecimal.valueOf(valor).stripTrailingZeros().scale() > maxDecimales) {
            throw new ReglaNegocioException(etiqueta + " no puede tener más de " + maxDecimales
                    + (maxDecimales == 1 ? " decimal" : " decimales"));
        }
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
            throw new ReglaNegocioException(etiqueta + " debe ser mayor a 0");
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
            throw new ReglaNegocioException(etiqueta + " debe ser mayor o igual a 0");
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
        validarLongitudMaxima(valor, maximo, etiqueta, null);
    }

    /**
     * Igual que {@link #validarLongitudMaxima(String, int, String)}, indicando en la excepción el campo del formulario afectado.
     *
     * @param campo Nombre del campo del formulario, o {@code null}.
     */
    public static void validarLongitudMaxima(String valor, int maximo, String etiqueta, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new ReglaNegocioException(etiqueta + " no puede superar " + maximo + " caracteres", campo);
        }
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
