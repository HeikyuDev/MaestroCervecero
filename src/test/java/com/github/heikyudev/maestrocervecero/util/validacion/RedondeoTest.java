package com.github.heikyudev.maestrocervecero.util.validacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedondeoTest {

    @Test
    @DisplayName("CP-RDN-01: redondear elimina el residuo de punto flotante de una suma decimal (0,1 + 0,2)")
    void redondear_debeEliminarElResiduoDeUnaSumaDecimal() {
        // Arrange
        double sumaConResiduo = 0.1 + 0.2;

        // Act
        double resultado = Redondeo.redondear(sumaConResiduo);

        // Assert
        assertThat(sumaConResiduo).isNotEqualTo(0.3);
        assertThat(resultado).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-RDN-02: redondear usa 4 decimales con redondeo hacia arriba en la mitad (HALF_UP)")
    void redondear_debeUsarCuatroDecimalesConHalfUp() {
        assertThat(Redondeo.redondear(1.00005)).isEqualTo(1.0001);
        assertThat(Redondeo.redondear(2.00004)).isEqualTo(2.0);
        assertThat(Redondeo.redondear(12.345678)).isEqualTo(12.3457);
        assertThat(LimitesTecnicos.ESCALA_CANTIDADES).isEqualTo(4);
    }

    @Test
    @DisplayName("CP-RDN-03: redondear aplica la misma regla a los valores negativos")
    void redondear_debeAplicarLaMismaReglaALosNegativos() {
        assertThat(Redondeo.redondear(-1.00005)).isEqualTo(-1.0001);
        assertThat(Redondeo.redondear(-0.30000000000000004)).isEqualTo(-0.3);
    }

    @Test
    @DisplayName("CP-RDN-04: redondear deja sin cambios un valor que ya tiene 4 decimales o menos")
    void redondear_noDebeAlterarUnValorExacto() {
        assertThat(Redondeo.redondear(89.9)).isEqualTo(89.9);
        assertThat(Redondeo.redondear(100.1)).isEqualTo(100.1);
        assertThat(Redondeo.redondear(0.0)).isEqualTo(0.0);
        assertThat(Redondeo.redondear(1_000_000.0)).isEqualTo(1_000_000.0);
    }

    @Test
    @DisplayName("CP-RDN-05: redondear convierte en cero un residuo de 1e-16")
    void redondear_debeConvertirUnResiduoMinimoEnCero() {
        assertThat(Redondeo.redondear(1e-16)).isEqualTo(0.0);
        assertThat(Redondeo.redondear(0.3 - 0.30000000000000004)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-RDN-06: redondear devuelve sin cambios los valores no finitos (NaN e infinitos)")
    void redondear_debeDevolverSinCambiosLosValoresNoFinitos() {
        assertThat(Redondeo.redondear(Double.NaN)).isNaN();
        assertThat(Redondeo.redondear(Double.POSITIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(Redondeo.redondear(Double.NEGATIVE_INFINITY)).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    @DisplayName("CP-RDN-07: acumulados decimales redondeados se comparan sin falsos excesos (10,2 + 89,9 contra 100,1)")
    void redondear_debePermitirCompararAcumuladosDecimales() {
        // Arrange
        double acumulado = 10.2 + 89.9;

        // Act & Assert
        assertThat(acumulado).isGreaterThan(100.1);
        assertThat(Redondeo.redondear(acumulado)).isEqualTo(Redondeo.redondear(100.1));
    }
}
