package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

public enum TipoIngreso {
    COMPRA,   // Entró por recepción de una Orden de Compra (requiere DetalleCompra)
    DIRECTO   // Entró sin orden previa: stock inicial, donación, etc. (DetalleCompra en null)
}
