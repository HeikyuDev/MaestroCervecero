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
 * Guardas de las plantillas del panel del encargado de distribución: integración en la pantalla de inicio, fragmentos,
 * enlaces y atajos con su permiso, alineación de la columna Acciones, estados vacíos y textos sin emojis.
 */
class DashboardEncargadoDistribucionPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-encargado-distribucion.html";
    private static final String CONSULTA_BARRIL = "#authorization.expression(T(com.github.heikyudev.maestrocervecero.util.PermisosPlantilla).CONSULTA_BARRIL)";

    @Test
    @DisplayName("CP-PDE-01: la pantalla de inicio muestra el panel solo al ENCARGADO_DE_DISTRIBUCION y no tiene tarjetas de resumen: cada uno de los seis roles ve su propio panel")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-encargado-distribucion :: panel(${dashboardEncargadoDistribucion})");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('ENCARGADO_DE_DISTRIBUCION')");
        assertThat(count(html, "<th:block th:replace=\"~{home/dashboard-")).as("un panel por rol").isEqualTo(6);
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("hasAnyRole").doesNotContain("Usuarios activos");
    }

    @Test
    @DisplayName("CP-PDE-02: el panel define los fragmentos panel, contadores, devolucionesPendientes, retirosPendientes, clientesConBarriles y barrilesListos, y los arma uno debajo de otro, cada bloque en una fila entera")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "devolucionesPendientes(dashboard)", "retirosPendientes(dashboard)", "clientesConBarriles(dashboard)", "barrilesListos(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        List<String> orden = List.of(":: contadores(", ":: devolucionesPendientes(", ":: retirosPendientes(", ":: clientesConBarriles(", ":: barrilesListos(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(panel.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(panel.indexOf(orden.get(i)));
        }
        assertThat(panel.substring(0, panel.indexOf("</div>"))).as("los bloques son hijos directos del panel, cada uno en su propia fila: ninguna grilla ni envoltorio de varias columnas").doesNotContain("grid").doesNotContain("<div");
        assertThat(count(html, "data-bloque=\"")).as("un bloque por fragmento de datos").isEqualTo(4);
        assertThat(html).doesNotContain("col-span").doesNotContain("xl:grid-cols");
        assertThat(html).contains("grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4");
        assertThat(count(html, "data-contador=\"")).isEqualTo(6);
    }

    @Test
    @DisplayName("CP-PDE-03: los contadores y los encabezados llevan al control de barriles filtrado por Despachado y por Con cerveza, y a la lista de clientes")
    void enlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/control-barriles(estadoOperativo='DESPACHADO')}").contains("@{/control-barriles(estadoOperativo='CON_CERVEZA')}")
                .contains("@{/clientes}").contains("@{/clientes(nombre=${c.nombre})}").contains("@{/control-barriles/{id}(id=${d.idBarril})}");
        for (String titulo : List.of("'En poder de clientes'", "'Devoluciones vencidas'", "'Vencen hoy'", "'Listos para despachar'")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("${dashboard.barrilesEnPoderDeClientes}").contains("${dashboard.devolucionesVencidas}").contains("${dashboard.devolucionesQueVencenHoy}")
                .contains("${dashboard.barrilesListos.total}").contains("${dashboard.clientesConBarriles.total}");
    }

    @Test
    @DisplayName("CP-PDE-04: cada atajo se protege con el permiso real de su destino y sin él los contadores quedan como texto plano")
    void permisosDeLosAtajos() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "th:if=\"${" + CONSULTA_BARRIL + "}\"")).as("dos contadores, Ver todos de barriles y dos botones Abrir").isEqualTo(5);
        assertThat(count(html, "th:unless=\"${" + CONSULTA_BARRIL + "}\"")).isEqualTo(2);
        assertThat(count(html, "sec:authorize=\"hasAuthority('CLIENTE_CONSULTAR')\"")).as("Ver todos y Abrir de clientes").isEqualTo(2);
        assertThat(botonProtegidoPor(html, "registrar(@{/control-barriles/{id}/devoluciones/nueva(id=${d.idBarril})}")).isEqualTo("hasAuthority('DEVOLUCION_BARRIL_REGISTRAR')");
        assertThat(botonProtegidoPor(html, "registrar(@{/control-barriles/{id}/despachos/nuevo(id=${b.id})}")).isEqualTo("hasAuthority('DESPACHO_BARRIL_REGISTRAR')");
        assertThat(botonProtegidoPor(html, "abrir(@{/clientes(nombre=${c.nombre})}")).isEqualTo("hasAuthority('CLIENTE_CONSULTAR')");
        assertThat(botonProtegidoPor(html, "registrar(@{/control-barriles/{id}/devoluciones/nueva(id=${r.idBarril})}")).isEqualTo("hasAuthority('DEVOLUCION_BARRIL_REGISTRAR')");
        assertThat(count(html, "sec:authorize=\"hasAuthority('SOLICITUD_BUSQUEDA_CONSULTAR')\"")).as("Ver todas y Ver de las solicitudes de retiro").isEqualTo(2);
    }

    @Test
    @DisplayName("CP-PDE-05: la devolución y el despacho de un barril se registran con el botón de registro con el barril en la ruta, y Abrir lleva a la consulta, siempre en la celda de la columna Acciones")
    void atajosDeRegistroYDeConsulta() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "fragments/boton-registrar :: registrar(")).isEqualTo(3);
        assertThat(html).contains("'Registrar devolución', 'bi-box-arrow-in-down-left'").contains("'Registrar despacho', 'bi-box-arrow-up-right'");
        assertThat(count(html, "fragments/boton-abrir :: abrir(")).isEqualTo(3);
        assertThat(html).doesNotContain("<form").doesNotContain("<button").doesNotContain("Situación").doesNotContain("data-atraso");
        assertThat(count(html, "<td class=\"col-acciones px-2 py-3\">")).isEqualTo(4);
        assertThat(count(html, "<table class=\"w-full text-sm text-left\">")).isEqualTo(4);
        assertThat(html).contains("data-dias-de-atraso").contains("data-cliente-barriles");
    }

    @Test
    @DisplayName("CP-PDE-06: en cada tabla el encabezado Acciones y la celda del botón comparten la alineación (centrada) con el mismo relleno horizontal")
    void alineacionDeLaColumnaAcciones() throws IOException {
        String html = leer(PANEL);
        List<String> encabezados = clases(html, "<th class=\"col-acciones");
        List<String> celdas = clases(html, "<td class=\"col-acciones");
        List<String> contenedores = clases(html, "<div class=\"flex items-center justify-").stream().filter(c -> c.endsWith("gap-1")).toList();

        assertThat(encabezados).hasSize(4).hasSameSizeAs(celdas);
        assertThat(encabezados).allSatisfy(c -> assertThat(c).contains("px-2 py-3").contains("text-center").doesNotContain("text-right"));
        assertThat(celdas).allSatisfy(c -> assertThat(c).contains("px-2 py-3").doesNotContain("text-right"));
        assertThat(contenedores).hasSize(4).allSatisfy(c -> assertThat(c).contains("justify-center").doesNotContain("justify-end"));
    }

    @Test
    @DisplayName("CP-PDE-07: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los cuatro textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay devoluciones vencidas ni que venzan hoy", "No hay retiros coordinados pendientes", "No hay clientes con barriles en su poder", "No hay barriles con cerveza listos para despachar")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(4);
        assertThat(count(html, "<div class=\"overflow-x-auto\" th:if=\"${!dashboard.")).isEqualTo(4);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDE-08: el panel no tiene emojis y ningún mensaje del panel termina en punto")
    void sinEmojisNiPuntoFinal() throws IOException {
        String html = leer(PANEL);

        html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
        assertThat(html).doesNotContain(".</p>").doesNotContain("hoy.<").doesNotContain("poder.<").doesNotContain("despachar.<");
    }

    @Test
    @DisplayName("CP-PDE-09: los retiros coordinados pendientes muestran el total y los vencidos, resaltan la fila vencida y llevan a las solicitudes pendientes y al detalle")
    void retirosPendientes() throws IOException {
        String html = leer(PANEL);
        String bloque = html.substring(html.indexOf("th:fragment=\"retirosPendientes(dashboard)\""), html.indexOf("th:fragment=\"clientesConBarriles(dashboard)\""));

        assertThat(bloque).contains("Retiros coordinados pendientes").contains("${dashboard.retirosPendientes.total}")
                .contains("th:if=\"${dashboard.retirosVencidos > 0}\"").contains("con la fecha propuesta vencida")
                .contains("@{/solicitudes-retiro(estado='PENDIENTE')}").contains("@{/solicitudes-retiro/{id}(id=${r.idSolicitud})}")
                .contains("${r.vencida} ? 'text-rose-700 font-semibold'").contains(">Pendiente vencida</span>")
                .contains("${r.numeroSerie}").contains("${r.identificadorInterno}").contains("${r.nombreCliente}");
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
