package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que indica la inexistencia o inaccesibilidad de un recurso solicitado.
 * <p>
 * Deriva de {@link BaseException} y formaliza el resultado fallido al intentar consultar o manipular
 * registros de la base de datos mediante identificadores no válidos o extintos.
 * </p>
 */
public class RecursoNoEncontradoException extends BaseException {

    /** Nombre del campo del formulario cuyo recurso no existe, o {@code null} si no es de un campo en particular. */
    private final String campo;

    /**
     * Construye la excepción especificando el identificador o entidad no encontrada.
     *
     * @param mensaje Detalle del recurso inexistente en la consulta.
     */
    public RecursoNoEncontradoException(String mensaje) {
        this(mensaje, (String) null);
    }

    /**
     * Construye la excepción indicando además el campo del formulario cuyo recurso no existe.
     *
     * @param mensaje Detalle del recurso inexistente en la consulta.
     * @param campo Nombre del campo del formulario afectado, o {@code null}.
     */
    public RecursoNoEncontradoException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    /**
     * @return El nombre del campo del formulario cuyo recurso no existe, o {@code null} si no indica uno.
     */
    /**
     * Crea la excepción con el mensaje indicado y la excepción que la originó (por ejemplo, el rechazo de una
     * operación anidada al que se le agrega contexto en el mensaje).
     *
     * @param mensaje El mensaje para el usuario.
     * @param causa La excepción original.
     */
    public RecursoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.campo = null;
    }

    public String getCampo() {
        return campo;
    }
}