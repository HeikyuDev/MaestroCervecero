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
 * Guardas de las plantillas del control de barriles: filas por tema del bloque de información y de los formularios, botón
 * Abrir y ojo de la lista, pestañas con ícono, botones de fila en la columna Acciones y observaciones solo en el diálogo.
 */
class ControlBarrilPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final List<String> PANTALLAS = List.of("control_barril/control-lista.html", "control_barril/control-panel.html", "control_barril/control-form.html",
            "control_barril/control-historial.html", "control_barril/operacion-detalle.html");

    @Test
    @DisplayName("CP-PCB-01: el bloque de información del barril separa en filas la identidad, las medidas con el estado y el mantenimiento")
    void informacionPorTema() throws IOException {
        List<String> filas = filas(leer("control_barril/barril-informacion.html"));

        assertThat(filaCon(filas, ">Identificador<")).contains(">Número de serie<", ">Fabricante<").doesNotContain("Capacidad").doesNotContain("Usos");
        assertThat(filaCon(filas, ">Capacidad (L)<")).contains(">Contenido (L)<", ">Estado operativo<").doesNotContain("Identificador").doesNotContain("Última limpieza");
        assertThat(filaCon(filas, ">Usos desde el último mantenimiento<")).contains(">Última limpieza<", ">Último mantenimiento<").doesNotContain("Capacidad").doesNotContain("Estado operativo");
    }

    @Test
    @DisplayName("CP-PCB-02: el formulario lleva cada tema en su fila (cantidad, cliente, fechas, observaciones), con asterisco y ejemplo, es novalidate y la ayuda de la devolución usa el estilo de las ayudas")
    void formularioPorTema() throws IOException {
        String html = leer("control_barril/control-form.html");
        List<String> filas = filas(html);

        assertThat(filaCon(filas, "for=\"cantidadExtraida\"")).doesNotContain("for=\"fecha\"").doesNotContain("observaciones");
        assertThat(filaCon(filas, "idCliente")).doesNotContain("for=\"fecha\"").doesNotContain("observaciones");
        assertThat(filaCon(filas, "for=\"fecha\"")).contains("for=\"fechaDevolucionEstimada\"").doesNotContain("observaciones").doesNotContain("idCliente");
        assertThat(filaCon(filas, "for=\"observaciones\"")).doesNotContain("for=\"fecha\"").contains("th:placeholder");
        assertThat(html).contains("novalidate").contains("datetime-local").contains("text-rose-600").contains("fragments/campo-unidad :: numero(")
                .contains("fragments/selector-entidad :: selector('idCliente'").contains("mt-1 text-xs text-slate-500\">Al registrar la devolución se descarta el contenido del barril");
    }

    @Test
    @DisplayName("CP-PCB-03: la lista usa el fragmento del botón Abrir sin copiar su marcado, el ojo es solo del gerente y lleva aria-label sin tooltip, y ningún dato de la fila es un enlace")
    void listaConAbrirYOjo() throws IOException {
        String html = leer("control_barril/control-lista.html");
        Matcher ojo = Pattern.compile("<a sec:authorize=\"hasRole\\('GERENTE_DE_PRODUCCION'\\)\"[^>]*data-ver-historial[^>]*>", Pattern.DOTALL).matcher(html);

        assertThat(html).contains("fragments/boton-abrir :: abrir(").doesNotContain("data-boton-abrir");
        assertThat(ojo.find()).isTrue();
        assertThat(ojo.group()).contains("aria-label").doesNotContain("title=").doesNotContain("data-tooltip");
        String cuerpo = html.substring(html.indexOf("<tbody"), html.indexOf("</tbody>"));
        assertThat(cuerpo).doesNotContain("text-amber-700").doesNotContain("enlace-");
    }

    @Test
    @DisplayName("CP-PCB-04: la lista tiene siete columnas, con Limpiar y Buscar a la derecha de la tarjeta de filtros, el fabricante como selector de entidad y el estado en un desplegable")
    void listaConSieteColumnas() throws IOException {
        String html = leer("control_barril/control-lista.html");
        String encabezado = html.substring(html.indexOf("<thead"), html.indexOf("</thead>"));

        assertThat(Pattern.compile("<th[ >]").matcher(encabezado).results().count()).isEqualTo(7);
        assertThat(encabezado).contains("Identificador").contains("Fabricante").contains("Capacidad (L)").contains("Contenido (L)").contains("Estado operativo").contains("Usos").contains("Acciones");
        assertThat(html).contains("colspan=\"7\"").contains("md:col-span-2 lg:col-span-3 flex items-center justify-end gap-3").contains("fragments/selector-entidad :: selector('idFabricante'")
                .contains("barril/estado :: badge(").contains("name=\"estadoOperativo\"").contains("placeholder=\"Ej: BRL-01\"");
    }

    @Test
    @DisplayName("CP-PCB-05: las pestañas del panel usan el fragmento de pestañas con el ícono de cada tipo de operación")
    void pestanasConIcono() throws IOException {
        String html = leer("control_barril/tabs.html");

        assertThat(html).contains("fragments/pestanas :: pestana(").contains("fragments/pestanas :: bi(${p.icono})");
    }

    @Test
    @DisplayName("CP-PCB-06: la barra lateral tiene Control de Barriles en Operación, con un ícono propio, para quien consulta alguna operación de barril, y la sección Operación la ve el encargado de distribución")
    void sidebar() throws IOException {
        String html = leer("fragments/sidebar.html");

        assertThat(html).contains("'/control-barriles'").contains("'control-barriles', 'bi-clipboard-check', 'Control de Barriles'").contains("LIMPIEZA_BARRIL_CONSULTAR")
                .contains("DESPACHO_BARRIL_CONSULTAR").contains("'ENCARGADO_DE_DISTRIBUCION')\">\n            <p th:replace=\"~{fragments/sidebar ::seccion('Operación')}\"");
        assertThat(html.indexOf("'/control-equipamiento'")).isLessThan(html.indexOf("'/control-barriles'"));
        assertThat(leer("layout/base.html")).contains("'control-barriles': 'Control de barriles'");
    }

    @Test
    @DisplayName("CP-PCB-07: el historial usa el patrón de las listas: filtros en una grilla con Limpiar y Buscar, tabla estándar, badge del estado del barril y fragmento de paginación")
    void historialConElPatronDeLasListas() throws IOException {
        String html = leer("control_barril/control-historial.html");

        assertThat(html).contains("fragments/paginacion :: paginacion(").contains("barril/estado :: badge(").contains("bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider")
                .contains("divide-y divide-slate-100").contains("Limpiar").contains("Buscar").contains("novalidate").contains("name=\"fechaDesde\"").contains("name=\"fechaHasta\"")
                .contains("El barril no tiene operaciones registradas").contains("No se encontraron resultados").contains("md:col-span-2 lg:col-span-3 flex items-center justify-end gap-3")
                .doesNotContain("boton-abrir");
        assertThat(html.substring(html.indexOf("<thead"), html.indexOf("</thead>")).split("<th ", -1)).hasSize(5);
    }

    @Test
    @DisplayName("CP-PCB-08: ninguna pantalla lleva Abrir ni el ojo del historial en su encabezado, ni Responsable, ni el usuario que anuló; solo el detalle de la operación muestra Registrado por")
    void sinAccesosCruzadosNiPersonas() throws IOException {
        for (String plantilla : PANTALLAS.subList(1, PANTALLAS.size())) {
            String html = leer(plantilla);
            assertThat(html).as(plantilla).doesNotContain("boton-abrir").doesNotContain("data-ver-historial").doesNotContain("Responsable")
                    .doesNotContain("usuarioAnulacion").doesNotContain("Anulado por");
            if (!plantilla.endsWith("operacion-detalle.html")) {
                assertThat(html).as(plantilla).doesNotContain("registradoPor").doesNotContain("Registrado por");
            }
        }
        assertThat(leer("control_barril/control-panel.html")).contains("botonSinUsuario");
    }

    @Test
    @DisplayName("CP-PCB-09: el panel repite la estructura de las pantallas de referencia (encabezado, tabla con Acciones fija, botón + Nueva en la tarjeta, aviso de solo consulta) y el formulario la suya (aviso de obligatorios, Cancelar y Registrar)")
    void consistenciaConLasPantallasDeReferencia() throws IOException {
        String panel = leer("control_barril/control-panel.html");
        assertThat(panel).contains("mt-2 flex flex-wrap items-center gap-x-3 gap-y-1").contains("data-nueva-operacion").contains("bi bi-plus-lg")
                .contains("px-5 py-3 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3").contains("col-acciones px-4 py-3 text-right")
                .contains("bg-amber-500 text-white hover:bg-amber-600 shadow-sm").contains("Solo puede consultar las operaciones del barril").doesNotContain("<form").doesNotContain("datetime-local");
        String formulario = leer("control_barril/control-form.html");
        assertThat(formulario).contains("Los campos marcados con").contains("novalidate data-un-solo-envio").contains("divide-y divide-slate-100")
                .contains("p-6 flex items-center justify-end gap-3 bg-slate-50 rounded-b-xl").contains(">Cancelar</a>").contains("text-rose-600\" title=\"Obligatorio\"")
                .contains("space-y-5 mt-4").contains("fragments/form-errors :: resumen(");
        for (String plantilla : List.of("control_barril/control-historial.html", "control_barril/operacion-detalle.html")) {
            assertThat(leer(plantilla)).as(plantilla).contains("flex flex-wrap items-center gap-x-3 gap-y-1").contains("inline-flex items-center gap-1 text-sm text-slate-500 hover:text-amber-600");
        }
    }

    @Test
    @DisplayName("CP-PCB-10: ninguna tabla escribe las observaciones en la fila: usan el botón i del fragmento compartido dentro de la celda Acciones, que va a la derecha")
    void observacionesSoloEnElDialogo() throws IOException {
        for (String plantilla : List.of("control_barril/control-historial.html", "control_barril/control-panel.html")) {
            String html = leer(plantilla);
            String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));
            assertThat(tabla).as(plantilla).contains("fragments/detalle-anulacion :: observaciones(").doesNotContain("th:text=\"${o.observaciones}\"").doesNotContain("th:text=\"${a.observaciones");
            String cuerpo = html.substring(html.indexOf("<tbody"), html.indexOf("</tbody>"));
            assertThat(cuerpo).as(plantilla).doesNotContain("text-amber-700").contains("bi bi-eye");
            String acciones = cuerpo.substring(cuerpo.indexOf("<td class=\"col-acciones"), cuerpo.indexOf("</td>", cuerpo.indexOf("<td class=\"col-acciones")));
            assertThat(acciones).as(plantilla).contains("fragments/detalle-anulacion :: observaciones(");
        }
        assertThat(leer("control_barril/operacion-detalle.html")).contains("break-words").contains("operacion.observaciones");
    }

    @Test
    @DisplayName("CP-PCB-11: en la celda de acciones del panel el orden es i, ojo y anular al final")
    void ordenDeLosBotonesDeFila() throws IOException {
        String panel = leer("control_barril/control-panel.html");
        String cuerpo = panel.substring(panel.indexOf("<tbody"), panel.indexOf("</tbody>"));

        assertThat(cuerpo.indexOf("detalle-anulacion :: observaciones(")).isLessThan(cuerpo.indexOf("botonSinUsuario"));
        assertThat(cuerpo.indexOf("botonSinUsuario")).isLessThan(cuerpo.indexOf("data-ver-operacion"));
        assertThat(cuerpo.indexOf("data-ver-operacion")).isLessThan(cuerpo.indexOf("data-motivo-dialogo"));
    }

    @Test
    @DisplayName("CP-PCB-12: el buscador de clientes usa la tabla de buscador, el botón Elegir sin alta al vuelo y el fragmento de paginación del modal")
    void buscadorDeClientes() throws IOException {
        String html = leer("cliente/buscador-cliente.html");

        assertThat(html).contains("class=\"tabla-buscador\"").contains("data-elegir").contains("shrink-0").contains("data-modal-buscar").contains("data-cerrar-modal")
                .contains("fragments/paginacion :: paginacion(").doesNotContain("/clientes/nuevo");
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
