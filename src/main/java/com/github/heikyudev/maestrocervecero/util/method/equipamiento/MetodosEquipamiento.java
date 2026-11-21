package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
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
     * (la capacidad útil, al ser menor, queda dentro de ese máximo). La capacidad útil debe ser de al menos
     * {@link LimitesTecnicos#CAPACIDAD_UTIL_MINIMA_LITROS} litros, para que todo equipo activo pueda recibir un lote.
     *
     * @param capacidadTotal Capacidad total del Equipamiento.
     * @param capacidadUtil  Capacidad util del Equipamiento.
     * @throws ReglaNegocioException Si alguna es nula, si la capacidad útil es mayor o igual a la capacidad total, si alguna no es un número mayor a 0 o supera el máximo, o si la capacidad útil es menor al mínimo.
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
        if (capacidadUtil < LimitesTecnicos.CAPACIDAD_UTIL_MINIMA_LITROS) {
            throw new ReglaNegocioException("La capacidad útil debe ser de al menos "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CAPACIDAD_UTIL_MINIMA_LITROS) + " litros");
        }
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
            throw new ReglaNegocioException("Los usos antes de mantenimiento son obligatorios");
        }
        if (usosMaximosAntesMantenimiento <= 0) {
            throw new ReglaNegocioException("Los usos antes de mantenimiento deben ser mayores a 0");
        }
        if (usosMaximosAntesMantenimiento > LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO) {
            throw new ReglaNegocioException("Los usos antes de mantenimiento no pueden superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO) + " usos");
        }
    }

    /**
     * Valida que un equipamiento (Molino, Macerador, Olla de Hervor o Fermentador) se pueda dar de baja: la baja es
     * definitiva y solo la impide lo que está en curso. Rechaza un equipamiento asignado a alguna etapa de un lote
     * Pendiente (en la cola de su fermentador) o En ejecución, y uno que no está Disponible. El historial (lotes
     * finalizados o cancelados, limpiezas, fallas y mantenimientos) no impide la baja.
     * <p>
     * El llamador ya bloqueó el equipamiento para escritura con
     * {@code IEquipamientoRepository#buscarPorIdParaOperacionCicloVida}. Ese bloqueo es sobre la misma fila que toman
     * las búsquedas con bloqueo de los equipos de un lote (al registrarlo, iniciarlo, avanzar sus etapas, finalizarlo o
     * cancelarlo) y el registro y la anulación de sus operaciones del ciclo de vida. Por eso la baja se serializa con el
     * registro de un lote que usa el equipamiento: o el lote se registra antes y esta validación lo encuentra, o la baja
     * se confirma antes y la búsqueda con bloqueo del registro no encuentra el equipamiento activo.
     * </p>
     *
     * @param equipamiento El equipamiento a dar de baja, ya bloqueado.
     * @param etapaLoteRepository El repositorio de etapas de lote, para consultar los lotes vigentes.
     * @param sujeto El equipamiento con su artículo, para los mensajes ("el fermentador", "la olla de hervor").
     * @throws ReglaNegocioException Si el equipamiento está asignado a un lote pendiente o en ejecución, o si no está disponible.
     */
    public static void validarBaja(EquipamientoEntity equipamiento, IEtapaLoteRepository etapaLoteRepository, String sujeto) {
        if (etapaLoteRepository.existsEtapaEnLoteVigente(equipamiento.getId())) {
            throw new ReglaNegocioException("No se puede dar de baja " + sujeto + " porque tiene lotes pendientes o en ejecución");
        }
        if (equipamiento.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja " + sujeto + " porque no se encuentra disponible");
        }
    }
}
