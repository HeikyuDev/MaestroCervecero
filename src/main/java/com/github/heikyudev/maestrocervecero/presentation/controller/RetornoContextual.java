package com.github.heikyudev.maestrocervecero.presentation.controller;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Pattern;

/**
 * Alta contextual: cuando un formulario necesita una entidad que todavía no existe, el selector
 * lleva al alta de esa entidad enviando dos parámetros:
 * <ul>
 *   <li>{@code retorno}: la URL del formulario de origen (solo rutas internas del sitio).</li>
 *   <li>{@code campo}: el nombre del campo del formulario de origen que recibe el id creado.</li>
 * </ul>
 * Al guardar, el alta redirige a {@code retorno} con {@code campo=<id creado>} y
 * {@link #PARAM_DESDE_ALTA}, que le indica al formulario de origen que debe recuperar lo que el
 * usuario ya había tipeado. Como el formulario de origen puede ser a su vez un alta contextual,
 * el mecanismo se encadena (país → provincia → localidad).
 */
public final class RetornoContextual {

    public static final String PARAM_DESDE_ALTA = "desdeAlta";

    private static final Pattern NOMBRE_CAMPO = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    private RetornoContextual() {
    }

    /**
     * Devuelve {@code retorno} si es una ruta interna del sitio, o {@code null} si es vacío o
     * podría llevar a otro sitio (esquema, {@code //host}, barras invertidas, caracteres de control).
     */
    public static String retornoValido(String retorno) {
        if (retorno == null || retorno.isBlank()) {
            return null;
        }
        boolean rutaInterna = retorno.startsWith("/")
                && !retorno.startsWith("//")
                && !retorno.contains("\\")
                && !retorno.contains("://")
                && retorno.chars().noneMatch(Character::isISOControl);
        return rutaInterna ? retorno : null;
    }

    /**
     * Nombre de campo seguro para usar como parámetro de la URL de vuelta, o {@code null}.
     */
    public static String campoValido(String campo) {
        return (campo != null && NOMBRE_CAMPO.matcher(campo).matches()) ? campo : null;
    }

    /**
     * URL a la que se vuelve tras crear la entidad: {@code retorno} con el id creado en
     * {@code campo}, o {@code null} si no hay un retorno válido.
     */
    public static String urlDeVuelta(String retorno, String campo, Long id) {
        String destino = retornoValido(retorno);
        if (destino == null) {
            return null;
        }
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(destino).replaceQueryParam(PARAM_DESDE_ALTA, 1);
        String campoSeguro = campoValido(campo);
        if (campoSeguro != null && id != null) {
            url.replaceQueryParam(campoSeguro, id);
        }
        return url.build().toUriString();
    }

    /**
     * URL del botón "Cancelar" de un alta contextual: vuelve al formulario de origen (que recupera lo
     * tipeado) o, si no hay retorno válido, a {@code urlPorDefecto}.
     */
    public static String urlCancelar(String retorno, String urlPorDefecto) {
        String destino = retornoValido(retorno);
        if (destino == null) {
            return urlPorDefecto;
        }
        return UriComponentsBuilder.fromUriString(destino).replaceQueryParam(PARAM_DESDE_ALTA, 1).build().toUriString();
    }
}
