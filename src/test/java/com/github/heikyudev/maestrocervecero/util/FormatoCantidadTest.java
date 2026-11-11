package com.github.heikyudev.maestrocervecero.util;

import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoCantidadTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-FC-01: formatear escribe punto de miles y coma decimal tanto en enteros como en decimales, sin ceros sobrantes")
    void formatear_debeEscribirMilesYComaDecimal() {
        assertThat(FormatoCantidad.formatear(2000.0)).isEqualTo("2.000");
        assertThat(FormatoCantidad.formatear(1847.3685)).isEqualTo("1.847,3685");
        assertThat(FormatoCantidad.formatear(11.655)).isEqualTo("11,655");
        assertThat(FormatoCantidad.formatear(62.0)).isEqualTo("62");
        assertThat(FormatoCantidad.formatear(4.5)).isEqualTo("4,5");
        assertThat(FormatoCantidad.formatear(1500000)).isEqualTo("1.500.000");
        assertThat(FormatoCantidad.formatear(new BigDecimal("1000.2500"))).isEqualTo("1.000,25");
    }

    @Test
    @DisplayName("CP-FC-02: formatear conserva hasta 4 decimales y redondea el resto hacia arriba")
    void formatear_debeConservarHastaCuatroDecimales() {
        assertThat(FormatoCantidad.formatear(0.0001)).isEqualTo("0,0001");
        assertThat(FormatoCantidad.formatear(0.12345)).isEqualTo("0,1235");
        assertThat(FormatoCantidad.formatear(0.00004)).isEqualTo("0");
    }

    @Test
    @DisplayName("CP-FC-03: formatear escribe un guion cuando no hay valor y conUnidad agrega la unidad")
    void formatear_debeEscribirGuionConNuloYConUnidadAgregarLaUnidad() {
        assertThat(FormatoCantidad.formatear(null)).isEqualTo("-");
        assertThat(FormatoCantidad.conUnidad(1847.3685, "g")).isEqualTo("1.847,3685 g");
        assertThat(FormatoCantidad.conUnidad(2000.0, "g")).isEqualTo("2.000 g");
    }

    @Test
    @DisplayName("CP-FC-04: los mensajes usan el mismo formato que las tablas (11,655 y no 11,65)")
    void formatearNumero_debeCoincidirConLasTablas() {
        assertThat(ValidacionesNumericas.formatearNumero(11.655)).isEqualTo("11,655");
        assertThat(ValidacionesNumericas.formatearNumero(12.655)).isEqualTo("12,655");
        assertThat(ValidacionesNumericas.formatearNumero(1250.5)).isEqualTo("1.250,5");
    }

    @Test
    @DisplayName("CP-FC-05: ninguna plantilla arma a mano el formato de una cantidad con reemplazo del punto decimal")
    void plantillas_noDebenArmarElFormatoAMano() throws IOException {
        List<String> infractoras;
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            infractoras = archivos.filter(p -> p.toString().endsWith(".html")).filter(p -> leer(p).contains("'.', ','")).map(p -> p.getFileName().toString()).toList();
        }
        assertThat(infractoras).isEmpty();
        assertThat(leer(TEMPLATES.resolve("receta/formato.html"))).contains("#cantidades.formatear(valor)").contains("#cantidades.conUnidad(valor, unidad)");
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
