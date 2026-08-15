package com.github.heikyudev.maestrocervecero.service.response_dto;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
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
}
