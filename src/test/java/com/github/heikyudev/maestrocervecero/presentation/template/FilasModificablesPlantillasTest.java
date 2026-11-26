package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de las tablas de ítems que se arman con una ventana emergente (maltas, lúpulos y levaduras de la receta, catálogo
 * del proveedor e ítems de la orden de compra): cada fila se puede modificar además de quitar. "Modificar" vuelve a abrir
 * la misma ventana con los datos de la fila y, al confirmar, la reemplaza en su lugar.
 */
class FilasModificablesPlantillasTest {

    private static final Path RECURSOS = Path.of("src/main/resources");

    /** Plantilla de las filas, cantidad de fragmentos de fila y atributos con los textos de los selectores. */
    private static final Map<String, List<String>> FILAS = Map.of(
            "templates/receta/receta-form-filas.html", List.of("3", "th:data-etiqueta-insumo=\"${nombre}\""),
            "templates/proveedor/proveedor-form-filas.html", List.of("1", "th:data-etiqueta-insumo=\"${insumo}\"", "th:data-etiqueta-presentacion=\"${presentacion}\""),
            "templates/orden_compra/orden-form-filas.html", List.of("1", "th:data-etiqueta-insumo=\"${insumo}\""));

    private static final List<String> SCRIPTS = List.of("static/js/receta-form.js", "static/js/proveedor-form.js", "static/js/orden-compra-form.js");

    @Test
    @DisplayName("CP-FMP-01: cada fila de las tablas de ítems ofrece \"Modificar\" (ícono de lápiz) antes de \"Quitar\" en la columna Acciones, y lleva los textos de sus selectores")
    void filas_ofrecenModificarYQuitar() throws IOException {
        for (Map.Entry<String, List<String>> plantilla : FILAS.entrySet()) {
            String html = leer(plantilla.getKey());
            int fragmentos = Integer.parseInt(plantilla.getValue().get(0));
            Matcher acciones = Pattern.compile("<td class=\"col-acciones[^\"]*\">(.*?)</td>", Pattern.DOTALL).matcher(html);
            int celdas = 0;
            while (acciones.find()) {
                celdas++;
                String celda = acciones.group(1);
                assertThat(celda).as(plantilla.getKey())
                        .contains("data-modificar-fila").contains("<i class=\"bi bi-pencil-square\"></i> Modificar")
                        .contains("data-quitar-fila").contains("<i class=\"bi bi-trash\"></i> Quitar");
                assertThat(celda.indexOf("data-modificar-fila")).as(plantilla.getKey()).isLessThan(celda.indexOf("data-quitar-fila"));
                assertThat(celda).as(plantilla.getKey()).contains("text-amber-600 hover:bg-amber-50").contains("text-rose-600 hover:bg-rose-50");
            }
            assertThat(celdas).as(plantilla.getKey()).isEqualTo(fragmentos);
            for (String atributo : plantilla.getValue().subList(1, plantilla.getValue().size())) {
                assertThat(html.split(Pattern.quote(atributo), -1).length - 1).as(plantilla.getKey() + " " + atributo).isEqualTo(fragmentos);
            }
        }
    }

    @Test
    @DisplayName("CP-FMP-02: cada ventana de agregar indica el título y el texto del botón que lleva al modificar una fila")
    void dialogos_tienenElModoModificar() throws IOException {
        Map<String, List<String>> dialogos = Map.of(
                "templates/receta/receta-dialogos.html", List.of("Modificar malta", "Modificar lúpulo", "Modificar levadura"),
                "templates/proveedor/proveedor-dialogos.html", List.of("Modificar ítem del catálogo"),
                "templates/orden_compra/orden-dialogos.html", List.of("Modificar ítem de la orden"));
        for (Map.Entry<String, List<String>> dialogo : dialogos.entrySet()) {
            String html = leer(dialogo.getKey());
            for (String titulo : dialogo.getValue()) {
                assertThat(html).as(dialogo.getKey()).containsPattern("<h3[^>]*data-titulo-dialogo data-titulo-modificar=\"" + titulo + "\"");
            }
            assertThat(html.split("data-confirmar-agregar data-texto-modificar=\"Guardar cambios\"", -1).length - 1)
                    .as(dialogo.getKey()).isEqualTo(dialogo.getValue().size());
        }
    }

