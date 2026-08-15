package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormatoLupulo {
    PELLET(1.10),
    FLOR(1.00);

    private final Double factorCorreccionUtilizacion;
}
