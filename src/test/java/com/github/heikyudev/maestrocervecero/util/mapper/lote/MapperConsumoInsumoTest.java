package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperConsumoInsumoTest {

    @Test
    @DisplayName("CP-MCI-01: toDTO informa fecha, motivo y usuario de la anulación de un consumo anulado")
    void toDTO_debeInformarLaAnulacionDeUnConsumoAnulado() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = consumo(EstadoTransaccion.ANULADO);
        consumo.setFechaAnulacion(LocalDateTime.of(2027, 7, 2, 9, 30));
        consumo.setMotivoAnulacion("Error de carga");
        consumo.setLastModifiedBy("operario");

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = MapperConsumoInsumo.toDTO(consumo);

        // === ASSERTS ===
        assertThat(resultado.getFechaAnulacion()).isEqualTo(LocalDateTime.of(2027, 7, 2, 9, 30));
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getUsuarioAnulacion()).isEqualTo("operario");
    }

    @Test
    @DisplayName("CP-MCI-02: toDTO no informa usuario de anulación en un consumo registrado aunque tenga un último modificador")
    void toDTO_noDebeInformarUsuarioDeAnulacionEnUnConsumoRegistrado() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = consumo(EstadoTransaccion.REGISTRADO);
        consumo.setLastModifiedBy("operario");

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = MapperConsumoInsumo.toDTO(consumo);

        // === ASSERTS ===
        assertThat(resultado.getUsuarioAnulacion()).isNull();
        assertThat(resultado.getFechaAnulacion()).isNull();
    }

    @Test
    @DisplayName("CP-MCI-04: toDTO marca vencidoAlRegistrar cuando el lote de insumo vencía antes de la fecha de registro del consumo")
    void toDTO_debeMarcarElLoteVencidoAlRegistrar() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = consumo(EstadoTransaccion.REGISTRADO);
        consumo.setCreatedDate(LocalDateTime.of(2027, 7, 1, 10, 15));
        consumo.setLoteInsumo(LoteInsumoEntity.builder().identificacionLoteProveedor("L-1").fechaVencimiento(LocalDate.of(2027, 6, 30)).build());

        // === EJECUCION + ASSERTS ===
        assertThat(MapperConsumoInsumo.toDTO(consumo).isVencidoAlRegistrar()).isTrue();
    }

    @Test
    @DisplayName("CP-MCI-05: toDTO no marca vencidoAlRegistrar si el lote vencía el mismo día del registro, venció después o no tiene fecha de vencimiento")
    void toDTO_noDebeMarcarElLoteVigenteAlRegistrar() {
        // === PREPARACION DE DATOS ===
        LocalDateTime registro = LocalDateTime.of(2027, 7, 1, 10, 15);
        ConsumoInsumoEntity venceElMismoDia = consumo(EstadoTransaccion.REGISTRADO);
        venceElMismoDia.setCreatedDate(registro);
        venceElMismoDia.setLoteInsumo(LoteInsumoEntity.builder().fechaVencimiento(LocalDate.of(2027, 7, 1)).build());
        ConsumoInsumoEntity vencioDespues = consumo(EstadoTransaccion.REGISTRADO);
        vencioDespues.setCreatedDate(registro);
        vencioDespues.setLoteInsumo(LoteInsumoEntity.builder().fechaVencimiento(LocalDate.of(2020, 1, 1).plusYears(8)).build());
        ConsumoInsumoEntity sinFecha = consumo(EstadoTransaccion.REGISTRADO);
        sinFecha.setCreatedDate(registro);
        sinFecha.setLoteInsumo(LoteInsumoEntity.builder().build());

        // === EJECUCION + ASSERTS ===
        assertThat(MapperConsumoInsumo.toDTO(venceElMismoDia).isVencidoAlRegistrar()).isFalse();
        assertThat(MapperConsumoInsumo.toDTO(vencioDespues).isVencidoAlRegistrar()).isFalse();
        assertThat(MapperConsumoInsumo.toDTO(sinFecha).isVencidoAlRegistrar()).isFalse();
    }

    @Test
    @DisplayName("CP-MCI-03: toDTO retorna null cuando el consumo es nulo")
    void toDTO_debeRetornarNullConConsumoNulo() {
        // === EJECUCION + ASSERTS ===
        assertThat(MapperConsumoInsumo.toDTO(null)).isNull();
    }

    private static ConsumoInsumoEntity consumo(EstadoTransaccion estado) {
        ConsumoInsumoEntity consumo = new ConsumoInsumoEntity();
        consumo.setId(7L);
        consumo.setCantidadConsumida(15.0);
        consumo.setEstado(estado);
        return consumo;
    }
}
