package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda del tooltip flotante compartido (static/js/tooltips.js): la tarjeta que muestra el contenido de
 * data-tooltip en todas las pantallas no deja que un texto largo sin espacios sobresalga de su caja.
 */
class TooltipFlotanteTest {

    private static final Path TOOLTIPS_JS = Path.of("").toAbsolutePath().resolve("src/main/resources/static/js/tooltips.js");

    @Test
    @DisplayName("CP-TTF-01 - La tarjeta del tooltip tiene ancho máximo y parte las palabras sin espacios en cualquier punto")
    void tarjeta_del_tooltip_parte_las_palabras_largas() throws IOException {
        // Arrange
        String js = Files.readString(TOOLTIPS_JS, StandardCharsets.UTF_8);

        // Act
        Matcher m = Pattern.compile("tarjeta\\.style\\.cssText\\s*=\\s*'([^']*)'").matcher(js);

        // Assert
        assertThat(js).as("tooltips.js crea la tarjeta #tooltip-flotante").contains("tarjeta.id = 'tooltip-flotante'");
        assertThat(m.find()).as("tooltips.js define los estilos en línea de la tarjeta").isTrue();
        assertThat(m.group(1)).as("Estilos en línea de la tarjeta del tooltip")
                .contains("max-width:20rem")
                .contains("overflow-wrap:anywhere");
    }
}
