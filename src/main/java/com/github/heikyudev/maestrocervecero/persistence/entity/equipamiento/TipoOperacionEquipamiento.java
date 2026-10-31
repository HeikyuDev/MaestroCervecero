package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de operación del ciclo de vida de un {@link EquipamientoEntity}: limpieza, falla o mantenimiento.
 * <p>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum TipoOperacionEquipamiento {
    LIMPIEZA("Limpieza"),
    FALLA("Falla"),
    MANTENIMIENTO("Mantenimiento");

    private final String etiqueta;

    /**
     * Resuelve el tipo de una operación a partir de su clase concreta.
     *
     * @param operacion La operación del ciclo de vida del equipamiento.
     * @return El tipo de la operación, o {@code null} si la operación es nula o de una clase desconocida.
     */
    public static TipoOperacionEquipamiento deOperacion(OperacionCicloVidaEquipamiento operacion) {
        if (operacion instanceof LimpiezaEquipamientoEntity) {
            return LIMPIEZA;
        }
        if (operacion instanceof FallaEquipamientoEntity) {
            return FALLA;
        }
        if (operacion instanceof MantenimientoEquipamientoEntity) {
            return MANTENIMIENTO;
        }
        return null;
    }
}
