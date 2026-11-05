package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoOperativoBarrilTest {

    @Test
    @DisplayName("CP-EOB-01: el estado EN_USO existe con la etiqueta En uso, igual a la del estado EN_USO de los equipamientos")
    void enUso_debeTenerLaEtiquetaEnUso() {
        // === PREPARACION DE DATOS ===
        EstadoOperativoBarril estado = EstadoOperativoBarril.valueOf("EN_USO");

        // === EJECUCION Y VERIFICACION ===
        assertThat(estado.getEtiqueta()).isEqualTo("En uso");
        assertThat(estado.getEtiqueta()).isEqualTo(com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo.EN_USO.getEtiqueta());
    }

    @Test
    @DisplayName("CP-EOB-02: el barril tiene seis estados operativos y todos tienen etiqueta legible sin guiones bajos")
    void estados_debenTenerEtiquetaLegible() {
        // === PREPARACION DE DATOS ===
        EstadoOperativoBarril[] estados = EstadoOperativoBarril.values();

        // === EJECUCION Y VERIFICACION ===
        assertThat(estados).containsExactlyInAnyOrder(EstadoOperativoBarril.CON_CERVEZA, EstadoOperativoBarril.DESPACHADO,
                EstadoOperativoBarril.DISPONIBLE, EstadoOperativoBarril.EN_LIMPIEZA, EstadoOperativoBarril.EN_MANTENIMIENTO,
                EstadoOperativoBarril.EN_USO);
        for (EstadoOperativoBarril estado : estados) {
            assertThat(estado.getEtiqueta()).isNotBlank().doesNotContain("_");
        }
    }
}
