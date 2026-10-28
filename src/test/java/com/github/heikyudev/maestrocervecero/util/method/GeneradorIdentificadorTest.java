package com.github.heikyudev.maestrocervecero.util.method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorIdentificadorTest {

    @Test
    @DisplayName("CP-GI-01: generar completa con ceros a la izquierda hasta tener dos dígitos como mínimo")
    void generar_debeCompletarConCerosHastaDosDigitos() {
        // Act + Assert
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_MOLINO, 1)).isEqualTo("MOL-01");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_MACERADOR, 9)).isEqualTo("MAC-09");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_OLLA_HERVOR, 10)).isEqualTo("OLL-10");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_FERMENTADOR, 99)).isEqualTo("FER-99");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_BARRIL, 5)).isEqualTo("BRL-05");
    }

    @Test
    @DisplayName("CP-GI-02: generar no trunca el número cuando supera los dos dígitos (99 pasa a 100)")
    void generar_noDebeTruncarNumerosDeTresOMasDigitos() {
        // Act + Assert
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_MOLINO, 99)).isEqualTo("MOL-99");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_MOLINO, 100)).isEqualTo("MOL-100");
        assertThat(GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_BARRIL, 12345)).isEqualTo("BRL-12345");
    }

    @Test
    @DisplayName("CP-GI-03: generar rechaza un número menor o igual a 0")
    void generar_debeRechazarNumerosNoPositivos() {
        // Act + Assert
        assertThatThrownBy(() -> GeneradorIdentificador.generar("MOL", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El número del identificador debe ser mayor a 0");
        assertThatThrownBy(() -> GeneradorIdentificador.generar("MOL", -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CP-GI-04: generar rechaza un prefijo nulo o en blanco")
    void generar_debeRechazarPrefijoVacio() {
        // Act + Assert
        assertThatThrownBy(() -> GeneradorIdentificador.generar(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El prefijo del identificador es obligatorio");
        assertThatThrownBy(() -> GeneradorIdentificador.generar("  ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
