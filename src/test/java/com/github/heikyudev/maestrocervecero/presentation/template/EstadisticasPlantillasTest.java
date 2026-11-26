package com.github.heikyudev.maestrocervecero.presentation.template;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas.ReporteDePestana;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TablaReporteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas del módulo de estadísticas: una sola entrada en el menú, una sola pantalla con pestañas,
 * una sola plantilla de reporte impreso en A4 vertical, los topes de la estructura y los textos.
 */
class EstadisticasPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANTALLA = "estadisticas/estadisticas.html";
    private static final String PIEZAS = "estadisticas/fragments/reporte.html";
    private static final String GRAFICO = "estadisticas/fragments/grafico.html";
    private static final String PDF = "estadisticas/pdf/reporte.html";
    private static final Pattern EMOJI = Pattern.compile("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]");

    @Test
    @DisplayName("CP-ESTT-01: el menú tiene una sola entrada 'Estadísticas' (/estadisticas, bi-bar-chart) visible con cualquiera de los dos permisos de estadísticas, y el título de la pestaña del navegador la nombra")
    void menuConUnaSolaEntrada() throws IOException {
        String sidebar = leer("fragments/sidebar.html");

        assertThat(sidebar).contains("enlace('/estadisticas', ${opcionActiva}, 'estadisticas', 'bi-bar-chart', 'Estadísticas')");
        assertThat(sidebar).doesNotContain("'/estadisticas/");
        int entrada = sidebar.indexOf("enlace('/estadisticas'");
        String bloque = sidebar.substring(sidebar.lastIndexOf("<th:block sec:authorize", entrada), entrada);
        assertThat(bloque).contains("hasAnyAuthority('ESTADISTICA_PRODUCCION_CONSULTAR', 'ESTADISTICA_COMPRAS_CONSULTAR')");
        assertThat(entrada).as("va en la sección Reportes, debajo de Administración y antes de Configuración")
                .isGreaterThan(sidebar.indexOf("seccion('Reportes')")).isGreaterThan(sidebar.indexOf("seccion('Administración')"))
                .isLessThan(sidebar.indexOf("seccion('Configuración')"));
        String base = leer("layout/base.html");
        assertThat(base).contains("'estadisticas': 'Estadísticas'").doesNotContain("'estadisticas-");
        assertThat(leer(PANTALLA)).contains("layout(~{::#estadisticas-content}, 'estadisticas')");
    }

    @Test
    @DisplayName("CP-ESTT-02: el módulo tiene solo cuatro plantillas genéricas (pantalla, piezas, gráfico y reporte impreso), ninguna propia de un reporte")
    void plantillasGenericas() throws IOException {
        Path carpeta = TEMPLATES.resolve("estadisticas");
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            assertThat(archivos.filter(Files::isRegularFile).map(p -> carpeta.relativize(p).toString().replace('\\', '/')))
                    .containsExactlyInAnyOrder("estadisticas.html", "fragments/reporte.html", "fragments/grafico.html", "pdf/reporte.html");
        }
    }

    @Test
    @DisplayName("CP-ESTT-03: la pantalla usa el componente de pestañas con el ícono de cada pestaña y la línea de la pregunta; cada pestaña tiene clave, texto, ícono, pregunta sin punto final y permiso")
    void pestanas() throws IOException {
        String html = leer(PANTALLA);

        assertThat(html).contains("fragments/pestanas :: pestanas('Reportes de estadísticas', 'estadisticas'")
                .contains("fragments/pestanas :: pestana(@{/estadisticas(pestana=${p.clave})}, ${pestanaActiva == p}, ${p.clave}, ~{fragments/pestanas :: bi(${p.icono})}, ${p.etiqueta})")
                .contains("data-pregunta th:text=\"${pestanaActiva.descripcion}\"");
        for (PestanaEstadistica pestana : PestanaEstadistica.values()) {
            assertThat(pestana.getClave()).matches("[a-z-]+");
            assertThat(pestana.getIcono()).startsWith("bi-");
            assertThat(pestana.getDescripcion()).isNotBlank().doesNotEndWith(".");
            assertThat(pestana.getPermiso().name()).isIn("ESTADISTICA_PRODUCCION_CONSULTAR", "ESTADISTICA_COMPRAS_CONSULTAR");
        }
        assertThat(PestanaEstadistica.LOTES.getDescripcion()).isEqualTo("Cuántos lotes se planificaron en el período y cómo terminaron");
    }

    @Test
    @DisplayName("CP-ESTT-04: los filtros son un formulario GET sin validación del navegador con la grilla de las listas: Desde y Hasta con asterisco rojo y placeholder, los criterios propios y, en su propia fila a la derecha, Limpiar y Filtrar")
    void barraDeFiltros() throws IOException {
        String piezas = leer(PIEZAS);

        assertThat(piezas).contains("method=\"get\" novalidate").doesNotContain(" required")
                .contains("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-end")
                .contains("<div class=\"col-span-full flex items-center justify-end gap-3\">")
                .contains("Desde <span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")
                .contains("Hasta <span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")
                .contains("placeholder=\"Ej: 01/07/2027\"").contains("placeholder=\"Ej: 31/07/2027\"")
                .contains("fragments/selector-entidad :: selector(").contains("th:text=\"${c.textoTodos}\"");
        List<String> orden = List.of("name=\"desde\"", "name=\"hasta\"", "th:each=\"c : ${campos}\"", ">Limpiar<", "</i> Filtrar", "</form>");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(piezas.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(piezas.indexOf(orden.get(i)));
        }
        assertThat(ReporteDePestana.MAXIMO_CAMPOS).isEqualTo(2);
        String formulario = piezas.substring(piezas.indexOf("th:fragment=\"filtros("), piezas.indexOf("</form>"));
        assertThat(formulario).as("Exportar PDF no va en la barra de filtros").doesNotContain("data-exportar-pdf");
    }

    @Test
    @DisplayName("CP-ESTT-09: debajo de los filtros va el encabezado de resultados con el período aplicado a la izquierda y, a la derecha, el botón secundario Exportar PDF; solo se muestra si hay reporte")
    void encabezadoDeResultados() throws IOException {
        String piezas = leer(PIEZAS);
        String pantalla = leer(PANTALLA);

        assertThat(piezas).contains("th:fragment=\"resultados(desde, hasta, urlPdf, criterioDeFecha)\"")
                .contains("flex flex-wrap items-center justify-between gap-3")
                .contains("|Resultados del ${#temporals.format(desde, 'dd/MM/yyyy')} al ${#temporals.format(hasta, 'dd/MM/yyyy')}|")
                .contains("data-criterio-fecha").contains("${criterioDeFecha}")
                .contains("data-exportar-pdf target=\"_blank\"").contains("border border-slate-300 bg-white").contains("bi bi-file-earmark-pdf");
        String resultados = piezas.substring(piezas.indexOf("th:fragment=\"resultados("));
        assertThat(resultados.substring(0, resultados.indexOf("</div>"))).as("botón secundario, sin relleno ámbar").doesNotContain("bg-amber-500");
        List<String> orden = List.of(":: filtros(", "th:if=\"${reporte != null}\"", ":: resultados(", ":: indicadores(", ":: grafico(", ":: tabla(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(pantalla.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(pantalla.indexOf(orden.get(i)));
        }
    }

    @Test
    @DisplayName("CP-ESTT-05: hasta cuatro indicadores en una fila con el tooltip del proyecto para la definición, un solo gráfico debajo a todo el ancho y una tabla de hasta siete columnas con Acciones centrada, totales, estado vacío y paginación estándar")
    void estructuraDelReporte() throws IOException {
        String piezas = leer(PIEZAS);
        String pantalla = leer(PANTALLA);

        assertThat(ReporteEstadisticoResponseDTO.MAXIMO_INDICADORES).isEqualTo(4);
        assertThat(TablaReporteResponseDTO.MAXIMO_COLUMNAS).isEqualTo(7);
        assertThat(piezas).contains("data-indicadores class=\"grid grid-cols-1 gap-4\"")
                .contains("fragments/tooltip :: ayuda(")
                .contains("estadisticas/fragments/grafico :: grafico(${grafico})")
                .contains("'col-acciones'").contains("fragments/boton-abrir :: abrir(")
                .contains("#authorization.expression('hasAuthority(''' + col.permiso + ''')')")
                .contains("data-fila-totales class=\"bg-white border-t-2 border-slate-300 font-semibold\"")
                .contains("Sin resultados para los criterios indicados").contains("${col.ayuda}")
                .contains("fragments/paginacion :: paginacion(");
        assertThat(count(pantalla, "estadisticas/fragments/reporte :: grafico(")).isEqualTo(1);
        assertThat(count(pantalla, "estadisticas/fragments/reporte :: tabla(")).isEqualTo(1);
        assertThat(pantalla).as("una tabla debajo de la otra, cada una con su título y sin paginación si lo tiene")
                .contains("th:each=\"tabla : ${reporte.tablas}\"").contains("${reporte.criterioDeFecha}");
        assertThat(piezas).contains("data-titulo-tabla").contains("th:if=\"${tabla.paginada}\"");
        assertThat(count(pantalla, "estadisticas/fragments/reporte :: indicadores(")).isEqualTo(1);
        assertThat(pantalla).as("indicadores en una fila y el gráfico debajo, a todo el ancho").doesNotContain("lg:grid-cols-2");
        assertThat(pantalla.indexOf(":: indicadores(")).isLessThan(pantalla.indexOf(":: grafico("));
    }

    @Test
    @DisplayName("CP-ESTT-12: la grilla de indicadores toma tantas columnas como tarjetas (2, 3 o 4) para llenar la fila sin huecos, y con menos de cuatro las tarjetas son más amplias y su valor más grande")
    void indicadoresSeAdaptanALaCantidad() throws IOException {
        String piezas = leer(PIEZAS);
        String indicadores = piezas.substring(piezas.indexOf("th:fragment=\"indicadores("), piezas.indexOf("<!-- ===================================================================== GRÁFICO"));

        assertThat(indicadores).contains("th:with=\"cantidad=${#lists.size(indicadores)}\"")
                .contains("'sm:grid-cols-2 lg:grid-cols-4'").contains("'md:grid-cols-3'").contains("'sm:grid-cols-2'")
                .contains("${cantidad >= 4} ? 'p-5' : 'p-7'").contains("'text-4xl'").contains("data-cantidad-indicadores")
                .doesNotContain("grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4");
        assertThat(leer(PDF)).as("el reporte impreso reparte las celdas de una sola fila en todo el ancho")
                .contains(".indicadores { width: 100%; table-layout: fixed; }").contains("th:each=\"i : ${reporte.indicadores}\"");
    }

    @Test
    @DisplayName("CP-ESTT-06: el gráfico se dibuja en el servidor con HTML (sin JavaScript): una fila por barra con la etiqueta, la barra con su largo en porcentaje del ancho disponible y el valor escrito a continuación, accesible con role img")
    void graficoAccesible() throws IOException {
        String html = leer(GRAFICO);

        assertThat(html).contains("th:fragment=\"grafico(grafico)\"").contains("role=\"img\"")
                .contains("aria-label=|${grafico.titulo}: ${grafico.descripcion}|")
                .contains("width: ${b.porcentaje}%").contains("style=\"width: 100%;")
                .contains("th:text=\"${b.valorTexto}\"").contains("data-valor-barra").contains("Sin datos para graficar");
        assertThat(html.indexOf("${b.porcentaje}")).as("el valor va después de la barra").isLessThan(html.indexOf("th:text=\"${b.valorTexto}\""));
        assertThat(html + leer(PANTALLA) + leer(PIEZAS)).doesNotContain("<script").doesNotContain("<svg");
    }

    @Test
    @DisplayName("CP-ESTT-07: el reporte impreso es A4 vertical, con encabezado y pie repetidos, datos de emisión, número de página, encabezado de tabla repetido, filas sin partir, sin la columna de acciones y sin expresiones web")
    void reporteImpreso() throws IOException {
        String html = leer(PDF);

        assertThat(html).contains("size: A4 portrait").doesNotContain("landscape")
                .contains("@top-center { content: element(encabezado); }").contains("@bottom-center { content: element(pie); }")
                .contains("position: running(encabezado)").contains("position: running(pie)")
                .contains("content: counter(page)").contains("content: counter(pages)")
                .contains("Página <span class=\"numero-pagina\"></span> de <span class=\"total-paginas\"></span>")
                .contains("font-family: 'Liberation Sans'")
                .contains("Emitido por").contains("Fecha de emisión").contains("<th>Terminal</th>").contains("${encabezado.terminal}").contains("Período").contains("Criterios de filtro")
                .contains("<p class=\"criterio-fecha\" th:if=\"${reporte.criterioDeFecha != null}\" th:text=\"${reporte.criterioDeFecha}\">")
                .contains("th:each=\"tabla : ${reporte.tablas}\"").contains("${tabla.titulo}")
                .contains("dd/MM/yyyy HH:mm").contains(">Maestro Cervecero<")
                .contains("table.detalle thead { display: table-header-group; }").contains("table.detalle tr { page-break-inside: avoid; }")
                .contains("${tabla.columnasImprimibles}").contains("${tabla.celdasImprimibles(f)}").contains("<tr class=\"totales\"")
                .contains("se alcanzó el máximo de filas de un reporte impreso").contains("Sin resultados para los criterios indicados")
                .contains("estadisticas/fragments/grafico :: grafico(${grafico})").contains(".grafico { width: 170mm;");
        assertThat(html).doesNotContain("@{/").doesNotContain("sec:authorize").doesNotContain("Fecha considerada");
        assertThat(html.indexOf("class=\"criterio-fecha\" th:if")).isGreaterThan(html.indexOf("Criterios de filtro"))
                .isLessThan(html.indexOf("<p class=\"pregunta\""));
    }

    @Test
    @DisplayName("CP-ESTT-08: las plantillas del módulo no tienen emojis, voseo ni mensajes con punto final en los textos de estado")
    void textos() throws IOException {
        for (String plantilla : List.of(PANTALLA, PIEZAS, GRAFICO, PDF)) {
            String html = leer(plantilla);
            assertThat(EMOJI.matcher(html).find()).as(plantilla).isFalse();
            assertThat(html).as(plantilla).doesNotContain("Indicá").doesNotContain("Elegí").doesNotContain("presioná")
                    .doesNotContain("indicados.").doesNotContain("graficar.").doesNotContain("reporte.<");
        }
    }

    @Test
    @DisplayName("CP-ESTT-10: la columna de acciones muestra 'Abrir' o, en un detalle de solo lectura, el ojo 'Ver', siempre protegida por el permiso de la columna y sin botón si la celda no tiene ID")
    void botonDeAcciones() throws IOException {
        String piezas = leer(PIEZAS);

        assertThat(piezas).contains("${celda.id != null and (col.permiso == null or #authorization.expression(")
                .contains("th:unless=\"${col.ver}\"").contains("fragments/boton-abrir :: abrir(")
                .contains("th:if=\"${col.ver}\"").contains("data-boton-ver").contains("bi bi-eye");
    }

    @Test
    @DisplayName("CP-ESTT-11: las nueve pestañas tienen clave, ícono y pregunta distintas, las de producción exigen ESTADISTICA_PRODUCCION_CONSULTAR y las de compras ESTADISTICA_COMPRAS_CONSULTAR")
    void pestanasDefinidas() {
        assertThat(PestanaEstadistica.values()).hasSize(9);
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).map(PestanaEstadistica::getClave).distinct()).hasSize(9);
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).map(PestanaEstadistica::getIcono).distinct()).hasSize(9);
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).map(PestanaEstadistica::getDescripcion).distinct()).hasSize(9);
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).limit(5).map(p -> p.getPermiso().name()))
                .containsOnly("ESTADISTICA_PRODUCCION_CONSULTAR");
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).skip(5).map(p -> p.getPermiso().name()))
                .containsOnly("ESTADISTICA_COMPRAS_CONSULTAR");
        assertThat(java.util.Arrays.stream(PestanaEstadistica.values()).map(PestanaEstadistica::getClave))
                .containsExactly("lotes", "costos", "tiempos", "insumos", "equipamiento", "compras", "proveedores", "precios", "recepcion");
    }

    private static String leer(String plantilla) throws IOException {
        return Files.readString(TEMPLATES.resolve(plantilla), StandardCharsets.UTF_8);
    }

    private static int count(String texto, String buscado) {
        int n = 0;
        for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + buscado.length())) {
            n++;
        }
        return n;
    }
}
