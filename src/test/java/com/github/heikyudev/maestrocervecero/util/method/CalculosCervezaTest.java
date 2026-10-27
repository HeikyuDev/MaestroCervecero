package com.github.heikyudev.maestrocervecero.util.method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculosCervezaTest {

    @Test
    @DisplayName("CP-CC-01: calcularAbv aplica (OG - FG) x 131,25 y redondea a un decimal hacia arriba en la mitad")
    void calcularAbv_debeRedondearLaMitadHaciaArriba() {
        // Arrange
        Double og = 1.05;
        Double fg = 1.01;

        // Act
        Double abv = CalculosCerveza.calcularAbv(og, fg);

        // Assert
        assertThat(abv).isEqualTo(5.3);
    }

    @Test
    @DisplayName("CP-CC-02: calcularAbv no arrastra errores de coma flotante en las restas de densidades")
    void calcularAbv_noDebeArrastrarErroresDeComaFlotante() {
        // Arrange: en coma flotante 1.065 - 1.012 da 0.05299999999999994
        // Act y Assert
        assertThat(CalculosCerveza.calcularAbv(1.065, 1.012)).isEqualTo(7.0);
        assertThat(CalculosCerveza.calcularAbv(1.05, 1.01)).isEqualTo(5.3);
        assertThat(CalculosCerveza.calcularAbv(1.04, 1.008)).isEqualTo(4.2);
    }

    @Test
    @DisplayName("CP-CC-03: calcularAbv devuelve un único decimal y sube a 5,0 un valor como 4,9875")
    void calcularAbv_debeDevolverUnSoloDecimal() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAbv(1.048, 1.010)).isEqualTo(5.0);
        assertThat(CalculosCerveza.calcularAbv(1.046, 1.010)).isEqualTo(4.7);
        assertThat(CalculosCerveza.calcularAbv(1.05, 1.0)).isEqualTo(6.6);
    }

    @Test
    @DisplayName("CP-CC-04: calcularAbv devuelve null si falta la OG o la FG")
    void calcularAbv_debeDevolverNullSiFaltaUnaDensidad() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAbv(null, 1.01)).isNull();
        assertThat(CalculosCerveza.calcularAbv(1.05, null)).isNull();
        assertThat(CalculosCerveza.calcularAbv(null, null)).isNull();
    }

    @Test
    @DisplayName("CP-CC-05: calcularAbv devuelve null si la OG es igual o menor que la FG")
    void calcularAbv_debeDevolverNullSiLaOgNoSuperaALaFg() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAbv(1.01, 1.01)).isNull();
        assertThat(CalculosCerveza.calcularAbv(1.01, 1.05)).isNull();
    }

    @Test
    @DisplayName("CP-CC-06: calcularAbv calcula valores grandes sin perder precisión")
    void calcularAbv_debeCalcularValoresGrandes() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAbv(1.12, 1.0)).isEqualTo(15.8);
        assertThat(CalculosCerveza.calcularAbv(2.0, 1.0)).isEqualTo(131.3);
    }

    @Test
    @DisplayName("CP-CC-07: calcularAlcoholLitros multiplica el volumen por el ABV sobre 100 con un decimal hacia arriba en la mitad")
    void calcularAlcoholLitros_debeCalcularLosLitrosConUnDecimal() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAlcoholLitros(5.3, 20.0)).isEqualTo(1.1);
        assertThat(CalculosCerveza.calcularAlcoholLitros(5.0, 300.0)).isEqualTo(15.0);
        assertThat(CalculosCerveza.calcularAlcoholLitros(4.5, 10.0)).isEqualTo(0.5);
        assertThat(CalculosCerveza.calcularAlcoholLitros(6.5, 10.5)).isEqualTo(0.7);
    }

    @Test
    @DisplayName("CP-CC-08: calcularAlcoholLitros devuelve null si falta el ABV o el volumen")
    void calcularAlcoholLitros_debeDevolverNullSiFaltanDatos() {
        // Act y Assert
        assertThat(CalculosCerveza.calcularAlcoholLitros(null, 20.0)).isNull();
        assertThat(CalculosCerveza.calcularAlcoholLitros(5.3, null)).isNull();
        assertThat(CalculosCerveza.calcularAlcoholLitros(null, null)).isNull();
    }
}
