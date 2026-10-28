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
 * Guarda del botón estándar "Abrir": la entrada a la pantalla de trabajo de un registro (hoy, la lista de lotes) usa el
 * fragmento fragments/boton-abrir, de modo que el aspecto, el texto y el tooltip no se desvíen.
 */
class BotonAbrirTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String FRAGMENTO = "fragments/boton-abrir :: abrir(";

    private static final String LISTA_DE_LOTES = "lote/lote-lista.html";

    @Test
    @DisplayName("CP-BAB-01 - El fragmento define un enlace con data-boton-abrir, texto Abrir, flecha y aria-label, sin tooltip")
    void el_fragmento_define_el_boton_estandar() throws IOException {
        // Arrange
        String html = leer(TEMPLATES.resolve("fragments/boton-abrir.html"));

        // Act
        Matcher boton = Pattern.compile("<a\\s+th:fragment=\"abrir\\(url, etiquetaAccesible\\)\"([^>]*)>(.*?)</a>", Pattern.DOTALL).matcher(html);

        // Assert
        assertThat(boton.find()).as("el fragmento abrir(url, etiquetaAccesible) es un enlace").isTrue();
        assertThat(boton.group(1))
                .contains("data-boton-abrir")
                .contains("th:href=\"${url}\"")
                .contains("th:aria-label=\"${etiquetaAccesible}\"")
                .contains("whitespace-nowrap")
                .contains("focus:ring-2")
                .contains("hover:bg-amber-500")
                .doesNotContain("data-tooltip")
                .doesNotContain("title=");
        assertThat(boton.group(2).replaceAll("<[^>]+>", "").strip()).isEqualTo("Abrir");
        assertThat(boton.group(2)).contains("bi-arrow-right");
    }

    @Test
    @DisplayName("CP-BAB-02 - La lista de lotes usa el fragmento y no copia su marcado")
    void la_lista_de_lotes_usa_el_fragmento() throws IOException {
        // Arrange
        String html = leer(TEMPLATES.resolve(LISTA_DE_LOTES));

        // Act
        boolean usaFragmento = html.contains(FRAGMENTO);
        boolean copiaMarcado = html.contains("data-boton-abrir");

        // Assert
        assertThat(usaFragmento).as("lote-lista usa fragments/boton-abrir").isTrue();
        assertThat(copiaMarcado).as("lote-lista no copia el marcado data-boton-abrir").isFalse();
    }

    @Test
    @DisplayName("CP-BAB-03 - La lista de lotes no conserva el ícono bi-gear ni el texto Gestionar lote")
    void la_lista_de_lotes_no_conserva_el_engranaje() throws IOException {
        // Arrange
        String html = leer(TEMPLATES.resolve(LISTA_DE_LOTES));

        // Act
        boolean conEngranaje = html.contains("bi-gear");
        boolean conTextoAnterior = html.contains("Gestionar lote");

        // Assert
        assertThat(conEngranaje).as("lote-lista sin bi-gear").isFalse();
        assertThat(conTextoAnterior).as("lote-lista sin el texto Gestionar lote").isFalse();
    }

    private String leer(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }
}
