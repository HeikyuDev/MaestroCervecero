package com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con la cabecera de la pantalla de detalle de un lote de insumo: el lote, el insumo al que
 * pertenece y sus cantidades de stock.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StockLoteInsumoDetalleResponseDTO {

    /**
     * Identificador único del lote de insumo.
     */
    private Long id;

    /**
     * Identificador del insumo al que pertenece el lote.
     */
    private Long idInsumo;

    /**
     * Nombre del insumo al que pertenece el lote.
     */
    private String nombreInsumo;

    /**
     * Unidad de medida en la que se expresan las cantidades.
     */
    private UnidadDeMedida unidadDeMedida;

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
