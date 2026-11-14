package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de un evento de la bitácora de auditoría, en el panel del administrador.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoAuditoriaResponseDTO {

    /** Identificador único de la entrada de la bitácora; con él se arma el enlace a su detalle. */
    private Long id;

    /** Fecha y hora del evento. */
    private LocalDateTime fechaHora;

    /** Nombre de usuario que realizó la acción. */
    private String username;

    /** Acción registrada. */
    private AccionAuditoria accion;

    /** Módulo sobre el que se realizó la acción. */
    private ConceptoAuditoria concepto;

    /** {@code true} si la entrada guardó un estado anterior o posterior, de modo que tiene pantalla de detalle. */
    private boolean tieneDetalle;
}
