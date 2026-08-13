package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que indica la inexistencia o inaccesibilidad de un recurso solicitado.
 * <p>
 * Deriva de {@link BaseException} y formaliza el resultado fallido al intentar consultar o manipular
 * registros de la base de datos mediante identificadores no válidos o extintos.
 * </p>
 */
public class RecursoNoEncontradoException extends BaseException {
    /**
     * Construye la excepción especificando el identificador o entidad no encontrada.
     *
     * @param mensaje Detalle del recurso inexistente en la consulta.
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}