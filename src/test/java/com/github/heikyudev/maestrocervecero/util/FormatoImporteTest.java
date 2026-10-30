package com.github.heikyudev.maestrocervecero.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoImporteTest {

    @Test
    @DisplayName("CP-FI-01: formatear escribe punto de miles, coma decimal y al menos dos decimales")
    void formatear_debeEscribirMilesYDosDecimalesComoMinimo() {
        assertThat(FormatoImporte.formatear(new BigDecimal("1250.5"))).isEqualTo("1.250,50");
        assertThat(FormatoImporte.formatear(new BigDecimal("1000000"))).isEqualTo("1.000.000,00");
        assertThat(FormatoImporte.formatear(BigDecimal.ZERO)).isEqualTo("0,00");
    }

    @Test
    @DisplayName("CP-FI-02: formatear conserva de 2 a 4 decimales y redondea el resto hacia arriba")
    void formatear_debeConservarHastaCuatroDecimales() {
        assertThat(FormatoImporte.formatear(new BigDecimal("0.1234"))).isEqualTo("0,1234");
        assertThat(FormatoImporte.formatear(new BigDecimal("12.30"))).isEqualTo("12,30");
        assertThat(FormatoImporte.formatear(new BigDecimal("0.123"))).isEqualTo("0,123");
        assertThat(FormatoImporte.formatear(new BigDecimal("0.12345"))).isEqualTo("0,1235");
    }

    @Test
    @DisplayName("CP-FI-03: formatear escribe un guion cuando el importe es nulo")
    void formatear_debeEscribirGuionConImporteNulo() {
        assertThat(FormatoImporte.formatear(null)).isEqualTo("-");
    }

    @Test
    @DisplayName("CP-FI-04: conSimbolo antepone el símbolo de pesos y un espacio al importe formateado, y escribe un guion si es nulo")
    void conSimbolo_debeAnteponerElSimbolo() {
        // Arrange + Act + Assert
        assertThat(FormatoImporte.conSimbolo(new BigDecimal("23460"))).isEqualTo("$ 23.460,00");
        assertThat(FormatoImporte.conSimbolo(new BigDecimal("0.1234"))).isEqualTo("$ 0,1234");
        assertThat(FormatoImporte.conSimbolo(null)).isEqualTo("-");
    }
}
