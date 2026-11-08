package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de consistencia de los botones "Limpiar" y "Buscar": en todo formulario de filtros de una lista, el contenedor de
 * los botones va alineado a la derecha (justify-end) y, si la grilla de filtros tiene más de dos columnas en algún tamaño de
 * pantalla, ocupa la fila completa en ese tamaño (col-span-N con las N columnas de la grilla, o col-span-full). Así los botones
 * quedan en el borde derecho de la tarjeta con cualquier cantidad de filtros y no en la primera columna de la última fila.
 *
 * Excepciones: lote-lista (cinco filtros más los botones llenan las filas, los botones caen en la última celda), consumo-lista y
 * envasado-lista (barra de filtro en línea dentro del encabezado de la tarjeta, con el botón de alta a la derecha).
 */
class BotonesDeFiltrosALaDerechaTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Set<String> EXCEPCIONES = Set.of("lote/lote-lista.html", "lote/consumo-lista.html", "lote/envasado-lista.html");
    private static final Pattern GRILLA = Pattern.compile("(?:(\\w+):)?grid-cols-(\\d+)");

    @Test
    @DisplayName("CP-BFD-01: el contenedor de los botones Limpiar y Buscar de cada formulario de filtros está a la derecha y ocupa la fila completa de la grilla")
    void botonesDeFiltrosALaDerecha() throws IOException {
        List<String> revisados = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".html")).toList()) {
                String relativo = TEMPLATES.relativize(archivo).toString().replace('\\', '/');
                String html = Files.readString(archivo, StandardCharsets.UTF_8);
                int limpiar = html.indexOf(">Limpiar<");
                if (limpiar < 0 || EXCEPCIONES.contains(relativo)) {
                    continue;
                }
                String hastaLimpiar = html.substring(0, limpiar);
                String form = hastaLimpiar.substring(hastaLimpiar.lastIndexOf("<form"));
                String etiquetaForm = form.substring(0, form.indexOf('>'));
                // El contenedor de los botones es el último div abierto antes del enlace Limpiar
                Matcher divs = Pattern.compile("<div class=\"([^\"]*)\"").matcher(form);
                String contenedor = null;
                while (divs.find()) {
                    contenedor = divs.group(1);
                }
                assertThat(contenedor).as(relativo + ": contenedor de los botones").isNotNull().contains("justify-end");
                Matcher grilla = GRILLA.matcher(etiquetaForm);
                while (grilla.find()) {
                    int columnas = Integer.parseInt(grilla.group(2));
                    if (columnas > 2) {
                        String prefijo = grilla.group(1) == null ? "" : grilla.group(1) + ":";
                        assertThat(contenedor).as(relativo + ": la grilla tiene " + columnas + " columnas en " + (prefijo.isEmpty() ? "todos los tamaños" : prefijo))
                                .satisfiesAnyOf(c -> assertThat(c).contains(prefijo + "col-span-" + columnas), c -> assertThat(c).contains("col-span-full"));
                    }
                }
                revisados.add(relativo);
            }
        }
        assertThat(revisados).contains("control_equipamiento/control-lista.html", "control_equipamiento/control-historial.html", "auditoria/auditoria-lista.html")
                .hasSizeGreaterThanOrEqualTo(20);
    }
}
