package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

/**
 * Clase de utilidades para validar reglas de negocio relacionadas con el equipamiento.
 */
public class MetodosEquipamiento {

    /**
     * Valida las capacidades de un equipamiento. Ambas deben haber sido informadas y la capacidad
     * util no puede ser mayor o igual a la capacidad total. Luego, ambas deben ser números finitos
     * mayores a 0, y la capacidad total no puede superar {@link LimitesTecnicos#CAPACIDAD_EQUIPO_MAXIMA_LITROS} litros
     * (la capacidad útil, al ser menor, queda dentro de ese máximo).
     *
     * @param capacidadTotal Capacidad total del Equipamiento.
     * @param capacidadUtil  Capacidad util del Equipamiento.
     * @throws ReglaNegocioException Si alguna es nula, si la capacidad útil es mayor o igual a la capacidad total, o si alguna no es un número mayor a 0 o supera el máximo.
     */
    public static void validarCapacidadUtil(Double capacidadTotal, Double capacidadUtil) {
        if (capacidadTotal == null) {
            throw new ReglaNegocioException("La capacidad total es obligatoria");
        }
        if (capacidadUtil == null) {
            throw new ReglaNegocioException("La capacidad útil es obligatoria");
        }
        if (capacidadUtil >= capacidadTotal) {
            throw new ReglaNegocioException("La capacidad útil debe ser menor a la capacidad total");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(capacidadTotal,
                LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS, "La capacidad total", "litros");
        ValidacionesNumericas.validarMayorACero(capacidadUtil, "La capacidad útil");
    }

    /**
     * Valida los usos máximos antes de mantenimiento: deben haber sido informados, ser mayores a 0 y
     * no superar {@link LimitesTecnicos#USOS_MAXIMOS_MANTENIMIENTO_MAXIMO}.
     *
     * @param usosMaximosAntesMantenimiento Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     * @throws ReglaNegocioException Si es nulo, menor o igual a 0 o supera el máximo.
     */
    public static void validarUsosMaximosAntesMantenimiento(Integer usosMaximosAntesMantenimiento) {
        if (usosMaximosAntesMantenimiento == null) {
            throw new ReglaNegocioException("Los usos máximos antes de mantenimiento son obligatorios");
        }
        if (usosMaximosAntesMantenimiento <= 0) {
            throw new ReglaNegocioException("Los usos máximos antes de mantenimiento deben ser mayores a 0");
        }
        if (usosMaximosAntesMantenimiento > LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO) {
            throw new ReglaNegocioException("Los usos máximos antes de mantenimiento no pueden superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO) + " usos");
        }
    }

}
