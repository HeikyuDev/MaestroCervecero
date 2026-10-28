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

    /** Nombre del campo del formulario al que se refiere la regla, o {@code null} si no es de un campo en particular. */
    private final String campo;

    /**
     * Construye la excepción especificando la regla de negocio incumplida.
     *
     * @param mensaje Explicación clara de la restricción lógica o estado no permitido.
     */
    public ReglaNegocioException(String mensaje) {
        this(mensaje, null);
    }

    /**
     * Construye la excepción indicando además el campo del formulario al que se refiere la regla, para que la capa web
     * pueda marcarlo.
     *
     * @param mensaje Explicación clara de la restricción lógica o estado no permitido.
     * @param campo Nombre del campo del formulario afectado, o {@code null}.
     */
    public ReglaNegocioException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    /**
     * @return El nombre del campo del formulario al que se refiere la regla, o {@code null} si no indica uno.
     */
    public String getCampo() {
        return campo;
    }
}
