package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

/**
 * Define los posibles estados operativos de un barril a lo largo de su ciclo de vida en la fábrica.
 */
public enum EstadoOperativoBarril {
    /**
     * El barril contiene cerveza y está listo para ser despachado o utilizado.
     */
    CON_CERVEZA,
    /**
     * El barril ha sido prestado y entregado a un cliente.
     */
    DESPACHADO,
    /**
     * El barril está vacío, limpio y listo para ser llenado con cerveza.
     */
    DISPONIBLE,
    /**
     * El barril ha sido devuelto y se encuentra en proceso de limpieza.
     */
    EN_LIMPIEZA,

    /***
     * El barril esta en proceso de mantenimiento, no puede ser utilizado hasta que se complete el mantenimiento.
     */
    EN_MANTENIMIENTO
}
