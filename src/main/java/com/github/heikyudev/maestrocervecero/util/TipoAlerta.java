package com.github.heikyudev.maestrocervecero.util;

import lombok.Getter;

@Getter
public enum TipoAlerta {
    SUCCESS("success"),
    INFO("info"),
    WARNING("warning"),
    DANGER("danger");

    private final String codigo;

    TipoAlerta(String codigo) {
        this.codigo = codigo;
    }
}