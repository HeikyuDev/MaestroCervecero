package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificacionProduccionServicioImplTest {

    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IRecetaRepository recetaRepository;
    @Mock
    private ILoteRepository loteRepository;

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

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la planificación de producción cuando el ID existe")
    void buscarPorId_debeRetornarPlanificacionExistente() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(planificacionProduccionRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(planificacionProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planificacionProduccionServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");

        verify(planificacionProduccionRepository).findById(99L);
    }

    // ==================== registrarPlanificacionProduccion ====================

    @Test
    @DisplayName("CP-RPP-01: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir es nula")
    void registrar_debeRechazarCantidadAProducirNula() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(null).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser mayor a 0");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-02: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir es cero")
    void registrar_debeRechazarCantidadAProducirNoPositiva() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(0.0).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser mayor a 0");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-03: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de inicio estimada es nula")
    void registrar_debeRechazarFechaInicioEstimadaNula() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).fechaInicioEstimada(null).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-04: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de finalización estimada es nula")
    void registrar_debeRechazarFechaFinalizacionEstimadaNula() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).fechaFinalizacionEstimada(null).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-05: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de finalización estimada es anterior a la de inicio")
    void registrar_debeRechazarFechaFinalizacionAnteriorAInicio() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(LocalDate.of(2026, 4, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2026, 3, 1))
                .build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-06: registrarPlanificacionProduccion lanza RecursoNoEncontradoException cuando la receta no existe")
    void registrar_debeRechazarRecetaInexistente() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(99L).build();
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 99");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RPP-07: registrarPlanificacionProduccion lanza RecursoNoEncontradoException cuando la receta no tiene una versión activa")
    void registrar_debeRechazarRecetaSinVersionActiva() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).build();
        RecetaEntity receta = crearRecetaConVersion(1L, false);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró una versión activa para la receta con ID: 1");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RPP-08: registrarPlanificacionProduccion registra la planificación en estado PENDIENTE con la versión activa de la receta (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDate fechaInicio = LocalDate.of(2026, 3, 1);
        LocalDate fechaFin = LocalDate.of(2026, 4, 1);
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(fechaInicio)
                .fechaFinalizacionEstimada(fechaFin)
                .cantidadAProducir(100.0)
                .build();
        RecetaEntity receta = crearRecetaConVersion(1L, true);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
        when(planificacionProduccionRepository.save(any(PlanificacionProduccionEntity.class))).thenAnswer(invocation -> {
            PlanificacionProduccionEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PlanificacionProduccionEntity> captor = ArgumentCaptor.forClass(PlanificacionProduccionEntity.class);
        verify(planificacionProduccionRepository).save(captor.capture());
        PlanificacionProduccionEntity guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(guardada.getCantidadAProducir()).isEqualTo(100.0);
        assertThat(guardada.getVersionReceta()).isSameAs(receta.getVersiones().get(0));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    // ==================== anularPlanificacionProduccion ====================

    @Test
    @DisplayName("CP-APP-01: anularPlanificacionProduccion lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-APP-02: anularPlanificacionProduccion lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-APP-03: anularPlanificacionProduccion lanza RecursoNoEncontradoException cuando la planificación no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(planificacionProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APP-04: anularPlanificacionProduccion lanza ReglaNegocioException cuando la planificación no se encuentra en estado PENDIENTE")
    void anular_debeRechazarPlanificacionNoPendiente() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.FINALIZADA, LocalDate.of(2026, 3, 1));
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular planificaciones de producción en estado PENDIENTE");

        verifyNoInteractions(loteRepository);
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APP-05: anularPlanificacionProduccion lanza ReglaNegocioException cuando tiene un lote PENDIENTE o EN_EJECUCION asociado")
    void anular_debeRechazarConLoteEnCursoAsociado() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(true);

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la planificación de producción porque tiene al menos un lote en estado PENDIENTE o EN_EJECUCION asociado");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APP-06: anularPlanificacionProduccion lanza ReglaNegocioException cuando tiene un lote FINALIZADO asociado")
    void anular_debeRechazarConLoteFinalizadoAsociado() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(true);

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la planificación de producción porque tiene al menos un lote en estado FINALIZADO asociado");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APP-07: anularPlanificacionProduccion anula la planificación sin lotes en curso ni finalizados asociados (camino feliz)")
    void anular_debeAnularSinLotesEnCursoNiFinalizados() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("Error de carga");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(false);
        when(planificacionProduccionRepository.save(planificacion)).thenReturn(planificacion);

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO);

        // === ASSERTS ===
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        assertThat(planificacion.getFechaAnulacion()).isNotNull();
        assertThat(planificacion.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
        verify(planificacionProduccionRepository).save(planificacion);
    }

    // ==================== finalizarPlanificacionProduccion ====================

    @Test
    @DisplayName("CP-FZPP-01: finalizarPlanificacionProduccion lanza ReglaNegocioException cuando el motivo de finalización es nulo")
    void finalizar_debeRechazarMotivoNulo() {
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO(null);

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-FZPP-02: finalizarPlanificacionProduccion lanza ReglaNegocioException cuando el motivo de finalización está en blanco")
    void finalizar_debeRechazarMotivoEnBlanco() {
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("   ");

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización es obligatorio");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-FZPP-03: finalizarPlanificacionProduccion lanza RecursoNoEncontradoException cuando la planificación no existe")
    void finalizar_debeLanzarExcepcionSiNoExiste() {
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("Cantidad producida insuficiente");
        when(planificacionProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZPP-04: finalizarPlanificacionProduccion lanza ReglaNegocioException cuando la planificación no se encuentra en estado PENDIENTE")
    void finalizar_debeRechazarPlanificacionNoPendiente() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.ANULADA, LocalDate.of(2026, 3, 1));
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("Cantidad producida insuficiente");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden finalizar planificaciones de producción en estado PENDIENTE");

        verifyNoInteractions(loteRepository);
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZPP-05: finalizarPlanificacionProduccion lanza ReglaNegocioException cuando tiene un lote PENDIENTE o EN_EJECUCION asociado")
    void finalizar_debeRechazarConLoteEnCursoAsociado() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("Cantidad producida insuficiente");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(true);

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede finalizar la planificación de producción porque tiene al menos un lote en estado PENDIENTE o EN_EJECUCION asociado");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZPP-06: finalizarPlanificacionProduccion lanza ReglaNegocioException cuando no tiene ningún lote FINALIZADO asociado")
    void finalizar_debeRechazarSinLoteFinalizadoAsociado() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("Cantidad producida insuficiente");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(false);

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado FINALIZADO asociado; corresponde anularla en su lugar");

        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FZPP-07: finalizarPlanificacionProduccion finaliza la planificación con al menos un lote FINALIZADO asociado (camino feliz)")
    void finalizar_debeFinalizarConLoteFinalizadoAsociado() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("Cantidad producida insuficiente");
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(true);
        when(planificacionProduccionRepository.save(planificacion)).thenReturn(planificacion);

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO);

        // === ASSERTS ===
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isNotNull();
        assertThat(planificacion.getMotivoFinalizacion()).isEqualTo("Cantidad producida insuficiente");
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(planificacionProduccionRepository).save(planificacion);
    }

    // ==================== helpers ====================

    private static PlanificacionProduccionFormDTO.PlanificacionProduccionFormDTOBuilder planificacionFormDTOValidoBuilder(Long idReceta) {
        return PlanificacionProduccionFormDTO.builder()
                .fechaInicioEstimada(LocalDate.of(2026, 3, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2026, 4, 1))
                .cantidadAProducir(100.0)
                .idReceta(idReceta);
    }

    private static AnulacionPlanificacionProduccionFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionPlanificacionProduccionFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static FinalizacionForzadaPlanificacionProduccionFormDTO finalizacionFormDTO(String motivoFinalizacion) {
        return FinalizacionForzadaPlanificacionProduccionFormDTO.builder().motivoFinalizacion(motivoFinalizacion).build();
    }

    private static RecetaEntity crearRecetaConVersion(Long idReceta, boolean esUltimaVersion) {
        RecetaEntity recetaEntity = RecetaEntity.builder()
                .id(idReceta)
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
                .esUltimaVersion(esUltimaVersion)
                .receta(recetaEntity)
                .build();

        recetaEntity.getVersiones().add(versionRecetaEntity);
        return recetaEntity;
    }

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
