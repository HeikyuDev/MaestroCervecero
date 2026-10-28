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
 * Guarda de consistencia de la columna de acciones fija: en toda tabla de listado (fuera de los buscadores modales)
 * la columna de acciones lleva la clase col-acciones en el encabezado y en cada celda, la tabla está dentro de un
 * contenedor overflow-x-auto y input.css define la clase con position: sticky.
 *
 * Criterio de detección de la columna de acciones:
 * - Encabezado: el último th del thead de la tabla tiene el texto "Acciones" o "Mover", o está vacío.
 * - Celdas: la última celda td de una fila (sin colspan) contiene un botón, un enlace con aria-label o el fragmento fragments/boton-abrir.
 * Las plantillas buscador-*.html y filtro-*.html quedan fuera: su tabla .tabla-buscador tiene su propio estilo.
 */
class ColumnaAccionesStickyTest {

    private static final Path RAIZ = Path.of("").toAbsolutePath();
    private static final Path TEMPLATES = RAIZ.resolve("src/main/resources/templates");
    private static final Path INPUT_CSS = RAIZ.resolve("frontend/tailwind/input.css");

    private static final String CLASE = "col-acciones";
    private static final Set<String> TEXTOS_ENCABEZADO_ACCIONES = Set.of("Acciones", "Mover", "");

    /** Contenido de una etiqueta de apertura que tolera ">" dentro de los valores de los atributos. */
    private static final String ATRIBUTOS = "((?:[^>\"']|\"[^\"]*\"|'[^']*')*)";

