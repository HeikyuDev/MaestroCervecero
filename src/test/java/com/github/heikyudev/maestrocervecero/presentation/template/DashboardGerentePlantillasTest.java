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
 * Guardas de las plantillas del panel del gerente de producción: integración en la pantalla de inicio, fragmentos, enlaces
 * con su permiso, estados vacíos, ausencia de botones de registro y textos sin emojis.
 */
class DashboardGerentePlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-gerente.html";

    @Test
    @DisplayName("CP-PDG-01: la pantalla de inicio muestra el panel solo al GERENTE_DE_PRODUCCION y no tiene tarjetas de resumen: cada rol ve su propio panel")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-gerente :: panel(${dashboardGerente})");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('GERENTE_DE_PRODUCCION')");
        assertThat(count(html, "<th:block th:replace=\"~{home/dashboard-")).as("un panel por rol").isEqualTo(6);
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("hasAnyRole").doesNotContain("Usuarios activos");
    }

    @Test
    @DisplayName("CP-PDG-02: el panel define los fragmentos panel, contadores, produccionEnCurso, colaDeFermentadores y planificacionesPendientes, y los arma uno debajo de otro, cada bloque en una fila entera")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "produccionEnCurso(dashboard)", "colaDeFermentadores(dashboard)",
                "planificacionesPendientes(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        List<String> orden = List.of(":: contadores(", ":: produccionEnCurso(", ":: colaDeFermentadores(", ":: planificacionesPendientes(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(panel.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(panel.indexOf(orden.get(i)));
        }
        assertThat(panel.substring(0, panel.indexOf("</div>"))).as("los bloques son hijos directos del panel, cada uno en su propia fila: ninguna grilla ni envoltorio de varias columnas").doesNotContain("grid").doesNotContain("<div");
        assertThat(count(html, "data-bloque=\"")).as("un bloque por fragmento de datos").isEqualTo(3);
        assertThat(html).doesNotContain("col-span").doesNotContain("xl:grid-cols");
        assertThat(html).contains("grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4");
        assertThat(html).doesNotContain("fragments/pestanas");
    }

    @Test
    @DisplayName("CP-PDG-03: los contadores llevan a las listas filtradas: lotes en ejecución, fermentadores, producción por etapa, cronograma y planificaciones")
    void contadoresYConteosConEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/lotes(estado='EN_EJECUCION')}").contains("@{/control-equipamiento(tipo='FERMENTADOR')}")
                .contains("@{/planificaciones-produccion(estado='PENDIENTE')}")
                .contains("@{/lotes(estado='EN_EJECUCION', etapaActual=${e.etapa.name()})}").contains("@{/cronograma-produccion}");
        for (String titulo : List.of("'Lotes en ejecución'", "'Lotes atrasados'", "'Fuera de servicio'")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("contenidoContador('Fermentadores ocupados', |${dashboard.fermentadoresOcupados} / ${dashboard.fermentadoresTotal}|");
        assertThat(html).doesNotContain("inicioEstimadoHasta").doesNotContain("lotesAIniciar").doesNotContain("stock-insumos").doesNotContain("insumosPorSituacion").doesNotContain("STOCK_INSUMO_CONSULTAR");
        assertThat(count(html, "data-contador=\"")).isEqualTo(6);
        assertThat(html).as("Lotes atrasados es una tarjeta sin enlace: su lista está en la producción en curso").doesNotContain("<a sec:authorize=\"hasAuthority('LOTE_CONSULTAR')\" data-contador=\"lotes-atrasados\"");
    }

    @Test
    @DisplayName("CP-PDG-04: cada enlace se protege con el permiso real de su destino y sin él los contadores quedan como texto plano")
    void permisosDeLosEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "sec:authorize=\"hasAuthority('LOTE_CONSULTAR')\"")).as("un contador de lotes, Ver todos, Ver cronograma y el botón Abrir de lotes").isEqualTo(4);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('LOTE_CONSULTAR')\"")).isEqualTo(1);
        assertThat(html).contains("sec:authorize=\"hasAuthority('PLANIFICACION_PRODUCCION_CONSULTAR')\"");
        assertThat(count(html, "fragments/boton-abrir :: abrir(")).as("los lotes se abren con Abrir").isEqualTo(1);
        assertThat(html).as("la planificación se consulta con el ojo Ver").contains("data-ver-planificacion").contains("bi bi-eye");
    }

    @Test
    @DisplayName("CP-PDG-05: el panel es de solo lectura: no usa el botón de registro ni enlaces a formularios de alta")
    void soloLectura() throws IOException {
        String html = leer(PANEL);

        assertThat(html).doesNotContain("boton-registrar").doesNotContain("data-boton-registrar").doesNotContain("/nueva").doesNotContain("/nuevo")
                .doesNotContain("<form").doesNotContain("<button");
    }

    @Test
    @DisplayName("CP-PDG-06: la producción en curso no tiene columna Situación ni etiquetas de atraso; la cola muestra el atraso del lote en ejecución en rosa; la prioridad usa su fragmento y el fermentador el enlace de equipo")
    void indicadoresDeLasFilas() throws IOException {
        String html = leer(PANEL);
        String produccion = html.substring(html.indexOf("th:fragment=\"produccionEnCurso(dashboard)\""), html.indexOf("th:fragment=\"colaDeFermentadores(dashboard)\""));
        String cola = html.substring(html.indexOf("th:fragment=\"colaDeFermentadores(dashboard)\""), html.indexOf("th:fragment=\"planificacionesPendientes(dashboard)\""));

        assertThat(produccion).doesNotContain("Situación").doesNotContain("data-atraso").doesNotContain("En plazo").doesNotContain("diasDeAtraso").doesNotContain("bg-rose-50");
        assertThat(count(produccion, "<th class=\"")).as("Lote, Fermentador, Prioridad, Fin estimado y Acciones").isEqualTo(5);
        assertThat(html).doesNotContain("data-en-plazo");
        assertThat(cola).contains("th:if=\"${f.loteEnEjecucion.diasDeAtraso > 0}\"").contains("bg-rose-50 text-rose-700\">Atrasado <th:block th:replace=\"~{lote/formato :: duracion(${f.loteEnEjecucion.diasDeAtraso}, 'día', 'días')}\"");
        assertThat(count(html, "planificacion_produccion/formato :: prioridadPlanificacion(")).isEqualTo(2);
        assertThat(html).contains("fragments/enlace-equipo :: enlace(${l.idFermentador}, ${l.identificadorInternoFermentador})")
                .contains("fragments/enlace-equipo :: enlace(${f.id}, ${f.identificadorInterno})")
                .contains("planificacion_produccion/formato :: fecha(${p.fechaInicioEstimada})");
    }

    @Test
    @DisplayName("CP-PDG-07: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los tres textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay lotes en ejecución", "No hay fermentadores registrados", "No hay planificaciones pendientes")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(3);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDG-08: el panel no tiene emojis y ningún mensaje del panel termina en punto")
    void sinEmojisNiPuntoFinal() throws IOException {
        String html = leer(PANEL);

        html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
        assertThat(html).doesNotContain(".</p>").doesNotContain("días.<").doesNotContain("servicio.<");
    }

    @Test
    @DisplayName("CP-PDG-09: cada bloque es una tarjeta con título y cada sub-bloque con filas lleva el total real y el enlace Ver todos protegido por el permiso de su lista")
    void encabezadosDeLosBloques() throws IOException {
        String html = leer(PANEL);

        for (String titulo : List.of("Producción en curso", "Cola de fermentadores", ">Planificaciones pendientes</h2>")) {
            assertThat(html).as(titulo).contains(titulo);
        }
        assertThat(html).contains("th:fragment=\"encabezado(titulo, total, lista, url, etiquetaAccesible)\"").contains("data-ver-todos").contains("data-ver-cronograma")
                .contains("${e.lotes.total}").contains("${dashboard.colaDeFermentadores.total}").contains("${dashboard.planificacionesPendientes.total}");
        assertThat(html).doesNotContain(".filas.size()");
        assertThat(html).contains("bg-white border border-slate-200 rounded-xl shadow-sm").contains("class=\"px-2 py-3\"");
    }

    @Test
    @DisplayName("CP-PDG-10: en cada tabla de los paneles el encabezado Acciones y la celda del botón comparten la alineación (centrado), con el mismo relleno horizontal, igual en el panel del gerente y en el del operario")
    void alineacionDeLaColumnaAcciones() throws IOException {
        for (String ruta : List.of(PANEL, "home/dashboard-operario.html")) {
            String html = leer(ruta);
            List<String> encabezados = clases(html, "<th class=\"col-acciones");
            List<String> celdas = clases(html, "<td class=\"col-acciones");
            List<String> contenedores = clases(html, "<div class=\"flex items-center justify-").stream().filter(c -> c.endsWith("gap-1")).toList();

            assertThat(encabezados).as(ruta + " un encabezado por cada celda").hasSameSizeAs(celdas);
            assertThat(encabezados).as(ruta + " encabezados").isNotEmpty().allSatisfy(c -> assertThat(c).contains("px-2 py-3").contains("text-center").doesNotContain("text-right"));
            assertThat(celdas).as(ruta + " celdas").allSatisfy(c -> assertThat(c).contains("px-2 py-3").doesNotContain("text-right"));
            assertThat(contenedores).as(ruta + " botones").isNotEmpty().allSatisfy(c -> assertThat(c).contains("justify-center").doesNotContain("justify-end"));
                    }
    }

    @Test
    @DisplayName("CP-PDG-11: el panel arma primero, en su propia fila, el aviso de planificaciones con litros sin planificar, con el fragmento compartido con el listado de planificaciones y protegido por el permiso LOTE_REGISTRAR")
    void avisoDePlanesPendientes() throws IOException {
        String html = leer(PANEL);
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        String aviso = html.substring(html.indexOf("th:fragment=\"avisoPlanesPendientes(dashboard)\""));
        String compartido = leer("planificacion_produccion/aviso-planes-pendientes.html");

        assertThat(panel.indexOf(":: avisoPlanesPendientes(")).isNotNegative().isLessThan(panel.indexOf(":: contadores("));
        assertThat(aviso.substring(0, aviso.indexOf("</th:block>\n</th:block>")))
                .contains("sec:authorize=\"hasAuthority('LOTE_REGISTRAR')\"")
                .contains("planificacion_produccion/aviso-planes-pendientes :: aviso(${dashboard.planificacionesConLitrosAPlanificar})");
        assertThat(leer("planificacion_produccion/planificacion-lista.html"))
                .contains("planificacion_produccion/aviso-planes-pendientes :: aviso(${planificacionesConLitrosAPlanificar})");
        assertThat(compartido).contains("th:fragment=\"aviso(planificaciones)\"").contains("!#lists.isEmpty(planificaciones)")
                .contains("bi bi-bell").contains("border-amber-200 bg-amber-50")
                .contains("'Hay 1 planificación de producción con litros sin planificar'")
                .contains("|Hay ${#lists.size(planificaciones)} planificaciones de producción con litros sin planificar|")
                .contains("@{/planificaciones-produccion/{id}/planes(id=${p.id})}")
                .contains("receta/formato :: numero(${p.litrosAPlanificar})");
        compartido.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
    }

    /** El valor del atributo class de cada etiqueta que empieza con el prefijo dado. */
    private static List<String> clases(String html, String prefijo) {
        List<String> clases = new java.util.ArrayList<>();
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
