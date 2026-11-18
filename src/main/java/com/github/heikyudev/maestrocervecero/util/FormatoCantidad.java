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

    /** Decimales de un volumen calculado en un aviso (por ejemplo, la mezcla de agua y grano de un macerador). */
    private static final int DECIMALES_VOLUMEN = 2;

    private FormatoCantidad() {
    }

    /**
     * El número con el formato de la aplicación, o un guion si no tiene valor.
     */
    public static String formatear(Number numero) {
        return formatear(numero, PATRON);
    }

    /**
     * Un volumen en litros con el formato de la aplicación pero con hasta dos decimales (por ejemplo {@code 628,27}),
     * para los volúmenes calculados que se informan en un aviso; un guion si no tiene valor.
     */
    public static String formatearVolumen(Number numero) {
        return formatear(numero, "#,##0." + "#".repeat(DECIMALES_VOLUMEN));
    }

    private static String formatear(Number numero, String patron) {
        if (numero == null) {
            return "-";
        }
        BigDecimal valor = numero instanceof BigDecimal decimal ? decimal : new BigDecimal(numero.toString());
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat(patron, simbolos);
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
