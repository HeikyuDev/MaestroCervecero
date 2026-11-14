package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas del panel del operario de producción: integración en la pantalla de inicio, fragmentos, atajos
 * con su permiso, botones Abrir y Registrar, estados vacíos y textos sin emojis.
 */
class DashboardOperarioPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-operario.html";

    @Test
    @DisplayName("CP-PDO-01: la pantalla de inicio muestra el panel solo al OPERARIO_DE_PRODUCCION y oculta para ese rol las tarjetas de resumen, que no cambian para los demás")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-operario :: panel(${dashboardOperario})");
        int tarjetas = html.indexOf("sec:authorize=\"!hasRole('OPERARIO_DE_PRODUCCION')\"");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('OPERARIO_DE_PRODUCCION')");
        assertThat(tarjetas).isGreaterThan(panel);
        assertThat(html.substring(html.lastIndexOf("<div", tarjetas), tarjetas + 80)).contains("<div sec:authorize=\"!hasRole('OPERARIO_DE_PRODUCCION')\" class=\"grid");
        assertThat(html).contains("Estado del sistema").contains("Usuarios activos").contains("Órdenes de compra").contains("Auditoría").contains("'dashboard')");
    }

    @Test
    @DisplayName("CP-PDO-02: el panel define los fragmentos panel, contadores, lotesDeHoy, limpiezasPendientes y pendientesDeMantenimiento, y los arma en tres filas que se leen de izquierda a derecha y de arriba abajo")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "lotesDeHoy(dashboard)", "limpiezasPendientes(dashboard)", "pendientesDeMantenimiento(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        assertThat(panel.indexOf(":: contadores(")).isLessThan(panel.indexOf(":: lotesDeHoy("));
        assertThat(panel.indexOf(":: lotesDeHoy(")).isLessThan(panel.indexOf(":: limpiezasPendientes("));
        assertThat(panel.indexOf(":: limpiezasPendientes(")).isLessThan(panel.indexOf(":: pendientesDeMantenimiento("));
        assertThat(panel).contains("grid grid-cols-1 xl:grid-cols-3 gap-6");
        assertThat(html).contains("grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4").contains("xl:col-span-2");
        // Los subgrupos de un bloque van apilados: ningún bloque de limpieza ni de mantenimiento los pone lado a lado
        String mantenimiento = html.substring(html.indexOf("th:fragment=\"pendientesDeMantenimiento(dashboard)\""), html.indexOf("PIEZAS COMPARTIDAS"));
        assertThat(mantenimiento).doesNotContain("grid").doesNotContain("md:grid-cols-2");
        assertThat(mantenimiento.indexOf("data-subbloque=\"equipos-en-mantenimiento\"")).isLessThan(mantenimiento.indexOf("data-subbloque=\"barriles-en-mantenimiento\""));
        assertThat(mantenimiento).contains("data-subbloque=\"barriles-en-mantenimiento\" class=\"p-4 min-w-0 border-t border-slate-200\"");
        assertThat(html).doesNotContain("fragments/pestanas");
    }

    @Test
    @DisplayName("CP-PDO-03: los contadores llevan a las listas filtradas: lotes en ejecución, lotes pendientes con inicio estimado hasta hoy y equipos y barriles en limpieza y en mantenimiento")
    void contadoresConEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/lotes(estado='EN_EJECUCION')}")
                .contains("@{/lotes(estado='PENDIENTE', inicioEstimadoHasta=${#temporals.format(#temporals.createToday(), 'yyyy-MM-dd')})}")
                .contains("@{/control-equipamiento(estadoOperativo='EN_LIMPIEZA')}").contains("@{/control-barriles(estadoOperativo='EN_LIMPIEZA')}")
                .contains("@{/control-equipamiento(estadoOperativo='EN_MANTENIMIENTO')}").contains("@{/control-barriles(estadoOperativo='EN_MANTENIMIENTO')}");
        for (String icono : List.of("bi-play-circle", "bi-hourglass-split", "bi-stars", "bi-wrench")) {
            assertThat(html).as(icono).contains(icono);
        }
        for (String titulo : List.of("'Lotes en ejecución'", "'Lotes a iniciar'", "'En limpieza'", "'Pendientes de mantenimiento'")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("${dashboard.equiposEnLimpieza.total + dashboard.barrilesEnLimpieza.total}")
                .contains("${dashboard.equiposEnMantenimiento.total + dashboard.barrilesEnMantenimiento.total}");
    }

    @Test
    @DisplayName("CP-PDO-04: cada atajo se protege con el permiso real de su destino y sin él los contadores quedan como texto plano")
    void permisosDeLosAtajos() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "sec:authorize=\"hasAuthority('LOTE_CONSULTAR')\"")).as("dos contadores, el Ver todos de lotes y los dos botones Abrir").isEqualTo(5);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('LOTE_CONSULTAR')\"")).isEqualTo(2);
        assertThat(html).contains("sec:authorize=\"hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')\"").contains("sec:authorize=\"!hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')\"")
                .contains("th:if=\"${#authorization.expression(T(com.github.heikyudev.maestrocervecero.util.PermisosPlantilla).CONSULTA_BARRIL)}\"")
                .contains("th:unless=\"${#authorization.expression(T(com.github.heikyudev.maestrocervecero.util.PermisosPlantilla).CONSULTA_BARRIL)}\"")
                .doesNotContain("LIMPIEZA_BARRIL_CONSULTAR");
        assertThat(botonesProtegidosPor(html, "'Registrar limpieza', 'bi-stars', 'Registrar limpieza de ' + ${e.identificadorInterno}")).isEqualTo("hasAuthority('LIMPIEZA_EQUIPAMIENTO_REGISTRAR')");
        assertThat(botonesProtegidosPor(html, "'Registrar limpieza', 'bi-stars', 'Registrar limpieza de ' + ${b.identificadorInterno}")).isEqualTo("hasAuthority('LIMPIEZA_BARRIL_REGISTRAR')");
        assertThat(botonesProtegidosPor(html, "'Registrar mantenimiento', 'bi-wrench', 'Registrar mantenimiento de ' + ${e.identificadorInterno}")).isEqualTo("hasAuthority('MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR')");
        assertThat(botonesProtegidosPor(html, "'Registrar mantenimiento', 'bi-wrench', 'Registrar mantenimiento de ' + ${b.identificadorInterno}")).isEqualTo("hasAuthority('MANTENIMIENTO_BARRIL_REGISTRAR')");
        assertThat(botonesProtegidosPor(html, "@{/lotes/{id}(id=${l.id}, etapa=")).isEqualTo("hasAuthority('LOTE_CONSULTAR')");
        assertThat(botonesProtegidosPor(html, "@{/lotes/{id}(id=${l.id})}")).isEqualTo("hasAuthority('LOTE_CONSULTAR')");
    }

    @Test
    @DisplayName("CP-PDO-05: los lotes se abren con el fragmento Abrir (en su etapa en curso si están en ejecución) y las limpiezas y los mantenimientos con Registrar, nunca con Abrir, siempre en la celda de la columna Acciones")
    void abrirParaLotesYRegistrarParaOperaciones() throws IOException {
        String html = leer(PANEL);

        assertThat(html).doesNotContain("data-boton-abrir");
        assertThat(count(html, "fragments/boton-abrir :: abrir(")).isEqualTo(2);
        assertThat(html).contains("etapa=${l.etapaActual.name().toLowerCase()}");
        String limpiezasYMantenimientos = html.substring(html.indexOf("th:fragment=\"limpiezasPendientes(dashboard)\""), html.indexOf("PIEZAS COMPARTIDAS"));
        assertThat(limpiezasYMantenimientos).doesNotContain("boton-abrir");
        // Cada fila termina en la celda col-acciones y la tabla tiene el encabezado Acciones fijo
        assertThat(count(html, "<th class=\"col-acciones px-2 py-3 text-right whitespace-nowrap\">Acciones</th>")).isEqualTo(6);
        assertThat(count(html, "<td class=\"col-acciones px-2 py-3\">")).isEqualTo(6);
        assertThat(count(html, "<table class=\"w-full text-sm text-left\">")).isEqualTo(6);
        assertThat(count(html, "<div class=\"overflow-x-auto\"")).isEqualTo(6);
    }

    @Test
    @DisplayName("CP-PDO-06: la fila de un lote en ejecución muestra el atraso en rosa con la duración, el indicador Falta consumo enfocable con su tooltip, y la causa de un mantenimiento como etiqueta Falla, Preventivo o guion")
    void indicadoresDeLasFilas() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("th:if=\"${l.diasDeAtraso > 0}\"").contains("bg-rose-50 text-rose-700\">Atrasado <th:block th:replace=\"~{lote/formato :: duracion(${l.diasDeAtraso}, 'día', 'días')}\"")
                .contains("th:if=\"${l.faltaConsumo}\"").contains("tabindex=\"0\"").contains("data-tooltip=\"La etapa en curso no alcanzó el consumo mínimo de insumos para finalizar\"")
                .contains("bi-exclamation-circle").contains("bg-amber-50 text-amber-700");
        assertThat(html).contains("th:fragment=\"causa(causa)\"").contains("'bg-rose-50 text-rose-700' : 'bg-sky-50 text-sky-700'").contains("<th:block th:if=\"${causa == null}\">-</th:block>");
        assertThat(count(html, "home/dashboard-operario :: causa(")).isEqualTo(2);
        assertThat(html).contains("fragments/enlace-equipo :: enlace(${l.idFermentador}, ${l.identificadorInternoFermentador})")
                .contains("planificacion_produccion/formato :: fecha(${l.fechaInicioEstimada})");
    }

    @Test
    @DisplayName("CP-PDO-07: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los seis textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay lotes en ejecución", "No hay lotes a iniciar", "No hay equipos en limpieza", "No hay barriles en limpieza",
                "No hay equipos pendientes de mantenimiento", "No hay barriles pendientes de mantenimiento")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(6);
        assertThat(count(html, "<div class=\"overflow-x-auto\" th:if=\"${!dashboard.")).as("la tabla solo se renderiza con filas").isEqualTo(6);
        assertThat(count(html, "<p th:if=\"${dashboard.")).isEqualTo(6);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDO-08: el panel y el botón de registro no tienen emojis, y ningún mensaje del panel termina en punto")
    void sinEmojisNiPuntoFinal() throws IOException {
        for (String ruta : List.of(PANEL, "fragments/boton-registrar.html")) {
            String html = leer(ruta);
            html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("%s: U+%04X", ruta, cp).isFalse());
        }
        String panel = leer(PANEL);
        assertThat(panel).doesNotContain(".</p>").doesNotContain("días.<").doesNotContain("finalizar.\"");
    }

    @Test
    @DisplayName("CP-PDO-09: cada bloque es una tarjeta con título, contador y enlace Ver todos protegido por el permiso de su lista, y cada contador de sub-bloque muestra el total real y no las filas visibles")
    void encabezadosDeLosBloques() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("<h2 class=\"px-4 py-3 border-b border-slate-200 text-base font-semibold text-slate-900\">Lotes de hoy</h2>")
                .contains(">Limpiezas pendientes</h2>").contains(">Pendientes de mantenimiento</h2>");
        assertThat(count(html, "encabezado('")).isEqualTo(6);
        assertThat(html).contains("th:fragment=\"encabezado(titulo, total, lista, url, etiquetaAccesible)\"").contains("data-ver-todos").contains("Ver todos</a>")
                .contains("${dashboard.lotesEnEjecucion.total}, 'lotes'").contains("${dashboard.equiposEnLimpieza.total}, 'equipos'").contains("${dashboard.barrilesEnMantenimiento.total}, 'barriles'");
        assertThat(html).doesNotContain(".filas.size()");
        assertThat(html).contains("bg-white border border-slate-200 rounded-xl shadow-sm").contains("class=\"px-2 py-3\"");
    }

    /** El permiso del bloque sec:authorize que envuelve el botón identificado por el texto dado. */
    private static String botonesProtegidosPor(String html, String marca) {
        int boton = html.indexOf(marca);
        assertThat(boton).as(marca).isNotNegative();
        String antes = html.substring(0, boton);
        int bloque = antes.lastIndexOf("<th:block sec:authorize=\"");
        String desde = antes.substring(bloque + "<th:block sec:authorize=\"".length());
        return desde.substring(0, desde.indexOf('"'));
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
