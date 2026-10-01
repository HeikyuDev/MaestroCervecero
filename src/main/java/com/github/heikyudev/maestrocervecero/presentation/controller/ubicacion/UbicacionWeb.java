package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;

/**
 * Utilidades propias de los controllers del módulo de ubicaciones.
 */
final class UbicacionWeb {

    private UbicacionWeb() {
    }

    /**
     * Texto con el que un selector muestra una provincia: su nombre y, entre paréntesis, su país.
     */
    static String etiqueta(ProvinciaResponseDTO provincia) {
        return provincia.getNombre() + " (" + provincia.getPais().getNombre() + ")";
    }
}
