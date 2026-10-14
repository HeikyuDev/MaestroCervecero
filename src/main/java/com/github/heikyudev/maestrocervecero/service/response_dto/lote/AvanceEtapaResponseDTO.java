package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta que representa el avance de una etapa de lote hacia el porcentaje mínimo de consumo exigido para
 * finalizarla.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AvanceEtapaResponseDTO {
    /**
     * Porcentaje mínimo de consumo configurado para avanzar de etapa. Con 0 no se controla el consumo.
     */
    private Double porcentajeMinimo;
    /**
     * Indica si todos los insumos requeridos de la etapa alcanzaron el mínimo (siempre {@code true} si la etapa no
     * requiere insumos).
     */
    private boolean cumpleMinimo;
    /**
     * Avance de cada insumo requerido de la etapa; vacía si la etapa no requiere insumos.
     */
    @Builder.Default
    private List<AvanceInsumoResponseDTO> insumos = List.of();
}
