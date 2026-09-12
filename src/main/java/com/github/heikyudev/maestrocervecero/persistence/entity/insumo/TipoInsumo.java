package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Representa cada uno de los 3 tipos concretos de insumo. Cada valor conoce su propia clase de
 * entidad concreta, usada para filtrar por tipo en consultas polimórficas sobre
 * {@link InsumoEntity} (ver {@code IInsumoRepository.filtrarInsumos}).
 */
@Getter
@RequiredArgsConstructor
public enum TipoInsumo {
    MALTA(MaltaEntity.class),
    LUPULO(LupuloEntity.class),
    LEVADURA(LevaduraEntity.class);

    private final Class<? extends InsumoEntity> entityClass;
}
