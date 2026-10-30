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
 * Guarda del orden y la agrupación de campos (docs/EstandaresUI.md): en el formulario y en el detalle del proveedor la
 * identificación va primero, el contacto después y la ubicación al final, y la dirección nunca comparte fila con el correo.
 * En el resto de las pantallas cada fila (una grilla) trata un solo tema: identidad, origen o relación, medidas con su
 * unidad, mantenimiento, fechas y estado nunca comparten fila.
 */
class OrdenDeCamposTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-ODC-01 - El detalle del proveedor agrupa identificación, contacto y ubicación, y la dirección va debajo de la ubicación en una fila propia")
    void el_detalle_del_proveedor_agrupa_los_datos() throws IOException {
        // Arrange
        String html = leer("proveedor/proveedor-detalle.html");

        // Act
        int razon = html.indexOf(">Razón social<");
        int cuit = html.indexOf(">CUIT<");
        int telefono = html.indexOf(">Teléfono<");
        int correo = html.indexOf(">Correo electrónico<");
        int pais = html.indexOf(">País<");
        int provincia = html.indexOf(">Provincia<");
        int localidad = html.indexOf(">Localidad<");
        int direccion = html.indexOf(">Dirección<");

        // Assert
        assertThat(razon).isPositive();
        assertThat(razon).isLessThan(cuit);
        assertThat(cuit).isLessThan(telefono);
        assertThat(telefono).isLessThan(correo);
        assertThat(correo).isLessThan(pais);
        assertThat(pais).isLessThan(provincia);
        assertThat(provincia).isLessThan(localidad);
        assertThat(localidad).as("la dirección va después de la ubicación, nunca junto al correo").isLessThan(direccion);
        assertThat(html.substring(html.lastIndexOf("<div", direccion), direccion)).contains("lg:col-span-3");
        assertThat(html.substring(html.lastIndexOf("<div", correo), correo)).contains("lg:col-span-2");
    }

    @Test
    @DisplayName("CP-ODC-02 - El formulario del proveedor sigue el mismo orden que el detalle: identificación, contacto, ubicación y dirección en su propia fila")
    void el_formulario_del_proveedor_sigue_el_orden_del_detalle() throws IOException {
        // Arrange
        String html = leer("proveedor/proveedor-form.html");

        // Act
        int razon = html.indexOf("for=\"razonSocial\"");
        int cuit = html.indexOf("for=\"cuit\"");
        int telefono = html.indexOf("for=\"telefono\"");
        int correo = html.indexOf("for=\"email\"");
        int ubicacion = html.indexOf("fragments/ubicacion-enlazada");
        int direccion = html.indexOf("for=\"direccion\"");

        // Assert
        assertThat(razon).isPositive();
        assertThat(razon).isLessThan(cuit);
        assertThat(cuit).isLessThan(telefono);
        assertThat(telefono).isLessThan(correo);
        assertThat(correo).isLessThan(ubicacion);
        assertThat(ubicacion).as("la dirección va después de la ubicación, nunca junto al correo").isLessThan(direccion);
        assertThat(html.substring(html.lastIndexOf("<div", direccion), direccion)).contains("md:col-span-3");
        assertThat(html.substring(html.lastIndexOf("<div", correo), correo)).contains("md:col-span-2");
    }

    @Test
    @DisplayName("CP-ODC-03 - Los formularios de equipamiento y de barril separan en filas propias las capacidades, los parámetros de proceso y los usos antes de mantenimiento")
    void los_formularios_de_equipamiento_separan_los_temas() throws IOException {
        // Arrange
        List<String> olla = filas(leer("equipamiento/olla-form.html"));
        List<String> macerador = filas(leer("equipamiento/macerador-form.html"));
        List<String> molino = filas(leer("equipamiento/molino-form.html"));
        List<String> fermentador = filas(leer("equipamiento/fermentador-form.html"));
        List<String> barril = filas(leer("barril/barril-form.html"));

        // Act
        String capacidadesOlla = filaCon(olla, "for=\"capacidadTotal\"");
        String procesoOlla = filaCon(olla, "for=\"evaporacion\"");

        // Assert
        assertThat(capacidadesOlla).contains("for=\"capacidadUtil\"").doesNotContain("evaporacion").doesNotContain("usosMaximos");
        assertThat(procesoOlla).contains("for=\"perdidaPorTrub\"").doesNotContain("capacidadTotal").doesNotContain("usosMaximos");
        assertThat(filaCon(macerador, "for=\"eficienciaMaceracion\"")).doesNotContain("capacidad").doesNotContain("usosMaximos");
        assertThat(filaCon(macerador, "for=\"usosMaximosAntesMantenimiento\"")).doesNotContain("capacidad").doesNotContain("eficiencia");
        assertThat(filaCon(molino, "for=\"rendimientoMolienda\"")).doesNotContain("usosMaximos");
        assertThat(filaCon(fermentador, "for=\"usosMaximosAntesMantenimiento\"")).doesNotContain("capacidad");
        assertThat(filaCon(barril, "for=\"numeroSerie\"")).contains("idFabricanteBarril").doesNotContain("for=\"capacidad\"");
        assertThat(filaCon(barril, "for=\"capacidad\"")).doesNotContain("usosMaximos").doesNotContain("numeroSerie");
    }

    @Test
    @DisplayName("CP-ODC-04 - Los formularios de insumo ponen el nombre y el tipo en una fila y la característica del insumo en otra")
    void los_formularios_de_insumo_separan_la_identificacion_de_la_caracteristica() throws IOException {
        // Arrange
        List<String> levadura = filas(leer("insumo/levadura-form.html"));
        List<String> lupulo = filas(leer("insumo/lupulo-form.html"));
        List<String> malta = filas(leer("insumo/malta-form.html"));

        // Act
        String identificacionLevadura = filaCon(levadura, "for=\"nombre\"");
        String identificacionLupulo = filaCon(lupulo, "for=\"nombre\"");
        String identificacionMalta = filaCon(malta, "for=\"nombre\"");

        // Assert
        assertThat(identificacionLevadura).contains("for=\"tipo\"").doesNotContain("celulasEnMillones");
        assertThat(identificacionLupulo).contains("for=\"formato\"").doesNotContain("for=\"aa\"");
        assertThat(identificacionMalta).contains("for=\"tipo\"").doesNotContain("potencialExtracto");
    }

    @Test
    @DisplayName("CP-ODC-05 - El detalle y el formulario de la planificación llevan la receta, la cantidad y las fechas estimadas cada una en su fila")
    void la_planificacion_separa_receta_cantidad_y_fechas() throws IOException {
        // Arrange
        List<String> detalle = filas(leer("planificacion_produccion/planificacion-detalle.html"));
        List<String> formulario = filas(leer("planificacion_produccion/planificacion-form.html"));

        // Act
        String fechasDetalle = filaCon(detalle, ">Inicio estimado<");
        String fechasFormulario = filaCon(formulario, "for=\"fechaInicioEstimada\"");

        // Assert
        assertThat(filaCon(detalle, ">Receta<")).doesNotContain("Cantidad a producir").doesNotContain("Inicio estimado");
        assertThat(filaCon(detalle, ">Cantidad a producir (L)<")).doesNotContain("Inicio estimado").doesNotContain(">Estado<");
        assertThat(fechasDetalle).contains(">Fin estimado<").doesNotContain(">Estado<").doesNotContain("Cantidad");
        assertThat(filaCon(detalle, ">Estado<")).doesNotContain("estimado");
        assertThat(fechasFormulario).contains("for=\"fechaFinalizacionEstimada\"").doesNotContain("cantidadAProducir");
        assertThat(filaCon(formulario, "for=\"cantidadAProducir\"")).doesNotContain("fecha");
    }

    @Test
    @DisplayName("CP-ODC-06 - El detalle de la orden de compra agrupa planificación y proveedor, luego fecha de entrega y estado, y el total solo")
    void el_detalle_de_la_orden_agrupa_por_tema() throws IOException {
        // Arrange
        List<String> detalle = filas(leer("orden_compra/orden-detalle.html"));
        List<String> formulario = filas(leer("orden_compra/orden-form.html"));

        // Act
        String origen = filaCon(detalle, ">Proveedor<");

        // Assert
        assertThat(origen).contains(">Planificación de producción<").doesNotContain("Fecha de entrega").doesNotContain("Total");
        assertThat(filaCon(detalle, ">Fecha de entrega estimada<")).contains(">Estado<").doesNotContain("Total").doesNotContain("Proveedor");
        assertThat(filaCon(detalle, ">Total ($)<")).doesNotContain(">Estado<").doesNotContain("Fecha");
        assertThat(filaCon(formulario, "data-selector-de=\"proveedor\"")).contains("data-selector-de=\"planificacion\"").doesNotContain("fechaEntregaEstimada");
    }

    @Test
    @DisplayName("CP-ODC-07 - Los datos de la receta van en filas de identificación, medidas base y objetivos, y el formulario separa el nombre de las medidas base")
    void la_receta_agrupa_identificacion_medidas_y_objetivos() throws IOException {
        // Arrange
        List<String> detalle = filas(leer("receta/receta-datos.html"));
        List<String> formulario = filas(leer("receta/receta-form.html"));

        // Act
        String identificacion = filaCon(detalle, ">Código<");

        // Assert
        assertThat(identificacion).contains(">Nombre<").doesNotContain("Volumen base").doesNotContain("OG objetivo");
        assertThat(filaCon(detalle, ">Volumen base (L)<")).contains(">Relación de empaste (L/kg)<").doesNotContain("OG objetivo").doesNotContain(">Nombre<");
        assertThat(filaCon(detalle, ">OG objetivo<")).contains(">FG objetivo<", ">IBU objetivo<", ">ABV estimado (%)<").doesNotContain("Volumen base");
        assertThat(filaCon(formulario, "for=\"nombre\"")).doesNotContain("volumenBase");
        assertThat(filaCon(formulario, "for=\"volumenBase\"")).contains("for=\"relacionDeEmpaste\"").doesNotContain("for=\"nombre\"");
    }

    @Test
    @DisplayName("CP-ODC-08 - Los formularios de ingreso de insumo separan el lote del proveedor, la cantidad y la fecha de ingreso")
    void los_ingresos_separan_lote_cantidad_y_fecha() throws IOException {
        // Arrange
        List<String> compra = filas(leer("ingreso_insumo/ingreso-compra-form.html"));
        List<String> directo = filas(leer("ingreso_insumo/ingreso-directo-form.html"));

        // Act
        String cantidadCompra = filaCon(compra, "for=\"cantidadPresentaciones\"");
        String cantidadDirecto = filaCon(directo, "for=\"cantidadRecibida\"");

        // Assert
        assertThat(cantidadCompra).doesNotContain("fechaIngreso").doesNotContain("ingreso-campos");
        assertThat(filaCon(compra, "for=\"fechaIngreso\"")).doesNotContain("cantidad");
        assertThat(cantidadDirecto).contains("for=\"costoUnitario\"").doesNotContain("fechaIngreso").doesNotContain("ingreso-campos");
        assertThat(filaCon(directo, "for=\"fechaIngreso\"")).doesNotContain("cantidad").doesNotContain("costoUnitario");
    }

    @Test
    @DisplayName("CP-ODC-09 - Los formularios de usuario, localidad, presentación, costo directo y configuración no mezclan identidad con contacto, medidas o reglas en una fila")
    void los_formularios_simples_no_mezclan_temas() throws IOException {
        // Arrange
        List<String> usuario = filas(leer("usuario/usuario-form.html"));
        List<String> localidad = filas(leer("ubicacion/localidad-form.html"));
        List<String> presentacion = filas(leer("presentacion_comercial/presentacion-form.html"));
        List<String> costo = filas(leer("configuracion/costo-directo-form.html"));
        List<String> configuracion = filas(leer("configuracion/configuracion-produccion.html"));

        // Assert
        assertThat(filaCon(usuario, "for=\"nombre\"")).contains("for=\"apellido\"").doesNotContain("telefono").doesNotContain("correo");
        assertThat(filaCon(usuario, "for=\"telefono\"")).contains("for=\"correo\"").doesNotContain("for=\"nombre\"");
        assertThat(filaCon(usuario, "for=\"username\"")).contains("for=\"password\"").doesNotContain("for=\"rol\"");
        assertThat(filaCon(localidad, "for=\"nombre\"")).contains("for=\"codigoPostal\"").doesNotContain("selector-entidad");
        assertThat(filaCon(presentacion, "for=\"nombre\"")).doesNotContain("for=\"cantidad\"");
        assertThat(filaCon(presentacion, "for=\"cantidad\"")).contains("for=\"unidadDeMedida\"");
        assertThat(filaCon(costo, "for=\"nombre\"")).doesNotContain("costoPorLitro");
        assertThat(filaCon(configuracion, "for=\"capacidadLoteEstandar\"")).contains("for=\"velocidadEstandarEnvasado\"").doesNotContain("porcentajeMinimoConsumoParaAvanzarEtapa");
    }

    @Test
    @DisplayName("CP-ODC-10 - La información del lote y la cabecera del panel de trabajo van en filas por tema las guías de etapa no muestran ABV ni alcohol y el ABV no aparece en ninguna pantalla de lote")
    void el_lote_agrupa_por_tema_y_las_guias_no_tienen_abv() throws IOException {
        // Arrange
        List<String> informacion = filas(leer("lote/lote-informacion.html"));
        List<String> panel = filas(leer("lote/lote-detalle.html"));
        String guias = leer("lote/lote-guias.html");

        // Act
        String identidad = filaCon(informacion, ">Identificador<");

        // Assert
        assertThat(identidad).contains(">Estado<", ">Etapa actual<").doesNotContain("Receta").doesNotContain("Volumen");
        assertThat(filaCon(informacion, ">Receta<")).contains(">Planificación<", ">Fermentador<").doesNotContain("Volumen").doesNotContain("Inicio");
        assertThat(filaCon(informacion, ">Volumen objetivo<")).contains("Alcohol estimado (L)").doesNotContain("OG objetivo").doesNotContain("IBU").doesNotContain("Inicio");
        assertThat(filaCon(informacion, ">OG objetivo<")).contains(">FG objetivo<", ">IBU objetivo<").doesNotContain("Volumen").doesNotContain("Alcohol");
        assertThat(filaCon(informacion, ">Inicio estimado<")).contains(">Fin estimado<").doesNotContain("real");
        assertThat(filaCon(informacion, ">Inicio real<")).contains(">Fin real<").doesNotContain("estimado");
        assertThat(filaCon(panel, ">Planificación<")).contains(">Receta<", ">Fermentador<").doesNotContain("Volumen").doesNotContain("Inicio");
        assertThat(filaCon(panel, ">Volumen objetivo<")).contains("Alcohol estimado (L)").doesNotContain("Inicio").doesNotContain(">Receta<");
        assertThat(filaCon(panel, ">Inicio estimado<")).doesNotContain("real");
        assertThat(filaCon(panel, ">Inicio real<")).doesNotContain("estimado");
        assertThat(guias + leer("lote/lote-detalle.html")).doesNotContain("IBU");
        assertThat(guias).doesNotContain("abvEstimado").doesNotContain("alcoholEstimado");
        assertThat(leer("lote/lote-informacion.html") + leer("lote/lote-detalle.html")).doesNotContain("abvEstimado").doesNotContain("ABV estimado (%)");
    }

    @Test
    @DisplayName("CP-ODC-11 - En las guías de etapa el equipo y la duración, las densidades, los insumos y el agua van cada uno en su fila")
    void las_guias_separan_equipo_densidades_insumos_y_agua() throws IOException {
        // Arrange
        List<String> guias = filas(leer("lote/lote-guias.html"));

        // Act
        String molino = filaCon(guias, ">Molino<");
        String macerador = filaCon(guias, ">Macerador<");
        String hervido = filaCon(guias, ">Olla de hervor<");
        String fermentacion = filaCon(guias, ">OG objetivo<");

        // Assert
        assertThat(molino).doesNotContain("Malta a moler");
        assertThat(macerador).contains(">Duración estimada<").doesNotContain("Malta").doesNotContain("Agua");
        assertThat(filaCon(guias, ">Agua de lavado<")).contains(">Agua de lavado<", ">Agua total<").doesNotContain(">Macerador<").doesNotContain(">Malta");
        assertThat(hervido).contains(">Duración estimada<").doesNotContain("Volumen pre-hervor").doesNotContain("Lúpulos");
        assertThat(fermentacion).contains(">FG objetivo<").doesNotContain("Fermentador").doesNotContain("Levadura");
        assertThat(filaCon(guias, ">Levadura a utilizar<")).doesNotContain("OG objetivo").doesNotContain(">Fermentador<");
    }

    @Test
    @DisplayName("CP-ODC-12 - La tabla Etapas del detalle del lote usa el mismo relleno horizontal en cada encabezado y en cada celda")
    void la_tabla_de_etapas_alinea_encabezados_y_celdas() throws IOException {
        // Arrange
        String html = leer("lote/lote-informacion.html");
        String tabla = html.substring(html.indexOf("data-seccion-etapas"), html.indexOf("data-seccion-costos"));

        // Act
        Matcher encabezados = Pattern.compile("<th class=\"(px-\\d+)").matcher(tabla);
        Matcher celdas = Pattern.compile("<td class=\"(px-\\d+)").matcher(tabla);
        List<String> rellenos = new ArrayList<>();
        while (encabezados.find()) {
            rellenos.add(encabezados.group(1));
        }
        while (celdas.find()) {
            rellenos.add(celdas.group(1));
        }

        // Assert
        assertThat(rellenos).hasSize(10).containsOnly("px-4");
    }

    /** Cada grilla (div o dl con la clase grid) es una fila lógica: devuelve el contenido de cada una. */
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
