package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Test
    @DisplayName("CP-MCV-04: validarObservaciones devuelve las observaciones normalizadas y rechaza nulas, en blanco o de más de 255 caracteres indicando el campo observaciones")
    void validarObservaciones_debeNormalizarYRechazar() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVida.validarObservaciones("  Limpieza   CIP  ")).isEqualTo("Limpieza CIP");
        assertThatCode(() -> MetodosCicloVida.validarObservaciones("a".repeat(255))).doesNotThrowAnyException();
        for (String invalida : new String[]{null, "   "}) {
            assertThatThrownBy(() -> MetodosCicloVida.validarObservaciones(invalida))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Las observaciones son obligatorias")
                    .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));
        }
        assertThatThrownBy(() -> MetodosCicloVida.validarObservaciones("a".repeat(256)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));
    }

    @Test
    @DisplayName("CP-MCV-05: validarMotivoAnulacion devuelve el motivo normalizado y rechaza nulo, en blanco o de más de 255 caracteres indicando el campo motivoAnulacion")
    void validarMotivoAnulacion_debeNormalizarYRechazar() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVida.validarMotivoAnulacion("  Cargada   por error ")).isEqualTo("Cargada por error");
        assertThatCode(() -> MetodosCicloVida.validarMotivoAnulacion("a".repeat(255))).doesNotThrowAnyException();
        for (String invalido : new String[]{null, "   "}) {
            assertThatThrownBy(() -> MetodosCicloVida.validarMotivoAnulacion(invalido))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El motivo de anulación es obligatorio")
                    .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));
        }
        assertThatThrownBy(() -> MetodosCicloVida.validarMotivoAnulacion("a".repeat(256)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));
    }

    @Test
    @DisplayName("CP-MCV-06: validarEsLaUltimaOperacion acepta la misma operación, rechaza si la última es otra y falla cerrado con IllegalStateException si no hay última")
    void validarEsLaUltimaOperacion_debeCompararPorId() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVida.validarEsLaUltimaOperacion(7L, 7L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> MetodosCicloVida.validarEsLaUltimaOperacion(7L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 7");
        assertThatThrownBy(() -> MetodosCicloVida.validarEsLaUltimaOperacion(7L, 8L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");
        assertThatThrownBy(() -> MetodosCicloVida.validarEsLaUltimaOperacion(8L, 7L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-MCV-07: esPosteriorAlUltimoEnvasado es verdadero sin envasado o con una operación estrictamente posterior, y falso con una operación anterior o de igual fecha")
    void esPosteriorAlUltimoEnvasado_debeSerEstricto() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVida.esPosteriorAlUltimoEnvasado(FECHA, null)).isTrue();
        assertThat(MetodosCicloVida.esPosteriorAlUltimoEnvasado(FECHA, FECHA.minusMinutes(1))).isTrue();
        assertThat(MetodosCicloVida.esPosteriorAlUltimoEnvasado(FECHA, FECHA)).isFalse();
        assertThat(MetodosCicloVida.esPosteriorAlUltimoEnvasado(FECHA, FECHA.plusMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("CP-MCV-08: validarPosteriorAlUltimoEnvasado rechaza una operación anterior o de igual fecha que el último envasado y acepta el resto")
    void validarPosteriorAlUltimoEnvasado_debeRechazarSiElEnvasadoEsPosteriorOIgual() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVida.validarPosteriorAlUltimoEnvasado(FECHA, FECHA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");
        assertThatThrownBy(() -> MetodosCicloVida.validarPosteriorAlUltimoEnvasado(FECHA, FECHA.plusDays(1)))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatCode(() -> MetodosCicloVida.validarPosteriorAlUltimoEnvasado(FECHA, FECHA.minusMinutes(1))).doesNotThrowAnyException();
        assertThatCode(() -> MetodosCicloVida.validarPosteriorAlUltimoEnvasado(FECHA, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-09: estaEnUso es verdadero solo para EN_USO y validarNoEstaEnUso rechaza únicamente ese estado")
    void estaEnUso_yValidarNoEstaEnUso_debenDistinguirElEstadoEnUso() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVida.estaEnUso(EstadoOperativoBarril.EN_USO)).isTrue();
        for (EstadoOperativoBarril estado : EstadoOperativoBarril.values()) {
            if (estado != EstadoOperativoBarril.EN_USO) {
                assertThat(MetodosCicloVida.estaEnUso(estado)).isFalse();
                assertThatCode(() -> MetodosCicloVida.validarNoEstaEnUso(estado)).doesNotThrowAnyException();
            }
        }
        assertThatThrownBy(() -> MetodosCicloVida.validarNoEstaEnUso(EstadoOperativoBarril.EN_USO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");
    }

    @Test
    @DisplayName("CP-MCV-10: resolverBarril devuelve el barril encontrado y lanza RecursoNoEncontradoException con campo idBarril si no existe")
    void resolverBarril_debeDevolverOLanzar() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = BarrilEntity.builder().id(3L).build();

        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVida.resolverBarril(Optional.of(barril), 3L)).isSameAs(barril);
        assertThatThrownBy(() -> MetodosCicloVida.resolverBarril(Optional.empty(), 99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));
    }
}
