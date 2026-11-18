package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que indica que el enlace de un proveedor para responder una solicitud de cotización no sirve:
 * no existe, la solicitud ya cerró o fue cancelada, el plazo venció o el proveedor ya no está activo.
 * <p>
 * Deriva de {@link BaseException} y es independiente de las reglas de la respuesta: la capa web la distingue para
 * mostrar una pantalla que explica el motivo en lugar del formulario.
 * </p>
 */
public class EnlaceCotizacionInvalidoException extends BaseException {

    /**
     * Construye la excepción con el motivo por el que el enlace no sirve.
     *
     * @param mensaje Motivo por el que el enlace no sirve.
     */
    public EnlaceCotizacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
