package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de los importes: toda pantalla muestra el importe con el símbolo de pesos delante (FormatoImporte.conSimbolo)
 * y los encabezados no repiten la unidad ($) que ya lleva el valor.
 */
class ImporteConSimboloTest {

    private static final Path RECURSOS = Path.of("").toAbsolutePath().resolve("src/main/resources");

    @Test
    @DisplayName("CP-IMS-01 - Ninguna plantilla muestra un importe sin símbolo: se usa conSimbolo y nunca formatear")
    void ninguna_plantilla_usa_formatear() throws IOException {
        // Arrange
        List<String> infractoras;
        try (Stream<Path> archivos = Files.walk(RECURSOS.resolve("templates"))) {
            infractoras = archivos.filter(p -> p.toString().endsWith(".html")).filter(p -> leer(p).contains("FormatoImporte).formatear(")).map(p -> p.getFileName().toString()).toList();
        }

        // Assert
        assertThat(infractoras).as("plantillas que muestran importes sin símbolo").isEmpty();
        assertThat(leer(RECURSOS.resolve("templates/lote/lote-informacion.html"))).contains("FormatoImporte).conSimbolo(");
    }

    @Test
    @DisplayName("CP-IMS-02 - Los encabezados de importes no repiten la unidad ($) y el cálculo de la orden de compra escribe los importes con el símbolo")
    void los_encabezados_no_repiten_la_unidad() throws IOException {
        // Arrange
        String costos = leer(RECURSOS.resolve("templates/lote/lote-informacion.html"));
        String ordenDetalle = leer(RECURSOS.resolve("templates/orden_compra/orden-detalle.html"));
        String ingresos = leer(RECURSOS.resolve("templates/ingreso_insumo/ingreso-lista.html"));
        String costosDirectos = leer(RECURSOS.resolve("templates/configuracion/costos-directos-lista.html"));
        String js = leer(RECURSOS.resolve("static/js/orden-compra-form.js"));

        // Assert
        assertThat(costos + ordenDetalle + ingresos + costosDirectos).doesNotContain("($)").doesNotContain("($ por litro)");
        assertThat(js).contains("return '$ ' + entera");
    }

    private static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
