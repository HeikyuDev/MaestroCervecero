package com.github.heikyudev.maestrocervecero.util.mapper.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;

/**
 * MapperAuditLog tiene la responsabilidad de mapear la entidad AuditLogEntity a AuditLogResponseDTO.
 */
public class MapperAuditLog {

    /**
     * Mapea una instancia de {@link AuditLogEntity} a {@link AuditLogResponseDTO}.
     *
     * @param auditLogEntity Entidad de la bitácora a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static AuditLogResponseDTO toDTO(AuditLogEntity auditLogEntity) {
        if (auditLogEntity == null) {
            return null;
        }

        return AuditLogResponseDTO.builder()
                .id(auditLogEntity.getId())
                .username(auditLogEntity.getUsername())
                .accion(auditLogEntity.getAccion())
                .conceptoAuditoria(auditLogEntity.getConceptoAuditoria())
                .entidadId(auditLogEntity.getEntidadId())
                .fechaHora(auditLogEntity.getFechaHora())
                .ipAddress(auditLogEntity.getIpAddress())
                .build();
    }
}
