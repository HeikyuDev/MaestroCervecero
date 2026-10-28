package com.github.heikyudev.maestrocervecero.util;

import java.util.List;

/**
 * Arma los mensajes que informan varios elementos a la vez (insumos faltantes, avisos de una consulta): una línea de
 * título seguida de un elemento por línea, separados por un salto de línea. La alerta del layout reconoce ese formato y
 * lo muestra como título y lista con viñetas; un mensaje de una sola línea se muestra como siempre.
 */
public final class MensajeConLista {

    /** Título de la lista cuando una consulta ignora varios parámetros a la vez. */
    public static final String TITULO_AVISOS = "Se ajustó la consulta:";

    private static final String SEPARADOR = "\n";

    private MensajeConLista() {
    }

    /**
     * @param titulo Primera línea del mensaje, que introduce la lista.
     * @param items  Un elemento por línea, en el orden dado.
     * @return El título seguido de cada elemento en su propia línea; solo el título si no hay elementos.
     */
    public static String construir(String titulo, List<String> items) {
        if (items == null || items.isEmpty()) {
            return titulo;
        }
        return titulo + SEPARADOR + String.join(SEPARADOR, items);
    }

    /**
     * @param avisos Avisos independientes de una misma consulta.
     * @return El aviso tal cual si es uno solo; con varios, {@link #TITULO_AVISOS} y un aviso por línea.
     */
    public static String deAvisos(List<String> avisos) {
        return avisos.size() == 1 ? avisos.get(0) : construir(TITULO_AVISOS, avisos);
    }

    /**
     * @param mensaje El mensaje de la alerta.
     * @return Las líneas del mensaje, sin líneas vacías; vacío si el mensaje es {@code null}.
     */
    public static List<String> lineas(String mensaje) {
        if (mensaje == null) {
            return List.of();
        }
        return mensaje.lines().map(String::strip).filter(linea -> !linea.isEmpty()).toList();
    }
}
