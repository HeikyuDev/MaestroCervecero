package com.github.heikyudev.maestrocervecero.persistence.enums;

/**
 * Situación de stock de un insumo, usada para filtrar el listado de stock de insumos.
 * <p>
 * Las situaciones no son excluyentes entre sí: un insumo con un lote vigente con saldo y otro vencido con saldo es
 * {@link #CON_STOCK_DISPONIBLE} y {@link #CON_VENCIDOS} a la vez.
 * </p>
 */
public enum SituacionStockInsumo {

    /**
     * El insumo tiene cantidad disponible mayor a 0 (suma de los lotes vigentes, sin contar lo reservado).
     */
    CON_STOCK_DISPONIBLE,

    /**
     * El insumo no tiene cantidad disponible (igual a 0), aunque pueda tener cantidad reservada o vencida.
     */
    SIN_STOCK_DISPONIBLE,

    /**
     * El insumo tiene cantidad vencida mayor a 0 (algún lote vencido conserva saldo).
     */
    CON_VENCIDOS
}
