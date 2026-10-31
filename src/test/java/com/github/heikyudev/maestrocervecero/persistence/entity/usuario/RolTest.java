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

    // ==================== permisos de control de equipamiento ====================

    @Test
    @DisplayName("CP-RO-03: el Operario de Producción registra, anula y consulta limpiezas, fallas y mantenimientos de equipamiento")
    void permisos_operarioDeProduccionOperaElCicloDeVidaDelEquipamiento() {
        // === PREPARACION DE DATOS ===
        Rol operario = Rol.OPERARIO_DE_PRODUCCION;

        // === EJECUCION Y VERIFICACION ===
        assertThat(operario.getPermisos()).contains(
                Permiso.EQUIPAMIENTO_CONSULTAR,
                Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR, Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR, Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR,
                Permiso.FALLA_EQUIPAMIENTO_REGISTRAR, Permiso.FALLA_EQUIPAMIENTO_ANULAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR,
                Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR);
    }

    @Test
    @DisplayName("CP-RO-04: el Gerente de Producción solo consulta limpiezas, fallas y mantenimientos de equipamiento")
    void permisos_gerenteDeProduccionSoloConsultaElCicloDeVidaDelEquipamiento() {
        // === PREPARACION DE DATOS ===
        Rol gerente = Rol.GERENTE_DE_PRODUCCION;

        // === EJECUCION Y VERIFICACION ===
        assertThat(gerente.getPermisos()).contains(
                Permiso.EQUIPAMIENTO_CREAR, Permiso.EQUIPAMIENTO_MODIFICAR, Permiso.EQUIPAMIENTO_ELIMINAR, Permiso.EQUIPAMIENTO_CONSULTAR,
                Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR);
        assertThat(gerente.getPermisos()).doesNotContain(
                Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR, Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR,
                Permiso.FALLA_EQUIPAMIENTO_REGISTRAR, Permiso.FALLA_EQUIPAMIENTO_ANULAR,
                Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR);
    }

    @Test
    @DisplayName("CP-RO-05: ningún otro rol tiene permisos sobre limpiezas, fallas o mantenimientos de equipamiento")
    void permisos_otrosRolesNoOperanElCicloDeVidaDelEquipamiento() {
        // === PREPARACION DE DATOS ===
        java.util.List<Rol> otrosRoles = java.util.List.of(Rol.ADMINISTRADOR, Rol.GERENTE_DE_COMPRAS, Rol.ENCARGADO_DE_DEPOSITO, Rol.GERENTE_COMERCIAL);

        // === EJECUCION Y VERIFICACION ===
        for (Rol rol : otrosRoles) {
            assertThat(rol.getPermisos()).as(rol.name()).doesNotContain(
                    Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR, Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR, Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR,
                    Permiso.FALLA_EQUIPAMIENTO_REGISTRAR, Permiso.FALLA_EQUIPAMIENTO_ANULAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR,
                    Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR);
        }
    }
}
