package com.github.heikyudev.maestrocervecero.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PaginacionTest {

    @Test
    @DisplayName("CP-PGN-01: numeroDePagina deja pasar un número de página dentro del rango")
    void numeroDePagina_debeConservarUnaPaginaValida() {
        assertThat(Paginacion.numeroDePagina(0)).isZero();
        assertThat(Paginacion.numeroDePagina(7)).isEqualTo(7);
        assertThat(Paginacion.numeroDePagina(Paginacion.PAGINA_MAXIMA)).isEqualTo(Paginacion.PAGINA_MAXIMA);
    }

    @Test
    @DisplayName("CP-PGN-02: numeroDePagina lleva un número negativo a la primera página")
    void numeroDePagina_debeLlevarLosNegativosACero() {
        assertThat(Paginacion.numeroDePagina(-1)).isZero();
        assertThat(Paginacion.numeroDePagina(Integer.MIN_VALUE)).isZero();
    }

    @Test
    @DisplayName("CP-PGN-03: numeroDePagina acota un número desmedido al máximo, de modo que el desplazamiento no desborda")
    void numeroDePagina_debeAcotarLosNumerosDesmedidos() {
        assertThat(Paginacion.numeroDePagina(107_000_000)).isEqualTo(Paginacion.PAGINA_MAXIMA);
        assertThat(Paginacion.numeroDePagina(Integer.MAX_VALUE)).isEqualTo(Paginacion.PAGINA_MAXIMA);
        assertThat(PageRequest.of(Paginacion.numeroDePagina(Integer.MAX_VALUE), 100).getOffset()).isLessThan(Integer.MAX_VALUE);
    }
}
