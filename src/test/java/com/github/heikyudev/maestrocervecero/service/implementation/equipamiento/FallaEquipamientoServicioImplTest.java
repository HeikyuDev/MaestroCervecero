package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;
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

import java.time.LocalDateTime;
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
class FallaEquipamientoServicioImplTest {

    @Mock
    private IFallaEquipamientoRepository fallaEquipamientoRepository;
    @Mock
    private IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    @Mock
    private ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private FallaEquipamientoServicioImpl fallaEquipamientoServicio;

    // ==================== filtrarFallasEquipamiento ====================

    @Test
    @DisplayName("CP-FFE-01: filtrarFallasEquipamiento filtra por los 5 criterios informados")
    void filtrar_debeFiltrarPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        when(fallaEquipamientoRepository.filtrarFallasEquipamiento(1L, EstadoTransaccion.REGISTRADO, desde, hasta, MaceradorEntity.class, pageable))
                .thenReturn(new PageImpl<>(List.of(falla)));

        // === EJECUCION ===
        Page<FallaEquipamientoResponseDTO> resultado = fallaEquipamientoServicio.filtrarFallasEquipamiento(1L, EstadoTransaccion.REGISTRADO, desde, hasta, TipoEquipamiento.MACERADOR, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(fallaEquipamientoRepository).filtrarFallasEquipamiento(1L, EstadoTransaccion.REGISTRADO, desde, hasta, MaceradorEntity.class, pageable);
    }

    @Test
    @DisplayName("CP-FFE-02: filtrarFallasEquipamiento con los 5 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCincoParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity registrada = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        FallaEquipamientoEntity anulada = crearFallaEquipamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 16, 10, 0), "Error de carga", macerador);
        when(fallaEquipamientoRepository.filtrarFallasEquipamiento(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada)));