    private static final Pattern TABLA = Pattern.compile("<table\\b" + ATRIBUTOS + ">(.*?)</table>", Pattern.DOTALL);
    private static final Pattern THEAD = Pattern.compile("<thead\\b" + ATRIBUTOS + ">(.*?)</thead>", Pattern.DOTALL);
    private static final Pattern TH = Pattern.compile("<th\\b" + ATRIBUTOS + ">(.*?)</th>", Pattern.DOTALL);
    private static final Pattern TR = Pattern.compile("<tr\\b" + ATRIBUTOS + ">(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern TD = Pattern.compile("<td\\b" + ATRIBUTOS + ">(.*?)</td>", Pattern.DOTALL);
    private static final Pattern DIV = Pattern.compile("<div\\b" + ATRIBUTOS + ">");
    private static final Pattern ETIQUETA = Pattern.compile("<[^>]*>");
    private static final Pattern ACCION = Pattern.compile("<button\\b|<a\\b" + ATRIBUTOS + "aria-label=|fragments/boton-abrir");

    @Test
    @DisplayName("CP-CAS-01 - El encabezado de toda columna de acciones lleva la clase col-acciones")
    void encabezado_de_acciones_tiene_la_clase() throws IOException {
        // Arrange
        List<String> encabezados = new ArrayList<>();
        List<String> infractores = new ArrayList<>();

        // Act
        for (Path p : plantillas()) {
            for (String atributosTh : encabezadosDeAcciones(leer(p))) {
                encabezados.add(nombre(p));
                if (!tieneClase(atributosTh)) {
                    infractores.add(nombre(p) + " -> <th" + atributosTh + ">");
                }
            }
        }

        // Assert
        assertThat(encabezados).as("tablas con columna de acciones detectadas").hasSizeGreaterThanOrEqualTo(20);
        assertThat(infractores).as("Encabezados de acciones sin class=\"" + CLASE + "\"").isEmpty();
    }

    @Test
    @DisplayName("CP-CAS-02 - Toda celda de acciones de una fila lleva la clase col-acciones")
    void celda_de_acciones_tiene_la_clase() throws IOException {
        // Arrange
        List<String> celdas = new ArrayList<>();
        List<String> infractoras = new ArrayList<>();

        // Act
        for (Path p : plantillas()) {
            Matcher fila = TR.matcher(leer(p));
            while (fila.find()) {
                Matcher celda = TD.matcher(fila.group(2));
                String atributos = null;
                String contenido = null;
                while (celda.find()) {
                    atributos = celda.group(1);
                    contenido = celda.group(2);
                }
                if (atributos == null || atributos.contains("colspan") || !ACCION.matcher(contenido).find()) {
                    continue;
                }
                celdas.add(nombre(p));
                if (!tieneClase(atributos)) {
                    infractoras.add(nombre(p) + " -> <td" + atributos + ">");
                }
            }
        }

        // Assert
        assertThat(celdas).as("celdas de acciones detectadas").hasSizeGreaterThanOrEqualTo(20);
        assertThat(infractoras).as("Celdas de acciones sin class=\"" + CLASE + "\"").isEmpty();
    }

    @Test
    @DisplayName("CP-CAS-03 - Toda tabla con columna de acciones está dentro de un contenedor overflow-x-auto")
    void tabla_con_acciones_esta_en_contenedor_desplazable() throws IOException {
        // Arrange
        List<String> infractoras = new ArrayList<>();

        // Act
        for (Path p : plantillas()) {
            String html = leer(p);
            Matcher tabla = TABLA.matcher(html);
            while (tabla.find()) {
                if (!tabla.group(2).contains(CLASE)) {
                    continue;
                }
                Matcher div = DIV.matcher(html.substring(0, tabla.start()));
                String ultimoDiv = null;
                while (div.find()) {
                    ultimoDiv = div.group(1);
                }
                if (ultimoDiv == null || !ultimoDiv.contains("overflow-x-auto")) {
                    infractoras.add(nombre(p));
                }
            }
        }

        // Assert
        assertThat(infractoras).as("Tablas con columna de acciones fuera de un <div class=\"overflow-x-auto\">").isEmpty();
    }

    @Test
    @DisplayName("CP-CAS-04 - input.css define .col-acciones fija a la derecha con el fondo de su fila")
    void input_css_define_la_columna_fija() throws IOException {
        // Arrange
        String css = Files.readString(INPUT_CSS, StandardCharsets.UTF_8);

        // Act
        Matcher regla = Pattern.compile("\\.col-acciones\\s*\\{([^}]*)}").matcher(css);

        // Assert
        assertThat(regla.find()).as("input.css define .col-acciones").isTrue();
        assertThat(regla.group(1)).as("Reglas de .col-acciones en input.css")
                .contains("position: sticky")
                .contains("right: 0")
                .contains("background-color: inherit");
        assertThat(css).as("Fondo por defecto de la fila con columna de acciones")
                .contains(":where(tbody > tr:has(> .col-acciones))");
    }

    private List<String> encabezadosDeAcciones(String html) {
        List<String> resultado = new ArrayList<>();
        Matcher tabla = TABLA.matcher(html);
        while (tabla.find()) {
            if (tabla.group(1).contains("tabla-buscador")) {
                continue;
            }
            Matcher thead = THEAD.matcher(tabla.group(2));
            if (!thead.find()) {
                continue;
            }
            Matcher th = TH.matcher(thead.group(2));
            String atributos = null;
            String texto = null;
            while (th.find()) {
                atributos = th.group(1);
                texto = ETIQUETA.matcher(th.group(2)).replaceAll("").strip();
            }
            if (atributos != null && TEXTOS_ENCABEZADO_ACCIONES.contains(texto)) {
                resultado.add(atributos);
            }
        }
        return resultado;
    }

    private boolean tieneClase(String atributos) {
        Matcher clase = Pattern.compile("\\bclass=\"([^\"]*)\"").matcher(atributos);
        return clase.find() && List.of(clase.group(1).split("\\s+")).contains(CLASE);
    }

    private List<Path> plantillas() throws IOException {
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".html") && !n.startsWith("buscador") && !n.startsWith("filtro-");
                    })
                    .sorted()
                    .toList();
        }
    }

    private String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + p, e);
        }
    }

    private String nombre(Path p) {
        return TEMPLATES.relativize(p).toString().replace('\\', '/');
    }
}
