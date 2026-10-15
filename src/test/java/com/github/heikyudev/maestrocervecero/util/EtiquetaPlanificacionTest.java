package com.github.heikyudev.maestrocervecero.util;

import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtiquetaPlanificacionTest {

    @Test
    @DisplayName("CP-EPL-01: de arma \"Plan #N - Nombre\" con el id y el nombre de la receta")
    void de_debeArmarLaEtiquetaCompleta() {
        assertThat(EtiquetaPlanificacion.de(7L, "Pale Ale")).isEqualTo("Plan #7 - Pale Ale");
    }

    @Test
    @DisplayName("CP-EPL-02: de omite el nombre cuando falta o está en blanco y devuelve null sin id")
    void de_debeOmitirElNombreAusenteYDevolverNullSinId() {
        assertThat(EtiquetaPlanificacion.de(7L, null)).isEqualTo("Plan #7");
        assertThat(EtiquetaPlanificacion.de(7L, "  ")).isEqualTo("Plan #7");
        assertThat(EtiquetaPlanificacion.de(null, "Pale Ale")).isNull();
    }

    @Test
    @DisplayName("CP-EPL-03: los DTO de planificación y de lote exponen la misma etiqueta")
    void losDtoExponenLaMismaEtiqueta() {
        PlanificacionProduccionResponseDTO planificacion = PlanificacionProduccionResponseDTO.builder().id(7L)
                .versionReceta(VersionRecetaResponseDTO.builder().nombre("Pale Ale").build()).build();
        LoteCronogramaResponseDTO lote = LoteCronogramaResponseDTO.builder().idPlanificacionProduccion(7L).nombreReceta("Pale Ale").build();

        assertThat(planificacion.getEtiqueta()).isEqualTo("Plan #7 - Pale Ale");
        assertThat(lote.getEtiquetaPlanificacion()).isEqualTo(planificacion.getEtiqueta());
    }
}
