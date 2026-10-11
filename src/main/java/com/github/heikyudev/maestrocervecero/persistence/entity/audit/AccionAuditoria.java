package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipos de acción que puede registrar la bitácora global de auditoría.
 * Incluye tanto acciones de negocio (CREAR/MODIFICAR/ELIMINAR/ANULAR/FINALIZAR)
 * como acciones de seguridad (LOGIN_EXITOSO/LOGOUT).
 */
@Getter
@RequiredArgsConstructor
public enum AccionAuditoria {
    CREAR("Alta"),
    MODIFICAR("Modificación"),
    ELIMINAR("Baja"),
    ANULAR("Anulación"),
    FINALIZAR("Finalización"),
    LOGIN_EXITOSO("Inicio de sesión"),
    LOGOUT("Cierre de sesión");

    /** Nombre legible de la acción, el que se muestra al usuario. */
    private final String etiqueta;
}
