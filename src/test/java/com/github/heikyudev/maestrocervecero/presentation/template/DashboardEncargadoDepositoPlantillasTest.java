package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas del panel del encargado de depósito: integración en la pantalla de inicio, fragmentos, enlaces
 * y atajos con su permiso, alineación de la columna Acciones, estados vacíos y textos sin emojis.
 */
class DashboardEncargadoDepositoPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-encargado-deposito.html";

    @Test
    @DisplayName("CP-PDD-01: la pantalla de inicio muestra el panel solo al ENCARGADO_DE_DEPOSITO y no tiene tarjetas de resumen: cada rol ve su propio panel")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-encargado-deposito :: panel(${dashboardEncargadoDeposito})");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('ENCARGADO_DE_DEPOSITO')");
        assertThat(count(html, "<th:block th:replace=\"~{home/dashboard-")).as("un panel por rol").isEqualTo(6);
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("hasAnyRole").doesNotContain("Usuarios activos");
    }

    @Test
    @DisplayName("CP-PDD-02: el panel define los fragmentos panel, contadores, ordenesPendientes, insumosSinStock y lotesVencidos, y los arma uno debajo de otro, cada bloque en una fila entera")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "ordenesPendientes(dashboard)", "insumosSinStock(dashboard)", "lotesVencidos(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        List<String> orden = List.of(":: contadores(", ":: ordenesPendientes(", ":: insumosSinStock(", ":: lotesVencidos(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(panel.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(panel.indexOf(orden.get(i)));
        }
        assertThat(panel.substring(0, panel.indexOf("</div>"))).as("los bloques son hijos directos del panel, cada uno en su propia fila: ninguna grilla ni envoltorio de varias columnas").doesNotContain("grid").doesNotContain("<div");
        assertThat(count(html, "data-bloque=\"")).as("un bloque por fragmento de datos").isEqualTo(3);
        assertThat(html).doesNotContain("col-span").doesNotContain("xl:grid-cols");
        assertThat(html).contains("grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4");
        assertThat(count(html, "data-contador=\"")).isEqualTo(6);
        assertThat(html).as("Entregas vencidas y Lotes vencidos son tarjetas sin enlace: su lista está en el bloque de abajo")
                .doesNotContain("data-contador=\"ordenes-entrega-vencida\" th:href").doesNotContain("data-contador=\"lotes-vencidos\" th:href");
    }

    @Test
    @DisplayName("CP-PDD-03: los contadores y los encabezados llevan a las listas filtradas: órdenes pendientes, insumos con lotes vencidos e insumos sin stock disponible")
    void contadoresConEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/ordenes-compra(estado='PENDIENTE')}").contains("@{/stock-insumos(situacionStock='CON_VENCIDOS')}")
                .contains("@{/stock-insumos(situacionStock='SIN_STOCK_DISPONIBLE')}");
        for (String titulo : List.of("'Órdenes pendientes'", "'Entregas vencidas'", "'Lotes vencidos'", "'Insumos sin stock'")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("${dashboard.ordenesPendientes.total}").contains("${dashboard.ordenesConEntregaVencida}")
                .contains("${dashboard.lotesVencidos.total}").contains("${dashboard.insumosSinStock.total}");
    }

    @Test
    @DisplayName("CP-PDD-04: cada atajo se protege con el permiso real de su destino y sin él los contadores quedan como texto plano")
    void permisosDeLosAtajos() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "sec:authorize=\"hasAuthority('ORDEN_COMPRA_CONSULTAR')\"")).as("un contador, Ver todas y el ojo").isEqualTo(3);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('ORDEN_COMPRA_CONSULTAR')\"")).isEqualTo(1);
        assertThat(count(html, "sec:authorize=\"hasAuthority('STOCK_INSUMO_CONSULTAR')\"")).as("un contador, Ver todos y los dos botones Abrir").isEqualTo(4);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('STOCK_INSUMO_CONSULTAR')\"")).isEqualTo(1);
        assertThat(botonProtegidoPor(html, "registrar(@{/ingresos-insumo/compra/nuevo(idOrdenCompra=${o.id})}")).isEqualTo("hasAuthority('INGRESO_INSUMO_REGISTRAR')");
        assertThat(botonProtegidoPor(html, "registrar(@{/ajustes-insumo/nuevo(idLoteInsumo=${l.id})}")).isEqualTo("hasAuthority('AJUSTE_INSUMO_REGISTRAR')");
        assertThat(botonProtegidoPor(html, "<a data-ver-orden")).isEqualTo("hasAuthority('ORDEN_COMPRA_CONSULTAR')");
        assertThat(botonProtegidoPor(html, "abrir(@{/stock-insumos/lotes/{id}(id=${l.id})}")).isEqualTo("hasAuthority('STOCK_INSUMO_CONSULTAR')");
        assertThat(botonProtegidoPor(html, "abrir(@{/stock-insumos/{id}(id=${i.id})}")).isEqualTo("hasAuthority('STOCK_INSUMO_CONSULTAR')");
    }

    @Test
    @DisplayName("CP-PDD-05: el ingreso de una orden y el ajuste de un lote se registran con el botón de registro y el ojo Ver y el botón Abrir llevan a la consulta, siempre en la celda de la columna Acciones")
    void atajosDeRegistroYDeConsulta() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "fragments/boton-registrar :: registrar(")).isEqualTo(2);
        assertThat(html).contains("'Registrar ingreso', 'bi-box-arrow-in-down'").contains("'Registrar ajuste', 'bi-sliders2'");
        assertThat(count(html, "fragments/boton-abrir :: abrir(")).isEqualTo(2);
        assertThat(html).contains("data-ver-orden").contains("bi bi-eye").doesNotContain("<form").doesNotContain("<button");
        assertThat(count(html, "<td class=\"col-acciones px-2 py-3\">")).isEqualTo(3);
        assertThat(count(html, "<table class=\"w-full text-sm text-left\">")).isEqualTo(3);
        assertThat(html).doesNotContain("Situación").doesNotContain("data-atraso");
    }

    @Test
    @DisplayName("CP-PDD-06: en cada tabla el encabezado Acciones y la celda del botón comparten la alineación (centrada) con el mismo relleno horizontal")
    void alineacionDeLaColumnaAcciones() throws IOException {
        String html = leer(PANEL);
        List<String> encabezados = clases(html, "<th class=\"col-acciones");
        List<String> celdas = clases(html, "<td class=\"col-acciones");
        List<String> contenedores = clases(html, "<div class=\"flex items-center justify-").stream().filter(c -> c.endsWith("gap-1")).toList();

        assertThat(encabezados).hasSize(3).hasSameSizeAs(celdas);
        assertThat(encabezados).allSatisfy(c -> assertThat(c).contains("px-2 py-3").contains("text-center").doesNotContain("text-right"));
        assertThat(celdas).allSatisfy(c -> assertThat(c).contains("px-2 py-3").doesNotContain("text-right"));
        assertThat(contenedores).hasSize(3).allSatisfy(c -> assertThat(c).contains("justify-center").doesNotContain("justify-end"));
    }

    @Test
    @DisplayName("CP-PDD-07: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los tres textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay órdenes de compra pendientes", "No hay insumos sin stock disponible", "No hay lotes de insumo vencidos con cantidad")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(3);
        assertThat(count(html, "<div class=\"overflow-x-auto\" th:if=\"${!dashboard.")).isEqualTo(3);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDD-08: el panel no tiene emojis y ningún mensaje del panel termina en punto")
    void sinEmojisNiPuntoFinal() throws IOException {
        String html = leer(PANEL);

        html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
        assertThat(html).doesNotContain(".</p>").doesNotContain("pendientes.<").doesNotContain("cantidad.<");
    }

    @Test
    @DisplayName("CP-PDD-09: cada bloque es una tarjeta con título, contador con el total real y enlace Ver todos protegido por el permiso de su lista")
    void encabezadosDeLosBloques() throws IOException {
        String html = leer(PANEL);

        for (String titulo : List.of(">Órdenes de compra pendientes</h2>", ">Insumos sin stock disponible</h2>", ">Lotes de insumo vencidos</h2>")) {
            assertThat(html).as(titulo).contains(titulo);
        }
        assertThat(count(html, "encabezado('")).isEqualTo(3);
        assertThat(html).contains("th:fragment=\"encabezado(titulo, total, lista, url, etiquetaAccesible)\"").contains("data-ver-todos")
                .contains("${dashboard.ordenesPendientes.total}, 'ordenes'").contains("${dashboard.insumosSinStock.total}, 'stock'").contains("${dashboard.lotesVencidos.total}, 'stock'");
        assertThat(html).doesNotContain(".filas.size()");
    }

    /** El permiso del bloque sec:authorize que envuelve el botón identificado por el texto dado. */
    private static String botonProtegidoPor(String html, String marca) {
        int boton = html.indexOf(marca);
        assertThat(boton).as(marca).isNotNegative();
        String antes = html.substring(0, boton);
        int bloque = antes.lastIndexOf("<th:block sec:authorize=\"");
        String desde = antes.substring(bloque + "<th:block sec:authorize=\"".length());
        return desde.substring(0, desde.indexOf('"'));
    }

    /** El valor del atributo class de cada etiqueta que empieza con el prefijo dado. */
    private static List<String> clases(String html, String prefijo) {
        List<String> clases = new ArrayList<>();
        int desde = 0;
        while ((desde = html.indexOf(prefijo, desde)) >= 0) {
            int inicio = html.indexOf("class=\"", desde) + 7;
            clases.add(html.substring(inicio, html.indexOf('"', inicio)));
            desde = inicio;
        }
        return clases;
    }

    private static boolean esEmoji(int cp) {
        return cp >= 0x1F000 || (cp >= 0x2600 && cp <= 0x27BF) || (cp >= 0x2B00 && cp <= 0x2BFF) || cp == 0xFE0F || cp == 0x200D;
    }

    private static String leer(String ruta) throws IOException {
        return Files.readString(TEMPLATES.resolve(ruta), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static int count(String texto, String buscado) {
        return texto.split(Pattern.quote(buscado), -1).length - 1;
    }
}
