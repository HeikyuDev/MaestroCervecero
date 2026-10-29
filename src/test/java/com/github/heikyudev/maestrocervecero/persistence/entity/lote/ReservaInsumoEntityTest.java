package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaInsumoEntityTest {

    // ==================== nueva ====================

    @Test
    @DisplayName("CP-RIE-01: una reserva nueva está activa, con todo lo reservado pendiente y sin consumo, liberación ni reducción")
    void nueva_debeQuedarActivaConTodoPendiente() {
        // Act
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Assert
        assertThat(reserva.getCantidadReservada()).isEqualTo(10.0);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(10.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(0.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
    }

    // ==================== consumir / revertirConsumo ====================

    @Test
    @DisplayName("CP-RIE-02: consumir una parte baja el pendiente, suma a lo consumido y deja la reserva activa")
    void consumir_unaParteDebeDejarlaActiva() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Act
        reserva.consumir(4.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(6.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(4.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(10.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
    }

    @Test
    @DisplayName("CP-RIE-03: consumir todo lo reservado deja el pendiente en cero y la reserva Consumida")
    void consumir_todoDebeDejarlaConsumida() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Act
        reserva.consumir(4.0);
        reserva.consumir(6.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(10.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.CONSUMIDA);
    }

    @Test
    @DisplayName("CP-RIE-04: consumir con decimales redondea a 4 decimales y no arrastra el residuo de la coma flotante")
    void consumir_conDecimalesDebeRedondear() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 0.3);

        // Act
        reserva.consumir(0.1);
        reserva.consumir(0.2);

        // Assert
        assertThat(0.3 - 0.1 - 0.2).isNotEqualTo(0.0);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.3);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.CONSUMIDA);
    }

    @Test
    @DisplayName("CP-RIE-05: revertir un consumo de una reserva Consumida devuelve el pendiente, baja lo consumido y la deja activa")
    void revertirConsumo_debeReactivarUnaReservaConsumida() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);
        reserva.consumir(10.0);

        // Act
        reserva.revertirConsumo(10.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(10.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
    }

    // ==================== liberar ====================

    @Test
    @DisplayName("CP-RIE-06: liberar una reserva con saldo pasa el pendiente a liberado, devuelve lo liberado y la deja Liberada")
    void liberar_debePasarElPendienteALiberado() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);
        reserva.consumir(6.0);

        // Act
        double liberado = reserva.liberar();

        // Assert
        assertThat(liberado).isEqualTo(4.0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(4.0);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(6.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(10.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }

    @Test
    @DisplayName("CP-RIE-07: liberar una reserva sin consumir libera todo lo reservado")
    void liberar_sinConsumirDebeLiberarTodo() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Act
        double liberado = reserva.liberar();

        // Assert
        assertThat(liberado).isEqualTo(10.0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(10.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }

    // ==================== reducirPorAjuste ====================

    @Test
    @DisplayName("CP-RIE-08: una reducción por ajuste parcial baja el pendiente, suma a lo reducido (no a lo liberado) y la deja activa")
    void reducirPorAjuste_parcialDebeSumarAReducida() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Act
        reserva.reducirPorAjuste(3.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(7.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(3.0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
    }

    @Test
    @DisplayName("CP-RIE-09: una reducción por ajuste que agota el pendiente sin haber consumido todo deja la reserva Liberada")
    void reducirPorAjuste_queAgotaDebeDejarlaLiberada() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);
        reserva.consumir(6.0);

        // Act
        reserva.reducirPorAjuste(4.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(4.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(6.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }

    @Test
    @DisplayName("CP-RIE-10: una reducción mayor al pendiente no lo deja negativo y solo suma lo que realmente quitó")
    void reducirPorAjuste_mayorAlPendienteNoDebeQuedarNegativo() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 5.0);

        // Act
        reserva.reducirPorAjuste(8.0);

        // Assert
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(5.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }

    @Test
    @DisplayName("CP-RIE-11: en cualquier secuencia de operaciones la reservada original es igual a consumida más liberada más reducida más pendiente")
    void invariante_debeCumplirseTrasUnaSecuenciaDeOperaciones() {
        // Arrange
        ReservaInsumoEntity reserva = ReservaInsumoEntity.nueva(null, null, 10.0);

        // Act
        reserva.consumir(3.0);
        reserva.reducirPorAjuste(2.0);
        reserva.revertirConsumo(1.0);
        reserva.liberar();

        // Assert
        double suma = reserva.getCantidadConsumida() + reserva.getCantidadLiberada()
                + reserva.getCantidadReducidaPorAjuste() + reserva.getCantidadPendiente();
        assertThat(suma).isEqualTo(reserva.getCantidadReservada());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
    }
}