    @Test
    @DisplayName("CP-FMP-03: los scripts abren la ventana con los datos de la fila, reemplazan la fila en su lugar y no cuentan la fila modificada como repetida")
    void scripts_modificanLaFilaEnSuLugar() throws IOException {
        for (String script : SCRIPTS) {
            String js = leer(script);
            assertThat(js).as(script)
                    .contains("closest('[data-modificar-fila]')").contains("function abrirModificar(fila)")
                    .contains("function prepararModo(dialogo, fila)").contains("data-titulo-modificar").contains("data-texto-modificar")
                    .contains("reemplazada.replaceWith(fila)").contains("escribirFila(seccion, datos, dialogo.filaModificada)")
                    .contains("return fila !== excluida;")
                    .contains("setAttribute('data-etiqueta-insumo'")
                    // Al cerrar la ventana de cualquier forma (Cancelar, Esc o tras confirmar) deja de modificar la fila
                    .containsPattern("(?s)document\\.addEventListener\\('close', function \\(evento\\) \\{.*?filaModificada = null;.*?\\}, true\\);");
        }
        assertThat(leer("static/js/receta-form.js")).contains("clavesExistentes(tipo, seccion, dialogo.filaModificada)");
        assertThat(leer("static/js/proveedor-form.js")).contains("clavesExistentes(seccion, dialogo.filaModificada)")
                .contains("setAttribute('data-etiqueta-presentacion'");
        // La orden también deja volver a elegir el ítem de la fila en el buscador y recalcula subtotal y total al reemplazarla
        assertThat(leer("static/js/orden-compra-form.js"))
                .contains("idsAgregados(seccion, dialogo.filaModificada)")
                .containsPattern("(?s)function escribirFila\\(seccion, datos, reemplazada\\).*?sincronizar\\(seccion\\);");
    }

    @Test
    @DisplayName("CP-FMP-04: una fila cuyo ítem ya no existe solo ofrece \"Quitar\"; la fila con su ítem y la plantilla vacía ofrecen también \"Modificar\"")
    void filaNoDisponible_soloOfreceQuitar() {
        SpringTemplateEngine motor = motor();
        Map<String, List<String>> casos = Map.of(
                "receta/receta-form-filas :: malta", List.of("malta(0, 7, 'Pilsen', null, false)", "malta(0, 7, null, null, false)", "malta(0, null, null, null, false)"),
                "receta/receta-form-filas :: lupulo", List.of("lupulo(0, 7, 'Cascade', null, null, null, null, true, false)",
                        "lupulo(0, 7, null, null, null, null, null, true, false)", "lupulo(0, null, null, null, null, null, null, true, false)"),
                "receta/receta-form-filas :: levadura", List.of("levadura(0, 7, 'US-05', null, false)", "levadura(0, 7, null, null, false)", "levadura(0, null, null, null, false)"),
                "proveedor/proveedor-form-filas :: item", List.of("item(0, 7, 9, 'Pilsen', 'Malta', 'Bolsa de 25 Kg', null, null, false)",
                        "item(0, 7, 9, 'Pilsen', 'Malta', null, null, null, false)", "item(0, null, null, null, null, null, null, null, false)"),
                "orden_compra/orden-form-filas :: item", List.of("item(0, 7, null, null, 'Pilsen', 'Bolsa de 25 Kg', '25 kg', false)",
                        "item(0, 7, null, null, null, null, null, false)", "item(0, null, null, null, null, null, null, false)"));
        for (Map.Entry<String, List<String>> caso : casos.entrySet()) {
            String plantilla = caso.getKey().substring(0, caso.getKey().indexOf(" :: "));
            String disponible = renderizar(motor, plantilla, caso.getValue().get(0));
            String noDisponible = renderizar(motor, plantilla, caso.getValue().get(1));
            String vacia = renderizar(motor, plantilla, caso.getValue().get(2));
            assertThat(disponible).as(caso.getKey() + " disponible").contains("data-modificar-fila").contains("data-quitar-fila");
            assertThat(noDisponible).as(caso.getKey() + " no disponible").doesNotContain("data-modificar-fila").contains("data-quitar-fila")
                    .containsPattern("no disponible</span>");
            assertThat(vacia).as(caso.getKey() + " plantilla").contains("data-modificar-fila").contains("data-quitar-fila");
        }
    }

    private static SpringTemplateEngine motor() {
        ClassLoaderTemplateResolver fragmentos = new ClassLoaderTemplateResolver();
        fragmentos.setPrefix("templates/");
        fragmentos.setSuffix(".html");
        fragmentos.setTemplateMode(TemplateMode.HTML);
        fragmentos.setResolvablePatterns(Set.of("receta/*", "proveedor/*", "orden_compra/*"));
        fragmentos.setOrder(1);
        StringTemplateResolver literal = new StringTemplateResolver();
        literal.setTemplateMode(TemplateMode.HTML);
        literal.setOrder(2);
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.addTemplateResolver(fragmentos);
        motor.addTemplateResolver(literal);
        return motor;
    }

    private static String renderizar(SpringTemplateEngine motor, String plantilla, String llamada) {
        return motor.process("<table><tbody><tr th:replace=\"~{" + plantilla + " :: " + llamada + "}\"></tr></tbody></table>", new Context());
    }

    private static String leer(String relativo) throws IOException {
        return Files.readString(RECURSOS.resolve(relativo), StandardCharsets.UTF_8);
    }
}
