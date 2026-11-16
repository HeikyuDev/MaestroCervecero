package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Estructura única de los reportes de estadísticas: topes de indicadores y columnas, coherencia de las filas, columnas y
 * celdas imprimibles, aviso de tope y formato de las celdas.
 */
class ReporteEstadisticoResponseDTOTest {

    private static final List<ColumnaReporteResponseDTO> COLUMNAS = List.of(
            ColumnaReporteResponseDTO.de("Lote", TipoColumna.TEXTO),
            ColumnaReporteResponseDTO.de("Litros", TipoColumna.NUMERO),
            ColumnaReporteResponseDTO.enlace("Acciones", "/lotes/{id}", "LOTE_CONSULTAR"));

    @Test
    @DisplayName("CP-ERG-01: un reporte admite hasta cuatro indicadores; con cinco se rechaza")
    void maximoIndicadores() {
        assertThat(reporte(4).getIndicadores()).hasSize(4);
        assertThatThrownBy(() -> reporte(5)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un reporte admite como máximo 4 indicadores");
    }

    @Test
    @DisplayName("CP-ERG-02: una tabla admite hasta seis columnas y cada fila (también la de totales) debe tener una celda por columna")
    void maximoColumnasYCeldas() {
        List<ColumnaReporteResponseDTO> siete = Collections.nCopies(7, ColumnaReporteResponseDTO.de("C", TipoColumna.TEXTO));
        assertThatThrownBy(() -> new TablaReporteResponseDTO(siete, new PageImpl<>(List.of()), null, "fila|filas"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("La tabla de un reporte admite como máximo 6 columnas");

        FilaReporteResponseDTO incompleta = new FilaReporteResponseDTO(List.of(CeldaReporteResponseDTO.texto("LOT-1")));
        assertThatThrownBy(() -> new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(incompleta)), null, "lote|lotes"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Cada fila de la tabla debe tener una celda por columna");
        assertThatThrownBy(() -> new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(fila(1))), incompleta, "lote|lotes"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CP-ERG-03: la fila de totales solo se muestra si hay filas; sin filas queda vacía")
    void totalesSoloConFilas() {
        FilaReporteResponseDTO totales = new FilaReporteResponseDTO(List.of(CeldaReporteResponseDTO.texto("Totales del período"),
                CeldaReporteResponseDTO.numero(10), CeldaReporteResponseDTO.vacia()));

        assertThat(new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(fila(1))), totales, "lote|lotes").getTotales()).isSameAs(totales);
        assertThat(new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of()), totales, "lote|lotes").getTotales()).isNull();
    }

