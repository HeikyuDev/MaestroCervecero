package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que señala el incumplimiento de una invariante o restricción lógica del negocio.
 * <p>
 * Deriva de {@link BaseException} y representa situaciones donde la petición es sintácticamente válida
 * y los recursos involucrados existen, pero el estado actual de los datos o las reglas de la industria
 * impiden completar la transacción.
 * </p>
 */
public class ReglaNegocioException extends BaseException {
    /**
     * Construye la excepción especificando la regla de negocio incumplida.
     *
     * @param mensaje Explicación clara de la restricción lógica o estado no permitido.
     */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
