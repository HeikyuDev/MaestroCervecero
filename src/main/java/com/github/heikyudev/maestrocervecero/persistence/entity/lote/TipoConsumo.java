package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Distingue por qué camino se registró un {@link ConsumoInsumoEntity}: descontando de una reserva
 * de insumo puntual ya existente ({@code RESERVADO}), o directamente de la cantidad disponible de
 * un lote de insumo, sin pasar por ninguna reserva ({@code DIRECTO}).
 * <p>
 * Es un dato propio del consumo, no una relación a {@code ReservaInsumoEntity} (esa relación se
 * eliminó deliberadamente): existe únicamente para que la anulación sepa qué revertir — un
 * consumo {@code RESERVADO} devuelve stock tanto a la cantidad actual como a la reservada (y a la
 * reserva puntual de esa etapa), mientras que uno {@code DIRECTO} solo devuelve a la cantidad
 * actual.
 * </p>
 * <p>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum TipoConsumo {
    RESERVADO("Reservado"),
    DIRECTO("Directo");

    private final String etiqueta;
}
