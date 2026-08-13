package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Clase base abstracta para todas las excepciones de dominio de la aplicación.
 * <p>
 * Hereda de {@link RuntimeException} (excepción no chequeada), permitiendo que los errores
 * de negocio fluyan limpiamente a través de las capas de la arquitectura hasta ser capturados
 * de forma centralizada por el controlador global de excepciones, sin necesidad de acoplar las
 * firmas de los métodos del servicio con cláusulas {@code throws}.
 * </p>
 */
public abstract class BaseException extends RuntimeException {
    /**
     * Construye la excepción base con un mensaje descriptivo de la falla.
     *
     * @param message Detalle explicativo de la condición de error.
     */
    protected BaseException(String message) {
        super(message);
    }
}
