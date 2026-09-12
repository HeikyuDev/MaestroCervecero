package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificacionProduccionServicioImplTest {

    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IRecetaRepository recetaRepository;

    @InjectMocks
    private PlanificacionProduccionServicioImpl planificacionProduccionServicio;

    // ==================== filtrarPlanificacionesProduccion ====================

    @Test
    @DisplayName("CP-FPP-01: filtrarPlanificacionesProduccion retorna una página correctamente mapeada a DTO cuando se filtra por los 3 criterios")
    void filtrarPlanificacionesProduccion_debeRetornarPaginaMapeadaFiltrandoPorLosTresCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate fechaInicio = LocalDate.of(2026, 3, 1);
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, fechaInicio);
        when(planificacionProduccionRepository.filtrarPlanificacionesProduccion(1L, EstadoSolicitud.PENDIENTE, fechaInicio, pageable))
                .thenReturn(new PageImpl<>(List.of(planificacion), pageable, 1));

        // === EJECUCION ===
        Page<PlanificacionProduccionResponseDTO> resultado = planificacionProduccionServicio.filtrarPlanificacionesProduccion(1L, EstadoSolicitud.PENDIENTE, fechaInicio, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.getContent().get(0).getFechaInicioEstimada()).isEqualTo(fechaInicio);
        verify(planificacionProduccionRepository).filtrarPlanificacionesProduccion(1L, EstadoSolicitud.PENDIENTE, fechaInicio, pageable);
    }

    @Test
    @DisplayName("CP-FPP-02: filtrarPlanificacionesProduccion propaga los 3 criterios nulos sin restringir la búsqueda")
    void filtrarPlanificacionesProduccion_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PlanificacionProduccionEntity planificacion1 = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        PlanificacionProduccionEntity planificacion2 = crearPlanificacionEntity(2L, EstadoSolicitud.FINALIZADA, LocalDate.of(2026, 4, 1));
        when(planificacionProduccionRepository.filtrarPlanificacionesProduccion(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(planificacion1, planificacion2), pageable, 2));

        // === EJECUCION ===
        Page<PlanificacionProduccionResponseDTO> resultado = planificacionProduccionServicio.filtrarPlanificacionesProduccion(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(planificacionProduccionRepository).filtrarPlanificacionesProduccion(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FPP-03: filtrarPlanificacionesProduccion retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarPlanificacionesProduccion_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(planificacionProduccionRepository.filtrarPlanificacionesProduccion(99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<PlanificacionProduccionResponseDTO> resultado = planificacionProduccionServicio.filtrarPlanificacionesProduccion(99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(planificacionProduccionRepository).filtrarPlanificacionesProduccion(99L, null, null, pageable);
    }

    // ==================== helpers ====================

    private static PlanificacionProduccionEntity crearPlanificacionEntity(Long id, EstadoSolicitud estado, LocalDate fechaInicioEstimada) {
        RecetaEntity recetaEntity = RecetaEntity.builder()
                .id(1L)
                .contadorLotes(1L)
                .estado(Estado.ACTIVO)
                .build();

        VersionRecetaEntity versionRecetaEntity = VersionRecetaEntity.builder()
                .id(1L)
                .nombre("IPA Clásica")
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.050)
                .fgObjetivo(1.010)
                .ibuObjetivo(40)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(14)
                .duracionMaduracion(7)
                .esUltimaVersion(true)
                .receta(recetaEntity)
                .build();

        return PlanificacionProduccionEntity.builder()
                .id(id)
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaInicioEstimada.plusDays(21))
                .cantidadAProducir(100.0)
                .estado(estado)
                .versionReceta(versionRecetaEntity)
                .build();
    }
}
