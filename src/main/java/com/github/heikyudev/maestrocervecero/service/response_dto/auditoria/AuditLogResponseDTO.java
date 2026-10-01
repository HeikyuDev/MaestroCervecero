package com.github.heikyudev.maestrocervecero.service.response_dto.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una entrada de la bitácora global de auditoría.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AuditLogResponseDTO {

    /**
     * Identificador único de la entrada de la bitácora.
     */
    private Long id;

    /**
     * Username del usuario que ejecutó la acción.
     */
    private String username;

    /**
     * Tipo de acción ejecutada (de negocio o de seguridad).
     */
    private AccionAuditoria accion;

    /**
     * Concepto (módulo) sobre el que se ejecutó la acción.
     */
    private ConceptoAuditoria conceptoAuditoria;

    /**
     * Id del registro afectado, o {@code null} si la acción no tiene un registro asociado
     * (por ejemplo, el inicio o cierre de sesión).
     */
    private String entidadId;

    /**
     * Fecha y hora en que se ejecutó la acción.
     */
    private LocalDateTime fechaHora;

    /**
     * IP remota desde la que se ejecutó la acción, o {@code null} si no había una request HTTP asociada.
     */
    private String ipAddress;
}
