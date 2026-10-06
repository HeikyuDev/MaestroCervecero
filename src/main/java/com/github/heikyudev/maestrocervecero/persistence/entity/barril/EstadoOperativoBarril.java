package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Define los posibles estados operativos de un barril a lo largo de su ciclo de vida en la fábrica.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoOperativoBarril {
    /**
     * El barril contiene cerveza y está listo para ser despachado o utilizado.
     */
    CON_CERVEZA("Con cerveza"),
    /**
     * El barril ha sido prestado y entregado a un cliente.
     */
    DESPACHADO("Despachado"),
    /**
     * El barril está vacío, limpio y listo para ser llenado con cerveza.
     */
    DISPONIBLE("Disponible"),
    /**
     * El barril ha sido devuelto y se encuentra en proceso de limpieza.
     */
    EN_LIMPIEZA("En limpieza"),

    /***
     * El barril esta en proceso de mantenimiento, no puede ser utilizado hasta que se complete el mantenimiento.
     */
    EN_MANTENIMIENTO("En mantenimiento");

    /**
     * Nombre legible del estado, el que se muestra al usuario.
     */
    private final String etiqueta;
}
