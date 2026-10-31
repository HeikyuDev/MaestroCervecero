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

    /**
     * Resuelve el tipo concreto de un equipamiento a partir de su clase.
     *
     * @param equipamiento El equipamiento (no un proxy).
     * @return El tipo concreto, o {@code null} si el equipamiento es nulo o de una clase desconocida.
     */
    public static TipoEquipamiento deEquipamiento(EquipamientoEntity equipamiento) {
        if (equipamiento instanceof MolinoEntity) {
            return MOLINO;
        }
        if (equipamiento instanceof MaceradorEntity) {
            return MACERADOR;
        }
        if (equipamiento instanceof OllaHervorEntity) {
            return OLLA_HERVOR;
        }
        if (equipamiento instanceof FermentadorEntity) {
            return FERMENTADOR;
        }
        return null;
    }
}
