package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas del control de equipamiento: filas por tema del bloque de información y del formulario,
 * botón Abrir y ojo de la lista, pestañas con ícono y enlace del identificador del equipo en las pantallas de lote.
 */
class ControlEquipamientoPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-PEQ-01: el bloque de información del equipo separa en filas la identidad, los parámetros del tipo y el mantenimiento")
    void informacionPorTema() throws IOException {
        List<String> filas = filas(leer("control_equipamiento/equipo-informacion.html"));

        assertThat(filaCon(filas, ">Identificador<")).contains(">Tipo<", ">Estado operativo<").doesNotContain("Capacidad").doesNotContain("Usos");
        assertThat(filaCon(filas, ">Capacidad total<")).contains(">Evaporación<", ">Rendimiento de molienda<").doesNotContain("Identificador").doesNotContain("Última limpieza");
        assertThat(filaCon(filas, ">Usos desde el último mantenimiento<")).contains(">Última limpieza<", ">Último mantenimiento<").doesNotContain("Capacidad").doesNotContain("Estado operativo");
    }

    @Test
    @DisplayName("CP-PEQ-02: el formulario de registro lleva la fecha en una fila y las observaciones en otra, con asterisco y ejemplo, y es novalidate")
    void formularioPorTema() throws IOException {
        String html = leer("control_equipamiento/control-form.html");
        List<String> filas = filas(html);

        assertThat(filaCon(filas, "for=\"fecha\"")).doesNotContain("observaciones").doesNotContain("fechaFin");
        assertThat(filaCon(filas, "for=\"observaciones\"")).doesNotContain("fecha\"").contains("th:placeholder");
        assertThat(html).contains("novalidate").contains("datetime-local").contains("text-rose-600");
    }

    @Test
    @DisplayName("CP-PEQ-03: la lista de control usa el fragmento del botón Abrir sin copiar su marcado, y el ojo es solo del gerente y lleva aria-label sin tooltip")
    void listaConAbrirYOjo() throws IOException {
        String html = leer("control_equipamiento/control-lista.html");
        Matcher ojo = Pattern.compile("<a sec:authorize=\"hasRole\\('GERENTE_DE_PRODUCCION'\\)\"[^>]*data-ver-historial[^>]*>", Pattern.DOTALL).matcher(html);

        assertThat(html).contains("fragments/boton-abrir :: abrir(").doesNotContain("data-boton-abrir");
        assertThat(ojo.find()).isTrue();
        assertThat(ojo.group()).contains("aria-label").doesNotContain("title=").doesNotContain("data-tooltip");
    }

    @Test
    @DisplayName("CP-PEQ-14: la lista de control tiene cinco columnas: Identificador, Tipo, Estado operativo, Usos y Acciones")
    void listaConCincoColumnas() throws IOException {
        String html = leer("control_equipamiento/control-lista.html");
        String encabezado = html.substring(html.indexOf("<thead"), html.indexOf("</thead>"));

        assertThat(Pattern.compile("<th[ >]").matcher(encabezado).results().count()).isEqualTo(5);
        assertThat(encabezado).contains("Identificador").contains("Tipo").contains("Estado operativo").contains("Usos").contains("Acciones")
                .doesNotContain("Última operación");
        assertThat(html).contains("colspan=\"5\"").doesNotContain("data-equipo-ultima-operacion");
    }

    @Test
    @DisplayName("CP-PEQ-04: las pestañas del panel usan el fragmento de pestañas con un ícono")
    void pestanasConIcono() throws IOException {
        String html = leer("control_equipamiento/tabs.html");

        assertThat(html).contains("fragments/pestanas :: pestana(").contains("fragments/pestanas :: bi('bi-wrench')");
    }

    @Test
    @DisplayName("CP-PEQ-05: el identificador del equipo es un enlace ámbar al detalle solo con EQUIPAMIENTO_CONSULTAR y texto plano sin ese permiso")
    void enlaceDelEquipo() throws IOException {
        String fragmento = leer("fragments/enlace-equipo.html");

        assertThat(fragmento).contains("sec:authorize=\"hasAuthority('EQUIPAMIENTO_CONSULTAR')\"").contains("sec:authorize=\"!hasAuthority('EQUIPAMIENTO_CONSULTAR')\"")
                .contains("/equipamiento/{id}/detalle").contains("text-amber-700");
    }

    @Test
    @DisplayName("CP-PEQ-06: las pantallas de lote y de planificación muestran el identificador del equipo con el fragmento de enlace")
    void pantallasDeLoteUsanElEnlace() throws IOException {
        assertThat(leer("lote/lote-informacion.html")).contains("fragments/enlace-equipo :: enlace(${lote.idFermentador}").contains("fragments/enlace-equipo :: enlace(${e.equipamiento?.id}");
        assertThat(leer("lote/lote-detalle.html")).contains("fragments/enlace-equipo :: enlace(${lote.idFermentador}").contains("fragments/enlace-equipo :: enlace(${etapaMostrada.equipamiento?.id}");
        assertThat(leer("lote/lote-guias.html").split("fragments/enlace-equipo", -1)).hasSize(6);
        assertThat(leer("planificacion_produccion/planificacion-detalle.html")).contains("fragments/enlace-equipo :: enlace(${l.idFermentador}");
    }

    @Test
    @DisplayName("CP-PEQ-07: las cuatro listas de gestión de equipamiento tienen el ojo hacia el detalle con aria-label y sin tooltip")
    void gestionConOjo() throws IOException {
        for (String lista : List.of("molino", "macerador", "olla", "fermentador")) {
            Matcher ojo = Pattern.compile("<a th:href=\"@\\{/equipamiento/\\{id\\}/detalle\\(id=\\$\\{x.id\\}\\)\\}\"[^>]*data-ver-equipo[^>]*>", Pattern.DOTALL).matcher(leer("equipamiento/" + lista + "-lista.html"));
            assertThat(ojo.find()).as(lista).isTrue();
            assertThat(ojo.group()).contains("aria-label").doesNotContain("title=");
        }
    }

    @Test
    @DisplayName("CP-PEQ-08: la barra lateral tiene Control de Equipamientos en Operación para quien consulta limpiezas")
    void sidebar() throws IOException {
        String html = leer("fragments/sidebar.html");

        assertThat(html).contains("hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')").contains("'/control-equipamiento'").contains("bi-wrench-adjustable");
    }

    @Test
    @DisplayName("CP-PEQ-11: el historial usa el patrón de las listas: filtros en una grilla con Limpiar y Buscar, tabla estándar con encabezado gris, badge de estado operativo y fragmento de paginación")
    void historialConElPatronDeLasListas() throws IOException {
        String html = leer("control_equipamiento/control-historial.html");

        assertThat(html).contains("fragments/paginacion :: paginacion(").contains("equipamiento/estado :: badge(").contains("bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider")
                .contains("divide-y divide-slate-100").contains("Limpiar").contains("Buscar").contains("novalidate").contains("name=\"fechaDesde\"").contains("name=\"fechaHasta\"")
                .contains("El equipamiento no tiene operaciones registradas").contains("No se encontraron resultados")
                .doesNotContain("boton-abrir");
        assertThat(html.substring(html.indexOf("<form"), html.indexOf("</form>"))).contains("grid grid-cols-1").contains("for=\"tipo\"").contains("for=\"fechaDesde\"").contains("for=\"fechaHasta\"");
    }

    @Test
    @DisplayName("CP-PEQ-10: el historial, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado, no muestran Responsable y solo el detalle de la operación muestra Registrado por")
    void sinAccesosCruzadosNiPersonas() throws IOException {
        for (String plantilla : List.of("control_equipamiento/control-historial.html", "control_equipamiento/control-panel.html",
                "control_equipamiento/operacion-detalle.html", "equipamiento/equipamiento-detalle.html")) {
            String html = leer(plantilla);
            if (!plantilla.endsWith("-historial.html") && !plantilla.endsWith("-panel.html")) {
                assertThat(html).as(plantilla).doesNotContain("bi-eye");
            }
            assertThat(html).as(plantilla).doesNotContain("boton-abrir").doesNotContain("data-ver-historial")
                    .doesNotContain("Responsable").doesNotContain("usuarioAnulacion").doesNotContain("Anulado por");
            if (!plantilla.endsWith("operacion-detalle.html")) {
                assertThat(html).as(plantilla).doesNotContain("registradoPor").doesNotContain("Registrado por");
            }
        }
        assertThat(leer("control_equipamiento/control-panel.html")).contains("botonSinUsuario");
    }

    @Test
    @DisplayName("CP-PEQ-13: las pantallas de Control de Equipamientos repiten la estructura de las pantallas de referencia (encabezado, tabla con Acciones fija, botón + Nueva en la tarjeta, formulario con aviso de obligatorios, Cancelar y Registrar)")
    void consistenciaConLasPantallasDeReferencia() throws IOException {
        String panel = leer("control_equipamiento/control-panel.html");
        assertThat(panel).contains("mt-2 flex flex-wrap items-center gap-x-3 gap-y-1").contains("data-nueva-operacion").contains("bi bi-plus-lg")
                .contains("px-5 py-3 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3").contains("col-acciones px-4 py-3 text-right")
                .contains("bg-amber-500 text-white hover:bg-amber-600 shadow-sm").doesNotContain("<form").doesNotContain("datetime-local");
        String formulario = leer("control_equipamiento/control-form.html");
        assertThat(formulario).contains("Los campos marcados con").contains("mt-1 text-sm text-slate-500").contains("novalidate data-un-solo-envio").contains("divide-y divide-slate-100")
                .contains("p-6 flex items-center justify-end gap-3 bg-slate-50 rounded-b-xl").contains(">Cancelar</a>").contains("text-rose-600\" title=\"Obligatorio\"")
                .contains("space-y-5 mt-4").contains("fragments/form-errors :: resumen(");
        for (String plantilla : List.of("control_equipamiento/control-historial.html", "control_equipamiento/operacion-detalle.html")) {
            assertThat(leer(plantilla)).as(plantilla).contains("flex flex-wrap items-center gap-x-3 gap-y-1").contains("inline-flex items-center gap-1 text-sm text-slate-500 hover:text-amber-600");
        }
    }

    @Test
    @DisplayName("CP-PEQ-12: ninguna tabla de Control de Equipamientos escribe las observaciones en la fila: usan el botón i del fragmento compartido, cuyo diálogo recibe el título, la etiqueta y la fecha por atributo")
    void observacionesSoloEnElDialogo() throws IOException {
        for (String plantilla : List.of("control_equipamiento/control-historial.html", "control_equipamiento/control-panel.html")) {
            String html = leer(plantilla);
            String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));
            assertThat(tabla).as(plantilla).contains("fragments/detalle-anulacion :: observaciones(").doesNotContain("th:text=\"${o.observaciones}\"").doesNotContain("th:text=\"${a.observaciones")
                    .doesNotContain("${a.observaciones != null} ?");
        }
        // Sin una columna propia para el botón i: va en la celda de la fecha (panel) o de la operación (historial)
        assertThat(leer("control_equipamiento/control-panel.html")).doesNotContain("<th class=\"px-4 py-3\">Observaciones</th>");
        String historial = leer("control_equipamiento/control-historial.html");
        assertThat(historial.substring(historial.indexOf("<thead"), historial.indexOf("</thead>")).split("<th ", -1)).hasSize(5);
        // Ningún dato es un enlace de texto: la navegación va en botones de la columna Acciones, y el botón i está dentro de esa celda
        for (String plantilla : List.of("control_equipamiento/control-historial.html", "control_equipamiento/control-panel.html")) {
            String html = leer(plantilla);
            String cuerpo = html.substring(html.indexOf("<tbody"), html.indexOf("</tbody>"));
            assertThat(cuerpo).as(plantilla).doesNotContain("text-amber-700").contains("bi bi-eye");
            String acciones = cuerpo.substring(cuerpo.indexOf("<td class=\"col-acciones"), cuerpo.indexOf("</td>", cuerpo.indexOf("<td class=\"col-acciones")));
            assertThat(acciones).as(plantilla).contains("fragments/detalle-anulacion :: observaciones(");
            assertThat(cuerpo.indexOf("fragments/detalle-anulacion :: observaciones(")).as(plantilla).isGreaterThan(cuerpo.indexOf("<td class=\"col-acciones"));
        }
        assertThat(leer("control_equipamiento/operacion-detalle.html")).contains("break-words").contains("operacion.observaciones");
        String fragmento = leer("fragments/detalle-anulacion.html");
        assertThat(fragmento).contains("th:fragment=\"observaciones(titulo, texto, fecha)\"").contains("bi bi-info-circle").contains("aria-label=\"Ver observaciones\"")
                .contains("data-detalle-etiqueta-motivo=\"Observaciones\"").contains("th:fragment=\"boton(cancelacion, motivo, usuario, fecha)\"");
        assertThat(leer("fragments/dialogo-detalle-anulacion.html")).contains("data-detalle-etiqueta-motivo-destino").contains("data-detalle-etiqueta-fecha-destino");
        String js = Files.readString(Path.of("").toAbsolutePath().resolve("src/main/resources/static/js/modales.js"), StandardCharsets.UTF_8);
        assertThat(js).contains("data-detalle-etiqueta-motivo").contains("'Motivo'").contains("'Fecha y hora'");
    }

    private static List<String> filas(String html) {
        List<String> resultado = new ArrayList<>();
        Matcher inicio = Pattern.compile("<(div|dl)\\b[^>]*class=\"[^\"]*\\bgrid\\b[^\"]*\"[^>]*>").matcher(html);
        while (inicio.find()) {
            String etiqueta = inicio.group(1);
            Matcher marcas = Pattern.compile("<(/?)" + etiqueta + "\\b[^>]*?(/?)>").matcher(html);
            int profundidad = 0;
            int pos = inicio.start();
            while (marcas.find(pos)) {
                pos = marcas.end();
                if (!marcas.group(2).isEmpty()) {
                    continue;
                }
                profundidad += marcas.group(1).isEmpty() ? 1 : -1;
                if (profundidad == 0) {
                    resultado.add(html.substring(inicio.end(), marcas.start()));
                    break;
                }
            }
        }
        return resultado;
    }

    private static String filaCon(List<String> filas, String marca) {
        return filas.stream().filter(f -> f.contains(marca)).findFirst().orElseThrow(() -> new AssertionError("No hay una fila con " + marca));
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(TEMPLATES.resolve(relativo), StandardCharsets.UTF_8);
    }
}
