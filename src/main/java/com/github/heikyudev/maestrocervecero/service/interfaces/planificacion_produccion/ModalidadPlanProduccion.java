package com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Modalidad de un plan de producción generado automáticamente. Cada valor trae su nombre legible
 * ({@code etiqueta}), que es el que se muestra al usuario.
 * <ul>
 *     <li>{@code SECUENCIAL}: un único fermentador que se reutiliza lote tras lote.</li>
 *     <li>{@code CONCURRENTE}: varios fermentadores que trabajan en paralelo y encolan lotes cuando no alcanzan.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ModalidadPlanProduccion {
    SECUENCIAL("Secuencial"),
    CONCURRENTE("Concurrente");

    private final String etiqueta;
}
