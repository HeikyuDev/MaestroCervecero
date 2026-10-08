package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CriterioSeleccionPlanTest {

    // ==================== etiqueta ====================

    @Test
    @DisplayName("CP-CE-01: cada criterio de selección expone su etiqueta legible, con tildes correctas")
    void etiqueta_debeSerElNombreLegibleDeCadaCriterio() {
        // === PREPARACION DE DATOS ===
        // (los criterios son constantes del enum, no requieren preparación)

        // === EJECUCION Y VERIFICACION ===
        assertThat(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD.getEtiqueta()).isEqualTo("Fermentador de mayor capacidad");
        assertThat(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA.getEtiqueta()).isEqualTo("Fermentador que se libera más temprano");
        assertThat(CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES.getEtiqueta()).isEqualTo("Menor cantidad de lotes");
        assertThat(CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA.getEtiqueta()).isEqualTo("Equipos que se liberan más temprano");
    }

    @Test
    @DisplayName("CP-CE-02: todos los criterios tienen etiqueta y ninguna contiene guiones bajos")
    void etiqueta_debeEstarDefinidaParaTodosLosCriterios() {
        // === PREPARACION DE DATOS ===
        CriterioSeleccionPlanSecuencial[] secuenciales = CriterioSeleccionPlanSecuencial.values();
        CriterioSeleccionPlanConcurrente[] concurrentes = CriterioSeleccionPlanConcurrente.values();

        // === EJECUCION Y VERIFICACION ===
        assertThat(secuenciales).hasSize(2);
        assertThat(concurrentes).hasSize(2);
        for (CriterioSeleccionPlanSecuencial criterio : secuenciales) {
            assertThat(criterio.getEtiqueta()).isNotBlank().doesNotContain("_");
        }
        for (CriterioSeleccionPlanConcurrente criterio : concurrentes) {
            assertThat(criterio.getEtiqueta()).isNotBlank().doesNotContain("_");
        }
    }
}
