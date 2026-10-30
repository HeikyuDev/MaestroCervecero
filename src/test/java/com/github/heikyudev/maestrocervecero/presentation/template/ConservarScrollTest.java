package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda del script que conserva el scroll del sidebar y de la página al cambiar de pestaña: el layout lo incluye al
 * final del body y el script usa las claves y los selectores que documenta docs/EstandaresUI.md.
 */
class ConservarScrollTest {

    private static final Path RECURSOS = Path.of("").toAbsolutePath().resolve("src/main/resources");

    @Test
    @DisplayName("CP-CSC-01 - El layout base incluye conservar-scroll.js al final del body, después del contenido")
    void el_layout_incluye_el_script_al_final() throws IOException {
        // Arrange
        String base = leer("templates/layout/base.html");

        // Act
        int script = base.indexOf("/js/conservar-scroll.js");
        int main = base.indexOf("</main>");
        int cierre = base.indexOf("</body>");

        // Assert
        assertThat(script).as("el script está incluido").isPositive();
        assertThat(script).isGreaterThan(main).isLessThan(cierre);
        assertThat(base.substring(script - 40, script + 40)).doesNotContain("defer");
        assertThat(leer("templates/fragments/sidebar.html")).contains("sidebar-scroll").contains("overflow-y-auto");
    }

    @Test
    @DisplayName("CP-CSC-02 - El script guarda el scroll del sidebar y el de las pestañas con sus claves, siempre con try/catch, y solo restaura el de la pestaña si la ruta coincide")
    void el_script_usa_las_claves_y_los_selectores_documentados() throws IOException {
        // Arrange
        String js = leer("static/js/conservar-scroll.js");

        // Assert
        assertThat(js).contains("'scroll-sidebar'").contains("'scroll-pestana'").contains("aside.sidebar-scroll").contains("pagehide");
        assertThat(js).contains("nav.border-b[aria-label] a").contains("nav[data-pestanas] a");
        assertThat(js).contains("dato.ruta === window.location.pathname");
        assertThat(js).contains("sessionStorage.getItem").contains("sessionStorage.setItem").contains("sessionStorage.removeItem");
        assertThat(js.split("catch [(]e[)]", -1).length - 1).isGreaterThanOrEqualTo(4);
        assertThat(leer("templates/fragments/pestanas.html")).contains("border-b border-slate-200");
        assertThat(leer("templates/lote/lote-detalle.html")).contains("data-pestanas>");
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(RECURSOS.resolve(relativo), StandardCharsets.UTF_8);
    }
}
