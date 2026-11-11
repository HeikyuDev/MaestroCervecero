package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardas de las plantillas de los motivos de ajuste: pestaña de Configuración de Producción, lista y formulario.
 */
class MotivoAjustePlantillasTest {

    private static final Path TEMPLATES = Path.of("").toAbsolutePath().resolve("src/main/resources/templates");

    @Test
    @DisplayName("CP-PMA-01: la pestaña Motivos de ajuste está guardada por MOTIVO_AJUSTE_CONSULTAR, usa el fragmento de pestañas con ícono y va junto a Costos directos adicionales")
    void pestana() throws IOException {
        String html = leer("configuracion/tabs.html");
        int costos = html.indexOf("'Costos directos adicionales'");
        int motivos = html.indexOf("'Motivos de ajuste'");

        assertThat(motivos).isGreaterThan(costos);
        String bloque = html.substring(html.lastIndexOf("<th:block sec:authorize", motivos), motivos);
        assertThat(bloque).contains("hasAuthority('MOTIVO_AJUSTE_CONSULTAR')").contains("fragments/pestanas :: pestana(").contains("fragments/pestanas :: bi('bi-sliders2')")
                .contains("${activa == 'motivos-ajuste'}");
    }

    @Test
    @DisplayName("CP-PMA-02: la lista usa el diálogo de baja compartido, los filtros con ejemplo con Limpiar y Buscar y la columna Acciones")
    void lista() throws IOException {
        String html = leer("configuracion/motivos-ajuste-lista.html");

        assertThat(html).contains("configuracion/tabs :: tabs('motivos-ajuste')").contains("fragments/dialogo-baja :: dialogo(").contains("placeholder=\"Ej: Merma por derrame\"")
                .contains(">Limpiar</a>").contains("Buscar").contains("novalidate").contains("class=\"col-acciones px-4 py-3 text-right\">Acciones</th>")
                .contains("hasAuthority('MOTIVO_AJUSTE_MODIFICAR')").contains("hasAuthority('MOTIVO_AJUSTE_ELIMINAR')").contains("hasAuthority('MOTIVO_AJUSTE_CREAR')");
    }

    @Test
    @DisplayName("CP-PMA-03: el formulario es novalidate con envío único, marca los obligatorios con asterisco rojo y solo deshabilita el selector del tipo con el bloqueo, con el tipo real en un campo oculto")
    void formulario() throws IOException {
        String html = leer("configuracion/motivo-ajuste-form.html");

        assertThat(html).contains("novalidate data-un-solo-envio").contains("Nombre <span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")
                .contains("Tipo de ajuste <span class=\"text-rose-600\" title=\"Obligatorio\">*</span>").contains("placeholder=\"Ej: Merma por derrame")
                .contains("th:disabled=\"${tipoBloqueado}\"").contains("<input type=\"hidden\" name=\"tipoAjuste\" th:if=\"${tipoBloqueado}\"")
                .contains("No se puede cambiar porque el motivo ya tiene ajustes.").contains("fragments/form-errors :: resumen(");
    }

    private static String leer(String ruta) throws IOException {
        return Files.readString(TEMPLATES.resolve(ruta), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
