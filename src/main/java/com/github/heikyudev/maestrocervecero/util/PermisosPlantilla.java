package com.github.heikyudev.maestrocervecero.util;

/**
 * Expresiones de seguridad que comparten varias plantillas, definidas en un único lugar para que no se repitan.
 */
public final class PermisosPlantilla {

    /**
     * Quien puede consultar alguna operación de barril: limpiezas, fallas, fraccionamientos, mantenimientos, despachos o
     * devoluciones. Es la condición para ver la pantalla de control de barriles.
     */
    public static final String CONSULTA_BARRIL = "hasAnyAuthority('LIMPIEZA_BARRIL_CONSULTAR', 'FALLA_BARRIL_CONSULTAR', "
            + "'FRACCIONAMIENTO_BARRIL_CONSULTAR', 'MANTENIMIENTO_BARRIL_CONSULTAR', 'DESPACHO_BARRIL_CONSULTAR', 'DEVOLUCION_BARRIL_CONSULTAR')";

    private PermisosPlantilla() {
    }
}
