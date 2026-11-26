package com.github.heikyudev.maestrocervecero.service.implementation.reporte;

import com.github.heikyudev.maestrocervecero.configuration.web.CantidadesDialect;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.EncabezadoReporte;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.GraficoBarras;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReportesDePrueba;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ColumnaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CriterioAplicadoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.FilaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.GraficoReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.IndicadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TablaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TipoColumna;
import org.springframework.data.domain.PageImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Genera de verdad el reporte impreso de las estadísticas con la plantilla única {@code estadisticas/pdf/reporte} y
 * revisa el documento resultante: un PDF válido en hoja A4 vertical con los elementos estándar de un reporte impreso.
 */
class GeneradorPdfServicioImplTest {

    private static final String PLANTILLA = "estadisticas/pdf/reporte";

    private static GeneradorPdfServicioImpl generador;

    @BeforeAll
    static void prepararMotorDePlantillas() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolver);
        motor.addDialect(new CantidadesDialect());
        generador = new GeneradorPdfServicioImpl(motor);
    }

    @Test
    @DisplayName("CP-GPDF-01: el reporte se genera como un PDF válido y no vacío, en hoja A4 vertical (595 x 842 pt)")
    void generar_debeDevolverUnPdfValidoEnA4Vertical() throws IOException {
        // === EJECUCION ===
        byte[] pdf = generador.generar(PLANTILLA, modelo(3, 3));

        // === VERIFICACION ===
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            PDRectangle hoja = documento.getPage(0).getMediaBox();
            assertThat(hoja.getWidth()).as("ancho A4 vertical").isCloseTo(595f, Offset.offset(1f));
            assertThat(hoja.getHeight()).as("alto A4 vertical").isCloseTo(842f, Offset.offset(1f));
        }
    }

    @Test
    @DisplayName("CP-GPDF-02: el reporte lleva el encabezado, los datos de emisión (con la terminal) con todos los criterios, la nota de los registros considerados, la pregunta, los indicadores con su definición, el gráfico, la tabla sin la columna de acciones con totales y el pie con la página")
    void generar_debeIncluirLosElementosEstandar() throws IOException {
        // === EJECUCION ===
        String texto = textoDe(generador.generar(PLANTILLA, modelo(3, 3)));

        // === VERIFICACION ===
        assertThat(texto).contains("Maestro Cervecero").contains("Estadísticas: Lotes")
                .contains("Emitido por").contains("gerente (Ana Pérez)")
                .contains("Fecha de emisión").contains("14/11/2026 09:30")
                .contains("Terminal").contains("pc-gerencia (192.168.0.20)")
                .contains("Período").contains("Desde 01/11/2026 hasta 30/11/2026")
                .contains("Criterios de filtro").contains("Receta: Todas").contains("Estado: Finalizado")
                .contains("Se consideran los lotes con inicio estimado dentro del período, en cualquier estado")
                .contains("Cuántos lotes se planificaron en el período y cómo terminaron")
                .contains("LOTES DEL PERÍODO").contains("TERMINADOS EN PLAZO").contains("66,67 %").contains("2 de 3 finalizados")
                .contains("Terminados en plazo: Porcentaje de los lotes finalizados")
                .contains("Lotes por estado")
                .contains("Lote Estado Litros envasados Fin estimado Fin real")
                .contains("LOT-R7-01").contains("Ñandú Añejo").contains("20/11/2026").contains("22/11/2026")
                .contains("Totales del período").contains("1.441,5")
                .contains("Página 1 de 1").contains("Emitido el 14/11/2026 09:30");
        assertThat(texto).doesNotContain("Acciones").doesNotContain("Abrir").doesNotContain("Sin resultados")
                .doesNotContain("se alcanzó el máximo").doesNotContain("Fecha considerada");
    }

    @Test
    @DisplayName("CP-GPDF-03: una tabla larga ocupa varias páginas y cada una repite el encabezado del sistema, el encabezado de la tabla y el pie con su número de página")
    void generar_debeRepetirEncabezadosEnCadaPagina() throws IOException {
        // === EJECUCION ===
        byte[] pdf = generador.generar(PLANTILLA, modelo(150, 150));

        // === VERIFICACION ===
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int paginas = documento.getNumberOfPages();
            assertThat(paginas).isGreaterThan(2);
            for (int pagina = 2; pagina <= paginas; pagina++) {
                PDFTextStripper lector = lector();
                lector.setStartPage(pagina);
                lector.setEndPage(pagina);
                String texto = normalizar(lector.getText(documento));
                assertThat(texto).as("página " + pagina)
                        .contains("Maestro Cervecero").contains("Estadísticas: Lotes")
                        .contains("Fin estimado")
                        .contains("Página " + pagina + " de " + paginas);
                assertThat(texto).as("los datos de emisión van solo en la primera página").doesNotContain("Criterios de filtro");
            }
            PDFTextStripper ultima = lector();
            ultima.setStartPage(paginas);
            assertThat(normalizar(ultima.getText(documento))).as("los totales van al final").contains("Totales del período");
        }
    }

    @Test
    @DisplayName("CP-GPDF-04: sin filas el reporte dice 'Sin resultados para los criterios indicados' y no dibuja la tabla; un gráfico sin valores dice 'Sin datos para graficar'")
    void generar_sinResultados() throws IOException {
        // === EJECUCION ===
        String texto = textoDe(generador.generar(PLANTILLA, modelo(0, 0)));

        // === VERIFICACION ===
        assertThat(texto).contains("Sin resultados para los criterios indicados").contains("Sin datos para graficar")
                .contains("Emitido por").contains("Página 1 de 1");
        assertThat(texto).doesNotContain("Totales del período").doesNotContain("Fin estimado");
    }

    @Test
    @DisplayName("CP-GPDF-05: si la tabla alcanzó el tope técnico de filas, el reporte lo informa con la cantidad mostrada y el total")
    void generar_debeAvisarElTope() throws IOException {
        // === EJECUCION ===
        String texto = textoDe(generador.generar(PLANTILLA, modelo(3, 5000)));

        // === VERIFICACION ===
        assertThat(texto).contains("La tabla muestra los primeros 3 de 5000 lotes del período: se alcanzó el máximo de filas de un reporte impreso");
    }

    @Test
    @DisplayName("CP-GPDF-07: el reporte de otra pestaña usa la misma plantilla en A4 vertical: importes, variaciones con barras negativas, columna 'Ver' sin imprimir y los criterios de ese reporte")
    void generar_otroReporte() throws IOException {
        // === PREPARACION DE DATOS ===
        TablaReporteResponseDTO tabla = new TablaReporteResponseDTO(List.of(
                ColumnaReporteResponseDTO.de("Insumo", TipoColumna.TEXTO),
                ColumnaReporteResponseDTO.de("Precio mínimo", TipoColumna.MONEDA),
                ColumnaReporteResponseDTO.de("Precio máximo", TipoColumna.MONEDA),
                ColumnaReporteResponseDTO.de("Precio promedio", TipoColumna.MONEDA),
                ColumnaReporteResponseDTO.de("Variación", TipoColumna.PORCENTAJE),
                ColumnaReporteResponseDTO.ver("Orden", "/ordenes-compra/{id}", "ORDEN_COMPRA_CONSULTAR")),
                new PageImpl<>(List.of(new FilaReporteResponseDTO(List.of(
                        CeldaReporteResponseDTO.texto("Pilsen", "Por kg"), CeldaReporteResponseDTO.moneda(new BigDecimal("10")),
                        CeldaReporteResponseDTO.moneda(new BigDecimal("12")), CeldaReporteResponseDTO.moneda(new BigDecimal("11.5")),
                        CeldaReporteResponseDTO.variacion(20), CeldaReporteResponseDTO.enlace(5L, "Ver la orden de compra N.º 5"))))),
                null, "insumo|insumos");
        ReporteEstadisticoResponseDTO reporte = ReporteEstadisticoResponseDTO.builder()
                .titulo("Precios").descripcion("Cómo variaron los precios de los insumos comprados")
                .desde(LocalDate.of(2026, 11, 1)).hasta(LocalDate.of(2026, 11, 30))
                .criterios(List.of(CriterioAplicadoResponseDTO.de("Tipo de insumo", null, "Todos"), CriterioAplicadoResponseDTO.de("Proveedor", "Maltería del Sur", "Todos")))
                .indicadores(List.of(new IndicadorResponseDTO("insumos", "Insumos comprados", "3", null, null)))
                .grafico(GraficoReporteResponseDTO.variacion("Variación del precio por insumo",
                        List.of(new DatoGraficoResponseDTO("Pilsen", 20), new DatoGraficoResponseDTO("Cascade", -20))))
                .tabla(tabla).build();
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("reporte", reporte);
        modelo.put("grafico", GraficoBarras.de("grafico-reporte", reporte.getGrafico()));
        modelo.put("encabezado", new EncabezadoReporte("gerente (Ana Pérez)", LocalDateTime.of(2026, 11, 14, 9, 30), "pc-compras (192.168.0.21)"));
        modelo.put("fuente", IGeneradorPdfServicio.FUENTE);

        // === EJECUCION ===
        byte[] pdf = generador.generar(PLANTILLA, modelo);

        // === VERIFICACION ===
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getPage(0).getMediaBox().getWidth()).isCloseTo(595f, Offset.offset(1f));
            assertThat(documento.getPage(0).getMediaBox().getHeight()).isCloseTo(842f, Offset.offset(1f));
        }
        String texto = textoDe(pdf);
        assertThat(texto).contains("Estadísticas: Precios").contains("Emitido por").contains("Fecha de emisión").contains("Terminal").contains("pc-compras (192.168.0.21)")
                .contains("Criterios de filtro").contains("Tipo de insumo: Todos").contains("Proveedor: Maltería del Sur")
                .contains("Variación del precio por insumo").contains("+20 %").contains("-20 %")
                .contains("Pilsen").contains("$ 11,50").contains("Página 1 de 1");
        assertThat(texto).doesNotContain("Orden Ver").doesNotContain("Ver la orden");
    }

    @Test
    @DisplayName("CP-GPDF-06: una plantilla que no existe se rechaza sin devolver un PDF")
    void generar_plantillaInexistente() {
        assertThatThrownBy(() -> generador.generar("estadisticas/pdf/inexistente", Map.of()))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== Apoyo ====================

    /** El modelo del reporte impreso con {@code filas} lotes de un total de {@code total} en el período. */
    static Map<String, Object> modelo(int filas, long total) {
        ReporteEstadisticoResponseDTO reporte = ReportesDePrueba.lotes(filas, total);
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("reporte", reporte);
        modelo.put("grafico", GraficoBarras.de("grafico-reporte", reporte.getGrafico()));
        modelo.put("encabezado", new EncabezadoReporte("gerente (Ana Pérez)", LocalDateTime.of(2026, 11, 14, 9, 30), "pc-gerencia (192.168.0.20)"));
        modelo.put("fuente", IGeneradorPdfServicio.FUENTE);
        return modelo;
    }

    private static String textoDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return normalizar(lector().getText(documento));
        }
    }

    /** Lee el texto en el orden en que se ve en la hoja ("Página 1 de 2"), no en el orden en que se escribió en el archivo. */
    private static PDFTextStripper lector() {
        PDFTextStripper lector = new PDFTextStripper();
        lector.setSortByPosition(true);
        return lector;
    }

    /** Une los saltos de línea y los espacios repetidos: el texto extraído de un PDF parte las líneas donde quiere. */
    private static String normalizar(String texto) {
        return texto.replaceAll("\\s+", " ");
    }
}
