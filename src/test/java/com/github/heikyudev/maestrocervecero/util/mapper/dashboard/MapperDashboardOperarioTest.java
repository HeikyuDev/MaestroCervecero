package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardOperarioTest {

    @Test
    @DisplayName("CP-MDO-01: el lote del día lleva los datos del lote del listado y el atraso y la falta de consumo recibidos")
    void toLoteDelDiaDTO_llevaLosDatosDelLoteYLosValoresCalculados() {
        // Arrange
        LoteCronogramaResponseDTO lote = LoteCronogramaResponseDTO.builder()
                .id(12L).identificadorInterno("LOT-R1-02").nombreReceta("Pale Ale").etapaActual(TipoEtapa.HERVIDO)
                .idFermentador(4L).identificadorInternoFermentador("FER-01")
                .fechaInicioEstimada(LocalDate.of(2026, 11, 1)).fechaFinalizacionEstimada(LocalDate.of(2026, 11, 20))
                .build();

        // Act
        LoteDelDiaResponseDTO dto = MapperDashboardOperario.toLoteDelDiaDTO(lote, 3, true);

        // Assert
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getIdentificadorInterno()).isEqualTo("LOT-R1-02");
        assertThat(dto.getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(dto.getEtapaActual()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(dto.getIdFermentador()).isEqualTo(4L);
        assertThat(dto.getIdentificadorInternoFermentador()).isEqualTo("FER-01");
        assertThat(dto.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(dto.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(dto.getDiasDeAtraso()).isEqualTo(3);
        assertThat(dto.isFaltaConsumo()).isTrue();
    }

    @Test
    @DisplayName("CP-MDO-02: el lote del día de un lote pendiente conserva la etapa y el fermentador nulos, y el atraso en 0")
    void toLoteDelDiaDTO_conservaLosNulosDeUnLotePendiente() {
        // Arrange
        LoteCronogramaResponseDTO lote = LoteCronogramaResponseDTO.builder()
                .id(13L).identificadorInterno("LOT-R1-03").nombreReceta("Pale Ale")
                .fechaInicioEstimada(LocalDate.of(2026, 11, 13)).build();

        // Act
        LoteDelDiaResponseDTO dto = MapperDashboardOperario.toLoteDelDiaDTO(lote, 0, false);

        // Assert
        assertThat(dto.getEtapaActual()).isNull();
        assertThat(dto.getIdFermentador()).isNull();
        assertThat(dto.getIdentificadorInternoFermentador()).isNull();
        assertThat(dto.getDiasDeAtraso()).isZero();
        assertThat(dto.isFaltaConsumo()).isFalse();
    }

    @Test
    @DisplayName("CP-MDO-03: un lote, un equipo o un barril nulo se mapean a nulo")
    void mapeoDeNulos_devuelveNulo() {
        // Act y Assert
        assertThat(MapperDashboardOperario.toLoteDelDiaDTO(null, 0, false)).isNull();
        assertThat(MapperDashboardOperario.toEquipoPendienteDTO(null, CausaMantenimiento.FALLA)).isNull();
        assertThat(MapperDashboardOperario.toBarrilPendienteDTO(null, CausaMantenimiento.FALLA)).isNull();
    }

    @Test
    @DisplayName("CP-MDO-04: el equipo pendiente lleva su identificador, su tipo y la causa recibida")
    void toEquipoPendienteDTO_llevaLosDatosDelEquipoYLaCausa() {
        // Arrange
        EquipamientoControlResponseDTO equipo = EquipamientoControlResponseDTO.builder()
                .id(3L).identificadorInterno("FER-01").tipo(TipoEquipamiento.FERMENTADOR).build();

        // Act
        EquipoPendienteResponseDTO conCausa = MapperDashboardOperario.toEquipoPendienteDTO(equipo, CausaMantenimiento.PREVENTIVO);
        EquipoPendienteResponseDTO sinCausa = MapperDashboardOperario.toEquipoPendienteDTO(equipo, null);

        // Assert
        assertThat(conCausa.getId()).isEqualTo(3L);
        assertThat(conCausa.getIdentificadorInterno()).isEqualTo("FER-01");
        assertThat(conCausa.getTipo()).isEqualTo(TipoEquipamiento.FERMENTADOR);
        assertThat(conCausa.getCausaMantenimiento()).isEqualTo(CausaMantenimiento.PREVENTIVO);
        assertThat(sinCausa.getCausaMantenimiento()).isNull();
    }

    @Test
    @DisplayName("CP-MDO-05: el barril pendiente lleva su identificador, su fabricante y la causa recibida")
    void toBarrilPendienteDTO_llevaLosDatosDelBarrilYLaCausa() {
        // Arrange
        BarrilControlResponseDTO barril = BarrilControlResponseDTO.builder()
                .id(8L).identificadorInterno("BAR-08").fabricante("Fabricante Sur").build();

        // Act
        BarrilPendienteResponseDTO conCausa = MapperDashboardOperario.toBarrilPendienteDTO(barril, CausaMantenimiento.FALLA);
        BarrilPendienteResponseDTO sinCausa = MapperDashboardOperario.toBarrilPendienteDTO(barril, null);

        // Assert
        assertThat(conCausa.getId()).isEqualTo(8L);
        assertThat(conCausa.getIdentificadorInterno()).isEqualTo("BAR-08");
        assertThat(conCausa.getFabricante()).isEqualTo("Fabricante Sur");
        assertThat(conCausa.getCausaMantenimiento()).isEqualTo(CausaMantenimiento.FALLA);
        assertThat(sinCausa.getCausaMantenimiento()).isNull();
    }

    @Test
    @DisplayName("CP-MDO-06: la causa de un equipo es Falla si su última operación es una falla")
    void causaDeEquipo_fallaDaFalla() {
        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeEquipo(FallaEquipamientoEntity.builder().id(1L).build()))
                .isEqualTo(CausaMantenimiento.FALLA);
    }

    @Test
    @DisplayName("CP-MDO-07: la causa de un equipo es Preventivo si su última operación es una limpieza que lo dejó en mantenimiento")
    void causaDeEquipo_limpiezaQueDejoEnMantenimientoDaPreventivo() {
        // Arrange
        LimpiezaEquipamientoEntity limpieza = LimpiezaEquipamientoEntity.builder()
                .id(2L).estadoOperativoResultante(EstadoOperativo.EN_MANTENIMIENTO).build();

        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeEquipo(limpieza)).isEqualTo(CausaMantenimiento.PREVENTIVO);
    }

    @Test
    @DisplayName("CP-MDO-08: la causa de un equipo es nula si su última operación no lo explica o no tiene operaciones")
    void causaDeEquipo_otraOperacionOSinOperacionDaNulo() {
        // Arrange
        LimpiezaEquipamientoEntity limpiezaQueLoDejoDisponible = LimpiezaEquipamientoEntity.builder()
                .id(2L).estadoOperativoResultante(EstadoOperativo.DISPONIBLE).build();

        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeEquipo(limpiezaQueLoDejoDisponible)).isNull();
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeEquipo(MantenimientoEquipamientoEntity.builder().id(3L).build())).isNull();
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeEquipo(null)).isNull();
    }

    @Test
    @DisplayName("CP-MDO-09: la causa de un barril es Falla si su última operación es una falla")
    void causaDeBarril_fallaDaFalla() {
        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeBarril(FallaBarrilEntity.builder().id(1L).build()))
                .isEqualTo(CausaMantenimiento.FALLA);
    }

    @Test
    @DisplayName("CP-MDO-10: la causa de un barril es Preventivo si su última operación es una limpieza que lo dejó en mantenimiento")
    void causaDeBarril_limpiezaQueDejoEnMantenimientoDaPreventivo() {
        // Arrange
        LimpiezaBarrilEntity limpieza = LimpiezaBarrilEntity.builder()
                .id(2L).estadoOperativoResultante(EstadoOperativoBarril.EN_MANTENIMIENTO).build();

        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeBarril(limpieza)).isEqualTo(CausaMantenimiento.PREVENTIVO);
    }

    @Test
    @DisplayName("CP-MDO-11: la causa de un barril es nula si su última operación no lo explica o no tiene operaciones")
    void causaDeBarril_otraOperacionOSinOperacionDaNulo() {
        // Arrange
        LimpiezaBarrilEntity limpiezaQueLoDejoDisponible = LimpiezaBarrilEntity.builder()
                .id(2L).estadoOperativoResultante(EstadoOperativoBarril.DISPONIBLE).build();

        // Act y Assert
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeBarril(limpiezaQueLoDejoDisponible)).isNull();
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeBarril(MantenimientoBarrilEntity.builder().id(3L).build())).isNull();
        assertThat(MapperDashboardOperario.toCausaMantenimientoDeBarril(null)).isNull();
    }
}
