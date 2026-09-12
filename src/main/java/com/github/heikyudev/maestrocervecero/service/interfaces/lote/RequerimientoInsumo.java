package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;

/**
 * Requerimiento escalado de un insumo puntual (una malta, un lúpulo o una levadura
 * específicos) para una etapa concreta del lote. {@code etapa} puede ser {@code null} cuando
 * el requerimiento se calcula fuera del contexto de una etapa concreta (por ejemplo, al
 * validar capacidad del macerador al registrar un lote, donde las etapas del lote todavía no
 * existen) — en ese caso solo se usa {@code cantidadRequerida}, nunca la etapa.
 */
public record RequerimientoInsumo(InsumoEntity insumo, EtapaLoteEntity etapa, double cantidadRequerida) {
}
