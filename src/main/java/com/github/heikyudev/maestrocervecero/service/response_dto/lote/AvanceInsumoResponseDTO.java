package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa el avance de un insumo requerido de una etapa hacia el porcentaje mínimo de consumo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AvanceInsumoResponseDTO {
    /**
     * Nombre del insumo.
     */
    private String nombreInsumo;
    /**
     * Unidad de medida del insumo.
     */
    private UnidadDeMedida unidadDeMedida;
    /**
     * Cantidad requerida del insumo en la etapa.
     */
    private Double requerida;
    /**
     * Cantidad ya consumida (registrada, no anulada) del insumo en la etapa.
     */
    private Double consumida;
    /**
     * Porcentaje consumido sobre lo requerido, redondeado a 4 decimales y sin tope.
     */
    private Double porcentaje;
    /**
     * Indica si el insumo alcanzó el porcentaje mínimo de consumo (siempre {@code true} si el mínimo es 0).
     */
    private boolean cumple;
}
