package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Criterio con el que la generación automática de planes de órdenes de compra ordena las ofertas de cada insumo. La
 * comparación es siempre por unidad de medida del insumo (precio por kilogramo o por gramo, y cantidad disponible en
 * esa unidad). Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 * <ul>
 *     <li>{@code MENOR_PRECIO}: primero la oferta más barata; ante igual precio, la de mayor cantidad.</li>
 *     <li>{@code MAYOR_CANTIDAD}: primero la oferta de mayor cantidad disponible; ante igual cantidad, la más barata.</li>
 *     <li>{@code PONDERADO}: puntaje que combina precio y cantidad según el peso del precio de la configuración.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum CriterioPlanCompra {
    MENOR_PRECIO("Menor precio"),
    MAYOR_CANTIDAD("Mayor cantidad disponible"),
    PONDERADO("Ponderado entre precio y cantidad");

    private final String etiqueta;
}
