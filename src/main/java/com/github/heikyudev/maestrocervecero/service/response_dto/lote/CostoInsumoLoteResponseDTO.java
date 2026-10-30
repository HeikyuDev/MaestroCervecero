package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Costo de un insumo en el costo de producción de un lote: lo consumido (sin contar los consumos anulados), su costo promedio por
 * unidad y el subtotal.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CostoInsumoLoteResponseDTO {

    /**
     * Identificador del insumo.
     */
    private Long idInsumo;

    /**
     * Nombre del insumo.
     */
    private String nombre;

    /**
     * Unidad de medida del insumo.
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Cantidad total consumida del insumo en el lote, en su unidad de medida.
     */
    private Double cantidadConsumida;

    /**
     * Costo promedio por unidad: el subtotal dividido por la cantidad consumida (4 decimales).
     */
    private BigDecimal costoPromedioPorUnidad;

    /**
     * Suma de cantidad consumida por costo promedio ponderado vigente al consumir, de cada consumo (4 decimales).
     */
    private BigDecimal subtotal;
}
