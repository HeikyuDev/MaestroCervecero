package com.github.heikyudev.maestrocervecero.presentation.view.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Criterio propio de una pestaña de estadísticas, tal como lo dibuja la barra de filtros (fragmento
 * {@code estadisticas/fragments/reporte :: filtros}) a continuación de "Desde" y "Hasta". Hay dos tipos: una lista
 * simple con "Todos" o "Todas" como primera opción, o el selector de entidad con su buscador en modal.
 */
@Getter
@AllArgsConstructor
public final class CampoFiltro {

    /** Tipo de campo de la barra de filtros. */
    public enum Tipo {
        /** Lista simple de valores (por ejemplo, los estados de un enum). */
        LISTA,
        /** Selector de una entidad relacionada con su buscador en modal (por ejemplo, la receta). */
        BUSCADOR
    }

    /** Opción de una lista simple. */
    @Getter
    @AllArgsConstructor
    public static final class Opcion {
        private final String valor;
        private final String etiqueta;
    }

    /** Nombre del parámetro de la petición (por ejemplo, {@code estado}). */
    private final String nombre;

    /** Etiqueta del campo (por ejemplo, "Estado"). */
    private final String etiqueta;

    private final Tipo tipo;

    /** Valor aplicado tal como viaja en la petición (el nombre del enum o el ID), o {@code null} si no se indicó. */
    private final String valor;

    /** Texto del valor elegido en un buscador (por ejemplo, el nombre de la receta), o {@code null}. */
    private final String texto;

    /** Opciones de una lista simple; vacía en un buscador. */
    private final List<Opcion> opciones;

    /** Primera opción de una lista simple, sin filtro ("Todos" o "Todas"). */
    private final String textoTodos;

    /** URL del fragmento del modal de búsqueda de un buscador; {@code null} en una lista. */
    private final String urlBuscador;

    /** Texto del buscador sin selección (por ejemplo, "Seleccione receta"); {@code null} en una lista. */
    private final String placeholder;

    /**
     * Una lista simple.
     *
     * @param nombre Nombre del parámetro.
     * @param etiqueta Etiqueta del campo.
     * @param valor Valor aplicado, o {@code null}.
     * @param opciones Opciones de la lista, sin la de "sin filtro".
     * @param textoTodos Texto de la opción sin filtro ("Todos" o "Todas").
     * @return El campo.
     */
    public static CampoFiltro lista(String nombre, String etiqueta, String valor, List<Opcion> opciones, String textoTodos) {
        return new CampoFiltro(nombre, etiqueta, Tipo.LISTA, valor, null, List.copyOf(opciones), textoTodos, null, null);
    }

    /**
     * Un selector de entidad con su buscador en modal.
     *
     * @param nombre Nombre del parámetro (el ID elegido).
     * @param etiqueta Etiqueta del campo.
     * @param valor ID elegido, o {@code null}.
     * @param texto Texto del elegido, o {@code null}.
     * @param urlBuscador URL del fragmento del modal de búsqueda.
     * @param placeholder Texto sin selección.
     * @return El campo.
     */
    public static CampoFiltro buscador(String nombre, String etiqueta, String valor, String texto, String urlBuscador, String placeholder) {
        return new CampoFiltro(nombre, etiqueta, Tipo.BUSCADOR, valor, texto, List.of(), null, urlBuscador, placeholder);
    }

    /** {@code true} si es un selector de entidad con buscador. */
    public boolean isBuscador() {
        return tipo == Tipo.BUSCADOR;
    }
}
