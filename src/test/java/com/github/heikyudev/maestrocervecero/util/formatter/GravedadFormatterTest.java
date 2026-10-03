package com.github.heikyudev.maestrocervecero.util.formatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GravedadFormatterTest {

    private final GravedadFormatter formatter = new GravedadFormatter();

    @Test
    @DisplayName("CP-GRF-01: parse convierte un texto de exactamente cuatro dígitos en la densidad equivalente")
    void parse_debeConvertirCuatroDigitosEnDensidad() throws ParseException {
        assertThat(formatter.parse("1050", Locale.getDefault())).isEqualTo(1.05);
        assertThat(formatter.parse("1010", Locale.getDefault())).isEqualTo(1.01);
        assertThat(formatter.parse("1000", Locale.getDefault())).isEqualTo(1.0);
        assertThat(formatter.parse("1120", Locale.getDefault())).isEqualTo(1.12);
        assertThat(formatter.parse("1055", Locale.getDefault())).isEqualTo(1.055);
    }

    @Test
    @DisplayName("CP-GRF-02: parse rechaza con ParseException todo texto que no tenga exactamente cuatro dígitos")
    void parse_debeRechazarTextosQueNoSonCuatroDigitos() {
        for (String invalido : new String[]{"105", "10500", "1,050", "1.050", "abcd", "10a0", " 1050", "1050 ", "-105", ""}) {
            assertThatThrownBy(() -> formatter.parse(invalido, Locale.getDefault()))
                    .as("texto: '%s'", invalido)
                    .isInstanceOf(ParseException.class);
        }
        assertThatThrownBy(() -> formatter.parse(null, Locale.getDefault()))
                .isInstanceOf(ParseException.class);
    }

    @Test
    @DisplayName("CP-GRF-03: print muestra la densidad como texto de cuatro dígitos")
    void print_debeMostrarLaDensidadConCuatroDigitos() {
        assertThat(formatter.print(1.05, Locale.getDefault())).isEqualTo("1050");
        assertThat(formatter.print(1.01, Locale.getDefault())).isEqualTo("1010");
        assertThat(formatter.print(1.0, Locale.getDefault())).isEqualTo("1000");
        assertThat(formatter.print(1.055, Locale.getDefault())).isEqualTo("1055");
    }

    @Test
    @DisplayName("CP-GRF-04: print devuelve una cadena vacía cuando la densidad es nula")
    void print_debeDevolverVacioConDensidadNula() {
        assertThat(formatter.print(null, Locale.getDefault())).isEmpty();
    }

    @Test
    @DisplayName("CP-GRF-05: parse y print son inversos entre sí para todas las densidades de cuatro dígitos")
    void parseYPrint_debenSerInversos() throws ParseException {
        for (int milesimas = 1000; milesimas <= 1200; milesimas++) {
            String texto = String.valueOf(milesimas);
            assertThat(formatter.print(formatter.parse(texto, Locale.getDefault()), Locale.getDefault())).isEqualTo(texto);
        }
    }
}
