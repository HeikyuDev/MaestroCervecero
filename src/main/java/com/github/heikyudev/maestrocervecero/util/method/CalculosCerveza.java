package com.github.heikyudev.maestrocervecero.util.method;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Clase de utilidades con los cálculos estimados a partir de los objetivos de una receta.
 * <p>
 * Es la única definición de esos cálculos: la usan el mapper de recetas, el detalle del lote y el formulario de
 * receta para mostrarlos. Se hacen con {@link BigDecimal} construido desde el valor decimal tal como está guardado
 * (por ejemplo {@code 1.05}), para que no aparezcan errores de coma flotante como {@code 5.249999}.
 * </p>
 */
public final class CalculosCerveza {

    /**
     * Factor de la fórmula estándar del alcohol por volumen: ABV (%) = (OG - FG) x 131,25.
     */
    private static final BigDecimal FACTOR_ABV = new BigDecimal("131.25");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DECIMALES = 1;

    private CalculosCerveza() {
    }

    /**
     * Calcula el alcohol por volumen estimado a partir de la densidad original y final objetivo de una receta.
     *
     * @param og Densidad original (por ejemplo {@code 1.05}).
     * @param fg Densidad final (por ejemplo {@code 1.01}).
     * @return El ABV en porcentaje con un decimal (redondeo {@code HALF_UP}, por ejemplo {@code 5.3}), o
     * {@code null} si alguna densidad es {@code null} o si la OG no es mayor que la FG.
     */
    public static Double calcularAbv(Double og, Double fg) {
        if (og == null || fg == null || og <= fg) {
            return null;
        }
        return BigDecimal.valueOf(og).subtract(BigDecimal.valueOf(fg))
                .multiply(FACTOR_ABV)
                .setScale(DECIMALES, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Calcula los litros de alcohol estimados de un volumen: {@code volumen x ABV / 100}.
     *
     * @param abv            Alcohol por volumen en porcentaje.
     * @param volumenLitros  Volumen en litros.
     * @return Los litros de alcohol con un decimal (redondeo {@code HALF_UP}), o {@code null} si alguno de los
     * datos es {@code null}.
     */
    public static Double calcularAlcoholLitros(Double abv, Double volumenLitros) {
        if (abv == null || volumenLitros == null) {
            return null;
        }
        return BigDecimal.valueOf(volumenLitros).multiply(BigDecimal.valueOf(abv))
                .divide(CIEN, DECIMALES, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
