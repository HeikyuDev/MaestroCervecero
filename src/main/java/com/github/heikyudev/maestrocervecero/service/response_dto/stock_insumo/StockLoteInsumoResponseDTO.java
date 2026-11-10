package com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con el stock de un lote de insumo: una fila de la tabla de lotes del detalle de un insumo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StockLoteInsumoResponseDTO {

    /**
     * Identificador único del lote de insumo.
     */
    private Long id;

    /**
     * Identificación del lote asignada por el proveedor.
     */
    private String identificacionLoteProveedor;

    /**
     * Fecha de vencimiento del lote.
     */
    private LocalDate fechaVencimiento;

    /**
     * Indica si el lote está vencido: su fecha de vencimiento es anterior a hoy (el que vence hoy es vigente).
     */
    private Boolean vencido;

    /**
     * Cantidades de stock del lote.
     */
    private CantidadesStockResponseDTO cantidades;
}
