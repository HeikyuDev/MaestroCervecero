package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda del orden y la agrupación de campos (docs/EstandaresUI.md): en el formulario y en el detalle del proveedor la
 * identificación va primero, el contacto después y la ubicación al final, y la dirección nunca comparte fila con el correo.
 */
class OrdenDeCamposTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-ODC-01 - El detalle del proveedor agrupa identificación, contacto y ubicación, y la dirección va debajo de la ubicación en una fila propia")
    void el_detalle_del_proveedor_agrupa_los_datos() throws IOException {
        // Arrange
        String html = leer("proveedor/proveedor-detalle.html");

        // Act
        int razon = html.indexOf(">Razón social<");
        int cuit = html.indexOf(">CUIT<");
        int telefono = html.indexOf(">Teléfono<");
        int correo = html.indexOf(">Correo electrónico<");
        int pais = html.indexOf(">País<");
        int provincia = html.indexOf(">Provincia<");
        int localidad = html.indexOf(">Localidad<");
        int direccion = html.indexOf(">Dirección<");

        // Assert
        assertThat(razon).isPositive();
        assertThat(razon).isLessThan(cuit);
        assertThat(cuit).isLessThan(telefono);
        assertThat(telefono).isLessThan(correo);
        assertThat(correo).isLessThan(pais);
        assertThat(pais).isLessThan(provincia);
        assertThat(provincia).isLessThan(localidad);
        assertThat(localidad).as("la dirección va después de la ubicación, nunca junto al correo").isLessThan(direccion);
        assertThat(html.substring(html.lastIndexOf("<div", direccion), direccion)).contains("lg:col-span-3");
        assertThat(html.substring(html.lastIndexOf("<div", correo), correo)).contains("lg:col-span-2");
    }

    @Test
    @DisplayName("CP-ODC-02 - El formulario del proveedor sigue el mismo orden que el detalle: identificación, contacto, ubicación y dirección en su propia fila")
    void el_formulario_del_proveedor_sigue_el_orden_del_detalle() throws IOException {
        // Arrange
        String html = leer("proveedor/proveedor-form.html");

        // Act
        int razon = html.indexOf("for=\"razonSocial\"");
        int cuit = html.indexOf("for=\"cuit\"");
        int telefono = html.indexOf("for=\"telefono\"");
        int correo = html.indexOf("for=\"email\"");
        int ubicacion = html.indexOf("fragments/ubicacion-enlazada");
        int direccion = html.indexOf("for=\"direccion\"");

        // Assert
        assertThat(razon).isPositive();
        assertThat(razon).isLessThan(cuit);
        assertThat(cuit).isLessThan(telefono);
        assertThat(telefono).isLessThan(correo);
        assertThat(correo).isLessThan(ubicacion);
        assertThat(ubicacion).as("la dirección va después de la ubicación, nunca junto al correo").isLessThan(direccion);
        assertThat(html.substring(html.lastIndexOf("<div", direccion), direccion)).contains("md:col-span-3");
        assertThat(html.substring(html.lastIndexOf("<div", correo), correo)).contains("md:col-span-2");
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(TEMPLATES.resolve(relativo), StandardCharsets.UTF_8);
    }
}
