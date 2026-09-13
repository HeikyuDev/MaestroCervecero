package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteEntityTest {

    // ==================== obtenerEtapaPorTipo ====================

    @Test
    @DisplayName("CP-LE-01: obtenerEtapaPorTipo retorna la etapa del lote que corresponde al tipo indicado")
    void obtenerEtapaPorTipo_debeRetornarLaEtapaCorrespondiente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = LoteEntity.builder().id(1L).etapas(new ArrayList<>()).build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(EtapaLoteEntity.builder().id(1L).etapa(TipoEtapa.MOLIENDA).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(2L).etapa(TipoEtapa.MACERACION).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(3L).etapa(TipoEtapa.HERVIDO).lote(lote).build());
        lote.setEtapas(etapas);

        // === EJECUCION ===
        EtapaLoteEntity resultado = lote.obtenerEtapaPorTipo(TipoEtapa.HERVIDO);

        // === ASSERTS ===
        assertThat(resultado.getEtapa()).isEqualTo(TipoEtapa.HERVIDO);
    }

    @Test
    @DisplayName("CP-LE-02: obtenerEtapaPorTipo lanza RecursoNoEncontradoException si el lote no tiene una etapa del tipo indicado")
    void obtenerEtapaPorTipo_debeLanzarExcepcionSiNoExisteLaEtapa() {
        // === PREPARACION DE DATOS ===
        LoteEntity loteSinEtapas = LoteEntity.builder().id(1L).etapas(new ArrayList<>()).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteSinEtapas.obtenerEtapaPorTipo(TipoEtapa.MACERACION))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lote no tiene una etapa de MACERACION");
    }
}
