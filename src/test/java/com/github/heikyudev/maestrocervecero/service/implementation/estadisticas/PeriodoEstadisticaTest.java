package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoEstadisticaTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);

    @Test
    @DisplayName("CP-ESTP-01: validar devuelve el período cuando tiene los dos extremos en orden, también si es de un solo día")
    void validar_periodoValido() {
        assertThat(PeriodoEstadistica.validar(DESDE, LocalDate.of(2026, 11, 30)))
                .isEqualTo(new PeriodoEstadistica(DESDE, LocalDate.of(2026, 11, 30)));
        assertThat(PeriodoEstadistica.validar(DESDE, DESDE)).isEqualTo(new PeriodoEstadistica(DESDE, DESDE));
    }

    @Test
    @DisplayName("CP-ESTP-02: validar rechaza el período sin fecha desde")
    void validar_sinDesde() {
        assertThatThrownBy(() -> PeriodoEstadistica.validar(null, DESDE))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La fecha desde es obligatoria");
    }

    @Test
    @DisplayName("CP-ESTP-03: validar rechaza el período sin fecha hasta")
    void validar_sinHasta() {
        assertThatThrownBy(() -> PeriodoEstadistica.validar(DESDE, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La fecha hasta es obligatoria");
    }

    @Test
    @DisplayName("CP-ESTP-04: validar rechaza el período con la fecha hasta anterior a la fecha desde")
    void validar_periodoInvertido() {
        assertThatThrownBy(() -> PeriodoEstadistica.validar(DESDE, DESDE.minusDays(1)))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La fecha hasta no puede ser anterior a la fecha desde");
    }

    @Test
    @DisplayName("CP-ESTP-05: validar acepta un período de 366 días contando ambos extremos (un año bisiesto completo)")
    void validar_rangoMaximo() {
        LocalDate desde = LocalDate.of(2028, 1, 1);
        LocalDate hasta = LocalDate.of(2028, 12, 31);

        assertThat(PeriodoEstadistica.validar(desde, hasta).hasta()).isEqualTo(hasta);
    }

    @Test
    @DisplayName("CP-ESTP-06: validar rechaza un período de 367 días")
    void validar_rangoExcedido() {
        LocalDate desde = LocalDate.of(2028, 1, 1);

        assertThatThrownBy(() -> PeriodoEstadistica.validar(desde, LocalDate.of(2029, 1, 1)))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El período no puede superar los 366 días");
    }

    @Test
    @DisplayName("CP-ESTP-07: el período por defecto va del primer día del mes en curso a hoy")
    void porDefecto_mesEnCurso() {
        assertThat(PeriodoEstadistica.porDefecto(LocalDate.of(2026, 11, 14)))
                .isEqualTo(new PeriodoEstadistica(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14)));
        assertThat(PeriodoEstadistica.porDefecto(LocalDate.of(2026, 11, 1)))
                .isEqualTo(new PeriodoEstadistica(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)));
    }
}
