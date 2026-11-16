package com.github.heikyudev.maestrocervecero.presentation.view.estadisticas;

import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.GraficoReporteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GraficoBarrasTest {

    private static GraficoBarras grafico(String unidad, double... valores) {
        List<DatoGraficoResponseDTO> datos = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            datos.add(new DatoGraficoResponseDTO("Categoría " + (i + 1), valores[i]));
        }
        return GraficoBarras.de("g", new GraficoReporteResponseDTO("Título", datos, unidad));
    }

    @Test
    @DisplayName("CP-ESTG-01: cada categoría es una barra en el orden recibido, con su largo proporcional al valor sobre el valor más alto")
    void barrasProporcionales() {
        GraficoBarras g = grafico(null, 4, 2, 1);

        assertThat(g.getBarras()).extracting(GraficoBarras.Barra::getEtiqueta).containsExactly("Categoría 1", "Categoría 2", "Categoría 3");
        assertThat(g.getBarras().get(1).getPorcentaje()).isCloseTo(g.getBarras().get(0).getPorcentaje() / 2, within(0.01));
        assertThat(g.getBarras().get(2).getPorcentaje()).isCloseTo(g.getBarras().get(0).getPorcentaje() / 4, within(0.01));
    }

    @Test
    @DisplayName("CP-ESTG-02: la barra más larga ocupa el porcentaje máximo del ancho disponible y deja lugar al valor escrito; ninguna lo supera")
    void ocupaElAnchoDisponible() {
        GraficoBarras g = grafico(null, 10, 3, 7);

        assertThat(g.getBarras().get(0).getPorcentaje()).isEqualTo(GraficoBarras.PORCENTAJE_MAXIMO);
        assertThat(g.getBarras()).allSatisfy(b -> assertThat(b.getPorcentaje()).isLessThanOrEqualTo(GraficoBarras.PORCENTAJE_MAXIMO));
        assertThat(GraficoBarras.PORCENTAJE_MAXIMO).isLessThan(100.0);
    }

    @Test
    @DisplayName("CP-ESTG-03: una categoría en cero no dibuja barra y una muy chica conserva un largo mínimo visible")
    void ceroYMinimo() {
        GraficoBarras g = grafico(null, 10000, 0, 1);

        assertThat(g.getBarras().get(1).getPorcentaje()).isZero();
        assertThat(g.getBarras().get(2).getPorcentaje()).isEqualTo(GraficoBarras.PORCENTAJE_MINIMO);
    }

    @Test
    @DisplayName("CP-ESTG-04: los valores negativos se dibujan como cero")
    void negativos() {
        GraficoBarras g = grafico(null, -5, 2);

        assertThat(g.getBarras().get(0).getPorcentaje()).isZero();
        assertThat(g.getBarras().get(0).getValorTexto()).isEqualTo("0");
    }

    @Test
    @DisplayName("CP-ESTG-05: sin categorías, con todas en cero o sin datos el gráfico queda vacío y no tiene barras")
    void vacio() {
        assertThat(grafico(null).isVacio()).isTrue();
        GraficoBarras ceros = grafico(null, 0, 0);
        assertThat(ceros.isVacio()).isTrue();
        assertThat(ceros.getBarras()).isEmpty();
        assertThat(GraficoBarras.de("g", new GraficoReporteResponseDTO("T", null, null)).isVacio()).isTrue();
    }

    @Test
    @DisplayName("CP-ESTG-06: el valor escrito usa el formato de la aplicación con su unidad y la descripción enumera cada categoría con su valor")
    void valoresYDescripcion() {
        GraficoBarras g = grafico("L", 1380.5, 2);

        assertThat(g.getBarras().get(0).getValorTexto()).isEqualTo("1.380,5 L");
        assertThat(g.getDescripcion()).isEqualTo("Categoría 1: 1.380,5 L; Categoría 2: 2 L");
        assertThat(g.getTitulo()).isEqualTo("Título");
    }

    @Test
    @DisplayName("CP-ESTG-08: un gráfico de importes escribe cada valor con el símbolo de pesos y, si lleva unidad, como 'importe / unidad'")
    void importes() {
        GraficoBarras g = GraficoBarras.de("g", GraficoReporteResponseDTO.moneda("Costo por litro",
                List.of(new DatoGraficoResponseDTO("Pale Ale", 1250.5), new DatoGraficoResponseDTO("Stout", 600)), "L"));

        assertThat(g.getBarras()).extracting(GraficoBarras.Barra::getValorTexto).containsExactly("$ 1.250,50 / L", "$ 600,00 / L");
        assertThat(GraficoBarras.de("g", GraficoReporteResponseDTO.moneda("T", List.of(new DatoGraficoResponseDTO("A", 5)), null))
                .getBarras().get(0).getValorTexto()).isEqualTo("$ 5,00");
    }

    @Test
    @DisplayName("CP-ESTG-09: un gráfico de variaciones dibuja las barras negativas con su propio color, escribe el signo y escala el largo por el valor absoluto")
    void variaciones() {
        GraficoBarras g = GraficoBarras.de("g", GraficoReporteResponseDTO.variacion("Variación",
                List.of(new DatoGraficoResponseDTO("Sube", 20), new DatoGraficoResponseDTO("Baja", -10), new DatoGraficoResponseDTO("Igual", 0))));

        assertThat(g.getBarras()).extracting(GraficoBarras.Barra::getValorTexto).containsExactly("+20 %", "-10 %", "0 %");
        assertThat(g.getBarras().get(0).getColor()).isEqualTo(GraficoBarras.COLOR_BARRA);
        assertThat(g.getBarras().get(1).getColor()).isEqualTo(GraficoBarras.COLOR_NEGATIVO);
        assertThat(g.getBarras().get(1).getPorcentaje()).isCloseTo(g.getBarras().get(0).getPorcentaje() / 2, within(0.01));
        assertThat(g.getBarras().get(2).getPorcentaje()).isZero();
        assertThat(g.getDescripcion()).isEqualTo("Sube: +20 %; Baja: -10 %; Igual: 0 %");
        assertThat(GraficoBarras.de("g", GraficoReporteResponseDTO.variacion("T", List.of(new DatoGraficoResponseDTO("A", 0)))).isVacio()).isTrue();
    }

    @Test
    @DisplayName("CP-ESTG-07: una etiqueta larga se abrevia en el gráfico y conserva el texto completo")
    void etiquetaLarga() {
        GraficoBarras g = GraficoBarras.de("g", new GraficoReporteResponseDTO("T",
                List.of(new DatoGraficoResponseDTO("Una etiqueta demasiado larga", 3)), null));

        GraficoBarras.Barra barra = g.getBarras().get(0);
        assertThat(barra.getEtiqueta()).isEqualTo("Una etiqueta demasiado larga");
        assertThat(barra.getEtiquetaCorta()).hasSizeLessThanOrEqualTo(GraficoBarras.LARGO_MAXIMO_ETIQUETA).endsWith("…");
        assertThat(GraficoBarras.abreviar("En ejecución")).isEqualTo("En ejecución");
    }
}
