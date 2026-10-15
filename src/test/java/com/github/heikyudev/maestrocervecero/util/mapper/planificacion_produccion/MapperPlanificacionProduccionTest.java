package com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperPlanificacionProduccionTest {

    @Test
    @DisplayName("CP-MPP-01: toDTO informa fecha, motivo y usuario de la anulación de una planificación anulada")
    void toDTO_debeInformarLaAnulacionDeUnaPlanificacionAnulada() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = planificacion(EstadoSolicitud.ANULADA);
        planificacion.setFechaAnulacion(LocalDateTime.of(2027, 7, 2, 9, 30));
        planificacion.setMotivoAnulacion("Se canceló el pedido del cliente");
        planificacion.setLastModifiedBy("gerente");

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = MapperPlanificacionProduccion.toDTO(planificacion, 0.0);

        // === ASSERTS ===
        assertThat(resultado.getFechaAnulacion()).isEqualTo(LocalDateTime.of(2027, 7, 2, 9, 30));
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Se canceló el pedido del cliente");
        assertThat(resultado.getUsuarioAnulacion()).isEqualTo("gerente");
    }

    @Test
    @DisplayName("CP-MPP-02: toDTO no informa usuario de anulación en una planificación que no está anulada aunque tenga un último modificador")
    void toDTO_noDebeInformarUsuarioDeAnulacionEnUnaPlanificacionNoAnulada() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = planificacion(EstadoSolicitud.PENDIENTE);
        planificacion.setLastModifiedBy("gerente");

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = MapperPlanificacionProduccion.toDTO(planificacion, 0.0);

        // === ASSERTS ===
        assertThat(resultado.getUsuarioAnulacion()).isNull();
    }

    private static PlanificacionProduccionEntity planificacion(EstadoSolicitud estado) {
        PlanificacionProduccionEntity planificacion = new PlanificacionProduccionEntity();
        planificacion.setId(9L);
        planificacion.setCantidadAProducir(100.0);
        planificacion.setEstado(estado);
        return planificacion;
    }
}
