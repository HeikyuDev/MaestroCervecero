package com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un costo directo adicional.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CostoDirectoAdicionalResponseDTO {

    /**
     * Identificador único del costo directo adicional.
     */
    private Long id;

    /**
     * Nombre del costo directo adicional.
     */
    private String nombre;

    /**
     * Costo por litro producido.
     */
    private BigDecimal costoPorLitro;

    /**
     * Estado lógico del costo directo adicional (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el costo directo adicional.
     */
    private String createdBy;

    /**
     * Fecha de creación del costo directo adicional.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el costo directo adicional por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del costo directo adicional.
     */
    private LocalDateTime lastModifiedDate;
}
