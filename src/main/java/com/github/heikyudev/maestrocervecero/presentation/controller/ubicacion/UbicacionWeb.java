package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Constantes y utilidades compartidas por los controllers del módulo de ubicaciones.
 */
final class UbicacionWeb {

    static final int TAMANIO_PAGINA = 20;
    static final int TAMANIO_PAGINA_BUSCADOR = 8;

    /**
     * Las ubicaciones son tablas maestras: se listan en orden alfabético por nombre (sin distinguir
     * mayúsculas), con el id como desempate para que el orden sea siempre estable.
     */
    static final Sort ORDEN_ALFABETICO = Sort.by(
            Sort.Order.asc("nombre").ignoreCase(),
            Sort.Order.asc("id"));

    private UbicacionWeb() {
    }

    static String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    static void agregarMensaje(RedirectAttributes redirectAttributes, String mensaje, TipoAlerta tipo) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
    }

    static String etiqueta(ProvinciaResponseDTO provincia) {
        return provincia.getNombre() + " (" + provincia.getPais().getNombre() + ")";
    }
}
