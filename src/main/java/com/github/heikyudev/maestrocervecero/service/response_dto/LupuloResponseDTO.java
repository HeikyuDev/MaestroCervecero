package com.github.heikyudev.maestrocervecero.service.response_dto;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para representar un lúpulo.
 * <p>
 * Excluye intencionalmente cualquier atributo de estado o borrado lógico
 * (soft delete): la capa de servicio solo expone lúpulos activos.
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LupuloResponseDTO {

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
    private Integer aa;
}
