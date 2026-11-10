package com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con las cuatro cantidades de stock de un lote de insumo o de un insumo (suma de sus lotes).
 * <p>
 * Se cumple siempre que {@code disponible + reservada + vencida = total}. Un lote vencido no aporta a
 * disponible ni a reservada: todo su saldo, incluida la parte que estaba reservada, se informa como vencida.
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CantidadesStockResponseDTO {

    /**
     * Cantidad libre para producción: cantidad actual menos cantidad reservada de los lotes vigentes.
     */
    private Double disponible;

    /**
     * Cantidad reservada para lotes de producción, de los lotes vigentes.
     */
    private Double reservada;

    /**
     * Cantidad que conservan los lotes vencidos (vencen antes de hoy), incluida la parte que estaba reservada.
     */
    private Double vencida;

    /**
     * Cantidad física total: la suma de las tres anteriores.
     */
    private Double total;
}
