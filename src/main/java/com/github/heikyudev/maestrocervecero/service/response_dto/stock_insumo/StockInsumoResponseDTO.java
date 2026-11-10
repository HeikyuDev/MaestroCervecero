package com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con el stock de un insumo: una fila del listado de stock de insumos y el resumen de la
 * pantalla de detalle del insumo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StockInsumoResponseDTO {

    /**
     * Identificador único del insumo.
     */
    private Long id;

    /**
     * Nombre del insumo.
     */
    private String nombre;

    /**
     * Tipo concreto del insumo (Malta, Lúpulo o Levadura).
     */
    private TipoInsumo tipo;

    /**
     * Unidad de medida en la que se expresan las cantidades.
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Cantidades de stock del insumo: la suma de las de todos sus lotes de insumo.
     */
    private CantidadesStockResponseDTO cantidades;
}
