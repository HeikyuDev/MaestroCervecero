package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de operación del ciclo de vida de un {@link BarrilEntity}: limpieza, falla, fraccionamiento, mantenimiento,
 * despacho o devolución.
 * <p>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum TipoOperacionBarril {

    LIMPIEZA("Limpieza"),
    FALLA("Falla"),
    FRACCIONAMIENTO("Fraccionamiento"),
    MANTENIMIENTO("Mantenimiento"),
    DESPACHO("Despacho"),
    DEVOLUCION("Devolución");

    private final String etiqueta;

    /**
     * Resuelve el tipo de una operación a partir de su clase concreta.
     *
     * @param operacion La operación del ciclo de vida del barril.
     * @return El tipo de la operación, o {@code null} si la operación es nula o de una clase desconocida.
     */
    public static TipoOperacionBarril deOperacion(OperacionCicloVidaBarril operacion) {
        if (operacion instanceof LimpiezaBarrilEntity) {
            return LIMPIEZA;
        }
        if (operacion instanceof FallaBarrilEntity) {
            return FALLA;
        }
        if (operacion instanceof FraccionamientoBarrilEntity) {
            return FRACCIONAMIENTO;
        }
        if (operacion instanceof MantenimientoBarrilEntity) {
            return MANTENIMIENTO;
        }
        if (operacion instanceof DespachoBarrilEntity) {
            return DESPACHO;
        }
        if (operacion instanceof DevolucionBarrilEntity) {
            return DEVOLUCION;
        }
        return null;
    }
}
