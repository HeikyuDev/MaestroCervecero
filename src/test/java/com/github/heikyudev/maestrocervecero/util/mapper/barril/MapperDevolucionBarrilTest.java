package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDevolucionBarrilTest {

    @Test
    @DisplayName("CP-MDVB-01: toDTO informa los litros descartados junto con los demás datos de la devolución")
    void toDTO_debeInformarLosLitrosDescartados() {
        // === PREPARACION DE DATOS ===
        DevolucionBarrilEntity devolucion = DevolucionBarrilEntity.builder()
                .id(5L)
                .fecha(LocalDateTime.of(2026, 2, 5, 10, 0))
                .observaciones("Barril recibido en depósito")
                .estado(EstadoTransaccion.REGISTRADO)
                .litrosDescartados(35.5)
                .barril(BarrilEntity.builder().id(2L).identificadorInterno("BAR-2").build())
                .build();

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = MapperDevolucionBarril.toDTO(devolucion);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(5L);
        assertThat(resultado.getFechaDevolucion()).isEqualTo(LocalDateTime.of(2026, 2, 5, 10, 0));
        assertThat(resultado.getLitrosDescartados()).isEqualTo(35.5);
        assertThat(resultado.getBarril().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-MDVB-02: una devolución sin litros descartados informados queda en 0 litros descartados")
    void toDTO_debeInformarCeroPorDefecto() {
        // === PREPARACION DE DATOS ===
        DevolucionBarrilEntity devolucion = DevolucionBarrilEntity.builder()
                .id(6L)
                .estado(EstadoTransaccion.REGISTRADO)
                .barril(BarrilEntity.builder().id(2L).identificadorInterno("BAR-2").build())
                .build();

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = MapperDevolucionBarril.toDTO(devolucion);

        // === ASSERTS ===
        assertThat(resultado.getLitrosDescartados()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-MDVB-03: toDTO retorna null cuando la devolución es nula")
    void toDTO_debeRetornarNullConDevolucionNula() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MapperDevolucionBarril.toDTO(null)).isNull();
    }
}
