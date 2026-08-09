package com.github.heikyudev.maestrocervecero.service;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;

/**
 * Contrato del servicio que persiste entradas en la bitácora global de auditoría.
 * <p>
 * Recibe la entidad ya armada (en vez de una lista de parámetros sueltos) porque
 * hay más de un origen que la construye con datos distintos: {@code AuditAspect}
 * arma entradas de negocio (con entidad/id afectados) y {@code SecurityAuditListener}
 * arma entradas de login/logout (sin entidad de negocio asociada).
 */
public interface IAuditLogService {

    /**
     * Persiste una entrada de la bitácora global de forma asíncrona.
     *
     * @param auditLogEntity entrada ya armada por el llamador (username, acción, IP, etc.)
     */
    void guardar(AuditLogEntity auditLogEntity);
}
