package com.github.heikyudev.maestrocervecero.configuration.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionUrlBaseProduccionTest {

    @Test
    @DisplayName("CP-VUB-01: una dirección que empieza con https:// es aceptada")
    void urlHttpsEsAceptada() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> new ValidacionUrlBaseProduccion("https://maestrocervecero.com")).doesNotThrowAnyException();
        assertThatCode(() -> new ValidacionUrlBaseProduccion("HTTPS://maestrocervecero.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VUB-02: una dirección http:// impide arrancar con un mensaje que indica la propiedad, la variable y el valor")
    void urlHttpEsRechazada() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> new ValidacionUrlBaseProduccion("http://localhost:7777"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La propiedad app.url-base (variable de entorno APP_URL_BASE) debe empezar con https:// en producción. "
                        + "Valor actual: 'http://localhost:7777'");
    }

    @Test
    @DisplayName("CP-VUB-03: una dirección nula o en blanco impide arrancar")
    void urlNulaOEnBlancoEsRechazada() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> new ValidacionUrlBaseProduccion(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ValidacionUrlBaseProduccion("   ")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("CP-VUB-04: la validación solo se registra en el perfil prod")
    void soloEnPerfilProd() {
        // === EJECUCION Y ASSERTS ===
        assertThat(ValidacionUrlBaseProduccion.class.getAnnotation(Profile.class).value()).containsExactly("prod");
    }
}
