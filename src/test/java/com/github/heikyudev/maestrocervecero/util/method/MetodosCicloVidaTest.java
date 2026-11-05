package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetodosCicloVidaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Test
    @DisplayName("CP-MCV-01: validarFechaPosteriorAUltimaOperacion rechaza una fecha igual o anterior a alguna existente y deja el campo en nulo si no se indica")
    void validarFechaPosterior_sinCampoDebeRechazarSinCampo() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(FECHA, "barril", FECHA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isNull());
        assertThatThrownBy(() -> MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(FECHA, "barril", FECHA.plusMinutes(1)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-MCV-02: validarFechaPosteriorAUltimaOperacion con campo rechaza indicando ese campo, y acepta una fecha estrictamente posterior")
    void validarFechaPosterior_conCampoDebeIndicarElCampo() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(FECHA, "barril", "fechaEnvasado", FECHA.minusMinutes(5), FECHA))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fechaEnvasado"));
        assertThatCode(() -> MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(FECHA, "barril", "fechaEnvasado", FECHA.minusMinutes(1), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-03: validarFechaNoFutura rechaza una fecha futura indicando el campo informado y acepta una fecha pasada")
    void validarFechaNoFutura_debeIndicarElCampo() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVida.validarFechaNoFutura(LocalDateTime.now().plusDays(1), "fechaEnvasado"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fechaEnvasado"));
        assertThatThrownBy(() -> MetodosCicloVida.validarFechaNoFutura(LocalDateTime.now().plusDays(1)))
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isNull());
        assertThatCode(() -> MetodosCicloVida.validarFechaNoFutura(FECHA, "fechaEnvasado")).doesNotThrowAnyException();
    }
}
