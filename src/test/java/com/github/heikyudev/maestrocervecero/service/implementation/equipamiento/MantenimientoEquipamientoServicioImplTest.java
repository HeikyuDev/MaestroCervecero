package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionMantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MantenimientoEquipamientoResponseDTO;
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
class MantenimientoEquipamientoServicioImplTest {

    @Mock
    private IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    @Mock
    private IFallaEquipamientoRepository fallaEquipamientoRepository;
    @Mock
    private ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private MantenimientoEquipamientoServicioImpl mantenimientoEquipamientoServicio;

    // ==================== filtrarMantenimientosEquipamiento ====================

    @Test
    @DisplayName("CP-FME-01: filtrarMantenimientosEquipamiento filtra por los 5 criterios informados")
    void filtrar_debeFiltrarPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        when(mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(1L, MaceradorEntity.class, EstadoTransaccion.REGISTRADO, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(mantenimiento)));

        // === EJECUCION ===
        Page<MantenimientoEquipamientoResponseDTO> resultado = mantenimientoEquipamientoServicio.filtrarMantenimientosEquipamiento(1L, TipoEquipamiento.MACERADOR, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(mantenimientoEquipamientoRepository).filtrarMantenimientosEquipamiento(1L, MaceradorEntity.class, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FME-02: filtrarMantenimientosEquipamiento con los 5 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCincoParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity registrado = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        MantenimientoEquipamientoEntity anulado = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Carga por error", macerador);
        when(mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado)));

