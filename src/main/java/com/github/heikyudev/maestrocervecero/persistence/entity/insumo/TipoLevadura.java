package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TipoLevadura {
    ALE(0.75),
    HIBRIDA(1.00),
    LAGER(1.50);

    private final double tasaInoculacion;
}
