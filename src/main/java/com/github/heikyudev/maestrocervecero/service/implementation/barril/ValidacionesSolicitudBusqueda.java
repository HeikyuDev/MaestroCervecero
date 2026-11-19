package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;

import java.time.LocalDateTime;

/**
 * Validaciones de los datos de una solicitud de búsqueda de barril, comunes a su registro directo y a la confirmación del
 * retiro desde el enlace del recordatorio que recibe el cliente. Cada uno nombra la fecha como la ve su usuario ("La fecha
 * de búsqueda", "La fecha y hora del retiro").
 */
public final class ValidacionesSolicitudBusqueda {

    private ValidacionesSolicitudBusqueda() {
    }

    /**
     * Valida que la fecha de búsqueda esté informada, no sea anterior al momento indicado y, si se indica un máximo de
     * días, no lo supere.
     *
     * @param fechaBusqueda Fecha y hora propuesta.
     * @param ahora Momento de referencia.
     * @param diasMaximos Días hacia adelante desde {@code ahora} que no puede superar, o {@code null} para no acotarla.
     * @param nombreDato Cómo se nombra la fecha en los mensajes (ej. "La fecha de búsqueda").
     * @param campo Campo del formulario al que se asocia el rechazo, o {@code null}.
     * @throws ReglaNegocioException Si la fecha falta, es anterior a {@code ahora} o supera el máximo de días.
     */
    public static void validarFechaBusqueda(LocalDateTime fechaBusqueda, LocalDateTime ahora, Integer diasMaximos,
                                            String nombreDato, String campo) {
        if (fechaBusqueda == null) {
            throw new ReglaNegocioException(nombreDato + " es obligatoria", campo);
        }
        if (fechaBusqueda.isBefore(ahora)) {
            throw new ReglaNegocioException(nombreDato + " no puede ser anterior a la fecha y hora actual", campo);
        }
        if (diasMaximos != null && fechaBusqueda.isAfter(ahora.plusDays(diasMaximos))) {
            throw new ReglaNegocioException(nombreDato + " no puede superar " + diasMaximos + (diasMaximos == 1 ? " día" : " días")
                    + " desde la fecha y hora actual", campo);
        }
    }

    /**
     * Normaliza las observaciones (opcionales: en blanco equivalen a no informadas) y valida su longitud.
     *
     * @param observaciones Observaciones tal como se recibieron.
     * @param campo Campo del formulario al que se asocia el rechazo, o {@code null}.
     * @return Las observaciones normalizadas, o {@code null} si no se informaron.
     * @throws ReglaNegocioException Si superan {@code LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO} caracteres.
     */
    public static String normalizarObservaciones(String observaciones, String campo) {
        String normalizadas = NormalizacionTexto.nombrePropio(observaciones);
        if (normalizadas != null && normalizadas.isEmpty()) {
            normalizadas = null;
        }
        if (normalizadas != null && normalizadas.length() > LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO) {
            throw new ReglaNegocioException("Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres", campo);
        }
        return normalizadas;
    }
}
