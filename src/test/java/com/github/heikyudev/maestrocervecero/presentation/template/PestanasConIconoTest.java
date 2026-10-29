package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de las pestañas: toda pestaña del sistema (fragmento fragments/pestanas :: pestana) lleva un ícono que la
 * acompaña, y las pestañas de etapa del lote también.
 */
class PestanasConIconoTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String USO = "fragments/pestanas :: pestana(";

    @Test
    @DisplayName("CP-PIC-01 - Todo uso del fragmento pestana pasa un ícono y ninguno pasa null")
    void todo_uso_de_pestana_pasa_un_icono() throws IOException {
        // Arrange
        List<Path> plantillas = plantillas();
        List<String> sinIcono = new ArrayList<>();
        int usos = 0;

        // Act
        for (Path plantilla : plantillas) {
            String html = leer(plantilla);
            Matcher uso = Pattern.compile(Pattern.quote(USO)).matcher(html);
            while (uso.find()) {
                usos++;
                String argumentos = html.substring(uso.end(), html.indexOf("}\"", uso.end()));
                List<String> partes = separarArgumentos(argumentos);
                String icono = partes.size() > 3 ? partes.get(3).strip() : "";
                if (icono.isEmpty() || icono.equals("null")) {
                    sinIcono.add(TEMPLATES.relativize(plantilla) + " -> " + argumentos);
                }
            }
        }

        // Assert
        assertThat(usos).as("hay pestañas que usan el fragmento").isGreaterThan(0);
        assertThat(sinIcono).as("pestañas sin ícono").isEmpty();
    }

    @Test
    @DisplayName("CP-PIC-02 - El fragmento pestana recibe el ícono y lo dibuja antes del texto")
    void el_fragmento_dibuja_el_icono_antes_del_texto() throws IOException {
        // Arrange
        String html = leer(TEMPLATES.resolve("fragments/pestanas.html"));

        // Act
        Matcher fragmento = Pattern.compile("th:fragment=\"pestana\\(url, activa, clave, icono, texto\\)\".*?</a>", Pattern.DOTALL).matcher(html);

        // Assert
        assertThat(fragmento.find()).as("el fragmento pestana(url, activa, clave, icono, texto) existe").isTrue();
        String cuerpo = fragmento.group();
        assertThat(cuerpo.indexOf("${icono}")).as("el ícono se dibuja").isPositive();
        assertThat(cuerpo.indexOf("${icono}")).as("antes del texto").isLessThan(cuerpo.indexOf("${texto}"));
    }

    @Test
    @DisplayName("CP-PIC-03 - Las pestañas de etapa del lote llevan un ícono por cada etapa")
    void las_pestanas_de_etapa_del_lote_llevan_icono() throws IOException {
        // Arrange
        String html = leer(TEMPLATES.resolve("lote/lote-detalle.html"));

        // Act
        Matcher nav = Pattern.compile("<nav aria-label=\"Etapas del lote\".*?</nav>", Pattern.DOTALL).matcher(html);

        // Assert
        assertThat(nav.find()).as("existe la navegación de etapas").isTrue();
        String cuerpo = nav.group();
        assertThat(cuerpo).contains("data-icono-etapa");
        for (String etapa : List.of("MOLIENDA", "MACERACION", "HERVIDO", "FERMENTACION", "MADURACION", "ENVASADO")) {
            assertThat(cuerpo).as("ícono de la etapa " + etapa).contains("'" + etapa + "': 'bi-");
        }
    }

    /** Separa los argumentos de nivel superior de una llamada al fragmento (respeta paréntesis, llaves y comillas). */
    private static List<String> separarArgumentos(String texto) {
        List<String> partes = new ArrayList<>();
        int profundidad = 0;
        boolean comillas = false;
        StringBuilder actual = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '\'') {
                comillas = !comillas;
            } else if (!comillas && (c == '(' || c == '{')) {
                profundidad++;
            } else if (!comillas && (c == ')' || c == '}')) {
                profundidad--;
            }
            if (c == ',' && profundidad == 0 && !comillas) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        partes.add(actual.toString());
        return partes;
    }

    private static List<Path> plantillas() throws IOException {
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            return archivos.filter(p -> p.toString().endsWith(".html")).toList();
        }
    }

    private static String leer(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }
}
