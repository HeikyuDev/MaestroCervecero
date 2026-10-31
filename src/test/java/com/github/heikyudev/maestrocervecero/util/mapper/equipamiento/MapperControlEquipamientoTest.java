package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperControlEquipamientoTest {

    private static final LocalDateTime FIN = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Test
    @DisplayName("CP-MCE-01: el bloque de una etapa de lote lleva el ID del lote, además del de la etapa")
    void bloqueDeEtapa_llevaElIdDelLote() {
        // Arrange
        EtapaLoteEntity etapa = EtapaLoteEntity.builder()
                .id(36L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.FINALIZADA)
                .fechaInicio(FIN.minusHours(3)).fechaFinalizacion(FIN)
                .lote(LoteEntity.builder().id(12L).identificadorInterno("LOT-R7-03").build())
                .build();

        // Act
        BloqueLineaTiempoEquipamientoResponseDTO bloque = MapperControlEquipamiento.toBloqueDTO(etapa, FIN);

        // Assert
        assertThat(bloque.getIdReferencia()).isEqualTo(36L);
        assertThat(bloque.getIdLote()).isEqualTo(12L);
    }

    @Test
    @DisplayName("CP-MCE-02: el bloque de una operación no lleva ningún ID de lote")
    void bloqueDeOperacion_noLlevaIdDeLote() {
        // Arrange
        LimpiezaEquipamientoEntity limpieza = limpieza(EstadoTransaccion.REGISTRADO);

        // Act
        BloqueLineaTiempoEquipamientoResponseDTO bloque = MapperControlEquipamiento.toBloqueDTO(limpieza);

        // Assert
        assertThat(bloque.getIdLote()).isNull();
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
