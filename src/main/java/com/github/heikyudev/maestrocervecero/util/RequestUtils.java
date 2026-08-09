package com.github.heikyudev.maestrocervecero.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utilidades para leer datos de la request HTTP actual desde código que no tiene
 * el {@code HttpServletRequest} inyectado (ej: un @Aspect o un listener de eventos).
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * Devuelve la IP remota del cliente de la request actual, o {@code null} si el
     * hilo actual no tiene una request web asociada (ej: un job batch, el arranque
     * de la app, o un hilo @Async que ya perdió el contexto de la request original).
     */
    public static String obtenerIpCliente() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest().getRemoteAddr();
        } catch (IllegalStateException ex) {
            // RequestContextHolder lanza esta excepción cuando no hay ninguna request
            // ligada al hilo actual: no es un error, simplemente no hay IP que capturar.
            return null;
        }
    }
}
