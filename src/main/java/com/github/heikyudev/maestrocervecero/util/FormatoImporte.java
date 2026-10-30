package com.github.heikyudev.maestrocervecero.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato con el que las pantallas muestran un importe en pesos: separador de miles {@code .}, separador decimal
 * {@code ,}, al menos dos decimales y hasta cuatro (los que admite el costo unitario), por ejemplo {@code 1.250,50}
 * o {@code 0,1234}. Todo importe que se muestra lleva el símbolo delante ({@link #conSimbolo}): {@code $ 1.250,50}.
 * /js/orden-compra-form.js calcula los subtotales con el mismo criterio.
 */
public final class FormatoImporte {

    private static final String PATRON = "#,##0.00##";
    private static final String SIMBOLO = "$";

    private FormatoImporte() {
    }

    /**
     * El importe con el símbolo de pesos y un espacio delante (por ejemplo {@code $ 1.250,50}), o un guion si no tiene valor.
     */
    public static String conSimbolo(BigDecimal importe) {
        return importe == null ? "-" : SIMBOLO + " " + formatear(importe);
    }

    /**
     * El importe con el formato de la aplicación, sin símbolo, o un guion si no tiene valor.
     */
    public static String formatear(BigDecimal importe) {
        if (importe == null) {
            return "-";
        }
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat(PATRON, simbolos);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(importe);
    }
}
