package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapperReservaInsumoTest {

    @Test
    @DisplayName("CP-MRI-01: toDTO informa la cantidad original, la consumida, la liberada, la reducida por ajuste, la pendiente y el estado de la reserva")
    void toDTO_debeInformarElHistorialCompletoDeLaReserva() {
        // Arrange
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().id(10L).identificacionLoteProveedor("L-01").cantidadActual(0.0).cantidadReservada(0.0)
                .insumo(MaltaEntity.builder().id(1L).nombre("Pilsen").estado(Estado.ACTIVO).build()).build();
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).loteInsumo(loteInsumo)
                .cantidadReservada(10.0).cantidadConsumida(6.0).cantidadLiberada(1.0).cantidadReducidaPorAjuste(3.0).cantidadPendiente(0.0)
                .estado(EstadoReservaInsumo.LIBERADA).build();

        // Act
        ReservaInsumoResponseDTO dto = MapperReservaInsumo.toDTO(reserva);

        // Assert
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getCantidadReservada()).isEqualTo(10.0);
        assertThat(dto.getCantidadConsumida()).isEqualTo(6.0);
        assertThat(dto.getCantidadLiberada()).isEqualTo(1.0);
        assertThat(dto.getCantidadReducidaPorAjuste()).isEqualTo(3.0);
        assertThat(dto.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(dto.getEstado()).isEqualTo(EstadoReservaInsumo.LIBERADA);
        assertThat(dto.getLoteInsumo().getIdentificacionLoteProveedor()).isEqualTo("L-01");
    }

    @Test
    @DisplayName("CP-MRI-02: toDTO devuelve null cuando la reserva es nula")
    void toDTO_debeDevolverNullConReservaNula() {
        // Act + Assert
        assertThat(MapperReservaInsumo.toDTO(null)).isNull();
    }
}
