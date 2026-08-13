package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que señala la violación de una restricción de unicidad en el sistema.
 * <p>
 * Deriva de {@link BaseException} y representa intentos de persistencia o modificación de datos
 * que colisionan con registros preexistentes en la base de datos.
 * </p>
 */
public class RecursoDuplicadoException extends BaseException {
    /**
     * Construye la excepción especificando el detalle de la duplicidad encontrada.
     *
     * @param mensaje Explicación del recurso o atributo duplicado.
     */
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
