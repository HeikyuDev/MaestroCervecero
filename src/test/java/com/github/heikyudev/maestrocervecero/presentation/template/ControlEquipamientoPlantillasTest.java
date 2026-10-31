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
    @DisplayName("CP-PEQ-02: el formulario de registro lleva las fechas en una fila y las observaciones en otra, con asterisco y ejemplo, y es novalidate")
    void formularioPorTema() throws IOException {
        String html = leer("control_equipamiento/control-panel.html");
        List<String> filas = filas(html);

        assertThat(filaCon(filas, "for=\"fechaInicio\"")).contains("for=\"fechaFin\"").doesNotContain("observaciones");
        assertThat(filaCon(filas, "for=\"observaciones\"")).doesNotContain("fechaInicio").contains("th:placeholder");
        assertThat(html).contains("novalidate").contains("datetime-local").contains("text-rose-600");
    }

    @Test
    @DisplayName("CP-PEQ-03: la lista de control usa el fragmento del botón Abrir sin copiar su marcado, y el ojo es solo del gerente y lleva aria-label sin tooltip")
    void listaConAbrirYOjo() throws IOException {
        String html = leer("control_equipamiento/control-lista.html");
        Matcher ojo = Pattern.compile("<a sec:authorize=\"hasRole\\('GERENTE_DE_PRODUCCION'\\)\"[^>]*data-ver-cronograma[^>]*>", Pattern.DOTALL).matcher(html);

        assertThat(html).contains("fragments/boton-abrir :: abrir(").doesNotContain("data-boton-abrir");
        assertThat(ojo.find()).isTrue();
        assertThat(ojo.group()).contains("aria-label").doesNotContain("title=").doesNotContain("data-tooltip");
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
    @DisplayName("CP-PEQ-08: la barra lateral tiene Control de Equipamiento en Operación para quien consulta limpiezas")
    void sidebar() throws IOException {
        String html = leer("fragments/sidebar.html");

        assertThat(html).contains("hasAuthority('LIMPIEZA_EQUIPAMIENTO_CONSULTAR')").contains("'/control-equipamiento'").contains("bi-wrench-adjustable");
    }

    @Test
    @DisplayName("CP-PEQ-09: el diagrama dibuja los bloques cortos como marcadores con ícono, la leyenda lleva los íconos de limpieza, falla y mantenimiento y los lotes conservan sus clases")
    void ganttConMarcadoresEIconos() throws IOException {
        String html = leer("planificacion_produccion/gantt.html");

        assertThat(html).contains("gantt-barra--marcador").contains("gantt-icono")
                .contains("gantt-color--equipo-limpieza\"><i class=\"bi bi-stars")
                .contains("gantt-color--equipo-falla\"><i class=\"bi bi-exclamation-triangle-fill")
                .contains("gantt-color--equipo-mantenimiento\"><i class=\"bi bi-wrench")
                .contains("gantt-color--pendiente").contains("gantt-color--en-ejecucion").contains("gantt-color--finalizado");
    }

    @Test
    @DisplayName("CP-PEQ-10: el cronograma, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado, no muestran Responsable y solo el detalle de la operación muestra Registrado por")
    void sinAccesosCruzadosNiPersonas() throws IOException {
        for (String plantilla : List.of("control_equipamiento/control-cronograma.html", "control_equipamiento/control-panel.html",
                "control_equipamiento/operacion-detalle.html", "equipamiento/equipamiento-detalle.html")) {
            String html = leer(plantilla);
            assertThat(html).as(plantilla).doesNotContain("boton-abrir").doesNotContain("data-ver-cronograma").doesNotContain("bi-eye")
                    .doesNotContain("Responsable").doesNotContain("usuarioAnulacion").doesNotContain("Anulado por");
            if (!plantilla.endsWith("operacion-detalle.html")) {
                assertThat(html).as(plantilla).doesNotContain("registradoPor").doesNotContain("Registrado por");
            }
        }
        assertThat(leer("control_equipamiento/control-panel.html")).contains("botonSinUsuario");
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
