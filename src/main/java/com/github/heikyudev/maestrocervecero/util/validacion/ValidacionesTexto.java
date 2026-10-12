package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validaciones de formato de datos de texto (CUIT, teléfono y nombre de usuario) reutilizables por los services.
 * <p>
 * Validan el valor ya normalizado con {@link NormalizacionTexto}. Todas lanzan {@link ReglaNegocioException} con un
 * mensaje en español.
 * </p>
 */
public final class ValidacionesTexto {

    private static final int DIGITOS_CUIT = 11;
    private static final int[] FACTORES_CUIT = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final Set<String> PREFIJOS_CUIT = Set.of("20", "23", "24", "27", "30", "33", "34");

    private static final Pattern SOLO_DIGITOS = Pattern.compile("[0-9]+");
    private static final Pattern FORMATO_USERNAME = Pattern.compile("[A-Za-z0-9._-]+");

    private ValidacionesTexto() {
    }

    /**
     * Valida un CUIT normalizado (ver {@link NormalizacionTexto#cuit(String)}): debe tener exactamente 11 dígitos,
     * un prefijo de tipo de persona válido (20, 23, 24, 27, 30, 33 o 34) y un dígito verificador correcto
     * según el algoritmo de módulo 11.
     *
     * @param cuit CUIT normalizado (solo dígitos).
     * @throws ReglaNegocioException Si el CUIT es nulo o no tiene 11 dígitos ("El CUIT debe tener 11 dígitos"), o si
     *                               el prefijo o el dígito verificador no son válidos ("El CUIT no es válido").
     */
    public static void validarCuit(String cuit) {
        if (cuit == null || cuit.length() != DIGITOS_CUIT || !SOLO_DIGITOS.matcher(cuit).matches()) {
            throw new ReglaNegocioException("El CUIT debe tener 11 dígitos");
        }
        if (!PREFIJOS_CUIT.contains(cuit.substring(0, 2)) || calcularDigitoVerificador(cuit) != cuit.charAt(10) - '0') {
            throw new ReglaNegocioException("El CUIT no es válido");
        }
    }

    /**
     * Valida un teléfono normalizado (ver {@link NormalizacionTexto#telefono(String)}). Es un dato opcional: un valor
     * nulo o vacío se acepta. Si se informa, debe tener solo dígitos y no superar
     * {@link LimitesTecnicos#LONGITUD_MAXIMA_TELEFONO} dígitos.
     *
     * @param telefono Teléfono normalizado, o {@code null} si no se informó.
     * @throws ReglaNegocioException Si contiene algún carácter que no es un dígito o supera el máximo de dígitos.
     */
    public static void validarTelefono(String telefono) {
        if (telefono == null || telefono.isEmpty()) {
            return;
        }
        if (!SOLO_DIGITOS.matcher(telefono).matches()) {
            throw new ReglaNegocioException("El teléfono solo puede contener dígitos");
        }
        if (telefono.length() > LimitesTecnicos.LONGITUD_MAXIMA_TELEFONO) {
            throw new ReglaNegocioException("El teléfono no puede superar " + LimitesTecnicos.LONGITUD_MAXIMA_TELEFONO + " dígitos");
        }
    }

    /**
     * Valida un nombre de usuario normalizado (ver {@link NormalizacionTexto#username(String)}): es obligatorio y solo
     * puede contener letras (sin tildes ni eñes), números, puntos, guiones y guiones bajos, sin espacios. Puede
     * ser, por ejemplo, un DNI.
     *
     * @param username Nombre de usuario normalizado.
     * @throws ReglaNegocioException Si es nulo o vacío, o si contiene algún carácter no permitido.
     */
    public static void validarUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new ReglaNegocioException("El nombre de usuario es obligatorio");
        }
        if (!FORMATO_USERNAME.matcher(username).matches()) {
            throw new ReglaNegocioException(
                    "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios");
        }
    }

    /**
     * Da formato de visualización a un CUIT, con guiones: {@code 20123456786} se muestra como
     * {@code 20-12345678-6}. Un valor que, sin separadores, no tiene 11 dígitos se devuelve sin cambios.
     *
     * @param cuit CUIT a formatear.
     * @return El CUIT con el formato {@code XX-XXXXXXXX-X}, el valor original si no tiene 11 dígitos, o {@code null}
     * si el valor es nulo.
     */
    public static String formatearCuit(String cuit) {
        String digitos = NormalizacionTexto.cuit(cuit);
        if (digitos == null || digitos.length() != DIGITOS_CUIT || !SOLO_DIGITOS.matcher(digitos).matches()) {
            return cuit;
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }

    /**
     * Calcula el dígito verificador esperado de un CUIT a partir de sus 10 primeros dígitos. Devuelve -1 si el
     * resultado del módulo 11 es 10, caso en el que no existe un dígito verificador válido para ese número.
     */
    private static int calcularDigitoVerificador(String cuit) {
        int suma = 0;
        for (int i = 0; i < FACTORES_CUIT.length; i++) {
            suma += (cuit.charAt(i) - '0') * FACTORES_CUIT[i];
        }
        int resto = suma % 11;
        if (resto == 0) {
            return 0;
        }
        int digito = 11 - resto;
        return digito == 10 ? -1 : digito;
    }
}