        // === EJECUCION ===
        Page<FallaEquipamientoResponseDTO> resultado = fallaEquipamientoServicio.filtrarFallasEquipamiento(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fallaEquipamientoRepository).filtrarFallasEquipamiento(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFE-03: filtrarFallasEquipamiento permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoEntity anulada = crearFallaEquipamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 16, 10, 0), "Error de carga", macerador);
        when(fallaEquipamientoRepository.filtrarFallasEquipamiento(null, EstadoTransaccion.ANULADO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada)));

        // === EJECUCION ===
        Page<FallaEquipamientoResponseDTO> resultado = fallaEquipamientoServicio.filtrarFallasEquipamiento(null, EstadoTransaccion.ANULADO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(fallaEquipamientoRepository).filtrarFallasEquipamiento(null, EstadoTransaccion.ANULADO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFE-04: filtrarFallasEquipamiento retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fallaEquipamientoRepository.filtrarFallasEquipamiento(null, null, null, null, MolinoEntity.class, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<FallaEquipamientoResponseDTO> resultado = fallaEquipamientoServicio.filtrarFallasEquipamiento(null, null, null, null, TipoEquipamiento.MOLINO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fallaEquipamientoRepository).filtrarFallasEquipamiento(null, null, null, null, MolinoEntity.class, pageable);
    }

    @Test
    @DisplayName("CP-FFE-05: filtrarFallasEquipamiento filtra por un equipamiento específico")
    void filtrar_debeFiltrarPorEquipamientoEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla1 = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        FallaEquipamientoEntity falla2 = crearFallaEquipamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 10, 0), "Vibración excesiva", macerador);
        when(fallaEquipamientoRepository.filtrarFallasEquipamiento(1L, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(falla1, falla2)));

        // === EJECUCION ===
        Page<FallaEquipamientoResponseDTO> resultado = fallaEquipamientoServicio.filtrarFallasEquipamiento(1L, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fallaEquipamientoRepository).filtrarFallasEquipamiento(1L, null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la falla de equipamiento cuando el ID existe")
    void buscarPorId_debeRetornarFallaExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(fallaEquipamientoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(fallaEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaEquipamientoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de equipamiento con ID: 99");

        verify(fallaEquipamientoRepository).findById(99L);
    }

    // ==================== registrarFallaEquipamiento ====================

    @Test
    @DisplayName("CP-RFE-01: registrarFallaEquipamiento lanza ReglaNegocioException cuando la fecha de falla es nula")
    void registrar_debeRechazarFechaFallaNula() {
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(1L, null, "Ruido anormal");

        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de falla es obligatoria");

        verifyNoInteractions(equipamientoRepository, fallaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-02: registrarFallaEquipamiento lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), null);

        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, fallaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-03: registrarFallaEquipamiento lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "   ");

        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, fallaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-04: registrarFallaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void registrar_debeRechazarEquipamientoInexistente() {
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(99L, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");

        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-05: registrarFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento no está en estado operativo DISPONIBLE")
    void registrar_debeRechazarEquipamientoNoDisponible() {
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_USO);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una falla sobre un equipamiento en estado operativo DISPONIBLE");

        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-06: registrarFallaEquipamiento registra la falla y pasa el equipamiento a EN_MANTENIMIENTO (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaFalla = LocalDateTime.of(2026, 1, 15, 10, 0);
        FallaEquipamientoFormDTO formDTO = fallaEquipamientoFormDTO(1L, fechaFalla, "Ruido anormal en el motor");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(fallaEquipamientoRepository.save(any(FallaEquipamientoEntity.class))).thenAnswer(invocation -> {
            FallaEquipamientoEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        verify(equipamientoRepository).save(macerador);

        ArgumentCaptor<FallaEquipamientoEntity> captor = ArgumentCaptor.forClass(FallaEquipamientoEntity.class);
        verify(fallaEquipamientoRepository).save(captor.capture());
        FallaEquipamientoEntity fallaGuardada = captor.getValue();
        assertThat(fallaGuardada.getFecha()).isEqualTo(fechaFalla);
        assertThat(fallaGuardada.getObservaciones()).isEqualTo("Ruido anormal en el motor");
        assertThat(fallaGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(fallaGuardada.getEquipamiento()).isSameAs(macerador);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularFallaEquipamiento ====================

    @Test
    @DisplayName("CP-AFE-01: anularFallaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fallaEquipamientoRepository, mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AFE-02: anularFallaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fallaEquipamientoRepository, mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AFE-03: anularFallaEquipamiento lanza RecursoNoEncontradoException cuando la falla no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de equipamiento con ID: 99");

        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-04: anularFallaEquipamiento lanza ReglaNegocioException cuando la falla no se encuentra en estado REGISTRADO")
    void anular_debeRechazarFallaNoRegistrada() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fallas de equipamiento en estado REGISTRADO");

        verifyNoInteractions(mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-05: anularFallaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento asociado no existe")
    void anular_debeRechazarEquipamientoAsociadoInexistente() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 2");

        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-06: anularFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado no está en estado operativo EN_MANTENIMIENTO")
    void anular_debeRechazarEquipamientoNoEnMantenimiento() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una falla cuyo equipamiento asociado se encuentre en estado operativo EN_MANTENIMIENTO");

        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-07: anularFallaEquipamiento anula la falla y restablece el equipamiento a DISPONIBLE (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(fallaEquipamientoRepository.save(falla)).thenReturn(falla);

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(falla.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(falla.getFechaAnulacion()).isNotNull();
        assertThat(falla.getMotivoAnulacion()).isEqualTo("Falla resuelta por error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(equipamientoRepository).save(macerador);
        verify(fallaEquipamientoRepository).save(falla);
    }

    @Test
    @DisplayName("CP-AFE-08: anularFallaEquipamiento lanza ReglaNegocioException cuando existe una falla posterior del mismo tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteFallaPosterior() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-09: anularFallaEquipamiento lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoEntity falla = crearFallaEquipamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Ruido anormal", macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 18, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static FallaEquipamientoFormDTO fallaEquipamientoFormDTO(Long idEquipamiento, LocalDateTime fechaFalla, String observaciones) {
        return FallaEquipamientoFormDTO.builder()
                .idEquipamiento(idEquipamiento)
                .fechaFalla(fechaFalla)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionFallaEquipamientoFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionFallaEquipamientoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno("MAC-" + id)
                .descripcion("Macerador de prueba")
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .capacidadTotal(500.0)
                .capacidadUtil(450.0)
                .espacioMuerto(10.0)
                .eficienciaMaceracion(0.75)
                .build();
    }

    private static FallaEquipamientoEntity crearFallaEquipamientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaFalla, String observaciones, EquipamientoEntity equipamiento) {
        return FallaEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaFalla)
                .observaciones(observaciones)
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }
}
