package com.github.heikyudev.maestrocervecero.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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

    @Test
    @DisplayName("CP-PGN-04: consultar repite la consulta con la última página cuando la pedida está fuera de rango")
    void consultar_debeUsarLaUltimaPaginaSiLaPedidaEstaFueraDeRango() {
        // Arrange: 25 elementos de a 10 son 3 páginas (0 a 2); la página 5 está fuera de rango
        Function<Pageable, Page<String>> consulta = p -> p.getPageNumber() == 2
                ? new PageImpl<>(List.of("x"), p, 25)
                : new PageImpl<>(List.of(), p, 25);

        // Act
        Page<String> resultado = Paginacion.consultar(5, 10, Sort.by("nombre"), consulta);

        // Assert
        assertThat(resultado.getNumber()).isEqualTo(2);
        assertThat(resultado.getContent()).containsExactly("x");
        assertThat(resultado.getPageable().getSort()).isEqualTo(Sort.by("nombre"));
    }

    @Test
    @DisplayName("CP-PGN-05: consultar no repite la consulta si la página es válida o si no hay resultados")
    void consultar_noDebeRepetirLaConsultaSiLaPaginaEsValidaOEstaVacia() {
        // Arrange
        AtomicInteger consultas = new AtomicInteger();
        Function<Pageable, Page<String>> conResultados = p -> {
            consultas.incrementAndGet();
            return new PageImpl<>(List.of("a"), p, 1);
        };
        Function<Pageable, Page<String>> sinResultados = p -> {
            consultas.incrementAndGet();
            return new PageImpl<>(List.of(), p, 0);
        };

        // Act
        Page<String> valida = Paginacion.consultar(0, 10, Sort.unsorted(), conResultados);
        Page<String> vacia = Paginacion.consultar(3, 10, Sort.unsorted(), sinResultados);

        // Assert
        assertThat(valida.getContent()).containsExactly("a");
        assertThat(vacia.getContent()).isEmpty();
        assertThat(consultas.get()).isEqualTo(2);
    }
}
