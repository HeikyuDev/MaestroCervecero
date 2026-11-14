package com.github.heikyudev.maestrocervecero.util.mapper.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.util.auditoria.ComparadorEstadosAuditoria;

/**
 * MapperAuditLog tiene la responsabilidad de mapear la entidad AuditLogEntity a AuditLogResponseDTO y a
 * AuditLogDetalleResponseDTO.
 */
public class MapperAuditLog {

    /**
     * Mapea una instancia de {@link AuditLogEntity} a {@link AuditLogResponseDTO}. No lee ni interpreta el JSON de
     * los estados: solo informa si la entrada guardó alguno.
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
                .tieneDetalle(hayTexto(auditLogEntity.getEstadoAnterior()) || hayTexto(auditLogEntity.getEstadoPosterior()))
                .build();
    }

    /**
     * Mapea una instancia de {@link AuditLogEntity} a {@link AuditLogDetalleResponseDTO}, con la cabecera de la entrada
     * y la comparación campo a campo de sus estados.
     *
     * @param auditLogEntity Entidad de la bitácora a convertir.
     * @param comparacion Comparación ya calculada de los estados anterior y posterior de la entrada.
     * @return Objeto DTO correspondiente o {@code null} si la entidad de entrada es nula.
     */
    public static AuditLogDetalleResponseDTO toDetalleDTO(AuditLogEntity auditLogEntity,
                                                          ComparadorEstadosAuditoria.Comparacion comparacion) {
        if (auditLogEntity == null) {
            return null;
        }

        return AuditLogDetalleResponseDTO.builder()
                .id(auditLogEntity.getId())
                .username(auditLogEntity.getUsername())
                .accion(auditLogEntity.getAccion())
                .conceptoAuditoria(auditLogEntity.getConceptoAuditoria())
                .entidadId(auditLogEntity.getEntidadId())
                .fechaHora(auditLogEntity.getFechaHora())
                .ipAddress(auditLogEntity.getIpAddress())
                .tieneDetalle(comparacion.tieneDetalle())
                .campos(comparacion.campos())
                .build();
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
