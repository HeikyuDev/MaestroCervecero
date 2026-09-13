package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

/**
 * Clase de utilidades para validar reglas de negocio relacionadas con el equipamiento.
 */
public class MetodosEquipamiento {

    /**
     * Valida que la capacidad total y la capacidad util hayan sido informadas, y que la capacidad
     * util no sea mayor o igual a la capacidad total.
     *
     * @param capacidadTotal Capacidad total del Equipamiento.
     * @param capacidadUtil  Capacidad util del Equipamiento.
     * @throws ReglaNegocioException Si alguna de las dos es nula, o si la capacidad util es mayor o igual a la capacidad total.
     */
    public static void validarCapacidadUtil(Double capacidadTotal, Double capacidadUtil) {
        if (capacidadTotal == null || capacidadUtil == null) {
            throw new ReglaNegocioException("La capacidad total y la capacidad util son obligatorias.");
        }
        if (capacidadUtil >= capacidadTotal) {
            throw new ReglaNegocioException("La capacidad util no puede ser mayor a la capacidad total.");
        }
    }

}
