package com.github.heikyudev.maestrocervecero.util;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato único de las cantidades y los números de la aplicación, tanto en las tablas como en los mensajes: separador
 * de miles {@code .}, separador decimal {@code ,}, hasta cuatro decimales (la escala de las cantidades) y sin ceros
 * sobrantes, por ejemplo {@code 2.000}, {@code 11,655} o {@code 1.847,3685}.
 */
public final class FormatoCantidad {

    private static final String PATRON = "#,##0." + "#".repeat(LimitesTecnicos.ESCALA_CANTIDADES);

    private FormatoCantidad() {
    }

    /**
     * El número con el formato de la aplicación, o un guion si no tiene valor.
     */
    public static String formatear(Number numero) {
        if (numero == null) {
            return "-";
        }
        BigDecimal valor = numero instanceof BigDecimal decimal ? decimal : new BigDecimal(numero.toString());
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat(PATRON, simbolos);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }

    /**
     * El número con el formato de la aplicación seguido de su unidad (por ejemplo {@code 4,5 kg}).
     */
    public static String conUnidad(Number numero, String unidad) {
        return formatear(numero) + " " + unidad;
    }
}
