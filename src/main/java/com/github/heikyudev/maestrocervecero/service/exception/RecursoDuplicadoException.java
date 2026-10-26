package com.github.heikyudev.maestrocervecero.service.exception;

/**
 * Excepción de dominio que señala la violación de una restricción de unicidad en el sistema.
 * <p>
 * Deriva de {@link BaseException} y representa intentos de persistencia o modificación de datos
 * que colisionan con registros preexistentes en la base de datos.
 * </p>
 * <p>
 * Opcionalmente indica el campo repetido (por ejemplo, {@code "cuit"}), para que el Controller marque ese campo del
 * formulario sin depender del texto del mensaje. Sin campo, el duplicado se informa como un error general.
 * </p>
 */
public class RecursoDuplicadoException extends BaseException {

    private final String campo;

    /**
     * Construye la excepción especificando el detalle de la duplicidad encontrada, sin indicar el campo repetido.
     *
     * @param mensaje Explicación del recurso o atributo duplicado.
     */
    public RecursoDuplicadoException(String mensaje) {
        this(mensaje, null);
    }

    /**
     * Construye la excepción especificando el detalle de la duplicidad encontrada y el campo repetido.
     *
     * @param mensaje Explicación del recurso o atributo duplicado.
     * @param campo   Nombre del campo repetido (por ejemplo, {@code "cuit"}), o {@code null} si no corresponde a uno.
     */
    public RecursoDuplicadoException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    /**
     * @return El nombre del campo repetido, o {@code null} si la excepción no lo indica.
     */
    public String getCampo() {
        return campo;
    }
}
