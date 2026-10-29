package com.github.heikyudev.maestrocervecero.util.validacion;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalización de los textos que ingresa el usuario, que se aplica antes de validar y de guardar.
 * <p>
 * Son funciones puras y tolerantes a {@code null}: un valor nulo devuelve {@code null}. Cada función
 * resuelve una clase de dato distinta:
 * </p>
 * <ul>
 *     <li>{@link #nombrePropio(String)}: nombres, razones sociales y demás textos libres. Conserva las
 *     mayúsculas y las tildes escritas.</li>
 *     <li>{@link #codigo(String)}: códigos e identificadores. Se guardan en mayúsculas.</li>
 *     <li>{@link #correo(String)}: correos electrónicos, en minúsculas.</li>
 *     <li>{@link #cuit(String)}: CUIT sin separadores (el proveedor lo guarda luego con el formato XX-XXXXXXXX-X).</li>
 *     <li>{@link #telefono(String)}: teléfonos, opcionales.</li>
 *     <li>{@link #username(String)}: nombres de usuario, en minúsculas.</li>
 * </ul>
 * <p>
 * La normalización no valida: un valor con caracteres no permitidos se devuelve con esos caracteres para que
 * lo rechace la validación correspondiente de {@link ValidacionesTexto}.
 * </p>
 */
public final class NormalizacionTexto {

    /** Espacios en blanco de cualquier tipo: ASCII, no separables (NBSP), de ancho fijo y separadores Unicode. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}\\u0085]+");

    /** Caracteres invisibles de ancho cero: no se ven, por lo que no se conservan. */
    private static final Pattern ANCHO_CERO = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");

    /** Separadores que se aceptan al tipear un CUIT: espacios, guiones, puntos y barras. */
    private static final Pattern SEPARADORES_CUIT = Pattern.compile("[\\s\\p{Z}\\u0085\\-./\\u2010-\\u2015\\u2212]");

    private NormalizacionTexto() {
    }

    /**
     * Normaliza un texto libre: lo pasa a forma Unicode NFC, elimina los caracteres invisibles de ancho cero,
     * recorta los espacios de los extremos y reemplaza cada secuencia interna de espacios en blanco
     * (espacios, tabulaciones, saltos de línea, espacios no separables) por un único espacio. Las
     * mayúsculas y las tildes se conservan.
     *
     * @param valor Texto a normalizar.
     * @return El texto normalizado (vacío si solo tenía espacios), o {@code null} si el valor es nulo.
     */
    public static String nombrePropio(String valor) {
        if (valor == null) {
            return null;
        }
        String sinAnchoCero = ANCHO_CERO.matcher(valor).replaceAll("");
        String colapsado = ESPACIOS.matcher(sinAnchoCero).replaceAll(" ").trim();
        return Normalizer.normalize(colapsado, Normalizer.Form.NFC);
    }

    /**
     * Normaliza un código o identificador: igual que {@link #nombrePropio(String)} y además lo pasa a
     * mayúsculas con {@link Locale#ROOT}, de modo que el resultado no dependa del idioma del servidor. La
     * puntuación se conserva.
     *
     * @param valor Código a normalizar.
     * @return El código normalizado, o {@code null} si el valor es nulo.
     */
    public static String codigo(String valor) {
        String normalizado = nombrePropio(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }

    /**
     * Normaliza un correo electrónico: recorta los espacios de los extremos y lo pasa a minúsculas con
     * {@link Locale#ROOT}.
     *
     * @param valor Correo a normalizar.
     * @return El correo normalizado, o {@code null} si el valor es nulo.
     */
    public static String correo(String valor) {
        return recortarYMinusculas(valor);
    }

    /**
     * Normaliza un CUIT: elimina los separadores habituales (espacios, guiones, puntos y barras), por lo que
     * {@code "20-12345678-6"} y {@code "20 12345678 6"} resultan en {@code "20123456786"}. Cualquier otro
     * carácter (por ejemplo una letra) se conserva para que {@link ValidacionesTexto#validarCuit(String)} rechace el
     * valor en lugar de aceptarlo en silencio.
     *
     * @param valor CUIT a normalizar.
     * @return El CUIT sin separadores, o {@code null} si el valor es nulo.
     */
    public static String cuit(String valor) {
        if (valor == null) {
            return null;
        }
        return SEPARADORES_CUIT.matcher(ANCHO_CERO.matcher(valor).replaceAll("")).replaceAll("");
    }

    /**
     * Normaliza un teléfono, que es un dato opcional: recorta los espacios de los extremos y devuelve
     * {@code null} si no queda contenido. Los caracteres que no son dígitos se conservan para que
     * {@link ValidacionesTexto#validarTelefono(String)} los rechace.
     *
     * @param valor Teléfono a normalizar.
     * @return El teléfono recortado, o {@code null} si el valor es nulo, vacío o solo tiene espacios.
     */
    public static String telefono(String valor) {
        String recortado = nombrePropio(valor);
        return (recortado == null || recortado.isEmpty()) ? null : recortado;
    }

    /**
     * Normaliza un nombre de usuario: recorta los espacios de los extremos y lo pasa a minúsculas con
     * {@link Locale#ROOT}. Los espacios internos se conservan para que
     * {@link ValidacionesTexto#validarUsername(String)} los rechace.
     *
     * @param valor Nombre de usuario a normalizar.
     * @return El nombre de usuario normalizado, o {@code null} si el valor es nulo.
     */
    public static String username(String valor) {
        return recortarYMinusculas(valor);
    }

    private static String recortarYMinusculas(String valor) {
        if (valor == null) {
            return null;
        }
        String sinAnchoCero = ANCHO_CERO.matcher(valor).replaceAll("");
        return ESPACIOS.matcher(sinAnchoCero).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }
}
