package com.github.heikyudev.maestrocervecero.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Representa cada una de las 6 etapas del proceso productivo de un lote, en orden
 * secuencial: Molienda del grano, Maceración, Hervido, Fermentación, Maduración y
 * Envasado (trasvase del fermentador a barriles).
 * <p>
 * Se reutiliza también en el módulo de Receta: para indicar en qué etapa se
 * incorpora un lúpulo Dry Hop ({@code DetalleLupuloEntity.etapaDeUso}, donde el
 * service restringe el valor a FERMENTACION o MADURACION). El subconjunto válido
 * lo valida el service, no el tipo — este enum siempre representa el universo
 * completo de las 6 etapas posibles.
 * <p>
 * Cada valor trae su nombre legible ({@code etiqueta}), que es el que se muestra
 * al usuario en los mensajes, y sabe qué operaciones se pueden registrar durante esa
 * etapa: {@code permiteRegistrarConsumo} y {@code permiteRegistrarEnvasado} los consultan
 * los services de consumo de insumo y de envasado para rechazar el registro en una etapa
 * que no lo admite. La validación del tipo de insumo que se puede consumir en cada
 * etapa (Malta solo en Molienda, Levadura solo en Fermentación) la resuelve el service.
 */
@Getter
@RequiredArgsConstructor
public enum TipoEtapa {
    MOLIENDA("Molienda", true, false),
    MACERACION("Maceración", false, false),
    HERVIDO("Hervido", true, false),
    FERMENTACION("Fermentación", true, false),
    MADURACION("Maduración", true, false),
    ENVASADO("Envasado", false, true);

    private final String etiqueta;
    private final boolean permiteRegistrarConsumo;
    private final boolean permiteRegistrarEnvasado;
}