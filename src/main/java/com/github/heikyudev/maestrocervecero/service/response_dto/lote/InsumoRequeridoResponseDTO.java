package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
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
    /**
     * Saldo que todavía está reservado de este insumo para esta etapa (suma de las reservas de la etapa, que se
     * descuentan a medida que se consumen). Es {@code 0.0} si no tiene reservas.
     */
    private Double cantidadReservada;
    /**
     * Unidad de medida del insumo.
     */
    private UnidadDeMedida unidadDeMedida;
    /**
     * Porcentaje consumido sobre lo requerido, redondeado a 4 decimales y sin tope (puede superar 100). Es {@code 0.0}
     * si la cantidad requerida es 0.
     */
    private Double porcentajeConsumido;
}
