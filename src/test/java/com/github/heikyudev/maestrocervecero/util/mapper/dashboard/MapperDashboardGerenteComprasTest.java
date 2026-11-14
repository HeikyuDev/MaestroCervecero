package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraFinalizadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionSinOrdenResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardGerenteComprasTest {

    @Test
    @DisplayName("CP-MDC-01: la planificación sin orden lleva su ID, su etiqueta con la receta, su fecha de inicio estimada y su prioridad")
    void toPlanificacionSinOrdenDTO_llevaLosDatosDeLaPlanificacion() {
        // Arrange
        PlanificacionProduccionEntity planificacion = PlanificacionProduccionEntity.builder().id(7L).fechaInicioEstimada(LocalDate.of(2027, 8, 1))
                .prioridad(PrioridadPlanificacion.ALTA).versionReceta(VersionRecetaEntity.builder().nombre("Pale Ale").build()).build();

        // Act
        PlanificacionSinOrdenResponseDTO dto = MapperDashboardGerenteCompras.toPlanificacionSinOrdenDTO(planificacion);

        // Assert
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getEtiqueta()).contains("7").contains("Pale Ale");
        assertThat(dto.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
        assertThat(dto.getPrioridad()).isEqualTo(PrioridadPlanificacion.ALTA);
    }

    @Test
    @DisplayName("CP-MDC-02: la orden en curso lleva su ID, la razón social de su proveedor, su fecha de entrega estimada y el avance recibido")
    void toOrdenCompraEnCursoDTO_llevaLosDatosYElAvance() {
        // Arrange
        OrdenCompraEntity orden = OrdenCompraEntity.builder().id(7L).fechaEntregaEstimada(LocalDate.of(2027, 8, 1))
                .versionProveedor(VersionProveedorEntity.builder().razonSocial("Malterías del Sur S.A.").build()).build();

        // Act
        OrdenCompraEnCursoResponseDTO dto = MapperDashboardGerenteCompras.toOrdenCompraEnCursoDTO(orden, 40);

        // Assert
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getProveedor()).isEqualTo("Malterías del Sur S.A.");
        assertThat(dto.getFechaEntregaEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
        assertThat(dto.getAvanceRecepcion()).isEqualTo(40);
    }

    @Test
    @DisplayName("CP-MDC-03: la orden finalizada lleva su fecha de finalización y su cierre es manual solo si guardó un motivo de finalización")
    void toOrdenCompraFinalizadaDTO_distingueElCierreManualDelAutomatico() {
        // Arrange
        VersionProveedorEntity proveedor = VersionProveedorEntity.builder().razonSocial("Lúpulos SA").build();
        OrdenCompraEntity automatica = OrdenCompraEntity.builder().id(5L).fechaFinalizacion(LocalDateTime.of(2027, 7, 1, 9, 0)).versionProveedor(proveedor).build();
        OrdenCompraEntity manual = OrdenCompraEntity.builder().id(6L).fechaFinalizacion(LocalDateTime.of(2027, 7, 2, 9, 0)).motivoFinalizacion("Motivo").versionProveedor(proveedor).build();

        // Act
        OrdenCompraFinalizadaResponseDTO dtoAutomatica = MapperDashboardGerenteCompras.toOrdenCompraFinalizadaDTO(automatica);
        OrdenCompraFinalizadaResponseDTO dtoManual = MapperDashboardGerenteCompras.toOrdenCompraFinalizadaDTO(manual);

        // Assert
        assertThat(dtoAutomatica.getId()).isEqualTo(5L);
        assertThat(dtoAutomatica.getProveedor()).isEqualTo("Lúpulos SA");
        assertThat(dtoAutomatica.getFechaFinalizacion()).isEqualTo(LocalDateTime.of(2027, 7, 1, 9, 0));
        assertThat(dtoAutomatica.isCierreManual()).isFalse();
        assertThat(dtoManual.isCierreManual()).isTrue();
    }

    @Test
    @DisplayName("CP-MDC-04: una planificación o una orden nulas se mapean a nulo")
    void toDTO_conEntradaNulaDevuelveNulo() {
        // Act and Assert
        assertThat(MapperDashboardGerenteCompras.toPlanificacionSinOrdenDTO(null)).isNull();
        assertThat(MapperDashboardGerenteCompras.toOrdenCompraEnCursoDTO(null, 0)).isNull();
        assertThat(MapperDashboardGerenteCompras.toOrdenCompraFinalizadaDTO(null)).isNull();
    }
}
