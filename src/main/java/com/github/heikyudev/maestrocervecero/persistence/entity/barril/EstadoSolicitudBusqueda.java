package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una solicitud de búsqueda de barril: {@code PENDIENTE} mientras el barril sigue en poder del cliente;
 * {@code BUSCADA} cuando se registra la devolución del barril (anular esa devolución la vuelve a dejar pendiente); y
 * {@code CANCELADA} cuando se anula el despacho sobre el que se hizo (estado final: el barril nunca estuvo afuera, no hay
 * nada que buscar). Cada valor trae su nombre legible ({@code etiqueta}).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoSolicitudBusqueda {
    PENDIENTE("Pendiente"),
    BUSCADA("Buscada"),
    CANCELADA("Cancelada");

    private final String etiqueta;
}
