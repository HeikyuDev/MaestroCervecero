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
 * service restringe el valor a FERMENTACION o MADURACION) y qué etapa supervisa
 * un plan de monitoreo ({@code EtapaDeControlEntity.etapaAControlar}). En ambos
 * casos el subconjunto válido lo valida el service, no el tipo — este enum siempre
 * representa el universo completo de las 6 etapas posibles.
 * <p>
 * Cada valor sabe qué operaciones se pueden registrar durante esa etapa (usado
 * por la capa de presentación para mostrar/ocultar acciones según corresponda).
 * La validación de negocio real (por ejemplo, qué tipo de insumo se puede
 * consumir en cada etapa: Malta solo en Maceración, Levadura solo en
 * Fermentación) la resuelve el service — este flag es solo una ayuda de UI, no
 * reemplaza esa validación.
 */
@Getter
@RequiredArgsConstructor
public enum TipoEtapa {
    MOLIENDA(false, false, false),
    MACERACION(true, true, false),
    HERVIDO(true, true, false),
    FERMENTACION(true, true, false),
    MADURACION(true, true, false),
    ENVASADO(false, false, true);

    private final boolean permiteRegistrarMediciones;
    private final boolean permiteRegistrarConsumo;
    private final boolean permiteRegistrarEnvasado;
}