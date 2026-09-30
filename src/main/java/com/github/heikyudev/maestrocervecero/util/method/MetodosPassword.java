package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Reglas de una contraseña válida, centralizadas para que el {@code UsuarioFormDTO} (validación
 * temprana, con error bajo el campo) y el service (regla de negocio) no se desincronicen.
 * <p>
 * Una contraseña válida tiene entre {@value #LONGITUD_MINIMA} y {@value #LONGITUD_MAXIMA}
 * caracteres, ninguno de ellos un espacio en blanco (ni al inicio, ni en el medio, ni al final),
 * y no supera los {@value #LONGITUD_MAXIMA_BYTES} bytes: es el límite de BCrypt, que lanza una
 * excepción al intentar codificar una contraseña más larga.
 * </p>
 */
public class MetodosPassword {

    public static final int LONGITUD_MINIMA = 8;
    public static final int LONGITUD_MAXIMA = 72;
    public static final int LONGITUD_MAXIMA_BYTES = 72;

    /**
     * Patrón de {@code @Pattern} para el formulario: una contraseña vacía es válida (en la
     * modificación significa "conservar la actual"; el alta exige que se informe en el service).
     * El flag {@code (?U)} hace que {@code \S} reconozca también los espacios Unicode (ej. el
     * espacio de no separación).
     */
    public static final String PATRON_OPCIONAL = "(?U)(\\S{" + LONGITUD_MINIMA + "," + LONGITUD_MAXIMA + "})?";

    public static final String MENSAJE_INVALIDA = "La contraseña debe tener entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA
            + " caracteres y no puede contener espacios en blanco";

    private static final Pattern PATRON_VALIDA = Pattern.compile("\\S{" + LONGITUD_MINIMA + "," + LONGITUD_MAXIMA + "}", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Valida que una contraseña informada cumpla las reglas.
     *
     * @param password Contraseña en texto plano a validar.
     * @throws ReglaNegocioException Si es nula, no tiene entre 8 y 72 caracteres, contiene algún espacio en blanco o supera los 72 bytes.
     */
    public static void validarPassword(String password) {
        if (password == null || !PATRON_VALIDA.matcher(password).matches()) {
            throw new ReglaNegocioException(MENSAJE_INVALIDA);
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BYTES) {
            throw new ReglaNegocioException("La contraseña es demasiado larga: el máximo es de " + LONGITUD_MAXIMA_BYTES
                    + " bytes (los caracteres con tilde o especiales ocupan más de uno)");
        }
    }
}
