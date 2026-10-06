package com.github.heikyudev.maestrocervecero.persistence.entity.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolTest {

    // ==================== etiqueta ====================

    @Test
    @DisplayName("CP-RO-01: cada Rol expone su etiqueta legible, con mayúsculas y tildes correctas")
    void etiqueta_debeSerElNombreLegibleDeCadaRol() {
        // === PREPARACION DE DATOS ===
        // (los roles son constantes del enum, no requieren preparación)

        // === EJECUCION Y VERIFICACION ===
        assertThat(Rol.ADMINISTRADOR.getEtiqueta()).isEqualTo("Administrador");
        assertThat(Rol.GERENTE_DE_PRODUCCION.getEtiqueta()).isEqualTo("Gerente de Producción");
        assertThat(Rol.OPERARIO_DE_PRODUCCION.getEtiqueta()).isEqualTo("Operario de Producción");
        assertThat(Rol.GERENTE_DE_COMPRAS.getEtiqueta()).isEqualTo("Gerente de Compras");
        assertThat(Rol.ENCARGADO_DE_DEPOSITO.getEtiqueta()).isEqualTo("Encargado de Depósito");
        assertThat(Rol.GERENTE_COMERCIAL.getEtiqueta()).isEqualTo("Gerente Comercial");
    }

    @Test
    @DisplayName("CP-RO-02: todos los roles tienen etiqueta y ninguna contiene guiones bajos")
    void etiqueta_debeEstarDefinidaParaTodosLosRoles() {
        // === PREPARACION DE DATOS ===
        Rol[] roles = Rol.values();

        // === EJECUCION Y VERIFICACION ===
        assertThat(roles).hasSize(6);
        for (Rol rol : roles) {
            assertThat(rol.getEtiqueta()).isNotBlank().doesNotContain("_");
        }
    }
}
