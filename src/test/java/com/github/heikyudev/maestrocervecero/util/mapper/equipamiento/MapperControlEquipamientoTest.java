package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperControlEquipamientoTest {

    private static final LocalDateTime FIN = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Test
    @DisplayName("CP-MCE-01: la fila de una etapa de lote lleva el ID del lote, el código, la etapa, sus fechas y el estado que deja en el equipamiento")
    void actividadDeEtapa_llevaLosDatosDelLote() {
        // Arrange
        EtapaLoteEntity etapa = EtapaLoteEntity.builder()
                .id(36L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.FINALIZADA)
                .fechaInicio(FIN.minusHours(3)).fechaFinalizacion(FIN)
                .lote(LoteEntity.builder().id(12L).identificadorInterno("LOT-R7-03").build())
                .build();

        // Act
        ActividadEquipamientoResponseDTO fila = MapperControlEquipamiento.toActividadDTO(etapa);

        // Assert
        assertThat(fila.getTipo()).isEqualTo(TipoActividadEquipamiento.LOTE);
        assertThat(fila.getIdReferencia()).isEqualTo(36L);
        assertThat(fila.getIdLote()).isEqualTo(12L);
        assertThat(fila.getCodigoLote()).isEqualTo("LOT-R7-03");
        assertThat(fila.getFechaInicio()).isEqualTo(FIN.minusHours(3));
        assertThat(fila.getFechaFin()).isEqualTo(FIN);
        assertThat(fila.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-MCE-02: la fila de una limpieza lleva su estado resultante real y la de una falla o un mantenimiento, el que cada una deja en el equipamiento")
    void actividadDeOperacion_llevaElEstadoResultante() {
        // Arrange
        LimpiezaEquipamientoEntity limpieza = limpieza(EstadoTransaccion.REGISTRADO);
        limpieza.setEstadoOperativoResultante(EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = FallaEquipamientoEntity.builder().id(6L).fecha(FIN).observaciones("Falla").estado(EstadoTransaccion.REGISTRADO).build();
        MantenimientoEquipamientoEntity mantenimiento = MantenimientoEquipamientoEntity.builder().id(7L).fecha(FIN).observaciones("Mantenimiento").estado(EstadoTransaccion.REGISTRADO).build();

        // Act
        ActividadEquipamientoResponseDTO filaLimpieza = MapperControlEquipamiento.toActividadDTO(limpieza);
        ActividadEquipamientoResponseDTO filaFalla = MapperControlEquipamiento.toActividadDTO(falla);
        ActividadEquipamientoResponseDTO filaMantenimiento = MapperControlEquipamiento.toActividadDTO(mantenimiento);

        // Assert
        assertThat(filaLimpieza.getIdLote()).isNull();
        assertThat(filaLimpieza.getFechaInicio()).isEqualTo(FIN.minusHours(2));
        assertThat(filaLimpieza.getFechaFin()).isNull();
        assertThat(filaLimpieza.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        assertThat(filaFalla.getTipo()).isEqualTo(TipoActividadEquipamiento.FALLA);
        assertThat(filaFalla.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        assertThat(filaMantenimiento.getTipo()).isEqualTo(TipoActividadEquipamiento.MANTENIMIENTO);
        assertThat(filaMantenimiento.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-MCE-03: la operación lleva su fecha de registro y el usuario que la registró")
    void operacionRegistrada_llevaLaFechaDeRegistro() {
        // Act
        OperacionEquipamientoResponseDTO dto = MapperControlEquipamiento.toOperacionDTO(limpieza(EstadoTransaccion.REGISTRADO), true);

        // Assert
        assertThat(dto.getFechaRegistro()).isEqualTo(FIN.plusMinutes(5));
        assertThat(dto.getRegistradoPor()).isEqualTo("operario1");
    }

    @Test
    @DisplayName("CP-MCE-04: la operación anulada lleva su estado, su fecha y su motivo de anulación y no es anulable")
    void operacionAnulada_llevaLosDatosDeLaAnulacion() {
        // Arrange
        LimpiezaEquipamientoEntity anulada = limpieza(EstadoTransaccion.ANULADO);
        anulada.setFechaAnulacion(FIN.plusHours(1));
        anulada.setMotivoAnulacion("Error de carga");

        // Act
        OperacionEquipamientoResponseDTO dto = MapperControlEquipamiento.toOperacionDTO(anulada, false);

        // Assert
        assertThat(dto.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(dto.getFechaAnulacion()).isEqualTo(FIN.plusHours(1));
        assertThat(dto.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(dto.isAnulable()).isFalse();
    }

    private static LimpiezaEquipamientoEntity limpieza(EstadoTransaccion estado) {
        LimpiezaEquipamientoEntity limpieza = LimpiezaEquipamientoEntity.builder()
                .id(5L).fecha(FIN.minusHours(2)).observaciones("Limpieza con soda cáustica")
                .estado(estado).estadoOperativoResultante(EstadoOperativo.DISPONIBLE)
                .equipamiento(FermentadorEntity.builder().id(3L).identificadorInterno("FER-01").build())
                .build();
        limpieza.setCreatedBy("operario1");
        limpieza.setCreatedDate(FIN.plusMinutes(5));
        limpieza.setLastModifiedBy("gerente1");
        return limpieza;
    }
}
