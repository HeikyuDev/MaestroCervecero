package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de la ubicación de los formularios: país, provincia y localidad son tres selectores de entidad enlazados
 * (fragments/ubicacion-enlazada), nunca desplegables, y las pantallas del proveedor usan el fragmento.
 */
class UbicacionEnlazadaTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-UEN-01 - El fragmento arma tres selectores de entidad (país, provincia y localidad) con sus buscadores y sin desplegables")
    void el_fragmento_arma_tres_selectores_de_entidad() throws IOException {
        // Arrange
        String html = leer("fragments/ubicacion-enlazada.html");

        // Assert
        assertThat(html).contains("data-ubicacion-enlazada").contains("data-ubicacion=\"pais\"").contains("data-ubicacion=\"provincia\"")
                .contains("data-ubicacion=\"localidad\"");
        assertThat(html).contains("/ubicaciones/paises/buscador").contains("/ubicaciones/provincias/buscador").contains("/ubicaciones/localidades/buscador");
        assertThat(html.split("fragments/selector-entidad :: selector\\(", -1)).as("tres selectores de entidad").hasSize(4);
        assertThat(html).doesNotContain("<select");
    }

    @Test
    @DisplayName("CP-UEN-02 - El formulario y el listado de proveedores usan el fragmento y su script, sin la versión con desplegables")
    void las_pantallas_del_proveedor_usan_el_fragmento() throws IOException {
        for (String plantilla : new String[]{"proveedor/proveedor-form.html", "proveedor/proveedor-lista.html"}) {
            // Arrange
            String html = leer(plantilla);

            // Assert
            assertThat(html).as(plantilla).contains("fragments/ubicacion-enlazada :: ubicacion(").contains("/js/ubicacion-enlazada.js")
                    .doesNotContain("ubicacion-cascada").doesNotContain("<select data-ubicacion");
        }
    }

    @Test
    @DisplayName("CP-UEN-03 - Los botones Elegir de los buscadores exponen el nombre y los datos del padre que usa el autocompletado")
    void los_buscadores_exponen_los_datos_del_padre() throws IOException {
        // Assert
        assertThat(leer("ubicacion/buscador-pais.html")).contains("th:data-nombre=");
        assertThat(leer("ubicacion/buscador-provincia.html")).contains("th:data-nombre=").contains("th:data-id-pais=").contains("th:data-texto-pais=");
        assertThat(leer("ubicacion/buscador-localidad.html")).contains("th:data-nombre=").contains("th:data-id-provincia=")
                .contains("th:data-texto-provincia=").contains("th:data-id-pais=").contains("th:data-texto-pais=");
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(TEMPLATES.resolve(relativo), StandardCharsets.UTF_8);
    }
}
