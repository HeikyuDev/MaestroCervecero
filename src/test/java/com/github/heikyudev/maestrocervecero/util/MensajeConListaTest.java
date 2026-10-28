package com.github.heikyudev.maestrocervecero.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeConListaTest {

    @Test
    @DisplayName("CP-MCL-01: construir con un elemento deja el título y el elemento en líneas separadas")
    void construir_conUnElemento() {
        // Arrange
        String titulo = "No hay stock suficiente para iniciar el lote:";

        // Act
        String mensaje = MensajeConLista.construir(titulo, List.of("Malta Pilsen: faltan 12,5 kg (disponible 7,5 kg, necesario 20 kg)"));

        // Assert
        assertThat(mensaje).isEqualTo(titulo + "\nMalta Pilsen: faltan 12,5 kg (disponible 7,5 kg, necesario 20 kg)");
    }

    @Test
    @DisplayName("CP-MCL-02: construir con varios elementos deja uno por línea, en el orden dado")
    void construir_conVariosElementos() {
        // Act
        String mensaje = MensajeConLista.construir("Faltan insumos:", List.of("Malta", "Lúpulo", "Levadura"));

        // Assert
        assertThat(mensaje).isEqualTo("Faltan insumos:\nMalta\nLúpulo\nLevadura");
        assertThat(MensajeConLista.lineas(mensaje)).containsExactly("Faltan insumos:", "Malta", "Lúpulo", "Levadura");
    }

    @Test
    @DisplayName("CP-MCL-03: construir sin elementos devuelve solo el título")
    void construir_sinElementos() {
        // Act + Assert
        assertThat(MensajeConLista.construir("Título", List.of())).isEqualTo("Título");
        assertThat(MensajeConLista.construir("Título", null)).isEqualTo("Título");
    }

    @Test
    @DisplayName("CP-MCL-04: deAvisos deja un aviso solo tal cual y agrupa varios bajo el título de avisos")
    void deAvisos() {
        // Act + Assert
        assertThat(MensajeConLista.deAvisos(List.of("El estado indicado no es válido"))).isEqualTo("El estado indicado no es válido");
        assertThat(MensajeConLista.deAvisos(List.of("Aviso uno", "Aviso dos"))).isEqualTo("Se ajustó la consulta:\nAviso uno\nAviso dos");
    }

    @Test
    @DisplayName("CP-MCL-05: lineas ignora líneas vacías, espacios sobrantes y el mensaje nulo")
    void lineas_normaliza() {
        // Act + Assert
        assertThat(MensajeConLista.lineas("  A \r\n\r\n B\n")).containsExactly("A", "B");
        assertThat(MensajeConLista.lineas(null)).isEmpty();
    }
}
