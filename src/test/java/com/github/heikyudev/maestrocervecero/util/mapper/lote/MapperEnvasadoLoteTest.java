package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperEnvasadoLoteTest {

    @Test
    @DisplayName("CP-MEL-01: toDTO informa fecha, motivo y usuario de la anulación de un envasado anulado")
    void toDTO_debeInformarLaAnulacionDeUnEnvasadoAnulado() {
        // === PREPARACION DE DATOS ===
        EnvasadoLoteEntity envasado = envasado(EstadoTransaccion.ANULADO);
        envasado.setFechaAnulacion(LocalDateTime.of(2027, 7, 2, 9, 30));
        envasado.setMotivoAnulacion("Barril equivocado");
        envasado.setLastModifiedBy("operario");

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = MapperEnvasadoLote.toDTO(envasado);

        // === ASSERTS ===
        assertThat(resultado.getFechaAnulacion()).isEqualTo(LocalDateTime.of(2027, 7, 2, 9, 30));
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Barril equivocado");
        assertThat(resultado.getUsuarioAnulacion()).isEqualTo("operario");
    }

    @Test
    @DisplayName("CP-MEL-02: toDTO no informa usuario de anulación en un envasado registrado aunque tenga un último modificador")
    void toDTO_noDebeInformarUsuarioDeAnulacionEnUnEnvasadoRegistrado() {
        // === PREPARACION DE DATOS ===
        EnvasadoLoteEntity envasado = envasado(EstadoTransaccion.REGISTRADO);
        envasado.setLastModifiedBy("operario");

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = MapperEnvasadoLote.toDTO(envasado);

        // === ASSERTS ===
        assertThat(resultado.getUsuarioAnulacion()).isNull();
        assertThat(resultado.getFechaAnulacion()).isNull();
    }

    @Test
    @DisplayName("CP-MEL-03: toDTO retorna null cuando el envasado es nulo")
    void toDTO_debeRetornarNullConEnvasadoNulo() {
        // === EJECUCION + ASSERTS ===
        assertThat(MapperEnvasadoLote.toDTO(null)).isNull();
    }

    private static EnvasadoLoteEntity envasado(EstadoTransaccion estado) {
        EnvasadoLoteEntity envasado = new EnvasadoLoteEntity();
        envasado.setId(4L);
        envasado.setCantidadEnvasada(30.0);
        envasado.setEstado(estado);
        return envasado;
    }
}
