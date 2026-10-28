package com.github.heikyudev.maestrocervecero.util.method;

/**
 * Arma los identificadores internos que genera el sistema a partir del valor de una secuencia de la base de datos:
 * el prefijo del tipo, un guion y el número con dos dígitos como mínimo (por ejemplo {@code MOL-01}, {@code MOL-99}
 * y {@code MOL-100}).
 * <p>
 * Es el único lugar donde se define el formato de estos identificadores.
 * </p>
 */
public final class GeneradorIdentificador {

    public static final String PREFIJO_MOLINO = "MOL";
    public static final String PREFIJO_MACERADOR = "MAC";
    public static final String PREFIJO_OLLA_HERVOR = "OLL";
    public static final String PREFIJO_FERMENTADOR = "FER";
    public static final String PREFIJO_BARRIL = "BRL";

    private static final int DIGITOS_MINIMOS = 2;

    private GeneradorIdentificador() {
    }

    /**
     * Arma el identificador con el prefijo y el número indicados.
     *
     * @param prefijo Prefijo del tipo (por ejemplo {@link #PREFIJO_MOLINO}).
     * @param numero  Valor de la secuencia, mayor a 0.
     * @return El identificador, con el número completado con ceros a la izquierda hasta tener dos dígitos.
     * @throws IllegalArgumentException Si el prefijo está vacío o el número no es mayor a 0.
     */
    public static String generar(String prefijo, long numero) {
        if (prefijo == null || prefijo.isBlank()) {
            throw new IllegalArgumentException("El prefijo del identificador es obligatorio");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("El número del identificador debe ser mayor a 0");
        }
        return prefijo + "-" + String.format("%0" + DIGITOS_MINIMOS + "d", numero);
    }
}
