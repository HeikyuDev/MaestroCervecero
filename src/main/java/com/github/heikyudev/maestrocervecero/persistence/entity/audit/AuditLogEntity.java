package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bitácora global de transacciones del sistema: un registro plano e independiente
 * de cada acción de negocio relevante (crear/editar/eliminar sobre cualquier entidad).
 * <p>
 * A propósito NO extiende de {@link AuditableEntity}: esta tabla ES el historial en sí
 * mismo, no un registro de negocio que necesite (a su vez) ser auditado a nivel de fila.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username del usuario que ejecutó la acción (extraído de Spring Security).
    @Column(nullable = false)
    private String username;

    // Tipo de acción ejecutada (de negocio o de seguridad, ver AccionAuditoria).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionAuditoria accion;

    // Nombre de la entidad/tabla afectada (ej: "UsuarioEntity"). Nullable porque
    // las acciones de seguridad (LOGIN_EXITOSO/LOGOUT) no afectan ninguna entidad de negocio.
    @Column(name = "entidad_afectada")
    private String entidadAfectada;

    // Id del registro afectado. String para no atarnos al tipo de PK de cada entidad (Long, UUID, etc).
    @Column(name = "entidad_id")
    private String entidadId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Detalle libre de la operación (puede ser texto plano o un JSON serializado).
    @Lob
    @Column(name = "detalles")
    private String detalles;

    // IP remota desde la que se ejecutó la acción. Nullable: si el hilo que audita
    // no tiene una request HTTP asociada, no hay IP que capturar (ver RequestUtils).
    @Column(name = "ip_address")
    private String ipAddress;
}
