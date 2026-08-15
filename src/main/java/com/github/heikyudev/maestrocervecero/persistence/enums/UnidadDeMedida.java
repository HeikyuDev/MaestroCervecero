package com.github.heikyudev.maestrocervecero.persistence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UnidadDeMedida {
    GRAMO("g", 1.0),
    KILOGRAMO("kg", 1_000.0),
    TONELADA("t", 1_000_000.0);
    private final String simbolo;
    private final double factorAGramos;

    /**
     * Convierte un valor expresado en esta unidad a la unidad base (Gramos).
     * Ejemplo: UnidadDeMedida.KILOGRAMO.aGramos(2.5) -> 2500.0 gramos
     */
    public double aGramos(double valor) {
        return valor * this.factorAGramos;
    }

    /**
     * Convierte una cantidad de gramos a esta unidad.
     * Ejemplo: UnidadDeMedida.KILOGRAMO.desdeGramos(2500.0) -> 2.5 kg
     */
    public double desdeGramos(double gramos) {
        return gramos / this.factorAGramos;
    }
}