package com.github.heikyudev.maestrocervecero.presentation.template;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renderiza el fragmento real {@code fragments/alerta-mensaje} que usa el layout para el texto de toda alerta.
 */
class AlertaMensajeTest {

    private static SpringTemplateEngine motor;

    @BeforeAll
    static void crearMotor() {
        ClassLoaderTemplateResolver fragmentos = new ClassLoaderTemplateResolver();
        fragmentos.setPrefix("templates/");
        fragmentos.setSuffix(".html");
        fragmentos.setTemplateMode(TemplateMode.HTML);
        fragmentos.setResolvablePatterns(java.util.Set.of("fragments/*"));
        fragmentos.setOrder(1);
        StringTemplateResolver literal = new StringTemplateResolver();
        literal.setTemplateMode(TemplateMode.HTML);
        literal.setOrder(2);
        motor = new SpringTemplateEngine();
        motor.addTemplateResolver(fragmentos);
        motor.addTemplateResolver(literal);
    }

    private static String renderizar(String mensaje) {
        Context contexto = new Context();
        contexto.setVariable("mensaje", mensaje);
        return motor.process("<div th:replace=\"~{fragments/alerta-mensaje :: texto(${mensaje})}\"></div>", contexto);
    }

    @Test
    @DisplayName("CP-AMS-01: un mensaje de una sola línea se muestra como texto corrido, sin lista")
    void unaLinea() {
        // Act
        String html = renderizar("Lote iniciado correctamente");

        // Assert
        assertThat(html).contains("Lote iniciado correctamente").doesNotContain("<ul").doesNotContain("data-alerta-con-lista");
    }

    @Test
    @DisplayName("CP-AMS-02: un mensaje de varias líneas muestra la primera como título y las demás como viñetas")
    void variasLineas() {
        // Act
        String html = renderizar("No hay stock suficiente para iniciar el lote:\nMalta Pilsen: faltan 12,5 kg (disponible 7,5 kg, necesario 20 kg)\nLevadura US-05: faltan 2 g (disponible 0 g, necesario 2 g)");

        // Assert
        assertThat(html).contains("data-alerta-con-lista")
                .containsPattern("<p[^>]*data-alerta-titulo[^>]*>No hay stock suficiente para iniciar el lote:</p>")
                .contains("<li>Malta Pilsen: faltan 12,5 kg (disponible 7,5 kg, necesario 20 kg)</li>")
                .contains("<li>Levadura US-05: faltan 2 g (disponible 0 g, necesario 2 g)</li>");
        assertThat(html.split("<li>", -1)).hasSize(3);
    }

    @Test
    @DisplayName("CP-AMS-03: el texto de cada línea se escapa, no se interpreta como HTML")
    void escapaElTexto() {
        // Act
        String html = renderizar("Título:\n<script>alert(1)</script>");

        // Assert
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
