package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

/**
 * Clase de utilidades para validar reglas de negocio relacionadas con el equipamiento.
 */
public class MetodosEquipamiento {

    /**
     * Valida que la capacidad util no sea mayor o igual a la capacidad total.
     *
     * @param capacidadTotal Capacidad total del Equipamiento.
     * @param capacidadUtil  Capacidad util del Equipamiento.
     * @throws ReglaNegocioException Si la capacidad util es mayor o igual a la capacidad total.
     */
    public static void validarCapacidadUtil(Double capacidadTotal, Double capacidadUtil) {
        if (capacidadUtil >= capacidadTotal) {
            throw new ReglaNegocioException("La capacidad util no puede ser mayor a la capacidad total.");
        }
    }

}
