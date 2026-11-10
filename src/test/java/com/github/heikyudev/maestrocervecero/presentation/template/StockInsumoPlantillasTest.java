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
 * Guardas de las plantillas del stock de insumos: entrada del menú, botón Abrir, columnas, pestañas con ícono y ausencia
 * de distintivos de vencido y de costos.
 */
class StockInsumoPlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");
    private static final List<String> PANTALLAS = List.of("stock_insumo/stock-lista.html", "stock_insumo/stock-detalle-insumo.html", "stock_insumo/stock-detalle-lote.html");

    @Test
    @DisplayName("CP-PSI-01: la barra lateral tiene Stock de Insumos en Operación, protegido por el permiso STOCK_INSUMO_CONSULTAR y con su clave activa")
    void sidebar() throws IOException {
        String html = leer("fragments/sidebar.html");
        int operacion = html.indexOf("seccion('Operación')");
        int administracion = html.indexOf("seccion('Administración')");
        int entrada = html.indexOf("'/stock-insumos'");

        assertThat(entrada).isGreaterThan(operacion).isLessThan(administracion);
        assertThat(html).contains("'stock-insumos', 'bi-stack', 'Stock de Insumos'");
        String bloque = html.substring(html.lastIndexOf("<th:block", entrada), entrada);
        assertThat(bloque).contains("hasAuthority('STOCK_INSUMO_CONSULTAR')");
        assertThat(html.substring(operacion - 400, operacion)).contains("'OPERARIO_DE_PRODUCCION'").contains("'GERENTE_DE_PRODUCCION'")
                .contains("'GERENTE_DE_COMPRAS'").contains("'ENCARGADO_DE_DEPOSITO'");
    }

    @Test
    @DisplayName("CP-PSI-02: el título de la pestaña del navegador conoce la clave stock-insumos")
    void tituloDelLayout() throws IOException {
        assertThat(leer("layout/base.html")).contains("'stock-insumos': 'Stock de insumos'");
        for (String pantalla : PANTALLAS) {
            assertThat(leer(pantalla)).as(pantalla).contains("layout/base :: layout(").contains(", 'stock-insumos')");
        }
    }

    @Test
    @DisplayName("CP-PSI-03: las listas usan el fragmento del botón Abrir sin copiar su marcado, dentro de la columna Acciones")
    void botonAbrir() throws IOException {
        for (String pantalla : List.of("stock_insumo/stock-lista.html", "stock_insumo/stock-detalle-insumo.html")) {
            String html = leer(pantalla);
            assertThat(html).as(pantalla).contains("fragments/boton-abrir :: abrir(").doesNotContain("data-boton-abrir");
            String celda = html.substring(html.indexOf("<td class=\"col-acciones"), html.indexOf("</td>", html.indexOf("<td class=\"col-acciones")));
            assertThat(celda).as(pantalla).contains("boton-abrir");
        }
    }

    @Test
    @DisplayName("CP-PSI-04: cada tabla lleva sus columnas de cantidades y la lista de insumos sus filtros con ejemplo, Limpiar y Buscar")
    void columnasYFiltros() throws IOException {
        String lista = leer("stock_insumo/stock-lista.html");
        String insumo = leer("stock_insumo/stock-detalle-insumo.html");
        String lote = leer("stock_insumo/stock-detalle-lote.html");

        assertThat(lista).contains(">Nombre</th>").contains(">Tipo</th>").contains(">Unidad</th>").contains(">Disponible</span>").contains(">Reservada</span>")
                .contains(">Vencida</span>").contains(">Total</span>").contains(">Acciones</th>")
                .contains("placeholder=\"Ej: Pilsen\"").contains(">Limpiar</a>").contains("Buscar").contains("name=\"situacionStock\"").contains("name=\"tipo\"").contains("novalidate");
        assertThat(insumo).contains(">Lote del proveedor</th>").contains(">Vence</th>").contains(">Disponible</span>").contains(">Reservada</span>")
                .contains(">Vencida</span>").contains(">Total</span>").contains(">Acciones</th>");
        assertThat(lote).contains("Reducida por ajuste").contains(">Pendiente</span>").contains(">Lote de producción</th>").contains(">Etapa</th>").contains(">Tipo</th>")
                .contains(">Fecha</th>").contains(">Estado</th>").contains(">Acciones</th>");
    }

    @Test
    @DisplayName("CP-PSI-05: el detalle del lote usa pestañas con ícono y el enlace al lote de producción se protege con LOTE_CONSULTAR")
    void detalleDelLote() throws IOException {
        String html = leer("stock_insumo/stock-detalle-lote.html");

        assertThat(html).contains("fragments/pestanas :: pestanas(").contains("fragments/pestanas :: bi('bi-bookmarks')").contains("fragments/pestanas :: bi('bi-box-arrow-up')")
                .doesNotContain("~{}");
        assertThat(count(html, "hasAuthority('LOTE_CONSULTAR')")).isEqualTo(4);
        assertThat(count(html, "!hasAuthority('LOTE_CONSULTAR')")).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-PSI-06: ninguna pantalla del stock muestra un distintivo Vencido ni costos")
    void sinVencidoNiCostos() throws IOException {
        for (String pantalla : PANTALLAS) {
            String html = leer(pantalla);
            assertThat(html).as(pantalla).doesNotContain("Vencido").doesNotContain("PPP").doesNotContain("Costo").doesNotContain("costo");
        }
    }

    private static String leer(String ruta) throws IOException {
        return Files.readString(TEMPLATES.resolve(ruta), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static int count(String texto, String buscado) {
        return texto.split(java.util.regex.Pattern.quote(buscado), -1).length - 1;
    }
}
