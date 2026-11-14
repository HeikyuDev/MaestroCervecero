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
 * Guardas de las plantillas del panel del gerente de compras: integración en la pantalla de inicio, fragmentos, enlaces y
 * atajos con su permiso, alineación de la columna Acciones, estados vacíos y textos sin emojis.
 */
class DashboardGerenteComprasPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-gerente-compras.html";

    @Test
    @DisplayName("CP-PDC-01: la pantalla de inicio muestra el panel solo al GERENTE_DE_COMPRAS y no tiene tarjetas de resumen: cada rol ve su propio panel")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-gerente-compras :: panel(${dashboardGerenteCompras})");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('GERENTE_DE_COMPRAS')");
        assertThat(count(html, "<th:block th:replace=\"~{home/dashboard-")).as("un panel por rol").isEqualTo(6);
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("hasAnyRole").doesNotContain("Usuarios activos");
    }

    @Test
    @DisplayName("CP-PDC-02: el panel define los fragmentos panel, contadores, planificacionesSinOrden, ordenesPendientes y ordenesFinalizadas, y los arma en tres filas que se leen de izquierda a derecha y de arriba abajo")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "planificacionesSinOrden(dashboard)", "ordenesPendientes(dashboard)", "ordenesFinalizadas(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        List<String> orden = List.of(":: contadores(", ":: planificacionesSinOrden(", ":: ordenesPendientes(", ":: ordenesFinalizadas(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(panel.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(panel.indexOf(orden.get(i)));
        }
        assertThat(panel).contains("grid grid-cols-1 xl:grid-cols-3 gap-6");
        assertThat(html).contains("grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4").contains("xl:col-span-2");
        assertThat(count(html, "data-contador=\"")).isEqualTo(6);
        assertThat(html).as("Planificaciones sin orden y Entregas vencidas son tarjetas sin enlace: su lista está en el bloque de abajo")
                .doesNotContain("data-contador=\"planificaciones-sin-orden\" th:href").doesNotContain("data-contador=\"ordenes-entrega-vencida\" th:href");
    }

    @Test
    @DisplayName("CP-PDC-03: los contadores y los encabezados llevan a las planificaciones pendientes y a las órdenes pendientes y finalizadas, y el contador de finalizadas dice la ventana de días")
    void contadoresConEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/planificaciones-produccion(estado='PENDIENTE')}").contains("@{/ordenes-compra(estado='PENDIENTE')}").contains("@{/ordenes-compra(estado='FINALIZADA')}");
        for (String titulo : List.of("'Planificaciones sin orden'", "'Órdenes pendientes'", "'Entregas vencidas'", "|Finalizadas, últimos ${dashboard.diasDeOrdenesFinalizadas} días|")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("${dashboard.planificacionesSinOrden.total}").contains("${dashboard.ordenesPendientes.total}")
                .contains("${dashboard.ordenesConEntregaVencida}").contains("${dashboard.ordenesFinalizadas.total}")
                .contains("|Órdenes de compra finalizadas, últimos ${dashboard.diasDeOrdenesFinalizadas} días|");
    }

    @Test
    @DisplayName("CP-PDC-04: cada atajo se protege con el permiso real de su destino y sin él los contadores quedan como texto plano")
    void permisosDeLosAtajos() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "sec:authorize=\"hasAuthority('PLANIFICACION_PRODUCCION_CONSULTAR')\"")).as("Ver todas y el ojo").isEqualTo(2);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('PLANIFICACION_PRODUCCION_CONSULTAR')\"")).isEqualTo(0);
        assertThat(count(html, "sec:authorize=\"hasAuthority('ORDEN_COMPRA_CONSULTAR')\"")).as("dos contadores, Ver todas de las dos listas de órdenes y dos ojos").isEqualTo(5);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('ORDEN_COMPRA_CONSULTAR')\"")).isEqualTo(2);
        assertThat(botonProtegidoPor(html, "registrar(@{/ordenes-compra/nuevo(idPlanificacion=${p.id})}")).isEqualTo("hasAuthority('ORDEN_COMPRA_REGISTRAR')");
        assertThat(botonProtegidoPor(html, "<a data-ver-planificacion")).isEqualTo("hasAuthority('PLANIFICACION_PRODUCCION_CONSULTAR')");
        assertThat(botonProtegidoPor(html, "<a data-ver-orden")).isEqualTo("hasAuthority('ORDEN_COMPRA_CONSULTAR')");
    }

    @Test
    @DisplayName("CP-PDC-05: la orden de una planificación se registra con el botón de registro con la planificación como parámetro aparte, y el ojo Ver lleva a la consulta, siempre en la celda de la columna Acciones")
    void atajosDeRegistroYDeConsulta() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "fragments/boton-registrar :: registrar(")).isEqualTo(1);
        assertThat(html).contains("'Registrar orden', 'bi-cart-plus'");
        assertThat(html).doesNotContain("boton-abrir").doesNotContain("<form").doesNotContain("<button").doesNotContain("Situación").doesNotContain("data-atraso");
        assertThat(count(html, "<td class=\"col-acciones px-2 py-3\">")).isEqualTo(3);
        assertThat(count(html, "<table class=\"w-full text-sm text-left\">")).isEqualTo(3);
        assertThat(html).contains("data-orden-avance").contains("data-orden-cierre");
    }

    @Test
    @DisplayName("CP-PDC-06: en cada tabla el encabezado Acciones y la celda del botón comparten la alineación (centrada) con el mismo relleno horizontal")
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
    @DisplayName("CP-PDC-07: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los tres textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay planificaciones pendientes sin orden de compra", "No hay órdenes de compra pendientes", "No hay órdenes de compra finalizadas en el período")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(3);
        assertThat(count(html, "<div class=\"overflow-x-auto\" th:if=\"${!dashboard.")).isEqualTo(3);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDC-08: el panel no tiene emojis, no muestra stock de insumos y ningún mensaje del panel termina en punto")
    void sinEmojisNiStockNiPuntoFinal() throws IOException {
        String html = leer(PANEL);

        html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
        assertThat(html).doesNotContain(".</p>").doesNotContain("orden de compra.<").doesNotContain("período.<")
                .doesNotContain("stock-insumos").doesNotContain("STOCK_INSUMO");
    }

    @Test
    @DisplayName("CP-PDC-09: cada bloque es una tarjeta con título, contador con el total real y enlace Ver todas protegido por el permiso de su lista")
    void encabezadosDeLosBloques() throws IOException {
        String html = leer(PANEL);

        for (String titulo : List.of(">Planificaciones por comprar</h2>", ">Órdenes de compra pendientes</h2>")) {
            assertThat(html).as(titulo).contains(titulo);
        }
        assertThat(count(html, "encabezado('")).isEqualTo(3);
        assertThat(html).contains("th:fragment=\"encabezado(titulo, total, lista, url, etiquetaAccesible)\"").contains("data-ver-todos")
                .contains("${dashboard.planificacionesSinOrden.total}, 'planificaciones'").contains("${dashboard.ordenesPendientes.total}, 'ordenes'").contains("${dashboard.ordenesFinalizadas.total}, 'ordenes'");
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