    @Test
    @DisplayName("CP-ERG-04: el reporte impreso omite la columna de acciones y sus celdas; las demás quedan en su orden con su alineación")
    void columnasImprimibles() {
        TablaReporteResponseDTO tabla = new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(fila(1))), null, "lote|lotes");

        assertThat(tabla.getColumnasImprimibles()).extracting(ColumnaReporteResponseDTO::getTitulo).containsExactly("Lote", "Litros");
        assertThat(tabla.celdasImprimibles(tabla.getFilas().getContent().get(0))).extracting(CeldaReporteResponseDTO::getTexto)
                .containsExactly("LOT-1", "1");
        assertThat(TipoColumna.TEXTO.getClaseAlineacion()).isEqualTo("text-left");
        assertThat(TipoColumna.NUMERO.getClaseAlineacion()).isEqualTo("text-right");
        assertThat(TipoColumna.PORCENTAJE.getClaseAlineacion()).isEqualTo("text-right");
        assertThat(TipoColumna.MONEDA.getClaseAlineacion()).isEqualTo("text-right");
        assertThat(TipoColumna.FECHA.getClaseAlineacion()).isEqualTo("text-left");
        assertThat(TipoColumna.ENLACE.getClaseAlineacion()).isEqualTo("text-center");
    }

    @Test
    @DisplayName("CP-ERG-05: la tabla está truncada si la página no trae todas las filas del período, y nombra sus filas en plural")
    void truncada() {
        TablaReporteResponseDTO completa = new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(fila(1), fila(2)), PageRequest.of(0, 2), 2), null, "lote|lotes");
        TablaReporteResponseDTO truncada = new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of(fila(1), fila(2)), PageRequest.of(0, 2), 5), null, "lote|lotes");

        assertThat(completa.isTruncada()).isFalse();
        assertThat(truncada.isTruncada()).isTrue();
        assertThat(truncada.getNombrePlural()).isEqualTo("lotes");
    }

    @Test
    @DisplayName("CP-ERG-06: cada celda usa el formato de la aplicación según su tipo y un valor sin dato es un guion")
    void formatoDeCeldas() {
        assertThat(CeldaReporteResponseDTO.numero(1380.5).getTexto()).isEqualTo("1.380,5");
        assertThat(CeldaReporteResponseDTO.numero(null).getTexto()).isEqualTo("-");
        assertThat(CeldaReporteResponseDTO.porcentaje(66.67).getTexto()).isEqualTo("66,67 %");
        assertThat(CeldaReporteResponseDTO.porcentaje(null).getTexto()).isEqualTo("-");
        assertThat(CeldaReporteResponseDTO.moneda(new BigDecimal("1250.5")).getTexto()).isEqualTo("$ 1.250,50");
        assertThat(CeldaReporteResponseDTO.fecha(LocalDate.of(2026, 11, 2)).getTexto()).isEqualTo("02/11/2026");
        assertThat(CeldaReporteResponseDTO.fecha(LocalDateTime.of(2026, 11, 22, 17, 0)).getTexto()).isEqualTo("22/11/2026");
        assertThat(CeldaReporteResponseDTO.fecha((LocalDate) null).getTexto()).isEqualTo("-");
        assertThat(CeldaReporteResponseDTO.texto(" ").getTexto()).isEqualTo("-");
        CeldaReporteResponseDTO conDetalle = CeldaReporteResponseDTO.texto("LOT-R7-01", "Pale Ale");
        assertThat(conDetalle.getDetalle()).isEqualTo("Pale Ale");
        CeldaReporteResponseDTO enlace = CeldaReporteResponseDTO.enlace(11L, "Abrir LOT-R7-01");
        assertThat(enlace.getId()).isEqualTo(11L);
        assertThat(enlace.getTexto()).isEqualTo("Abrir LOT-R7-01");
    }

    @Test
    @DisplayName("CP-ERG-07: un criterio sin valor se muestra con el texto de sin filtro ('Todos' o 'Todas')")
    void criterioAplicado() {
        assertThat(CriterioAplicadoResponseDTO.de("Receta", null, "Todas").getValor()).isEqualTo("Todas");
        assertThat(CriterioAplicadoResponseDTO.de("Estado", " ", "Todos").getValor()).isEqualTo("Todos");
        assertThat(CriterioAplicadoResponseDTO.de("Estado", "Finalizado", "Todos").getValor()).isEqualTo("Finalizado");
    }

    private static ReporteEstadisticoResponseDTO reporte(int indicadores) {
        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Lotes").descripcion("Pregunta").desde(LocalDate.of(2026, 11, 1)).hasta(LocalDate.of(2026, 11, 30))
                .criterios(List.of())
                .indicadores(Collections.nCopies(indicadores, new IndicadorResponseDTO("c", "Nombre", "1", null, null)))
                .grafico(new GraficoReporteResponseDTO("G", List.of(), null))
                .tabla(new TablaReporteResponseDTO(COLUMNAS, new PageImpl<>(List.of()), null, "lote|lotes"))
                .build();
    }

    private static FilaReporteResponseDTO fila(int numero) {
        return new FilaReporteResponseDTO(List.of(CeldaReporteResponseDTO.texto("LOT-" + numero), CeldaReporteResponseDTO.numero(numero),
                CeldaReporteResponseDTO.enlace((long) numero, "Abrir LOT-" + numero)));
    }
}
