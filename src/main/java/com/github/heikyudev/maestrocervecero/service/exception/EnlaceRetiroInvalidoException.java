package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que indica que el enlace de un cliente para coordinar el retiro de sus barriles no sirve: no
 * existe, venció, fue reemplazado por un recordatorio más nuevo o el cliente ya no está activo.
 * <p>
 * Deriva de {@link BaseException} y es independiente de las reglas del retiro: la capa web la distingue para mostrar una
 * pantalla que explica el motivo en lugar del formulario.
 * </p>
 */
public class EnlaceRetiroInvalidoException extends BaseException {

    /**
     * Construye la excepción con el motivo por el que el enlace no sirve.
     *
     * @param mensaje Motivo por el que el enlace no sirve.
     */
    public EnlaceRetiroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
