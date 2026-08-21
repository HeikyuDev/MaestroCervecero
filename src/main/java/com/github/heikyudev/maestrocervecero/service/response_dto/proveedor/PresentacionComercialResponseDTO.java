package com.github.heikyudev.maestrocervecero.service.response_dto.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una presentación comercial.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PresentacionComercialResponseDTO {

    /**
     * Identificador único de la presentación comercial.
     */
    private Long id;

    /**
     * Nombre de la presentación comercial.
     */
    private String nombre;

    /**
     * Cantidad expresada en la unidad de medida indicada.
     */
    private Double cantidad;

    /**
     * Unidad de medida de la cantidad (GRAMO, KILOGRAMO, TONELADA).
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Estado lógico de la presentación comercial (activa o dada de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la presentación comercial.
     */
    private String createdBy;

    /**
     * Fecha de creación de la presentación comercial.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la presentación comercial por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la presentación comercial.
     */
    private LocalDateTime lastModifiedDate;
}
