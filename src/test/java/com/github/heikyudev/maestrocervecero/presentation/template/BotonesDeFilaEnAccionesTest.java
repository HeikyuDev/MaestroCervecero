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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de consistencia: todo botón de fila de una tabla (el botón i del detalle de una anulación, cancelación u observaciones,
 * el ojo "Ver", modificar, dar de baja, anular, "Abrir") va en la celda de la columna Acciones ({@code col-acciones}), a la
 * derecha de todo, y nunca en otra celda junto a un badge o a un dato. El botón del encabezado de un detalle (junto al badge
 * de estado de la cabecera) no es de una fila y no se revisa. Tampoco los buscadores modales, cuyo botón "Elegir" es la fila.
 */
class BotonesDeFilaEnAccionesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Pattern TABLA = Pattern.compile("<table.*?</table>", Pattern.DOTALL);
    private static final Pattern CELDA = Pattern.compile("<td\\b([^>]*)>(.*?)</td>", Pattern.DOTALL);
    private static final Pattern BOTON_DE_FILA = Pattern.compile(
            "detalle-anulacion ::|data-detalle-anulacion|fragments/boton-abrir|data-ver-|bi-eye|bi-pencil-square|bi-trash|bi-info-circle|data-motivo-dialogo|data-baja-url");

    @Test
    @DisplayName("CP-BFA-01: ningún botón de fila (i de anulación u observaciones, ojo, modificar, baja, anular, Abrir) está fuera de una celda de la columna Acciones")
    void botonesDeFilaSoloEnAcciones() throws IOException {
        List<String> fuera = new ArrayList<>();
        int tablasConBoton = 0;
        try (Stream<Path> archivos = Files.walk(TEMPLATES)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".html")).toList()) {
                String relativo = TEMPLATES.relativize(archivo).toString().replace('\\', '/');
                if (archivo.getFileName().toString().startsWith("buscador-")) {
                    continue;
                }
                String html = Files.readString(archivo, StandardCharsets.UTF_8);
                Matcher tablas = TABLA.matcher(html);
                while (tablas.find()) {
                    Matcher celdas = CELDA.matcher(tablas.group());
                    while (celdas.find()) {
                        boolean enAcciones = celdas.group(1).contains("col-acciones");
                        if (BOTON_DE_FILA.matcher(celdas.group(2)).find()) {
                            if (enAcciones) {
                                tablasConBoton++;
                            } else {
                                fuera.add(relativo + ": " + celdas.group(2).strip().replaceAll("\\s+", " ").substring(0, Math.min(110, celdas.group(2).strip().length())));
                            }
                        }
                    }
                }
            }
        }
        assertThat(fuera).as("celdas con un botón de fila fuera de la columna Acciones").isEmpty();
        assertThat(tablasConBoton).isGreaterThan(15);
    }

    @Test
    @DisplayName("CP-BFA-02: la columna Acciones de las tablas de ingresos, consumos y envasados no depende del permiso ni del estado operable: solo el contenido de cada celda se condiciona")
    void columnaAccionesIncondicional() throws IOException {
        for (String plantilla : List.of("ingreso_insumo/ingreso-lista.html", "lote/consumo-lista.html", "lote/envasado-lista.html")) {
            String html = Files.readString(TEMPLATES.resolve(plantilla), StandardCharsets.UTF_8);
            Matcher encabezado = Pattern.compile("<th\\b([^>]*)>Acciones</th>").matcher(html);
            assertThat(encabezado.find()).as(plantilla).isTrue();
            assertThat(encabezado.group(1)).as(plantilla).doesNotContain("th:if").doesNotContain("sec:authorize");
            Matcher celda = Pattern.compile("<td\\b([^>]*col-acciones[^>]*)>").matcher(html);
            assertThat(celda.find()).as(plantilla).isTrue();
            assertThat(celda.group(1)).as(plantilla).doesNotContain("th:if").doesNotContain("sec:authorize");
        }
    }

    @Test
    @DisplayName("CP-BFA-03: las tablas de lote-informacion (consumos y envasados) y de envasados del detalle del lote tienen su columna Acciones con el botón i de la anulación")
    void tablasDeSoloLecturaConAcciones() throws IOException {
        for (String plantilla : List.of("lote/lote-informacion.html", "lote/lote-detalle.html")) {
            String html = Files.readString(TEMPLATES.resolve(plantilla), StandardCharsets.UTF_8);
            assertThat(html).as(plantilla).contains(">Acciones</th>").contains("col-acciones px-");
        }
        assertThat(Files.readString(TEMPLATES.resolve("control_equipamiento/control-panel.html"), StandardCharsets.UTF_8))
                .contains("botonSinUsuario").contains("observaciones(");
    }
}
