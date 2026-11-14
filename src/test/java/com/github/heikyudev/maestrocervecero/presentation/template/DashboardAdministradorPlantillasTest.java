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
 * Guardas de las plantillas del panel del administrador: integración en la pantalla de inicio, fragmentos, enlaces con su
 * permiso, alineación de la columna Acciones, estados vacíos, textos sin emojis y ausencia de cualquier referencia a las
 * ubicaciones.
 */
class DashboardAdministradorPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final String PANEL = "home/dashboard-administrador.html";

    @Test
    @DisplayName("CP-PDA-01: la pantalla de inicio muestra el panel solo al ADMINISTRADOR y no tiene tarjetas de resumen: cada rol ve su propio panel")
    void integracionEnLaPantallaDeInicio() throws IOException {
        String html = leer("home.html");
        int panel = html.indexOf("home/dashboard-administrador :: panel(${dashboardAdministrador})");

        assertThat(panel).isPositive();
        assertThat(html.substring(html.lastIndexOf("<th:block sec:authorize", panel), panel)).contains("hasRole('ADMINISTRADOR')");
        assertThat(count(html, "<th:block th:replace=\"~{home/dashboard-")).as("un panel por rol").isEqualTo(6);
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("hasAnyRole").doesNotContain("Usuarios activos");
    }

    @Test
    @DisplayName("CP-PDA-02: el panel define los fragmentos panel, contadores, usuariosPorRol, usuariosRecientes y actividadReciente, y los arma en tres filas que se leen de izquierda a derecha y de arriba abajo")
    void fragmentosYFilas() throws IOException {
        String html = leer(PANEL);

        for (String fragmento : List.of("panel(dashboard)", "contadores(dashboard)", "usuariosPorRol(dashboard)", "usuariosRecientes(dashboard)", "actividadReciente(dashboard)")) {
            assertThat(html).as(fragmento).contains("th:fragment=\"" + fragmento + "\"");
        }
        String panel = html.substring(html.indexOf("th:fragment=\"panel(dashboard)\""), html.indexOf("th:fragment=\"contadores(dashboard)\""));
        List<String> orden = List.of(":: contadores(", ":: usuariosPorRol(", ":: usuariosRecientes(", ":: actividadReciente(");
        for (int i = 1; i < orden.size(); i++) {
            assertThat(panel.indexOf(orden.get(i - 1))).as(orden.get(i - 1)).isNotNegative().isLessThan(panel.indexOf(orden.get(i)));
        }
        assertThat(panel).contains("grid grid-cols-1 xl:grid-cols-3 gap-6");
        assertThat(html).contains("xl:col-span-2");
        assertThat(count(html, "data-contador=\"")).isEqualTo(4);
    }

    @Test
    @DisplayName("CP-PDA-03: los contadores, las fichas por rol y los encabezados llevan a la lista de usuarios, a la lista de usuarios filtrada por rol, a la bitácora del día y a la bitácora")
    void enlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(html).contains("@{/usuarios}").contains("@{/usuarios(rol=${c.codigo})}").contains("@{/usuarios(username=${u.username})}")
                .contains("@{/auditoria(fechaDesde=${#temporals.format(#temporals.createToday(), 'yyyy-MM-dd')}, fechaHasta=${#temporals.format(#temporals.createToday(), 'yyyy-MM-dd')})}")
                .contains("@{/auditoria}").contains("@{/auditoria/{id}(id=${e.id})}");
        for (String titulo : List.of("'Usuarios activos'", "'Eventos de hoy'")) {
            assertThat(html).as(titulo).contains("contenidoContador(" + titulo);
        }
        assertThat(html).contains("${dashboard.usuariosActivos}").contains("${dashboard.eventosDeHoy}").contains("${dashboard.usuariosRecientes.total}").contains("${dashboard.actividadReciente.total}");
    }

    @Test
    @DisplayName("CP-PDA-04: cada enlace se protege con el permiso de su destino (USUARIO_CONSULTAR para los usuarios y el rol ADMINISTRADOR para la bitácora) y sin él los contadores y las fichas quedan como texto plano")
    void permisosDeLosEnlaces() throws IOException {
        String html = leer(PANEL);

        assertThat(count(html, "sec:authorize=\"hasAuthority('USUARIO_CONSULTAR')\"")).as("contador, ficha por rol, Ver todos y Abrir").isEqualTo(4);
        assertThat(count(html, "sec:authorize=\"!hasAuthority('USUARIO_CONSULTAR')\"")).as("contador y ficha por rol").isEqualTo(2);
        assertThat(count(html, "sec:authorize=\"hasRole('ADMINISTRADOR')\"")).as("contador, Ver todos y ojo").isEqualTo(3);
        assertThat(count(html, "sec:authorize=\"!hasRole('ADMINISTRADOR')\"")).isEqualTo(1);
        assertThat(count(html, "fragments/boton-abrir :: abrir(")).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-PDA-05: el panel es de solo lectura: no usa el botón de registro, no enlaza a formularios de alta ni de modificación y el ojo Ver solo se muestra con las entradas que tienen detalle")
    void soloLectura() throws IOException {
        String html = leer(PANEL);

        assertThat(html).doesNotContain("boton-registrar").doesNotContain("/nuevo").doesNotContain("/editar").doesNotContain("/baja")
                .doesNotContain("<form").doesNotContain("<button");
        assertThat(html).contains("th:if=\"${e.tieneDetalle}\"").contains("data-ver-auditoria").contains("bi bi-eye");
    }

    @Test
    @DisplayName("CP-PDA-06: el panel no menciona las ubicaciones, ni países, provincias o localidades, ni lleva columnas de situación")
    void sinUbicaciones() throws IOException {
        String html = leer(PANEL).toLowerCase();

        assertThat(html).doesNotContain("ubicaci").doesNotContain("pais").doesNotContain("país").doesNotContain("provincia").doesNotContain("localidad")
                .doesNotContain("situación");
    }

    @Test
    @DisplayName("CP-PDA-07: en cada tabla el encabezado Acciones y la celda del botón comparten la alineación (centrada) con el mismo relleno horizontal")
    void alineacionDeLaColumnaAcciones() throws IOException {
        String html = leer(PANEL);
        List<String> encabezados = clases(html, "<th class=\"col-acciones");
        List<String> celdas = clases(html, "<td class=\"col-acciones");
        List<String> contenedores = clases(html, "<div class=\"flex items-center justify-").stream().filter(c -> c.endsWith("gap-1")).toList();

        assertThat(encabezados).hasSize(2).hasSameSizeAs(celdas);
        assertThat(encabezados).allSatisfy(c -> assertThat(c).contains("px-2 py-3").contains("text-center").doesNotContain("text-right"));
        assertThat(celdas).allSatisfy(c -> assertThat(c).contains("px-2 py-3").doesNotContain("text-right"));
        assertThat(contenedores).hasSize(2).allSatisfy(c -> assertThat(c).contains("justify-center").doesNotContain("justify-end"));
    }

    @Test
    @DisplayName("CP-PDA-08: cada sub-bloque sin filas muestra una sola línea de texto gris, sin tabla ni encabezado de columnas, con los dos textos del diseño")
    void estadosVacios() throws IOException {
        String html = leer(PANEL);

        for (String texto : List.of("No hay usuarios activos", "No hay eventos registrados en la bitácora")) {
            assertThat(html).as(texto).contains(">" + texto + "</p>");
        }
        assertThat(count(html, "data-estado-vacio")).isEqualTo(2);
        assertThat(count(html, "<div class=\"overflow-x-auto\" th:if=\"${!dashboard.")).isEqualTo(2);
        assertThat(html).doesNotContain("bg-slate-50 rounded").doesNotContain("border-dashed");
    }

    @Test
    @DisplayName("CP-PDA-09: el panel no tiene emojis y ningún mensaje del panel termina en punto")
    void sinEmojisNiPuntoFinal() throws IOException {
        String html = leer(PANEL);

        html.codePoints().forEach(cp -> assertThat(esEmoji(cp)).as("U+%04X", cp).isFalse());
        assertThat(html).doesNotContain(".</p>").doesNotContain("activos.<").doesNotContain("bitácora.<");
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
