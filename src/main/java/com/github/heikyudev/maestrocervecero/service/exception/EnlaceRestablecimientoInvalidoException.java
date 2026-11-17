package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que indica que un enlace para restablecer la contraseña no sirve: no existe, ya se usó, venció
 * (o fue reemplazado por uno más nuevo) o su usuario ya no está activo.
 * <p>
 * Deriva de {@link BaseException} y es independiente de las reglas de la contraseña nueva: la capa web la distingue
 * para mostrar la pantalla que ofrece solicitar un enlace nuevo en lugar del formulario.
 * </p>
 */
public class EnlaceRestablecimientoInvalidoException extends BaseException {

    /**
     * Construye la excepción con el motivo por el que el enlace no sirve.
     *
     * @param mensaje Motivo por el que el enlace no sirve.
     */
    public EnlaceRestablecimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
