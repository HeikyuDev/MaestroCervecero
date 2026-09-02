package com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un motivo de ajuste.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MotivoAjusteResponseDTO {

    /**
     * Identificador único del motivo de ajuste.
     */
    private Long id;

    /**
     * Nombre del motivo de ajuste.
     */
    private String nombre;

    /**
     * Tipo de ajuste que representa este motivo: {@code INGRESO} (suma cantidad al lote de
     * insumo) o {@code EGRESO} (resta cantidad al lote de insumo).
     */
    private TipoAjuste tipoAjuste;

    /**
     * Estado lógico del motivo de ajuste (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el motivo de ajuste.
     */
    private String createdBy;

    /**
     * Fecha de creación del motivo de ajuste.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el motivo de ajuste por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del motivo de ajuste.
     */
    private LocalDateTime lastModifiedDate;
}
