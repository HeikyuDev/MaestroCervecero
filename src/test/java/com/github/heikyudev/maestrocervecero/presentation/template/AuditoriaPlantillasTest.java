package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas de la bitácora de auditoría: ojo "Ver" en la columna Acciones de la lista, clave del layout,
 * escape del contenido y colores de la comparación de estados del detalle.
 */
class AuditoriaPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final List<String> PANTALLAS = List.of("auditoria/auditoria-lista.html", "auditoria/auditoria-detalle.html");

    @Test
    @DisplayName("CP-PAU-01: la lista tiene la columna Acciones con el ojo Ver dentro de su celda, condicionado a que la entrada tenga detalle, y la tabla dentro de un contenedor desplazable")
    void listaConOjoVer() throws IOException {
        String html = leer("auditoria/auditoria-lista.html");
        int celda = html.indexOf("<td class=\"col-acciones");
        String contenidoCelda = html.substring(celda, html.indexOf("</td>", celda));

        assertThat(html).contains("<th class=\"col-acciones px-4 py-3 text-right\">Acciones</th>");
        assertThat(contenidoCelda).contains("bi-eye").contains("th:if=\"${l.tieneDetalle}\"").contains("@{/auditoria/{id}(id=${l.id})}")
                .contains("th:aria-label=\"'Ver detalle de la entrada ' + ${l.id}\"").doesNotContain("data-tooltip").doesNotContain("title=");
        assertThat(html.substring(html.lastIndexOf("<div", html.indexOf("<table")), html.indexOf("<table"))).contains("overflow-x-auto");
        assertThat(html).contains("<td colspan=\"7\"");
    }

    @Test
    @DisplayName("CP-PAU-02: las dos pantallas usan la clave auditoria del layout para el título de la pestaña y la opción activa del menú")
    void claveDelLayout() throws IOException {
        for (String pantalla : PANTALLAS) {
            assertThat(leer(pantalla)).as(pantalla).contains("layout/base :: layout(").contains(", 'auditoria')");
        }
    }

    @Test
    @DisplayName("CP-PAU-03: el detalle escapa todo el contenido (th:text) y no usa th:utext")
    void detalleEscapaElContenido() throws IOException {
        for (String pantalla : PANTALLAS) {
            assertThat(leer(pantalla)).as(pantalla).doesNotContain("th:utext").doesNotContain("[(");
        }
    }

    @Test
    @DisplayName("CP-PAU-04: el detalle compara en tres columnas (Campo, Estado anterior, Estado posterior) y colorea modificado en ámbar, agregado en verde y eliminado en rojo")
    void detalleConComparacion() throws IOException {
        String html = leer("auditoria/auditoria-detalle.html");

        assertThat(html).contains(">Campo</th>").contains(">Estado anterior</th>").contains(">Estado posterior</th>");
        assertThat(html.indexOf(">Campo</th>")).isLessThan(html.indexOf(">Estado anterior</th>"));
        assertThat(html.indexOf(">Estado anterior</th>")).isLessThan(html.indexOf(">Estado posterior</th>"));
        assertThat(html).contains("'MODIFICADO'} ? 'bg-amber-50'").contains("'AGREGADO'} ? 'bg-emerald-50'").contains("'ELIMINADO'} ? 'bg-rose-50'")
                .contains("data-estado-campo");
        assertThat(html).contains("<div class=\"overflow-x-auto\">");
    }

    @Test
    @DisplayName("CP-PAU-05: el detalle muestra un guion en los valores vacíos, y un aviso gris sin detalle")
    void detalleGracefulYJson() throws IOException {
        String html = leer("auditoria/auditoria-detalle.html");

        assertThat(html).contains("${c.valorAnterior != null} ? ${c.valorAnterior} : '-'").contains("${c.valorPosterior != null} ? ${c.valorPosterior} : '-'");
        assertThat(html).contains("data-sin-detalle").contains("No hay detalle de estados para este registro").contains("text-slate-400");
        assertThat(html).doesNotContain("<details").doesNotContain("JSON guardado");
    }

    @Test
    @DisplayName("CP-PAU-06: la cabecera del detalle agrupa quién y cuándo en una fila, y módulo, acción y registro en otra")
    void detalleCabeceraPorTema() throws IOException {
        String html = leer("auditoria/auditoria-detalle.html");

        int usuario = html.indexOf(">Usuario<");
        int fecha = html.indexOf(">Fecha y hora<");
        int ip = html.indexOf(">IP<");
        int modulo = html.indexOf(">Módulo<");
        int accion = html.indexOf(">Acción<");
        int id = html.indexOf(">ID afectado<");

        assertThat(usuario).isPositive();
        assertThat(usuario).isLessThan(fecha);
        assertThat(fecha).isLessThan(ip);
        assertThat(ip).isLessThan(modulo);
        assertThat(modulo).isLessThan(accion);
        assertThat(accion).isLessThan(id);
        assertThat(html.substring(usuario, ip)).doesNotContain(">Módulo<");
    }

    private static String leer(String ruta) throws IOException {
        return Files.readString(TEMPLATES.resolve(ruta), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
