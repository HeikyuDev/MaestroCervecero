package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de consistencia de los buscadores modales: todas las plantillas buscador-*.html y filtro-*.html
 * comparten el mismo estilo de tabla y de botón "Elegir", y el cascarón del modal no permite desborde horizontal.
 */
class BuscadoresModalTest {

    private static final Path RAIZ = Path.of("").toAbsolutePath();
    private static final Path TEMPLATES = RAIZ.resolve("src/main/resources/templates");
    private static final Path MODALES_JS = RAIZ.resolve("src/main/resources/static/js/modales.js");
    private static final Path INPUT_CSS = RAIZ.resolve("frontend/tailwind/input.css");

    private static final Pattern BOTON_ELEGIR = Pattern.compile("<button[^>]*data-elegir[^>]*>");

    @Test
    @DisplayName("CP-BMO-01 - Toda tabla de un buscador modal usa la clase tabla-buscador")
    void toda_tabla_de_buscador_usa_tabla_buscador() throws IOException {
        // Arrange
        List<Path> plantillas = plantillasConTabla();

        // Act
        List<String> infractoras = plantillas.stream()
                .filter(p -> contarTablasSinClase(leer(p)) > 0)
                .map(this::nombre)
                .toList();

        // Assert
        assertThat(plantillas).as("plantillas de buscadores con tabla").isNotEmpty();
        assertThat(infractoras).as("Plantillas cuya tabla no usa class=\"tabla-buscador\"").isEmpty();
    }

    @Test
    @DisplayName("CP-BMO-02 - Todo botón data-elegir de un buscador modal tiene shrink-0")
    void todo_boton_elegir_tiene_shrink_0() throws IOException {
        // Arrange
        List<Path> plantillas = plantillasConTabla();

        // Act
        List<String> infractoras = plantillas.stream()
                .filter(p -> {
                    Matcher m = BOTON_ELEGIR.matcher(leer(p));
                    boolean hay = false;
                    while (m.find()) {
                        hay = true;
                        if (!m.group().contains("shrink-0")) {
                            return true;
                        }
                    }
                    return !hay;
                })
                .map(this::nombre)
                .toList();

        // Assert
        assertThat(infractoras).as("Plantillas cuyo botón data-elegir falta o no tiene shrink-0").isEmpty();
    }

    @Test
    @DisplayName("CP-BMO-03 - El cascarón del modal en modales.js oculta el desborde horizontal")
    void cascaron_del_modal_tiene_overflow_x_hidden() throws IOException {
        // Arrange
        String js = Files.readString(MODALES_JS, StandardCharsets.UTF_8);

        // Act
        Matcher m = Pattern.compile("CLASES_MODAL\\s*=\\s*'([^']*)'").matcher(js);

        // Assert
        assertThat(m.find()).as("modales.js define CLASES_MODAL").isTrue();
        assertThat(m.group(1)).as("Clases del cascarón del modal en modales.js").contains("overflow-x-hidden");
    }

    @Test
    @DisplayName("CP-BMO-04 - input.css define .tabla-buscador y .tabla-buscador [data-elegir]")
    void input_css_define_las_clases_del_buscador() throws IOException {
        // Arrange
        String css = Files.readString(INPUT_CSS, StandardCharsets.UTF_8);

        // Act / Assert
        assertThat(css).as("frontend/tailwind/input.css")
                .contains(".tabla-buscador {")
                .contains(".tabla-buscador [data-elegir]");
    }

    private List<Path> plantillasConTabla() throws IOException {
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".html") && (n.startsWith("buscador") || n.startsWith("filtro-"));
                    })
                    .filter(p -> leer(p).contains("<table"))
                    .sorted()
                    .toList();
        }
    }

    private long contarTablasSinClase(String html) {
        Matcher m = Pattern.compile("<table[^>]*>").matcher(html);
        long sinClase = 0;
        while (m.find()) {
            if (!m.group().contains("class=\"tabla-buscador\"")) {
                sinClase++;
            }
        }
        return sinClase;
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
