package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa cuánto de un insumo requiere una etapa de lote según la receta,
 * y cuánto de esa cantidad ya fue consumido.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class InsumoRequeridoResponseDTO {

    /**
     * Insumo requerido, en su DTO concreto correspondiente (Malta, Lúpulo o Levadura).
     */
    private InsumoResponseDTO insumo;

    /**
     * Cantidad total requerida de este insumo para esta etapa, según el escalado de la receta.
     * Es un valor estable: no lo afectan mermas ni ajustes posteriores sobre el stock reservado.
     */
    private Double cantidadRequerida;

    /**
     * Cantidad ya consumida (registrada, no anulada) de este insumo en esta etapa.
     */
    private Double cantidadConsumida;
}
