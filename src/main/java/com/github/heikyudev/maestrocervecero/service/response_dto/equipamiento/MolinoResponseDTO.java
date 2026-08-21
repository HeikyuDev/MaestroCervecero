package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de un Molino.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MolinoResponseDTO {

    /**
     * Identificador único del molino.
     */
    private Long id;

    /**
     * Código de identificación interno del equipamiento en la fábrica.
     */
    private String identificadorInterno;

    /**
     * Características específicas u otra información relevante sobre el molino.
     */
    private String descripcion;

    /**
     * Estado operativo actual del molino.
     * - DISPONIBLE: Listo para ser utilizado en un nuevo lote.
     * - EN_LIMPIEZA: Requiere limpieza para volver a estar disponible.
     * - OCUPADO: Está siendo utilizado en un lote de producción.
     */
    private EstadoOperativo estadoOperativo;


    /**
     * Rendimiento del molino (Kilos Por Hora)
     */
    private Double rendimientoMolienda;

    /**
     * Estado lógico del molino (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el molino.
     */
    private String createdBy;

    /**
     * Fecha de creación del molino.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el molino por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del molino.
     */
    private LocalDateTime lastModifiedDate;
}
