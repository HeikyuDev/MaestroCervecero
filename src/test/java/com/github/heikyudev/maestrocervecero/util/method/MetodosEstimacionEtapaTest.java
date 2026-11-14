package com.github.heikyudev.maestrocervecero.util.method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MetodosEstimacionEtapaTest {

    @Test
    @DisplayName("CP-MEE-01: minutosDeMolienda divide la malta por el rendimiento del molino y lo expresa en minutos")
    void minutosDeMolienda_dividePorElRendimiento() {
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(45.0, 30.0)).isEqualTo(90L);
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(100.0, 40.0)).isEqualTo(150L);
    }

    @Test
    @DisplayName("CP-MEE-02: minutosDeMolienda redondea hacia arriba al minuto, sin sumar uno por el error de coma flotante")
    void minutosDeMolienda_redondeaHaciaArriba() {
        // 4,3952 / 5 * 60 = 52,7424 minutos
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(4.3952, 5.0)).isEqualTo(53L);
        // 1,1 / 0,1 * 60 vale 660,0000000000001 en coma flotante, pero son 660 minutos exactos
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(1.1, 0.1)).isEqualTo(660L);
    }

    @Test
    @DisplayName("CP-MEE-03: minutosDeMolienda nunca devuelve menos de un minuto para una cantidad positiva")
    void minutosDeMolienda_minimoUnMinuto() {
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(0.001, 100.0)).isEqualTo(1L);
    }

    @Test
    @DisplayName("CP-MEE-04: minutosDeMolienda devuelve null con la malta o el rendimiento nulos, no positivos o no finitos")
    void minutosDeMolienda_datosInvalidosDevuelveNull() {
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(null, 30.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(45.0, null)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(0.0, 30.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(-1.0, 30.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(45.0, 0.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(45.0, -5.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(Double.NaN, 30.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeMolienda(Double.POSITIVE_INFINITY, 30.0)).isNull();
    }

    @Test
    @DisplayName("CP-MEE-05: minutosDeEnvasado divide el volumen por la velocidad de envasado y devuelve null con datos inválidos")
    void minutosDeEnvasado_dividePorLaVelocidad() {
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(300.0, 120.0)).isEqualTo(150L);
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(20.0, 10.0)).isEqualTo(120L);
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(100.0, 30.0)).isEqualTo(200L);
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(300.0, 0.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(null, 120.0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeEnvasado(300.0, null)).isNull();
    }

    @Test
    @DisplayName("CP-MEE-06: minutosDeReceta conserva los minutos y minutosDeDias los convierte, ambos null si el dato falta o no es positivo")
    void duracionesDeLaReceta() {
        assertThat(MetodosEstimacionEtapa.minutosDeReceta(60)).isEqualTo(60L);
        assertThat(MetodosEstimacionEtapa.minutosDeReceta(90)).isEqualTo(90L);
        assertThat(MetodosEstimacionEtapa.minutosDeReceta(0)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeReceta(null)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeDias(1)).isEqualTo(1_440L);
        assertThat(MetodosEstimacionEtapa.minutosDeDias(10)).isEqualTo(14_400L);
        assertThat(MetodosEstimacionEtapa.minutosDeDias(-1)).isNull();
        assertThat(MetodosEstimacionEtapa.minutosDeDias(null)).isNull();
    }

    @Test
    @DisplayName("CP-MEE-07: calcularFinPrevisto suma la duración al inicio real, también cuando la etapa dura días, y devuelve null si falta un dato")
    void calcularFinPrevisto_sumaLaDuracion() {
        LocalDateTime inicio = LocalDateTime.of(2027, 7, 1, 8, 0);

        assertThat(MetodosEstimacionEtapa.calcularFinPrevisto(inicio, 90L)).isEqualTo(LocalDateTime.of(2027, 7, 1, 9, 30));
        assertThat(MetodosEstimacionEtapa.calcularFinPrevisto(inicio, 14_400L)).isEqualTo(LocalDateTime.of(2027, 7, 11, 8, 0));
        assertThat(MetodosEstimacionEtapa.calcularFinPrevisto(null, 90L)).isNull();
        assertThat(MetodosEstimacionEtapa.calcularFinPrevisto(inicio, null)).isNull();
    }

    @Test
    @DisplayName("CP-MEE-09: formatearDuracion redacta minutos, horas, horas y minutos, y días completos con su singular y plural")
    void formatearDuracion_redactaLaDuracion() {
        assertThat(MetodosEstimacionEtapa.formatearDuracion(0L)).isEqualTo("0 min");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(45L)).isEqualTo("45 min");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(60L)).isEqualTo("1 h");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(90L)).isEqualTo("1 h 30 min");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(150L)).isEqualTo("2 h 30 min");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(1_440L)).isEqualTo("1 día");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(14_400L)).isEqualTo("10 días");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(1_500L)).isEqualTo("25 h");
        assertThat(MetodosEstimacionEtapa.formatearDuracion(1_530L)).isEqualTo("25 h 30 min");
    }

    @Test
    @DisplayName("CP-MEE-10: formatearDuracion devuelve null con una duración nula o negativa")
    void formatearDuracion_nulaONegativaDevuelveNull() {
        assertThat(MetodosEstimacionEtapa.formatearDuracion(null)).isNull();
        assertThat(MetodosEstimacionEtapa.formatearDuracion(-5L)).isNull();
    }
}
