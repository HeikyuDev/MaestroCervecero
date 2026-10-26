package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.util.regex.Pattern;

/**
 * Validaciones de formato de datos de texto (CUIT, teléfono, nombre de usuario y correo electrónico) reutilizables por
 * los services.
 * <p>
 * Validan el valor ya normalizado con {@link NormalizacionTexto}. Todas lanzan {@link ReglaNegocioException} con un
 * mensaje en español.
 * </p>
 */
public final class ValidacionesTexto {

    private static final int DIGITOS_CUIT = 11;

    private static final Pattern SOLO_DIGITOS = Pattern.compile("[0-9]+");
    private static final Pattern FORMATO_USERNAME = Pattern.compile("[A-Za-z0-9._-]+");

    /**
     * Mensaje único del formato inválido del correo electrónico, el mismo en el service y en el formulario.
     */
    public static final String MENSAJE_CORREO_INVALIDO = "El correo electrónico no tiene un formato válido";

    /**
     * Formato del correo electrónico: una parte local de letras ASCII, dígitos y {@code . _ % + -} (sin puntos al
     * inicio, al final ni consecutivos), un único {@code @} y un dominio de etiquetas de letras, dígitos y guiones
     * (sin guion al inicio ni al final de cada etiqueta) separadas por puntos, que termina en una etiqueta de al menos
     * dos letras. No admite espacios. No cubre todo el RFC 5322.
     */
    private static final String REGEX_CORREO = "[a-z0-9_%+-]+(?:\\.[a-z0-9_%+-]+)*"
            + "@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*\\.[a-z]{2,}";

    /**
     * Patrón de {@code @Pattern} para el correo electrónico de los formularios: el mismo formato que
     * {@link #validarCorreo(String)}, sin distinguir mayúsculas y con espacios en los extremos, porque el valor llega sin
     * normalizar. Un valor vacío o de solo espacios es válido para que la obligatoriedad la informe {@code @NotBlank}.
     */
    public static final String PATRON_CORREO_FORMULARIO = "(?i)[\\s\\p{Z}]*(?:" + REGEX_CORREO + ")?[\\s\\p{Z}]*";

    private static final Pattern FORMATO_CORREO = Pattern.compile(REGEX_CORREO, Pattern.CASE_INSENSITIVE);

    private ValidacionesTexto() {
    }

    /**
     * Valida un CUIT normalizado (ver {@link NormalizacionTexto#cuit(String)}): debe tener exactamente 11 dígitos.
     * No se verifica el prefijo ni el dígito verificador.
     *
     * @param cuit CUIT normalizado (solo dígitos).
     * @throws ReglaNegocioException Si el CUIT es nulo o no tiene 11 dígitos ("El CUIT debe tener 11 dígitos").
     */
    public static void validarCuit(String cuit) {
        if (cuit == null || cuit.length() != DIGITOS_CUIT || !SOLO_DIGITOS.matcher(cuit).matches()) {
            throw new ReglaNegocioException("El CUIT debe tener 11 dígitos");
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
     * Valida el formato de un correo electrónico normalizado (ver {@link NormalizacionTexto#correo(String)}): una parte
     * local, un único {@code @} y un dominio con al menos un punto, sin espacios ni caracteres inválidos y sin puntos
     * consecutivos ni al inicio o al final. Se aplica después de validar que el correo es obligatorio y no supera su
     * longitud máxima.
     *
     * @param correo Correo electrónico normalizado.
     * @throws ReglaNegocioException Si es nulo o no tiene el formato de un correo electrónico
     *                               ({@value #MENSAJE_CORREO_INVALIDO}).
     */
    public static void validarCorreo(String correo) {
        if (correo == null || !FORMATO_CORREO.matcher(correo).matches()) {
            throw new ReglaNegocioException(MENSAJE_CORREO_INVALIDO);
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
}
