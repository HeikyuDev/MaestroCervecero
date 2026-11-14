package com.github.heikyudev.maestrocervecero.service.response_dto.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con el detalle de una entrada de la bitácora: su cabecera y la comparación campo a campo entre el
 * estado anterior y el posterior del registro afectado.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AuditLogDetalleResponseDTO {

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
     * Id del registro afectado, o {@code null} si la acción no tiene un registro asociado.
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

    /**
     * Indica si la entrada tiene un detalle de estados legible. Es {@code false} cuando la entrada no guardó estados
     * (sesión, entradas anteriores a esta función, operaciones que no modificaron el registro) o cuando el JSON
     * guardado es inválido.
     */
    private boolean tieneDetalle;

    /**
     * Comparación de todos los campos de los dos estados, en el orden en que se guardaron. Vacía si no hay detalle.
     */
    private List<CampoAuditoriaResponseDTO> campos;
}
