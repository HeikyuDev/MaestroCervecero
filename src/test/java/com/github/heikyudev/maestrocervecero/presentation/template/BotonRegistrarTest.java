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
 * Guarda del botón de registro en la fila: la acción que lleva desde la fila de un listado al formulario de alta de una
 * operación usa el fragmento fragments/boton-registrar, con el aspecto suave ámbar del botón "Abrir" pero con el ícono de la
 * operación y sin flecha, para no confundirse con la entrada a una pantalla de trabajo.
 */
class BotonRegistrarTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-BRG-01 - El fragmento define un enlace con data-boton-registrar, ícono de la operación a la izquierda, texto recibido, aria-label y el mismo aspecto ámbar suave de Abrir, sin flecha ni tooltip")
    void el_fragmento_define_el_boton_de_registro() throws IOException {
        // Arrange
        String html = leer("fragments/boton-registrar.html");
        String abrir = leer("fragments/boton-abrir.html");

        // Act
        Matcher boton = Pattern.compile("<a\\s+th:fragment=\"registrar\\(url, texto, icono, etiquetaAccesible\\)\"([^>]*)>(.*?)</a>", Pattern.DOTALL).matcher(html);
        Matcher botonAbrir = Pattern.compile("<a\\s+th:fragment=\"abrir\\(url, etiquetaAccesible\\)\"[^>]*class=\"([^\"]*)\"", Pattern.DOTALL).matcher(abrir);

        // Assert
        assertThat(boton.find()).as("el fragmento registrar(url, texto, icono, etiquetaAccesible) es un enlace").isTrue();
        assertThat(botonAbrir.find()).isTrue();
        assertThat(boton.group(1))
                .contains("data-boton-registrar")
                .contains("th:href=\"${url}\"")
                .contains("th:aria-label=\"${etiquetaAccesible}\"")
                .contains("class=\"" + botonAbrir.group(1) + "\"")
                .doesNotContain("data-tooltip")
                .doesNotContain("title=")
                .doesNotContain("data-boton-abrir");
        String contenido = boton.group(2);
        assertThat(contenido).contains("th:class=\"'bi ' + ${icono}\"").contains("th:text=\"${texto}\"").doesNotContain("bi-arrow-right");
        assertThat(contenido.indexOf("${icono}")).as("el ícono va a la izquierda del texto").isLessThan(contenido.indexOf("${texto}"));
    }

    @Test
    @DisplayName("CP-BRG-02 - Las filas de limpiezas y mantenimientos del panel del operario usan el fragmento sin copiar su marcado y con los textos, íconos y destinos de cada operación")
    void el_panel_del_operario_usa_el_fragmento() throws IOException {
        // Arrange
        String html = leer("home/dashboard-operario.html");

        // Act and Assert
        assertThat(html).doesNotContain("data-boton-registrar");
        assertThat(count(html, "fragments/boton-registrar :: registrar(")).isEqualTo(4);
        assertThat(html).contains("@{/control-equipamiento/{id}/limpiezas/nueva(id=${e.id})}, 'Registrar limpieza', 'bi-stars'")
                .contains("@{/control-barriles/{id}/limpiezas/nueva(id=${b.id})}, 'Registrar limpieza', 'bi-stars'")
                .contains("@{/control-equipamiento/{id}/mantenimientos/nuevo(id=${e.id})}, 'Registrar mantenimiento', 'bi-wrench'")
                .contains("@{/control-barriles/{id}/mantenimientos/nuevo(id=${b.id})}, 'Registrar mantenimiento', 'bi-wrench'");
    }

    private static String leer(String ruta) throws IOException {
        return Files.readString(TEMPLATES.resolve(ruta), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static int count(String texto, String buscado) {
        return texto.split(Pattern.quote(buscado), -1).length - 1;
    }
}
