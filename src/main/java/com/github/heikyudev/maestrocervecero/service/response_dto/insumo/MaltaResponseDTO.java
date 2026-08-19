package com.github.heikyudev.maestrocervecero.service.response_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una malta.
 * <p>
 * Excluye intencionalmente cualquier atributo de estado o borrado lógico
 * (soft delete): la capa de servicio solo expone maltas activas.
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MaltaResponseDTO {

    /**
     * Identificador único de la malta.
     */
    private Long id;

    /**
     * Nombre de la malta.
     */
    private String nombre;

    /**
     * Unidad de medida de la malta (siempre {@code KILOGRAMO} por regla de negocio).
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Clasificación de la malta (BASE, CARAMELO, TOSTADA, ESPECIAL).
     */
    private TipoMalta tipo;

    /**
     * Rendimiento potencial en porcentaje (entre 0 y 100 inclusive).
     */
    private Integer rendimiento;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la malta.
     */
    private String createdBy;

    /**
     * Fecha de creación de la malta.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la malta por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la malta.
     */
    private LocalDateTime lastModifiedDate;
}
