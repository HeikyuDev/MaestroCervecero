package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

/**
 * Tipos de acción que puede registrar la bitácora global de auditoría.
 * Incluye tanto acciones de negocio (CREAR/MODIFICAR/ELIMINAR/ANULAR)
 * como acciones de seguridad (LOGIN_EXITOSO/LOGOUT).
 */
public enum AccionAuditoria {
    CREAR,
    MODIFICAR,
    ELIMINAR,
    ANULAR,
    LOGIN_EXITOSO,
    LOGOUT
}
