package com.github.heikyudev.maestrocervecero.presentation.controller;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Locale;

/**
 * Constantes y utilidades compartidas por los controllers de las pantallas de listado.
 */
public final class MetodosControlador {

    public static final int TAMANIO_PAGINA = 20;
    public static final int TAMANIO_PAGINA_BUSCADOR = 8;

    /**
     * Los módulos maestros (ubicaciones, insumos, etc.) se listan en orden alfabético por nombre (sin
     * distinguir mayúsculas), con el id como desempate para que el orden sea siempre estable.
     */
    public static final Sort ORDEN_ALFABETICO = Sort.by(
            Sort.Order.asc("nombre").ignoreCase(),
            Sort.Order.asc("id"));

    /**
     * El equipamiento no tiene nombre sino identificador interno: se lista en orden alfabético por
     * identificador (sin distinguir mayúsculas), con el id como desempate.
     */
    public static final Sort ORDEN_POR_IDENTIFICADOR = Sort.by(
            Sort.Order.asc("identificadorInterno").ignoreCase(),
            Sort.Order.asc("id"));

    private MetodosControlador() {
    }

    /**
     * Un texto vacío o en blanco equivale a no filtrar: devuelve {@code null}; si no, el texto sin espacios sobrantes.
     */
    public static String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    /**
     * El texto como id, o {@code null} si está vacío, no es un número o no es positivo.
     */
    public static Long aId(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            long valor = Long.parseLong(limpio);
            return valor > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Agrega la alerta (mensaje + tipo) que el layout muestra tras un Post-Redirect-Get.
     */
    public static void agregarMensaje(RedirectAttributes redirectAttributes, String mensaje, TipoAlerta tipo) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
    }

    /**
     * El nombre de la etapa tal como viaja en la URL (por ejemplo, "maceracion").
     */
    public static String slugDe(TipoEtapa tipoEtapa) {
        return tipoEtapa.name().toLowerCase(Locale.ROOT);
    }

    /**
     * La etapa cuyo nombre en la URL coincide con el texto, o {@code null} si el texto está vacío o ninguna coincide.
     */
    public static TipoEtapa aEtapa(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        return Arrays.stream(TipoEtapa.values())
                .filter(tipo -> slugDe(tipo).equals(limpio.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }
}