        // === EJECUCION ===
        Page<MantenimientoEquipamientoResponseDTO> resultado = mantenimientoEquipamientoServicio.filtrarMantenimientosEquipamiento(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(mantenimientoEquipamientoRepository).filtrarMantenimientosEquipamiento(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FME-03: filtrarMantenimientosEquipamiento permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        MantenimientoEquipamientoEntity anulado = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Carga por error", macerador);
        when(mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<MantenimientoEquipamientoResponseDTO> resultado = mantenimientoEquipamientoServicio.filtrarMantenimientosEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(mantenimientoEquipamientoRepository).filtrarMantenimientosEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FME-04: filtrarMantenimientosEquipamiento retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(null, MolinoEntity.class, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<MantenimientoEquipamientoResponseDTO> resultado = mantenimientoEquipamientoServicio.filtrarMantenimientosEquipamiento(null, TipoEquipamiento.MOLINO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(mantenimientoEquipamientoRepository).filtrarMantenimientosEquipamiento(null, MolinoEntity.class, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FME-05: filtrarMantenimientosEquipamiento filtra por un equipamiento específico")
    void filtrar_debeFiltrarPorEquipamientoEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento1 = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        MantenimientoEquipamientoEntity mantenimiento2 = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Carga por error", macerador);
        when(mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(1L, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(mantenimiento1, mantenimiento2)));

        // === EJECUCION ===
        Page<MantenimientoEquipamientoResponseDTO> resultado = mantenimientoEquipamientoServicio.filtrarMantenimientosEquipamiento(1L, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(mantenimientoEquipamientoRepository).filtrarMantenimientosEquipamiento(1L, null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del mantenimiento de equipamiento cuando el ID existe")
    void buscarPorId_debeRetornarMantenimientoExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));

        // === EJECUCION ===
        MantenimientoEquipamientoResponseDTO resultado = mantenimientoEquipamientoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(mantenimientoEquipamientoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(mantenimientoEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de equipamiento con ID: 99");

        verify(mantenimientoEquipamientoRepository).findById(99L);
    }

    // ==================== registrarMantenimientoEquipamiento ====================

    @Test
    @DisplayName("CP-RME-01: registrarMantenimientoEquipamiento lanza ReglaNegocioException cuando la fecha de mantenimiento es nula")
    void registrar_debeRechazarFechaMantenimientoNula() {
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(1L, null, "Se reemplazó el rodamiento");

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de mantenimiento es obligatoria");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RME-02: registrarMantenimientoEquipamiento lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null);

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RME-03: registrarMantenimientoEquipamiento lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "   ");

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RME-04: registrarMantenimientoEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void registrar_debeRechazarEquipamientoInexistente() {
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento");
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");

        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RME-05: registrarMantenimientoEquipamiento lanza ReglaNegocioException cuando el equipamiento no está en estado operativo EN_MANTENIMIENTO")
    void registrar_debeRechazarEquipamientoNoEnMantenimiento() {
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un mantenimiento sobre un equipamiento en estado operativo EN_MANTENIMIENTO");

        verify(equipamientoRepository, never()).save(any());
        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RME-06: registrarMantenimientoEquipamiento registra el mantenimiento y pasa el equipamiento a DISPONIBLE (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaMantenimiento = LocalDateTime.of(2026, 1, 20, 9, 0);
        MantenimientoEquipamientoFormDTO formDTO = mantenimientoEquipamientoFormDTO(1L, fechaMantenimiento, "Se reemplazó el rodamiento y se lubricó el eje");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(mantenimientoEquipamientoRepository.save(any(MantenimientoEquipamientoEntity.class))).thenAnswer(invocation -> {
            MantenimientoEquipamientoEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        MantenimientoEquipamientoResponseDTO resultado = mantenimientoEquipamientoServicio.registrarMantenimientoEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).save(macerador);

        ArgumentCaptor<MantenimientoEquipamientoEntity> captor = ArgumentCaptor.forClass(MantenimientoEquipamientoEntity.class);
        verify(mantenimientoEquipamientoRepository).save(captor.capture());
        MantenimientoEquipamientoEntity mantenimientoGuardado = captor.getValue();
        assertThat(mantenimientoGuardado.getFecha()).isEqualTo(fechaMantenimiento);
        assertThat(mantenimientoGuardado.getObservaciones()).isEqualTo("Se reemplazó el rodamiento y se lubricó el eje");
        assertThat(mantenimientoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(mantenimientoGuardado.getEquipamiento()).isSameAs(macerador);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularMantenimientoEquipamiento ====================

    @Test
    @DisplayName("CP-AME-01: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(mantenimientoEquipamientoRepository, fallaEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AME-02: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(mantenimientoEquipamientoRepository, fallaEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AME-03: anularMantenimientoEquipamiento lanza RecursoNoEncontradoException cuando el mantenimiento no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de equipamiento con ID: 99");

        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AME-04: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando el mantenimiento no se encuentra en estado REGISTRADO")
    void anular_debeRechazarMantenimientoNoRegistrado() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular mantenimientos de equipamiento en estado REGISTRADO");

        verifyNoInteractions(fallaEquipamientoRepository, limpiezaEquipamientoRepository, equipamientoRepository);
        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AME-05: anularMantenimientoEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento asociado no existe")
    void anular_debeRechazarEquipamientoAsociadoInexistente() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 2");

        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AME-06: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado no está en estado operativo DISPONIBLE")
    void anular_debeRechazarEquipamientoNoDisponible() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_USO);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un mantenimiento cuyo equipamiento asociado se encuentre en estado operativo DISPONIBLE");

        verify(equipamientoRepository, never()).save(any());
        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AME-07: anularMantenimientoEquipamiento anula el mantenimiento y restablece el equipamiento a EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(mantenimientoEquipamientoRepository.save(mantenimiento)).thenReturn(mantenimiento);

        // === EJECUCION ===
        MantenimientoEquipamientoResponseDTO resultado = mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        assertThat(mantenimiento.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(mantenimiento.getFechaAnulacion()).isNotNull();
        assertThat(mantenimiento.getMotivoAnulacion()).isEqualTo("Mantenimiento cargado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(equipamientoRepository).save(macerador);
        verify(mantenimientoEquipamientoRepository).save(mantenimiento);
    }

    @Test
    @DisplayName("CP-AME-08: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando existe un mantenimiento posterior del mismo tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteMantenimientoPosterior() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AME-09: anularMantenimientoEquipamiento lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        MantenimientoEquipamientoEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó el rodamiento", macerador);
        AnulacionMantenimientoEquipamientoFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoEquipamientoRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoEquipamientoServicio.anularMantenimientoEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(mantenimientoEquipamientoRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static MantenimientoEquipamientoFormDTO mantenimientoEquipamientoFormDTO(Long idEquipamiento, LocalDateTime fechaMantenimiento, String observaciones) {
        return MantenimientoEquipamientoFormDTO.builder()
                .idEquipamiento(idEquipamiento)
                .fechaMantenimiento(fechaMantenimiento)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionMantenimientoEquipamientoFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionMantenimientoEquipamientoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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

    private static MantenimientoEquipamientoEntity crearMantenimientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaMantenimiento, String observaciones, EquipamientoEntity equipamiento) {
        return MantenimientoEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaMantenimiento)
                .observaciones(observaciones)
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }
}
