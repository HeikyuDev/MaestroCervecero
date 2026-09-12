package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Representa cada uno de los 4 tipos concretos de equipamiento productivo. Cada valor conoce
 * su propia clase de entidad concreta, usada para filtrar por tipo en consultas polimórficas
 * sobre {@link EquipamientoEntity} (ver {@code IEquipamientoRepository.filtrarEquipamientos}).
 */
@Getter
@RequiredArgsConstructor
public enum TipoEquipamiento {
    MOLINO(MolinoEntity.class),
    MACERADOR(MaceradorEntity.class),
    OLLA_HERVOR(OllaHervorEntity.class),
    FERMENTADOR(FermentadorEntity.class);

    private final Class<? extends EquipamientoEntity> entityClass;
}
