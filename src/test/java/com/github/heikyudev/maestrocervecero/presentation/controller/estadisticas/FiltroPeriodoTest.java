package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas.FiltroPeriodo.PeriodoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroPeriodoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Test
    @DisplayName("CP-ESTF-01: sin ninguno de los dos parámetros el período es el por defecto: del primer día del mes a hoy")
    void resolver_porDefecto() {
        PeriodoPedido periodo = FiltroPeriodo.resolver(null, null, HOY);

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(periodo.hasta()).isEqualTo(HOY);
        assertThat(periodo.errores()).isEmpty();
    }

    @Test
    @DisplayName("CP-ESTF-02: con las dos fechas en formato ISO el período es el indicado")
    void resolver_fechasIndicadas() {
        PeriodoPedido periodo = FiltroPeriodo.resolver("2026-10-01", " 2026-10-31 ", HOY);

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(periodo.hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(periodo.errores()).isEmpty();
    }

    @Test
    @DisplayName("CP-ESTF-03: un extremo enviado vacío o ausente junto a otro indicado queda sin valor y sin error: el Service lo rechaza como obligatorio")
    void resolver_extremoVacio() {
        PeriodoPedido sinDesde = FiltroPeriodo.resolver("", "2026-10-31", HOY);
        PeriodoPedido sinHasta = FiltroPeriodo.resolver("2026-10-01", null, HOY);
        PeriodoPedido ambosVacios = FiltroPeriodo.resolver("", " ", HOY);

        assertThat(sinDesde.desde()).isNull();
        assertThat(sinDesde.hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(sinHasta.hasta()).isNull();
        assertThat(ambosVacios.desde()).isNull();
        assertThat(ambosVacios.hasta()).isNull();
        assertThat(sinDesde.errores()).isEmpty();
        assertThat(sinHasta.errores()).isEmpty();
        assertThat(ambosVacios.errores()).isEmpty();
    }

    @Test
    @DisplayName("CP-ESTF-04: un texto que no es una fecha existente se informa como error y el extremo queda sin valor")
    void resolver_fechaInvalida() {
        PeriodoPedido periodo = FiltroPeriodo.resolver("2026-02-30", "ayer", HOY);

        assertThat(periodo.desde()).isNull();
        assertThat(periodo.hasta()).isNull();
        assertThat(periodo.errores()).containsExactly("La fecha desde no es válida", "La fecha hasta no es válida");
    }
}
