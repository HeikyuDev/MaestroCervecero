package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.SugerenciaFechaFinalizacionResponseDTO;
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
import static org.mockito.Mockito.times;
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
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IConfiguracionProduccionRepository configuracionProduccionRepository;

    @InjectMocks
    private PlanificacionProduccionServicioImpl planificacionProduccionServicio;

    private static final LocalDate FECHA_INICIO_SUGERENCIA = LocalDate.of(2099, 11, 2);

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

    @Test
    @DisplayName("CP-FPP-04: filtrarPlanificacionesProduccion mapea los litros producidos con una sola consulta de agregación y 0.0 para las planificaciones sin envasados")
    void filtrarPlanificacionesProduccion_debeMapearLitrosProducidosConUnaSolaConsulta() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PlanificacionProduccionEntity planificacion1 = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        PlanificacionProduccionEntity planificacion2 = crearPlanificacionEntity(2L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 2));
        PlanificacionProduccionEntity planificacion3 = crearPlanificacionEntity(3L, EstadoSolicitud.FINALIZADA, LocalDate.of(2026, 3, 3));
        when(planificacionProduccionRepository.filtrarPlanificacionesProduccion(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(planificacion1, planificacion2, planificacion3), pageable, 3));
        when(envasadoLoteRepository.sumarLitrosProducidosPorPlanificacion(List.of(1L, 2L, 3L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2350.0}, new Object[]{3L, 100.5}));

        // === EJECUCION ===
        Page<PlanificacionProduccionResponseDTO> resultado = planificacionProduccionServicio.filtrarPlanificacionesProduccion(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).extracting(PlanificacionProduccionResponseDTO::getLitrosProducidos)
                .containsExactly(2350.0, 0.0, 100.5);
        verify(envasadoLoteRepository, times(1)).sumarLitrosProducidosPorPlanificacion(List.of(1L, 2L, 3L));
        verify(envasadoLoteRepository, never()).sumarLitrosProducidosDePlanificacion(any());
    }

    @Test
    @DisplayName("CP-FPP-05: filtrarPlanificacionesProduccion no consulta los litros producidos cuando la página está vacía")
    void filtrarPlanificacionesProduccion_noDebeConsultarLitrosProducidosConPaginaVacia() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(planificacionProduccionRepository.filtrarPlanificacionesProduccion(99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<PlanificacionProduccionResponseDTO> resultado = planificacionProduccionServicio.filtrarPlanificacionesProduccion(99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verifyNoInteractions(envasadoLoteRepository);
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

    @Test
    @DisplayName("CP-BI-03: buscarPorId retorna los litros producidos de la planificación")
    void buscarPorId_debeRetornarLitrosProducidos() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(1250.5);

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getLitrosProducidos()).isEqualTo(1250.5);
        verify(envasadoLoteRepository).sumarLitrosProducidosDePlanificacion(1L);
    }

    @Test
    @DisplayName("CP-BI-04: buscarPorId retorna 0.0 litros producidos cuando la planificación no tiene envasados registrados")
    void buscarPorId_debeRetornarCeroLitrosProducidosSinEnvasados() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getLitrosProducidos()).isEqualTo(0.0);
    }

    // ==================== registrarPlanificacionProduccion ====================

    @Test
    @DisplayName("CP-RPP-01: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir es nula")
    void registrar_debeRechazarCantidadAProducirNula() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(null).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir es obligatoria");

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
                .hasMessage("La fecha de inicio estimada es obligatoria");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-04: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de finalización estimada es nula")
    void registrar_debeRechazarFechaFinalizacionEstimadaNula() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).fechaFinalizacionEstimada(null).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de finalización estimada es obligatoria");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-05: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de finalización estimada es anterior a la de inicio")
    void registrar_debeRechazarFechaFinalizacionAnteriorAInicio() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(LocalDate.of(2099, 4, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2099, 3, 1))
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
        LocalDate fechaInicio = LocalDate.of(2099, 3, 1);
        LocalDate fechaFin = LocalDate.of(2099, 4, 1);
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

    @Test
    @DisplayName("CP-RPP-09: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la fecha de inicio estimada es anterior a hoy (ayer)")
    void registrar_debeRechazarFechaInicioAnteriorAHoy() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(LocalDate.now().minusDays(1))
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(30))
                .build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio estimada no puede ser anterior a la fecha actual");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-10: registrarPlanificacionProduccion acepta una fecha de inicio estimada igual a hoy (límite)")
    void registrar_debeAceptarFechaInicioIgualAHoy() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(30))
                .build();
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(crearRecetaConVersion(1L, true)));
        when(planificacionProduccionRepository.save(any(PlanificacionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.getFechaInicioEstimada()).isEqualTo(formDTO.getFechaInicioEstimada());
        verify(planificacionProduccionRepository).save(any(PlanificacionProduccionEntity.class));
    }

    @Test
    @DisplayName("CP-RPP-11: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir es NaN")
    void registrar_debeRechazarCantidadAProducirNaN() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(Double.NaN).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser un número mayor a 0");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-12: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir es +Infinity")
    void registrar_debeRechazarCantidadAProducirInfinita() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(Double.POSITIVE_INFINITY).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser un número mayor a 0");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-13: registrarPlanificacionProduccion acepta una cantidad a producir igual al techo de 10.000.000 L (límite)")
    void registrar_debeAceptarCantidadAProducirEnElTecho() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(10_000_000.0).build();
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(crearRecetaConVersion(1L, true)));
        when(planificacionProduccionRepository.save(any(PlanificacionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO);

        assertThat(resultado.getCantidadAProducir()).isEqualTo(10_000_000.0);
        verify(planificacionProduccionRepository).save(any(PlanificacionProduccionEntity.class));
    }

    @Test
    @DisplayName("CP-RPP-14: registrarPlanificacionProduccion lanza ReglaNegocioException cuando la cantidad a producir supera el techo de 10.000.000 L")
    void registrar_debeRechazarCantidadAProducirSobreElTecho() {
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L).cantidadAProducir(10_000_000.01).build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir no puede superar 10.000.000 litros");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
    }

    @Test
    @DisplayName("CP-RPP-15: registrarPlanificacionProduccion acepta un rango de exactamente 1.825 días entre el inicio y el fin estimados (límite)")
    void registrar_debeAceptarRangoDe1825Dias() {
        LocalDate inicio = LocalDate.of(2099, 3, 1);
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(inicio.plusDays(1825))
                .build();
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(crearRecetaConVersion(1L, true)));
        when(planificacionProduccionRepository.save(any(PlanificacionProduccionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanificacionProduccionResponseDTO resultado = planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO);

        assertThat(resultado.getFechaFinalizacionEstimada()).isEqualTo(inicio.plusDays(1825));
        verify(planificacionProduccionRepository).save(any(PlanificacionProduccionEntity.class));
    }

    @Test
    @DisplayName("CP-RPP-16: registrarPlanificacionProduccion lanza ReglaNegocioException cuando el rango entre el inicio y el fin estimados es de 1.826 días")
    void registrar_debeRechazarRangoDe1826Dias() {
        LocalDate inicio = LocalDate.of(2099, 3, 1);
        PlanificacionProduccionFormDTO formDTO = planificacionFormDTOValidoBuilder(1L)
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(inicio.plusDays(1826))
                .build();

        assertThatThrownBy(() -> planificacionProduccionServicio.registrarPlanificacionProduccion(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación no puede durar más de 5 años (1.825 días) entre el inicio y el fin estimados");

        verifyNoInteractions(recetaRepository, planificacionProduccionRepository);
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
                .hasMessage("Solo se pueden anular planificaciones de producción en estado Pendiente");

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
                .hasMessage("No se puede anular la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado");

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
                .hasMessage("No se puede anular la planificación de producción porque tiene al menos un lote en estado Finalizado asociado");

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

    @Test
    @DisplayName("CP-APP-08: anularPlanificacionProduccion lanza ReglaNegocioException y no consulta la BD cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-APP-09: anularPlanificacionProduccion anula la planificación cuando el motivo de anulación tiene exactamente 255 caracteres (límite)")
    void anular_debeAceptarMotivoDe255Caracteres() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        AnulacionPlanificacionProduccionFormDTO formDTO = anulacionFormDTO("a".repeat(255));
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(false);
        when(planificacionProduccionRepository.save(planificacion)).thenReturn(planificacion);

        planificacionProduccionServicio.anularPlanificacionProduccion(1L, formDTO);

        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.ANULADA);
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
                .hasMessage("Solo se pueden finalizar planificaciones de producción en estado Pendiente");

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
                .hasMessage("No se puede finalizar la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado");

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
                .hasMessage("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado Finalizado asociado; corresponde anularla en su lugar");

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

    @Test
    @DisplayName("CP-FZPP-08: finalizarPlanificacionProduccion lanza ReglaNegocioException y no consulta la BD cuando el motivo de finalización tiene 256 caracteres")
    void finalizar_debeRechazarMotivoDe256Caracteres() {
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de finalización no puede superar 255 caracteres");

        verifyNoInteractions(planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-FZPP-09: finalizarPlanificacionProduccion finaliza la planificación cuando el motivo de finalización tiene exactamente 255 caracteres (límite)")
    void finalizar_debeAceptarMotivoDe255Caracteres() {
        PlanificacionProduccionEntity planificacion = crearPlanificacionEntity(1L, EstadoSolicitud.PENDIENTE, LocalDate.of(2026, 3, 1));
        FinalizacionForzadaPlanificacionProduccionFormDTO formDTO = finalizacionFormDTO("a".repeat(255));
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, EstadoLote.FINALIZADO)).thenReturn(true);
        when(planificacionProduccionRepository.save(planificacion)).thenReturn(planificacion);

        planificacionProduccionServicio.finalizarPlanificacionProduccion(1L, formDTO);

        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(planificacionProduccionRepository).save(planificacion);
    }

    // ==================== sugerirFechaFinalizacion ====================

    @Test
    @DisplayName("CP-SF-01: sugerirFechaFinalizacion retorna la sugerencia de 4000 L con 14 lotes de 18 días (camino feliz)")
    void sugerirFechaFinalizacion_debeRetornarSugerenciaExitosa() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 4000.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2100, 8, 7));
        assertThat(resultado.getCantidadLotes()).isEqualTo(14);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        verify(planificacionProduccionRepository, never()).save(any());
        verify(recetaRepository, never()).save(any());
        verify(configuracionProduccionRepository, never()).save(any());
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-02: sugerirFechaFinalizacion copia al DTO los datos de la simulación")
    void sugerirFechaFinalizacion_debeCopiarLosDatosDeLaSimulacionAlDTO() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 4000.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2099, 11, 2));
        assertThat(resultado.getCapacidadLoteEstandar()).isEqualTo(300.0);
        assertThat(resultado.getCantidadLotes()).isEqualTo(14);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
    }

    @Test
    @DisplayName("CP-SF-03: sugerirFechaFinalizacion con múltiplo exacto de la capacidad da 2 lotes y no 3 (límite)")
    void sugerirFechaFinalizacion_debeCalcularDosLotesConMultiploExacto() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 600.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(2);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 12, 10));
    }

    @Test
    @DisplayName("CP-SF-04: sugerirFechaFinalizacion con cantidad igual a la capacidad da 1 lote (límite)")
    void sugerirFechaFinalizacion_debeCalcularUnLoteConCantidadIgualALaCapacidad() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 300.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(1);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 20));
    }

    @Test
    @DisplayName("CP-SF-05: sugerirFechaFinalizacion con cantidad menor a la capacidad da 1 lote sin días de separación")
    void sugerirFechaFinalizacion_debeCalcularUnLoteConCantidadMenorALaCapacidad() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(1);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 20));
    }

    @Test
    @DisplayName("CP-SF-06: sugerirFechaFinalizacion con cantidad decimal que no es múltiplo redondea a 2 lotes")
    void sugerirFechaFinalizacion_debeRedondearHaciaArribaConCantidadDecimal() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 450.5, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(2);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 12, 10));
    }

    @Test
    @DisplayName("CP-SF-07: sugerirFechaFinalizacion con cantidad apenas por encima de un múltiplo exacto da 3 lotes (límite)")
    void sugerirFechaFinalizacion_debeCalcularTresLotesApenasPorEncimaDeUnMultiploExacto() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 600.1, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(3);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 12, 30));
    }

    @Test
    @DisplayName("CP-SF-08: sugerirFechaFinalizacion con día de brewing de exactamente 1440 minutos cuenta 1 día (límite)")
    void sugerirFechaFinalizacion_debeContarUnDiaDeBrewingConExactamente1440Minutos() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(600, 840, 10, 7)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(1);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(18L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 20));
    }

    @Test
    @DisplayName("CP-SF-09: sugerirFechaFinalizacion con día de brewing de 1441 minutos se redondea hacia arriba a 2 días (límite)")
    void sugerirFechaFinalizacion_debeRedondearHaciaArribaConDiaDeBrewingDe1441Minutos() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(600, 841, 10, 7)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(1);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(19L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 21));
    }

    @Test
    @DisplayName("CP-SF-10: sugerirFechaFinalizacion con duraciones de brewing en cero o nulas cuenta 0 días de brewing")
    void sugerirFechaFinalizacion_debeContarCeroDiasDeBrewingConDuracionesEnCeroONulas() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(0, 0, 10, 7)));
        when(recetaRepository.findById(2L)).thenReturn(Optional.of(recetaConDuraciones(null, null, 10, 7)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultadoEnCero = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA);
        SugerenciaFechaFinalizacionResponseDTO resultadoNulo = planificacionProduccionServicio.sugerirFechaFinalizacion(2L, 100.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultadoEnCero.getDuracionLoteDias()).isEqualTo(17L);
        assertThat(resultadoEnCero.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 19));
        assertThat(resultadoNulo.getDuracionLoteDias()).isEqualTo(17L);
        assertThat(resultadoNulo.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 19));
    }

    @Test
    @DisplayName("CP-SF-11: sugerirFechaFinalizacion con duraciones de fermentación y maduración nulas las cuenta como 0")
    void sugerirFechaFinalizacion_debeContarCeroConFermentacionYMaduracionNulas() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(60, 90, null, null)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        // === EJECUCION ===
        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA);

        // === ASSERTS ===
        assertThat(resultado.getCantidadLotes()).isEqualTo(1);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(1L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(LocalDate.of(2099, 11, 3));
    }

    @Test
    @DisplayName("CP-SF-12: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad a producir es nula")
    void sugerirFechaFinalizacion_debeRechazarCantidadNula() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, null, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir es obligatoria");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-13: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad a producir es cero (límite)")
    void sugerirFechaFinalizacion_debeRechazarCantidadEnCero() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 0.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser mayor a 0");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-14: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad a producir es negativa")
    void sugerirFechaFinalizacion_debeRechazarCantidadNegativa() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, -5.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser mayor a 0");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-15: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad a producir no es finita")
    void sugerirFechaFinalizacion_debeRechazarCantidadNoFinita() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, Double.NaN, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser un número mayor a 0");
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, Double.POSITIVE_INFINITY, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir debe ser un número mayor a 0");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-16: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la fecha de inicio estimada es nula")
    void sugerirFechaFinalizacion_debeRechazarFechaInicioNula() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio estimada es obligatoria");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-17: sugerirFechaFinalizacion valida la cantidad antes que la fecha")
    void sugerirFechaFinalizacion_debeValidarLaCantidadAntesQueLaFecha() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir es obligatoria");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-18: sugerirFechaFinalizacion lanza RecursoNoEncontradoException cuando la receta no existe o está dada de baja")
    void sugerirFechaFinalizacion_debeRechazarRecetaInexistente() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(99L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 99");
        verifyNoInteractions(configuracionProduccionRepository);
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-19: sugerirFechaFinalizacion lanza RecursoNoEncontradoException cuando la receta no tiene versión activa")
    void sugerirFechaFinalizacion_debeRechazarRecetaSinVersionActiva() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(crearRecetaConVersion(1L, false)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró una versión activa para la receta con ID: 1");
        verifyNoInteractions(configuracionProduccionRepository);
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-20: sugerirFechaFinalizacion lanza RecursoNoEncontradoException cuando no existe la configuración de producción")
    void sugerirFechaFinalizacion_debeRechazarConfiguracionInexistente() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la configuración de producción");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-21: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la capacidad de lote estándar es nula")
    void sugerirFechaFinalizacion_debeRechazarCapacidadNula() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(null)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad de lote estándar configurada es obligatoria");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-22: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la capacidad de lote estándar es cero (límite), sin dividir por cero")
    void sugerirFechaFinalizacion_debeRechazarCapacidadEnCero() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(0.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad de lote estándar configurada debe ser mayor a 0");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-23: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la capacidad de lote estándar es negativa")
    void sugerirFechaFinalizacion_debeRechazarCapacidadNegativa() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(-300.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad de lote estándar configurada debe ser mayor a 0");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-24: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad de lotes excede el máximo representable")
    void sugerirFechaFinalizacion_debeRechazarCantidadDeLotesMayorAIntegerMaxValue() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(1.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 3.0E10, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir es demasiado grande para la capacidad de lote estándar configurada");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-26: sugerirFechaFinalizacion no invoca colaboradores posteriores a la validación fallida ni ejecuta save()")
    void sugerirFechaFinalizacion_noDebeInvocarColaboradoresPosterioresNiPersistir() {
        // === PREPARACION DE DATOS ===
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(null)));

        // === EJECUCION Y ASSERTS ===
        // Orden de validación: cantidad, fecha, fecha no anterior a hoy, receta, versión activa, configuración, capacidad, cantidad de lotes
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad de lote estándar configurada es obligatoria");
        verify(planificacionProduccionRepository, never()).save(any());
        verify(recetaRepository, never()).save(any());
        verify(configuracionProduccionRepository, never()).save(any());
        verifyNoInteractions(loteRepository);
    }

    @Test
    @DisplayName("CP-SF-27: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la fecha de inicio estimada es anterior a hoy (ayer), antes de consultar la receta")
    void sugerirFechaFinalizacion_debeRechazarFechaInicioAnteriorAHoy() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 100.0, LocalDate.now().minusDays(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio estimada no puede ser anterior a la fecha actual");
        verifyNoInteractions(recetaRepository, configuracionProduccionRepository, planificacionProduccionRepository, loteRepository);
    }

    @Test
    @DisplayName("CP-SF-28: sugerirFechaFinalizacion acepta una cantidad a producir igual al techo de 10.000.000 L (límite)")
    void sugerirFechaFinalizacion_debeAceptarCantidadEnElTecho() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(60, 60, 0, 0)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(100_000.0)));

        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 10_000_000.0, FECHA_INICIO_SUGERENCIA);

        // 100 lotes de 1 día + 99 separaciones de 2 días
        assertThat(resultado.getCantidadLotes()).isEqualTo(100);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(FECHA_INICIO_SUGERENCIA.plusDays(298));
    }

    @Test
    @DisplayName("CP-SF-29: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la cantidad a producir supera el techo de 10.000.000 L")
    void sugerirFechaFinalizacion_debeRechazarCantidadSobreElTecho() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(60, 60, 0, 0)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(100_000.0)));

        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 10_000_000.01, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a producir no puede superar 10.000.000 litros");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-30: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la simulación supera los 5 años (100 lotes de 18 días = 1.998 días)")
    void sugerirFechaFinalizacion_debeRechazarSimulacionDeMasDeCincoAnios() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaBaseSugerencia()));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 30_000.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La simulación supera los 5 años (100 lotes de 18 días). Reducí la cantidad a producir o aumentá la capacidad de lote estándar en la configuración");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-SF-31: sugerirFechaFinalizacion acepta una simulación de exactamente 1.825 días (87 lotes de 19 días) (límite)")
    void sugerirFechaFinalizacion_debeAceptarSimulacionDe1825Dias() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(600, 841, 10, 7)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        SugerenciaFechaFinalizacionResponseDTO resultado = planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 26_100.0, FECHA_INICIO_SUGERENCIA);

        assertThat(resultado.getCantidadLotes()).isEqualTo(87);
        assertThat(resultado.getDuracionLoteDias()).isEqualTo(19L);
        assertThat(resultado.getFechaFinalizacionSugerida()).isEqualTo(FECHA_INICIO_SUGERENCIA.plusDays(1825));
    }

    @Test
    @DisplayName("CP-SF-32: sugerirFechaFinalizacion lanza ReglaNegocioException cuando la simulación es de 1.826 días (457 lotes de 2 días)")
    void sugerirFechaFinalizacion_debeRechazarSimulacionDe1826Dias() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaConDuraciones(60, 60, 1, 0)));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(crearConfiguracion(300.0)));

        assertThatThrownBy(() -> planificacionProduccionServicio.sugerirFechaFinalizacion(1L, 137_100.0, FECHA_INICIO_SUGERENCIA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La simulación supera los 5 años (457 lotes de 2 días). Reducí la cantidad a producir o aumentá la capacidad de lote estándar en la configuración");
    }

    // ==================== helpers ====================

    private static PlanificacionProduccionFormDTO.PlanificacionProduccionFormDTOBuilder planificacionFormDTOValidoBuilder(Long idReceta) {
        return PlanificacionProduccionFormDTO.builder()
                .fechaInicioEstimada(LocalDate.of(2099, 3, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2099, 4, 1))
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

    private static RecetaEntity recetaBaseSugerencia() {
        return recetaConDuraciones(60, 90, 10, 7);
    }

    private static RecetaEntity recetaConDuraciones(Integer maceracion, Integer hervido, Integer fermentacion, Integer maduracion) {
        RecetaEntity recetaEntity = crearRecetaConVersion(1L, true);
        VersionRecetaEntity version = recetaEntity.getVersiones().get(0);
        version.setDuracionMaceracion(maceracion);
        version.setDuracionHervido(hervido);
        version.setDuracionFermentacion(fermentacion);
        version.setDuracionMaduracion(maduracion);
        return recetaEntity;
    }

    private static ConfiguracionProduccionEntity crearConfiguracion(Double capacidadLoteEstandar) {
        return ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .capacidadLoteEstandar(capacidadLoteEstandar)
                .build();
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
