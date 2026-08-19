package com.github.heikyudev.maestrocervecero.service.response_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar una levadura.
 * <p>
 * Excluye intencionalmente cualquier atributo de estado o borrado lógico
 * (soft delete): la capa de servicio solo expone levaduras activas.
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LevaduraResponseDTO {

    /**
     * Identificador único de la levadura.
     */
    private Long id;

    /**
     * Nombre de la levadura.
     */
    private String nombre;

    /**
     * Unidad de medida de la levadura (siempre {@code GRAMO} por regla de negocio).
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Tipo de la levadura (ALE, HIBRIDA, LAGER).
     */
    private TipoLevadura tipo;

    /**
     * Cantidad de células por gramo de la levadura. Positiva, mayor a 0.
     */
    private Double cantidadCelulasPorGramo;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la levadura.
     */
    private String createdBy;

    /**
     * Fecha de creación de la levadura.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la levadura por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la levadura.
     */
    private LocalDateTime lastModifiedDate;
}
