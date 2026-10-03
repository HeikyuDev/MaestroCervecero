package com.github.heikyudev.maestrocervecero.util.formatter;

import org.springframework.format.Formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convierte la densidad (OG/FG) entre el texto de cuatro dígitos que escribe el usuario y el valor
 * decimal que maneja el sistema: {@code "1050"} equivale a {@code 1.050}.
 * <p>
 * El texto debe tener exactamente cuatro dígitos; cualquier otro formato (menos o más dígitos, coma,
 * punto, letras, espacios) se rechaza con una {@link ParseException}, que Spring MVC informa como un
 * error de conversión del campo. Un texto vacío no llega a este formatter: Spring lo enlaza como
 * {@code null} y lo informa la validación de obligatoriedad del campo.
 * </p>
 * <p>
 * El formatter solo ayuda a ingresar el dato; los rangos permitidos los valida el service.
 * </p>
 */
public class GravedadFormatter implements Formatter<Double> {

    private static final Pattern CUATRO_DIGITOS = Pattern.compile("\\d{4}");
    private static final int DECIMALES = 3;

    /**
     * Convierte el texto de cuatro dígitos en la densidad equivalente.
     *
     * @param texto  Texto ingresado por el usuario (por ejemplo {@code "1050"}).
     * @param locale Locale de la petición (no influye en la conversión).
     * @return La densidad con tres decimales (por ejemplo {@code 1.05}).
     * @throws ParseException Si el texto no tiene exactamente cuatro dígitos.
     */
    @Override
    public Double parse(String texto, Locale locale) throws ParseException {
        if (texto == null || !CUATRO_DIGITOS.matcher(texto).matches()) {
            throw new ParseException("La densidad debe tener exactamente cuatro dígitos", 0);
        }
        return new BigDecimal(texto).movePointLeft(DECIMALES).doubleValue();
    }

    /**
     * Convierte la densidad en el texto de cuatro dígitos con el que se ingresa (por ejemplo
     * {@code 1.05} se muestra como {@code "1050"}).
     *
     * @param gravedad Densidad a mostrar.
     * @param locale   Locale de la petición (no influye en la conversión).
     * @return El texto de cuatro dígitos, o una cadena vacía si la densidad es {@code null}.
     */
    @Override
    public String print(Double gravedad, Locale locale) {
        if (gravedad == null) {
            return "";
        }
        return BigDecimal.valueOf(gravedad).movePointRight(DECIMALES)
                .setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
