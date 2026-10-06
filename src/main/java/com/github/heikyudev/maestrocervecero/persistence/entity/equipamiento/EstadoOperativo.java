package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Representa el estado operativo actual de un {@link EquipamientoEntity} dentro
 * del ciclo de vida productivo. La transición entre estados no la maneja la entidad
 * (ver convención del proyecto: reglas de negocio en la capa de service) — por ejemplo,
 * el pasaje automático a {@code EN_LIMPIEZA} cuando se ejecuta una operación de
 * producción sobre la fase asociada a este equipamiento es responsabilidad del
 * service correspondiente, que llama a {@code setEstadoOperativo(...)}.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoOperativo {
    DISPONIBLE("Disponible"),
    EN_USO("En uso"),
    EN_LIMPIEZA("En limpieza"),
    EN_MANTENIMIENTO("En mantenimiento");

    /** Nombre legible del estado, el que se muestra al usuario. */
    private final String etiqueta;
}