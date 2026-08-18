package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import lombok.Getter;

/**
 * Estado de una {@code EtapaLoteEntity} dentro del ciclo de vida de su lote.
 */
@Getter
public enum EstadoEtapaLote {
    PENDIENTE,
    EN_CURSO,
    FINALIZADA
}