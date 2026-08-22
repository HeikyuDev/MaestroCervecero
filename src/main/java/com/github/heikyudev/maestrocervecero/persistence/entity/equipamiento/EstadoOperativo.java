package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import lombok.Getter;

/**
 * Representa el estado operativo actual de un {@link EquipamientoEntity} dentro
 * del ciclo de vida productivo. La transición entre estados no la maneja la entidad
 * (ver convención del proyecto: reglas de negocio en la capa de service) — por ejemplo,
 * el pasaje automático a {@code EN_LIMPIEZA} cuando se ejecuta una operación de
 * producción sobre la fase asociada a este equipamiento es responsabilidad del
 * service correspondiente, que llama a {@code setEstadoOperativo(...)}.
 */
@Getter
public enum EstadoOperativo {
    DISPONIBLE,
    EN_USO,
    EN_LIMPIEZA,
    EN_MANTENIMIENTO
}