package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado de una {@code EtapaLoteEntity} dentro del ciclo de vida de su lote.
 * <p>
 * {@code CANCELADA} se aplica a una etapa que todavía estaba {@code PENDIENTE} o {@code EN_CURSO}
 * cuando se canceló el lote entero: en ese caso, {@code fechaInicio} distingue si el equipamiento
 * llegó a usarse de verdad (no nulo, estaba {@code EN_CURSO}) o nunca se tocó (nulo, estaba
 * {@code PENDIENTE}).
 * </p>
 * <p>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra al usuario.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum EstadoEtapaLote {
    PENDIENTE("Pendiente"),
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String etiqueta;
}