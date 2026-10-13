package com.github.heikyudev.maestrocervecero.util.validacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LimitesTecnicosTest {

    @Test
    @DisplayName("CP-LT-01: los límites de texto tienen los valores aprobados")
    void limitesDeTexto_debenTenerLosValoresAprobados() {
        // Act & Assert
        assertThat(LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE).isEqualTo(100);
        assertThat(LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO).isEqualTo(255);
    }

    @Test
    @DisplayName("CP-LT-02: los límites de equipamiento tienen los valores aprobados")
    void limitesDeEquipamiento_debenTenerLosValoresAprobados() {
        // Act & Assert
        assertThat(LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS).isEqualTo(100_000.0);
        assertThat(LimitesTecnicos.RENDIMIENTO_MOLINO_MAXIMO_KG_H).isEqualTo(10_000.0);
        assertThat(LimitesTecnicos.USOS_MAXIMOS_MANTENIMIENTO_MAXIMO).isEqualTo(100_000);
    }

    @Test
    @DisplayName("CP-LT-03: los límites de recetas tienen los valores aprobados")
    void limitesDeRecetas_debenTenerLosValoresAprobados() {
        // Act & Assert
        assertThat(LimitesTecnicos.VOLUMEN_BASE_RECETA_MAXIMO_LITROS).isEqualTo(100_000.0);
        assertThat(LimitesTecnicos.RELACION_EMPASTE_MAXIMA_L_POR_KG).isEqualTo(20.0);
        assertThat(LimitesTecnicos.IBU_MAXIMO).isEqualTo(500);
        assertThat(LimitesTecnicos.CANTIDAD_INSUMO_RECETA_MAXIMA).isEqualTo(1_000_000.0);
        assertThat(LimitesTecnicos.DURACION_MACERACION_HERVIDO_MAXIMA_MINUTOS).isEqualTo(10_080);
        assertThat(LimitesTecnicos.DURACION_FERMENTACION_MADURACION_MAXIMA_DIAS).isEqualTo(3_650);
    }

    @Test
    @DisplayName("CP-LT-04: los límites de planificación y configuración tienen los valores aprobados")
    void limitesDePlanificacionYConfiguracion_debenTenerLosValoresAprobados() {
        // Act & Assert
        assertThat(LimitesTecnicos.CANTIDAD_A_PRODUCIR_MAXIMA_LITROS).isEqualTo(10_000_000.0);
        assertThat(LimitesTecnicos.RANGO_MAXIMO_PLANIFICACION_DIAS).isEqualTo(1_825);
        assertThat(LimitesTecnicos.CAPACIDAD_LOTE_ESTANDAR_MAXIMA_LITROS).isEqualTo(100_000.0);
        assertThat(LimitesTecnicos.VELOCIDAD_ENVASADO_MAXIMA_L_POR_HORA).isEqualTo(100_000.0);
    }

    @Test
    @DisplayName("CP-LT-05: los límites de consumos y envasados y la escala de las cantidades tienen los valores aprobados")
    void limitesDeConsumosYEnvasados_debenTenerLosValoresAprobados() {
        // Act & Assert
        assertThat(LimitesTecnicos.CANTIDAD_CONSUMIDA_MAXIMA).isEqualTo(1_000_000.0);
        assertThat(LimitesTecnicos.CANTIDAD_ENVASADA_MAXIMA_LITROS).isEqualTo(100_000.0);
        assertThat(LimitesTecnicos.ESCALA_CANTIDADES).isEqualTo(4);
    }
}
