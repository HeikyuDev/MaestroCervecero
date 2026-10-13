package com.github.heikyudev.maestrocervecero.util.validacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Redondeo de las cantidades de stock, reservas, consumos y envasados.
 * <p>
 * Los números de punto flotante no representan con exactitud la mayoría de los decimales (por
 * ejemplo, {@code 0.1 + 0.2} da {@code 0.30000000000000004}), por lo que sumar y restar cantidades
 * deja residuos que hacen fallar comparaciones como "la cantidad consumida no supera la disponible".
 * Para evitarlo, toda cantidad se redondea a {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales
 * tanto al escribirla como al compararla.
 * </p>
 */
public final class Redondeo {

    private Redondeo() {
    }

    /**
     * Redondea una cantidad a {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales, con el modo
     * {@link RoundingMode#HALF_UP}. Un valor no finito ({@code NaN} o infinito) se devuelve sin cambios.
     *
     * @param cantidad Cantidad a redondear.
     * @return La cantidad redondeada.
     */
    public static double redondear(double cantidad) {
        if (!Double.isFinite(cantidad)) {
            return cantidad;
        }
        return BigDecimal.valueOf(cantidad).setScale(LimitesTecnicos.ESCALA_CANTIDADES, RoundingMode.HALF_UP).doubleValue();
    }
}
