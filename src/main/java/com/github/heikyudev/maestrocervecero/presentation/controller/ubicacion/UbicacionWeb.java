package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Constantes y utilidades compartidas por los controllers del módulo de ubicaciones.
 */
final class UbicacionWeb {

    /**
     * Valor del parámetro {@code modo} con el que un formulario de alta se abre desde un selector
     * (alta "al vuelo"): al guardar no se redirige, se devuelve la entidad creada para dejarla elegida.
     */
    static final String MODO_SELECCION = "seleccion";

    /**
     * Estado HTTP con el que se vuelve a mostrar un formulario de modal que tiene errores.
     */
    static final int ESTADO_FORMULARIO_INVALIDO = 422;

    /**
     * Estado HTTP con el que se responde un alta al vuelo exitosa.
     */
    static final int ESTADO_CREADO = 201;

    static final int TAMANIO_PAGINA = 20;
    static final int TAMANIO_PAGINA_BUSCADOR = 8;

    static final String VISTA_RESULTADO_ALTA = "ubicacion/resultado-alta :: resultado";

    /**
     * Las ubicaciones son tablas maestras: se listan en orden alfabético por nombre (sin distinguir
     * mayúsculas), con el id como desempate para que el orden sea siempre estable.
     */
    static final Sort ORDEN_ALFABETICO = Sort.by(
            Sort.Order.asc("nombre").ignoreCase(),
            Sort.Order.asc("id"));

    private UbicacionWeb() {
    }

    static boolean esAltaAlVuelo(String modo) {
        return MODO_SELECCION.equals(modo);
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

    static String etiqueta(LocalidadResponseDTO localidad) {
        return localidad.getNombre() + " (" + localidad.getProvincia().getNombre() + ")";
    }
}
