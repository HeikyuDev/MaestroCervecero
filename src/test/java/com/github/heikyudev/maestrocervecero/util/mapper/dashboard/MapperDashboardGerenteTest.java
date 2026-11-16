package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ColaFermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteEnProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardGerenteTest {

    @Test
    @DisplayName("CP-MDG-01: el lote en producción lleva los datos del lote del listado, su prioridad y el atraso recibido")
    void toLoteEnProduccionDTO_llevaLosDatosDelLoteYElAtraso() {
        // Arrange
        LoteCronogramaResponseDTO lote = LoteCronogramaResponseDTO.builder()
                .id(12L).identificadorInterno("LOT-R1-02").nombreReceta("Pale Ale").etapaActual(TipoEtapa.HERVIDO)
                .idFermentador(4L).identificadorInternoFermentador("FER-01").prioridad(PrioridadPlanificacion.ALTA)
                .fechaInicioEstimada(LocalDate.of(2026, 11, 1)).fechaFinalizacionEstimada(LocalDate.of(2026, 11, 20))
                .build();

        // Act
        LoteEnProduccionResponseDTO dto = MapperDashboardGerente.toLoteEnProduccionDTO(lote, 3);

        // Assert
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getIdentificadorInterno()).isEqualTo("LOT-R1-02");
        assertThat(dto.getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(dto.getEtapaActual()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(dto.getIdFermentador()).isEqualTo(4L);
        assertThat(dto.getIdentificadorInternoFermentador()).isEqualTo("FER-01");
        assertThat(dto.getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
        assertThat(dto.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(dto.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(dto.getDiasDeAtraso()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-MDG-02: el lote en producción de un lote pendiente conserva la etapa y el fermentador nulos, y el atraso en 0")
    void toLoteEnProduccionDTO_conservaLosNulosDeUnLotePendiente() {
        // Arrange
        LoteCronogramaResponseDTO lote = LoteCronogramaResponseDTO.builder()
                .id(13L).identificadorInterno("LOT-R1-03").nombreReceta("Stout").prioridad(PrioridadPlanificacion.BAJA)
                .fechaInicioEstimada(LocalDate.of(2026, 11, 25)).fechaFinalizacionEstimada(LocalDate.of(2026, 12, 10))
                .build();

        // Act
        LoteEnProduccionResponseDTO dto = MapperDashboardGerente.toLoteEnProduccionDTO(lote, 0);

        // Assert
        assertThat(dto.getEtapaActual()).isNull();
        assertThat(dto.getIdFermentador()).isNull();
        assertThat(dto.getIdentificadorInternoFermentador()).isNull();
        assertThat(dto.getDiasDeAtraso()).isZero();
        assertThat(dto.getPrioridad()).isEqualTo(PrioridadPlanificacion.BAJA);
    }

    @Test
    @DisplayName("CP-MDG-03: un lote nulo, un fermentador nulo y una planificación nula se mapean a nulo")
    void toDTO_conEntradaNulaDevuelveNulo() {
        // Act and Assert
        assertThat(MapperDashboardGerente.toLoteEnProduccionDTO(null, 0)).isNull();
        assertThat(MapperDashboardGerente.toColaFermentadorDTO(null, null, null, 0)).isNull();
        assertThat(MapperDashboardGerente.toPlanificacionPendienteDTO(null)).isNull();
    }

    @Test
    @DisplayName("CP-MDG-04: la cola de un fermentador lleva sus datos de control, su lote en ejecución, su próximo lote y la cantidad de pendientes")
    void toColaFermentadorDTO_llevaLosDatosYLosLotes() {
        // Arrange
        EquipamientoControlResponseDTO fermentador = EquipamientoControlResponseDTO.builder()
                .id(4L).identificadorInterno("FER-04").tipo(TipoEquipamiento.FERMENTADOR).estadoOperativo(EstadoOperativo.EN_USO).build();
        LoteEnProduccionResponseDTO enEjecucion = LoteEnProduccionResponseDTO.builder().id(1L).build();
        LoteEnProduccionResponseDTO proximo = LoteEnProduccionResponseDTO.builder().id(2L).build();

        // Act
        ColaFermentadorResponseDTO dto = MapperDashboardGerente.toColaFermentadorDTO(fermentador, enEjecucion, proximo, 3);

        // Assert
        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getIdentificadorInterno()).isEqualTo("FER-04");
        assertThat(dto.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        assertThat(dto.getLoteEnEjecucion()).isSameAs(enEjecucion);
        assertThat(dto.getProximoLote()).isSameAs(proximo);
        assertThat(dto.getLotesPendientes()).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-MDG-05: la planificación pendiente lleva su ID, su etiqueta, su prioridad y sus fechas estimadas")
    void toPlanificacionPendienteDTO_llevaLosDatosDeLaPlanificacion() {
        // Arrange
        PlanificacionProduccionResponseDTO planificacion = PlanificacionProduccionResponseDTO.builder()
                .id(7L).prioridad(PrioridadPlanificacion.ALTA)
                .fechaInicioEstimada(LocalDate.of(2027, 8, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 8, 20)).build();

        // Act
        PlanificacionPendienteResponseDTO dto = MapperDashboardGerente.toPlanificacionPendienteDTO(planificacion);

        // Assert
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getEtiqueta()).isEqualTo(planificacion.getEtiqueta());
        assertThat(dto.getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
        assertThat(dto.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
        assertThat(dto.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2027, 8, 20));
    }
}
