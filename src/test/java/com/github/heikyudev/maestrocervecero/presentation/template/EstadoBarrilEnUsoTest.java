package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de los colores de los estados operativos: un estado que se llama igual en barriles y en equipamientos usa el
 * mismo color en las dos etiquetas, y el estado propio de los barriles (Con cerveza) tiene un color que ningún otro
 * estado usa.
 */
class EstadoBarrilEnUsoTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final Pattern COLOR_DE_EN_USO = Pattern.compile("'EN_USO'\\}\\s*\\?\\s*'([^']+)'");

    @Test
    @DisplayName("CP-EBU-01: la etiqueta de color de un barril En uso tiene el mismo color que la del equipamiento En uso")
    void el_barril_en_uso_usa_el_color_del_equipamiento_en_uso() throws IOException {
        // Arrange
        String barril = Files.readString(TEMPLATES.resolve("barril/estado.html"), StandardCharsets.UTF_8);
        String equipamiento = Files.readString(TEMPLATES.resolve("equipamiento/estado.html"), StandardCharsets.UTF_8);

        // Act
        Matcher colorEquipamiento = COLOR_DE_EN_USO.matcher(equipamiento);
        Matcher colorBarril = COLOR_DE_EN_USO.matcher(barril);

        // Assert
        assertThat(colorEquipamiento.find()).as("el equipamiento define el color de EN_USO").isTrue();
        assertThat(colorBarril.find()).as("el barril define el color de EN_USO").isTrue();
        assertThat(colorBarril.group(1)).isEqualTo(colorEquipamiento.group(1));
    }

    @Test
    @DisplayName("CP-EBU-02: los estados que tienen barril y equipamiento usan el mismo color en las dos etiquetas")
    void los_estados_compartidos_usan_el_mismo_color() throws IOException {
        // Arrange
        String barril = Files.readString(TEMPLATES.resolve("barril/estado.html"), StandardCharsets.UTF_8);
        String equipamiento = Files.readString(TEMPLATES.resolve("equipamiento/estado.html"), StandardCharsets.UTF_8);

        // Act + Assert
        for (String estado : List.of("DISPONIBLE", "EN_USO", "EN_LIMPIEZA")) {
            assertThat(colorDe(estado, barril)).as("color de " + estado + " en barril").isEqualTo(colorDe(estado, equipamiento));
        }
        assertThat(colorPorDefecto(barril)).as("color de EN_MANTENIMIENTO en barril").isEqualTo(colorPorDefecto(equipamiento));
    }

    @Test
    @DisplayName("CP-EBU-03: el estado Con cerveza del barril tiene un color propio que ningún otro estado usa")
    void con_cerveza_tiene_un_color_propio() throws IOException {
        // Arrange
        String barril = Files.readString(TEMPLATES.resolve("barril/estado.html"), StandardCharsets.UTF_8);
        String equipamiento = Files.readString(TEMPLATES.resolve("equipamiento/estado.html"), StandardCharsets.UTF_8);

        // Act
        String conCerveza = colorDe("CON_CERVEZA", barril);
        Set<String> otros = new HashSet<>();
        for (String estado : List.of("DISPONIBLE", "EN_USO", "DESPACHADO", "EN_LIMPIEZA")) {
            otros.add(colorDe(estado, barril));
        }
        otros.add(colorPorDefecto(barril));
        otros.add(colorPorDefecto(equipamiento));
        otros.add(colorDe("EN_LIMPIEZA", equipamiento));

        // Assert
        assertThat(conCerveza).isNotBlank();
        assertThat(otros).doesNotContain(conCerveza);
    }

    /** Color asignado a un estado con la forma {@code 'ESTADO'} ? 'clases'. */
    private static String colorDe(String estado, String plantilla) {
        Matcher matcher = Pattern.compile("'" + estado + "'\\}\\s*\\?\\s*'([^']+)'").matcher(plantilla);
        assertThat(matcher.find()).as("la plantilla define el color de " + estado).isTrue();
        return matcher.group(1);
    }

    /** Color del último estado de la cadena (EN_MANTENIMIENTO), que no lleva condición propia. */
    private static String colorPorDefecto(String plantilla) {
        Matcher matcher = Pattern.compile("'EN_LIMPIEZA'\\}\\s*\\?\\s*'[^']+'\\s*:\\s*'([^']+)'").matcher(plantilla);
        assertThat(matcher.find()).as("la plantilla define el color por defecto").isTrue();
        return matcher.group(1);
    }
}
