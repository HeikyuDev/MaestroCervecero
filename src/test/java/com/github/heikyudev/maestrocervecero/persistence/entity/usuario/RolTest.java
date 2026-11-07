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
        assertThat(Rol.ENCARGADO_DE_DISTRIBUCION.getEtiqueta()).isEqualTo("Encargado de Distribución");
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

    // ==================== el operario solo consulta insumos, recetas, barriles y equipamiento ====================

    @Test
    @DisplayName("CP-RO-06: el Operario de Producción consulta insumos, recetas, barriles y equipamiento y no los crea, modifica ni elimina")
    void permisos_operarioDeProduccionSoloConsultaInsumosRecetasBarrilesYEquipamiento() {
        Rol operario = Rol.OPERARIO_DE_PRODUCCION;

        assertThat(operario.getPermisos()).contains(Permiso.INSUMO_CONSULTAR, Permiso.RECETA_CONSULTAR, Permiso.BARRIL_CONSULTAR, Permiso.EQUIPAMIENTO_CONSULTAR);
        assertThat(operario.getPermisos()).doesNotContain(
                Permiso.INSUMO_CREAR, Permiso.INSUMO_MODIFICAR, Permiso.INSUMO_ELIMINAR,
                Permiso.RECETA_CREAR, Permiso.RECETA_MODIFICAR, Permiso.RECETA_ELIMINAR,
                Permiso.BARRIL_CREAR, Permiso.BARRIL_MODIFICAR, Permiso.BARRIL_ELIMINAR,
                Permiso.EQUIPAMIENTO_CREAR, Permiso.EQUIPAMIENTO_MODIFICAR, Permiso.EQUIPAMIENTO_ELIMINAR);
    }

    @Test
    @DisplayName("CP-RO-07: el Gerente de Producción conserva todos los permisos de insumos, recetas, barriles y equipamiento")
    void permisos_gerenteDeProduccionConservaLaGestion() {
        assertThat(Rol.GERENTE_DE_PRODUCCION.getPermisos()).contains(
                Permiso.INSUMO_CREAR, Permiso.INSUMO_MODIFICAR, Permiso.INSUMO_ELIMINAR, Permiso.INSUMO_CONSULTAR,
                Permiso.RECETA_CREAR, Permiso.RECETA_MODIFICAR, Permiso.RECETA_ELIMINAR, Permiso.RECETA_CONSULTAR,
                Permiso.BARRIL_CREAR, Permiso.BARRIL_MODIFICAR, Permiso.BARRIL_ELIMINAR, Permiso.BARRIL_CONSULTAR,
                Permiso.EQUIPAMIENTO_CREAR, Permiso.EQUIPAMIENTO_MODIFICAR, Permiso.EQUIPAMIENTO_ELIMINAR, Permiso.EQUIPAMIENTO_CONSULTAR);
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
        java.util.List<Rol> otrosRoles = java.util.List.of(Rol.ADMINISTRADOR, Rol.GERENTE_DE_COMPRAS, Rol.ENCARGADO_DE_DEPOSITO, Rol.ENCARGADO_DE_DISTRIBUCION);

        // === EJECUCION Y VERIFICACION ===
        for (Rol rol : otrosRoles) {
            assertThat(rol.getPermisos()).as(rol.name()).doesNotContain(
                    Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR, Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR, Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR,
                    Permiso.FALLA_EQUIPAMIENTO_REGISTRAR, Permiso.FALLA_EQUIPAMIENTO_ANULAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR,
                    Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR);
        }
    }

    @Test
    @DisplayName("CP-RO-08: el Encargado de Distribución tiene los permisos de clientes, despachos y devoluciones de barril, la consulta de barriles y ningún otro")
    void permisos_encargadoDeDistribucionGestionaClientesYMueveBarriles() {
        // === EJECUCION Y VERIFICACION ===
        assertThat(Rol.ENCARGADO_DE_DISTRIBUCION.getPermisos()).containsExactlyInAnyOrder(
                Permiso.CLIENTE_CREAR, Permiso.CLIENTE_MODIFICAR, Permiso.CLIENTE_ELIMINAR, Permiso.CLIENTE_CONSULTAR,
                Permiso.BARRIL_CONSULTAR,
                Permiso.DESPACHO_BARRIL_REGISTRAR, Permiso.DESPACHO_BARRIL_ANULAR, Permiso.DESPACHO_BARRIL_CONSULTAR,
                Permiso.DEVOLUCION_BARRIL_REGISTRAR, Permiso.DEVOLUCION_BARRIL_ANULAR, Permiso.DEVOLUCION_BARRIL_CONSULTAR);
    }

    // ==================== permisos del control de barriles ====================

    @Test
    @DisplayName("CP-RO-09: el Gerente de Producción tiene exactamente sus permisos y, del ciclo de vida del barril, solo los de consultar las seis operaciones")
    void permisos_gerenteDeProduccionTieneElConjuntoExacto() {
        // === EJECUCION Y VERIFICACION ===
        assertThat(Rol.GERENTE_DE_PRODUCCION.getPermisos()).containsExactlyInAnyOrder(
                Permiso.INSUMO_CREAR, Permiso.INSUMO_MODIFICAR, Permiso.INSUMO_ELIMINAR, Permiso.INSUMO_CONSULTAR,
                Permiso.EQUIPAMIENTO_CREAR, Permiso.EQUIPAMIENTO_MODIFICAR, Permiso.EQUIPAMIENTO_ELIMINAR, Permiso.EQUIPAMIENTO_CONSULTAR,
                Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR,
                Permiso.RECETA_CREAR, Permiso.RECETA_MODIFICAR, Permiso.RECETA_ELIMINAR, Permiso.RECETA_CONSULTAR,
                Permiso.PLANIFICACION_PRODUCCION_REGISTRAR, Permiso.PLANIFICACION_PRODUCCION_FINALIZAR,
                Permiso.PLANIFICACION_PRODUCCION_ANULAR, Permiso.PLANIFICACION_PRODUCCION_CONSULTAR,
                Permiso.LOTE_REGISTRAR, Permiso.LOTE_ANULAR, Permiso.LOTE_CONSULTAR,
                Permiso.BARRIL_CREAR, Permiso.BARRIL_MODIFICAR, Permiso.BARRIL_ELIMINAR, Permiso.BARRIL_CONSULTAR,
                Permiso.LIMPIEZA_BARRIL_CONSULTAR, Permiso.FALLA_BARRIL_CONSULTAR, Permiso.MANTENIMIENTO_BARRIL_CONSULTAR,
                Permiso.FRACCIONAMIENTO_BARRIL_CONSULTAR, Permiso.DESPACHO_BARRIL_CONSULTAR, Permiso.DEVOLUCION_BARRIL_CONSULTAR,
                Permiso.COSTO_DIRECTO_ADICIONAL_CREAR, Permiso.COSTO_DIRECTO_ADICIONAL_MODIFICAR,
                Permiso.COSTO_DIRECTO_ADICIONAL_ELIMINAR, Permiso.COSTO_DIRECTO_ADICIONAL_CONSULTAR,
                Permiso.COSTO_FIJO_CREAR, Permiso.COSTO_FIJO_MODIFICAR, Permiso.COSTO_FIJO_ELIMINAR, Permiso.COSTO_FIJO_CONSULTAR,
                Permiso.MOTIVO_AJUSTE_CREAR, Permiso.MOTIVO_AJUSTE_MODIFICAR, Permiso.MOTIVO_AJUSTE_ELIMINAR, Permiso.MOTIVO_AJUSTE_CONSULTAR,
                Permiso.AJUSTE_STOCK_REGISTRAR, Permiso.AJUSTE_STOCK_ANULAR, Permiso.AJUSTE_STOCK_CONSULTAR);
    }

    @Test
    @DisplayName("CP-RO-10: el Operario de Producción tiene exactamente sus permisos, con registrar, anular y consultar limpiezas, fallas, fraccionamientos y mantenimientos de barril")
    void permisos_operarioDeProduccionTieneElConjuntoExacto() {
        // === EJECUCION Y VERIFICACION ===
        assertThat(Rol.OPERARIO_DE_PRODUCCION.getPermisos()).containsExactlyInAnyOrder(
                Permiso.INSUMO_CONSULTAR, Permiso.RECETA_CONSULTAR, Permiso.EQUIPAMIENTO_CONSULTAR, Permiso.BARRIL_CONSULTAR,
                Permiso.LOTE_INICIAR, Permiso.LOTE_CONSULTAR,
                Permiso.LOTE_FINALIZAR_MOLIENDA, Permiso.LOTE_FINALIZAR_MACERACION, Permiso.LOTE_FINALIZAR_HERVIDO,
                Permiso.LOTE_FINALIZAR_FERMENTACION, Permiso.LOTE_FINALIZAR_MADURACION, Permiso.LOTE_FINALIZAR_ENVASADO,
                Permiso.CONSUMO_INSUMO_REGISTRAR, Permiso.CONSUMO_INSUMO_ANULAR, Permiso.CONSUMO_INSUMO_CONSULTAR,
                Permiso.ENVASADO_REGISTRAR, Permiso.ENVASADO_ANULAR, Permiso.ENVASADO_CONSULTAR,
                Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR, Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR, Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR,
                Permiso.FALLA_EQUIPAMIENTO_REGISTRAR, Permiso.FALLA_EQUIPAMIENTO_ANULAR, Permiso.FALLA_EQUIPAMIENTO_CONSULTAR,
                Permiso.MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_ANULAR, Permiso.MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR,
                Permiso.LIMPIEZA_BARRIL_REGISTRAR, Permiso.LIMPIEZA_BARRIL_ANULAR, Permiso.LIMPIEZA_BARRIL_CONSULTAR,
                Permiso.FALLA_BARRIL_REGISTRAR, Permiso.FALLA_BARRIL_ANULAR, Permiso.FALLA_BARRIL_CONSULTAR,
                Permiso.FRACCIONAMIENTO_BARRIL_REGISTRAR, Permiso.FRACCIONAMIENTO_BARRIL_ANULAR, Permiso.FRACCIONAMIENTO_BARRIL_CONSULTAR,
                Permiso.MANTENIMIENTO_BARRIL_REGISTRAR, Permiso.MANTENIMIENTO_BARRIL_ANULAR, Permiso.MANTENIMIENTO_BARRIL_CONSULTAR);
    }

    @Test
    @DisplayName("CP-RO-11: el Operario de Producción y el Gerente de Producción no registran ni anulan despachos ni devoluciones de barril")
    void permisos_produccionNoRegistraNiAnulaDespachosNiDevoluciones() {
        // === EJECUCION Y VERIFICACION ===
        assertThat(Rol.OPERARIO_DE_PRODUCCION.getPermisos()).doesNotContain(
                Permiso.DESPACHO_BARRIL_REGISTRAR, Permiso.DESPACHO_BARRIL_ANULAR, Permiso.DESPACHO_BARRIL_CONSULTAR,
                Permiso.DEVOLUCION_BARRIL_REGISTRAR, Permiso.DEVOLUCION_BARRIL_ANULAR, Permiso.DEVOLUCION_BARRIL_CONSULTAR);
        assertThat(Rol.GERENTE_DE_PRODUCCION.getPermisos()).doesNotContain(
                Permiso.DESPACHO_BARRIL_REGISTRAR, Permiso.DESPACHO_BARRIL_ANULAR,
                Permiso.DEVOLUCION_BARRIL_REGISTRAR, Permiso.DEVOLUCION_BARRIL_ANULAR);
    }

    @Test
    @DisplayName("CP-RO-12: ningún otro rol tiene permisos sobre limpiezas, fallas, mantenimientos ni fraccionamientos de barril")
    void permisos_otrosRolesNoOperanLimpiezasFallasMantenimientosNiFraccionamientosDeBarril() {
        // === PREPARACION DE DATOS ===
        java.util.List<Rol> otrosRoles = java.util.List.of(Rol.ADMINISTRADOR, Rol.GERENTE_DE_COMPRAS, Rol.ENCARGADO_DE_DEPOSITO, Rol.ENCARGADO_DE_DISTRIBUCION);

        // === EJECUCION Y VERIFICACION ===
        for (Rol rol : otrosRoles) {
            assertThat(rol.getPermisos()).as(rol.name()).doesNotContain(
                    Permiso.LIMPIEZA_BARRIL_REGISTRAR, Permiso.LIMPIEZA_BARRIL_ANULAR, Permiso.LIMPIEZA_BARRIL_CONSULTAR,
                    Permiso.FALLA_BARRIL_REGISTRAR, Permiso.FALLA_BARRIL_ANULAR, Permiso.FALLA_BARRIL_CONSULTAR,
                    Permiso.MANTENIMIENTO_BARRIL_REGISTRAR, Permiso.MANTENIMIENTO_BARRIL_ANULAR, Permiso.MANTENIMIENTO_BARRIL_CONSULTAR,
                    Permiso.FRACCIONAMIENTO_BARRIL_REGISTRAR, Permiso.FRACCIONAMIENTO_BARRIL_ANULAR, Permiso.FRACCIONAMIENTO_BARRIL_CONSULTAR);
        }
    }
}
