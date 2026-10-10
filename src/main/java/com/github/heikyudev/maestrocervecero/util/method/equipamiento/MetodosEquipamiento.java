package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

/**
 * Clase de utilidades para validar reglas de negocio relacionadas con el equipamiento.
 */
public class MetodosEquipamiento {

    /**
     * Valida las capacidades de un equipamiento: ambas deben ser números finitos mayores a 0 que no
     * superen {@link LimitesTecnicos#CAPACIDAD_EQUIPO_MAXIMA_LITROS} litros, y la capacidad útil debe
     * ser estrictamente menor a la capacidad total.
     *
     * @param capacidadTotal Capacidad total del Equipamiento.
     * @param capacidadUtil  Capacidad util del Equipamiento.
     * @throws ReglaNegocioException Si alguna es nula, no finita, menor o igual a 0 o supera el máximo, o si la capacidad útil es mayor o igual a la capacidad total.
     */
    public static void validarCapacidadUtil(Double capacidadTotal, Double capacidadUtil) {
        ValidacionesNumericas.validarMayorACeroConMaximo(capacidadTotal,
                LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS, "La capacidad total", "litros");
        ValidacionesNumericas.validarMayorACeroConMaximo(capacidadUtil,
                LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS, "La capacidad útil", "litros");
        if (capacidadUtil >= capacidadTotal) {
            throw new ReglaNegocioException("La capacidad útil debe ser menor a la capacidad total");
        }
    }

    /**
     * Valida los usos máximos antes de mantenimiento: deben ser un entero mayor a 0 que no supere
     * {@link LimitesTecnicos#USOS_MAXIMOS_MANTENIMIENTO_MAXIMO}.
     *
     * @param usosMaximosAntesMantenimiento Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     * @throws ReglaNegocioException Si es nulo, menor o igual a 0 o supera el máximo.
     */
    public static void validarUsosMaximosAntesMantenimiento(Integer usosMaximosAntesMantenimiento) {
        ValidacionesNumericas.validarEnteroMayorACeroConMaximo(usosMaximosAntesMantenimiento,
                LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO,
                "La cantidad de usos máximos antes de mantenimiento", "usos");
    }

    /**
     * Valida el identificador interno de un equipamiento: no puede superar
     * {@link LimitesTecnicos#LONGITUD_MAXIMA_NOMBRE} caracteres.
     *
     * @param identificadorInterno Identificador interno a validar.
     * @throws ReglaNegocioException Si supera la longitud máxima.
     */
    public static void validarLongitudIdentificadorInterno(String identificadorInterno) {
        ValidacionesNumericas.validarLongitudMaxima(identificadorInterno,
                LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El identificador interno");
    }

}
