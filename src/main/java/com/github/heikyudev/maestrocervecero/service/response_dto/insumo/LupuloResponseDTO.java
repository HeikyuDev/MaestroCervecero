package com.github.heikyudev.maestrocervecero.service.response_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un lúpulo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LupuloResponseDTO implements InsumoResponseDTO {

    /**
     * Identificador único del lúpulo.
     */
    private Long id;

    /**
     * Nombre del lúpulo.
     */
    private String nombre;

    /**
     * Unidad de medida del lúpulo (siempre {@code GRAMO} por regla de negocio).
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Formato de presentación del lúpulo (PELLET, FLOR).
     */
    private FormatoLupulo formato;

    /**
     * Porcentaje de alfa ácidos (AA%). Positivo, mayor a 0.
     */
    private Double aa;

    /**
     * Estado lógico del lúpulo (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la lupulo.
     */
    private String createdBy;

    /**
     * Fecha de creación del lupulo.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el lupulo por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del lupulo.
     */
    private LocalDateTime lastModifiedDate;
}
