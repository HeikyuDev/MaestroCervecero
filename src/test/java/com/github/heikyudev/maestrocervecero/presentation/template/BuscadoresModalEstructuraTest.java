package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de consistencia de los buscadores en modal (archivos {@code buscador-*.html}): todos comparten la misma
 * estructura, para que ninguno se rompa con muchas filas ni con un ancho angosto.
 * <ul>
 *   <li>La tabla {@code tabla-buscador} va dentro de un {@code contenedor-buscador} (desplazamiento horizontal controlado).</li>
 *   <li>La acción de la fila es un botón "Elegir" en la columna {@code col-acciones} (fija a la derecha), tanto en el
 *       encabezado como en la celda.</li>
 *   <li>El formulario de filtros es una grilla de dos columnas con el botón "Buscar" en un contenedor a la derecha que ocupa
 *       la fila completa.</li>
 * </ul>
 */
class BuscadoresModalEstructuraTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    @DisplayName("CP-BME-01: todo buscador en modal tiene el contenedor con desplazamiento horizontal, la columna de acción fija con el botón Elegir y el formulario con Buscar a la derecha")
    void todosLosBuscadoresCompartenLaMismaEstructura() throws IOException {
        List<String> revisados = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            for (Path archivo : archivos.filter(p -> p.getFileName().toString().startsWith("buscador-") && p.toString().endsWith(".html")).toList()) {
                String relativo = TEMPLATES.relativize(archivo).toString().replace('\\', '/');
                String html = Files.readString(archivo, StandardCharsets.UTF_8);

                int contenedor = html.indexOf("<div class=\"contenedor-buscador\">");
                int tabla = html.indexOf("<table class=\"tabla-buscador\">");
                int cierreTabla = html.indexOf("</table>");
                assertThat(contenedor).as(relativo + ": contenedor-buscador").isNotNegative();
                assertThat(tabla).as(relativo + ": la tabla va dentro del contenedor").isGreaterThan(contenedor);
                assertThat(html.indexOf("</div>", cierreTabla)).as(relativo + ": el contenedor cierra después de la tabla").isGreaterThan(cierreTabla);

                assertThat(html).as(relativo + ": columna de acción del encabezado").contains("<th class=\"col-acciones px-3 py-3\">");
                assertThat(html).as(relativo + ": columna de acción de la fila").contains("<td class=\"col-acciones px-3 py-3 text-right\">");
                assertThat(html).as(relativo + ": botón Elegir").contains("data-elegir").contains(">Elegir</button>");
                assertThat(html).as(relativo + ": sin anchos fijos que ensanchen el modal").doesNotContain("overflow-x-hidden").doesNotContain("min-w-[");

                if (html.contains("<form")) {
                    String form = html.substring(html.indexOf("<form"), html.indexOf("</form>"));
                    assertThat(form).as(relativo + ": grilla del formulario").contains("p-6 grid grid-cols-1 sm:grid-cols-2 gap-4 items-end");
                    assertThat(form).as(relativo + ": botones a la derecha, a fila completa").contains("<div class=\"sm:col-span-2 flex items-center justify-end gap-3\">");
                }
                revisados.add(relativo);
            }
        }
        assertThat(revisados).contains("lote/buscador-barril.html", "cliente/buscador-cliente.html", "orden_compra/buscador-proveedor.html",
                "ubicacion/buscador-localidad.html", "ingreso_insumo/buscador-item-orden.html").hasSizeGreaterThanOrEqualTo(23);
    }
}
