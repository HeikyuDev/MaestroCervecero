package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionRecetaResponseDTOTest {

    private static VersionRecetaResponseDTO version(Integer maceracion, Integer hervido, Integer fermentacion, Integer maduracion) {
        return VersionRecetaResponseDTO.builder()
                .duracionMaceracion(maceracion)
                .duracionHervido(hervido)
                .duracionFermentacion(fermentacion)
                .duracionMaduracion(maduracion)
                .build();
    }

    @Test
    @DisplayName("CP-VRR-01: la duración total combina días (fermentación + maduración) con horas y minutos (maceración + hervido)")
    void duracionTotalEstimada_debeCombinarDiasHorasYMinutos() {
        assertThat(version(60, 90, 10, 4).getDuracionTotalEstimada()).isEqualTo("14 días 2 h 30 min");
    }

    @Test
    @DisplayName("CP-VRR-02: las unidades en cero se omiten")
    void duracionTotalEstimada_debeOmitirLasUnidadesEnCero() {
        assertThat(version(45, 0, 0, 0).getDuracionTotalEstimada()).isEqualTo("45 min");
        assertThat(version(60, 60, 0, 0).getDuracionTotalEstimada()).isEqualTo("2 h");
        assertThat(version(0, 0, 7, 0).getDuracionTotalEstimada()).isEqualTo("7 días");
    }

    @Test
    @DisplayName("CP-VRR-03: un solo día se muestra en singular")
    void duracionTotalEstimada_debeUsarSingularParaUnDia() {
        assertThat(version(0, 30, 1, 0).getDuracionTotalEstimada()).isEqualTo("1 día 30 min");
    }

    @Test
    @DisplayName("CP-VRR-04: una duración nula cuenta como cero y sin ninguna duración devuelve \"0 min\"")
    void duracionTotalEstimada_debeTratarLosNulosComoCero() {
        assertThat(version(null, 90, null, 3).getDuracionTotalEstimada()).isEqualTo("3 días 1 h 30 min");
        assertThat(version(null, null, null, null).getDuracionTotalEstimada()).isEqualTo("0 min");
    }
}
