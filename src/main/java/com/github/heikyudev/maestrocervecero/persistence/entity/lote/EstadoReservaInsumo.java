package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una {@link ReservaInsumoEntity}. Una reserva nunca se borra: cambia de estado y conserva su historial.
 * <ul>
 *     <li>{@code ACTIVA}: todavía tiene saldo pendiente reservado.</li>
 *     <li>{@code CONSUMIDA}: se usó todo lo reservado (consumida igual a la cantidad original).</li>
 *     <li>{@code LIBERADA}: se cerró sin consumirse por completo, ya sea porque se devolvió el saldo al stock (total o
 *     parcialmente) o porque una merma la redujo hasta agotarla.</li>
 * </ul>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoReservaInsumo {
    ACTIVA("Activa"),
    CONSUMIDA("Consumida"),
    LIBERADA("Liberada");

    private final String etiqueta;
}
