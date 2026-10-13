package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteInsumoEntityTest {

    // ==================== getCantidadDisponible ====================

    @Test
    @DisplayName("CP-LIE-01: getCantidadDisponible ignora un residuo de punto flotante (1e-16) y informa 0")
    void getCantidadDisponible_debeIgnorarUnResiduoDePuntoFlotante() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.1 + 0.2, 0.3);

        // === EJECUCION Y ASSERTS ===
        assertThat((0.1 + 0.2) - 0.3).isGreaterThan(0.0);
        assertThat(loteInsumo.getCantidadDisponible()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-LIE-02: getCantidadDisponible informa la diferencia entre la cantidad actual y la reservada, redondeada a 4 decimales")
    void getCantidadDisponible_debeRedondearLaDiferencia() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.7, 0.4);

        // === EJECUCION Y ASSERTS ===
        assertThat(0.7 - 0.4).isNotEqualTo(0.3);
        assertThat(loteInsumo.getCantidadDisponible()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-LIE-03: getCantidadDisponible trata una cantidad actual o reservada nula como cero")
    void getCantidadDisponible_debeTratarLosNulosComoCero() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().build();
        loteInsumo.setCantidadActual(null);
        loteInsumo.setCantidadReservada(null);

        // === EJECUCION Y ASSERTS ===
        assertThat(loteInsumo.getCantidadDisponible()).isEqualTo(0.0);
    }

    // ==================== reservar / liberarReserva ====================

    @Test
    @DisplayName("CP-LIE-04: reservar permite reservar todo el stock aunque la cantidad traiga un residuo decimal (0,1 + 0,2 contra 0,3)")
    void reservar_debePermitirReservarTodoElStockConResiduoDecimal() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.3, 0.0);

        // === EJECUCION ===
        loteInsumo.reservar(0.1 + 0.2);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadDisponible()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-LIE-05: reservar lanza ReglaNegocioException si la cantidad supera la cantidad disponible, sin modificar la reserva")
    void reservar_debeLanzarExcepcionSiSuperaLaCantidadDisponible() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(1.0, 0.4);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteInsumo.reservar(0.7))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Stock disponible insuficiente para reservar");
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.4);
    }

    @Test
    @DisplayName("CP-LIE-06: tres reservas de 0,1 acumulan exactamente 0,3 y liberarlas una por una deja la reserva en 0")
    void reservarYLiberarReserva_noDebenAcumularResiduos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.3, 0.0);

        // === EJECUCION ===
        loteInsumo.reservar(0.1);
        loteInsumo.reservar(0.1);
        loteInsumo.reservar(0.1);
        double reservadaTrasReservar = loteInsumo.getCantidadReservada();
        loteInsumo.liberarReserva(0.1);
        loteInsumo.liberarReserva(0.1);
        loteInsumo.liberarReserva(0.1);

        // === ASSERTS ===
        assertThat(reservadaTrasReservar).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
    }

    // ==================== ejecutarConsumo / consumirDirecto ====================

    @Test
    @DisplayName("CP-LIE-07: tres consumos reservados de 0,1 sobre 0,3 reservado dejan la cantidad actual y la reservada en 0")
    void ejecutarConsumo_noDebeAcumularResiduos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.3, 0.3);

        // === EJECUCION ===
        loteInsumo.ejecutarConsumo(0.1);
        loteInsumo.ejecutarConsumo(0.1);
        loteInsumo.ejecutarConsumo(0.1);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-LIE-08: tres consumos directos de 0,1 sobre un stock de 0,3 dejan la cantidad actual en 0 sin tocar la reservada")
    void consumirDirecto_noDebeAcumularResiduos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.3, 0.0);

        // === EJECUCION ===
        loteInsumo.consumirDirecto(0.1);
        loteInsumo.consumirDirecto(0.1);
        loteInsumo.consumirDirecto(0.1);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
    }

    // ==================== revertirConsumo / revertirConsumoDirecto ====================

    @Test
    @DisplayName("CP-LIE-09: revertir tres consumos reservados de 0,1 devuelve exactamente 0,3 a la cantidad actual y a la reservada")
    void revertirConsumo_noDebeAcumularResiduos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.0, 0.0);

        // === EJECUCION ===
        loteInsumo.revertirConsumo(0.1);
        loteInsumo.revertirConsumo(0.1);
        loteInsumo.revertirConsumo(0.1);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-LIE-10: revertir tres consumos directos de 0,1 devuelve exactamente 0,3 a la cantidad actual sin tocar la reservada")
    void revertirConsumoDirecto_noDebeAcumularResiduos() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteInsumo = loteInsumo(0.0, 0.0);

        // === EJECUCION ===
        loteInsumo.revertirConsumoDirecto(0.1);
        loteInsumo.revertirConsumoDirecto(0.1);
        loteInsumo.revertirConsumoDirecto(0.1);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
    }

    // ==================== helpers de construcción ====================

    private static LoteInsumoEntity loteInsumo(double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(1L)
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }
}
