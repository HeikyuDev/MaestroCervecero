package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionLimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.LimpiezaEquipamientoResponseDTO;
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
class LimpiezaEquipamientoServicioImplTest {

    @Mock
    private ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    @Mock
    private IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    @Mock
    private IFallaEquipamientoRepository fallaEquipamientoRepository;
    @Mock
    private IEquipamientoRepository equipamientoRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private LimpiezaEquipamientoServicioImpl limpiezaEquipamientoServicio;

    // ==================== filtrarLimpiezasEquipamiento ====================

    @Test
    @DisplayName("CP-FLE-01: filtrarLimpiezasEquipamiento filtra por los 5 criterios informados")
    void filtrar_debeFiltrarPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        when(limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(1L, MaceradorEntity.class, EstadoTransaccion.REGISTRADO, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(limpieza)));

        // === EJECUCION ===
        Page<LimpiezaEquipamientoResponseDTO> resultado = limpiezaEquipamientoServicio.filtrarLimpiezasEquipamiento(1L, TipoEquipamiento.MACERADOR, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(limpiezaEquipamientoRepository).filtrarLimpiezasEquipamiento(1L, MaceradorEntity.class, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FLE-02: filtrarLimpiezasEquipamiento con los 5 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCincoParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity registrada = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        LimpiezaEquipamientoEntity anulada = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Carga por error", EstadoOperativo.DISPONIBLE, macerador);
        when(limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada)));

        // === EJECUCION ===
        Page<LimpiezaEquipamientoResponseDTO> resultado = limpiezaEquipamientoServicio.filtrarLimpiezasEquipamiento(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(limpiezaEquipamientoRepository).filtrarLimpiezasEquipamiento(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLE-03: filtrarLimpiezasEquipamiento permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        LimpiezaEquipamientoEntity anulada = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Carga por error", EstadoOperativo.DISPONIBLE, macerador);
        when(limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada)));

        // === EJECUCION ===
        Page<LimpiezaEquipamientoResponseDTO> resultado = limpiezaEquipamientoServicio.filtrarLimpiezasEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(limpiezaEquipamientoRepository).filtrarLimpiezasEquipamiento(null, null, EstadoTransaccion.ANULADO, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLE-04: filtrarLimpiezasEquipamiento retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(null, MolinoEntity.class, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<LimpiezaEquipamientoResponseDTO> resultado = limpiezaEquipamientoServicio.filtrarLimpiezasEquipamiento(null, TipoEquipamiento.MOLINO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(limpiezaEquipamientoRepository).filtrarLimpiezasEquipamiento(null, MolinoEntity.class, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLE-05: filtrarLimpiezasEquipamiento filtra por un equipamiento específico")
    void filtrar_debeFiltrarPorEquipamientoEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza1 = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        LimpiezaEquipamientoEntity limpieza2 = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Carga por error", EstadoOperativo.DISPONIBLE, macerador);
        when(limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(1L, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(limpieza1, limpieza2)));

        // === EJECUCION ===
        Page<LimpiezaEquipamientoResponseDTO> resultado = limpiezaEquipamientoServicio.filtrarLimpiezasEquipamiento(1L, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(limpiezaEquipamientoRepository).filtrarLimpiezasEquipamiento(1L, null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la limpieza de equipamiento cuando el ID existe")
    void buscarPorId_debeRetornarLimpiezaExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(limpiezaEquipamientoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(limpiezaEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de equipamiento con ID: 99");

        verify(limpiezaEquipamientoRepository).findById(99L);
    }

    // ==================== registrarLimpiezaEquipamiento ====================

    @Test
    @DisplayName("CP-RLE-01: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha de limpieza es nula")
    void registrar_debeRechazarFechaLimpiezaNula() {
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, null, "Limpieza CIP estándar");

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de limpieza es obligatoria");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-02: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null);

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-03: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "   ");

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository, limpiezaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-04: registrarLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void registrar_debeRechazarEquipamientoInexistente() {
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");

        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-05: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento no está en estado operativo EN_LIMPIEZA")
    void registrar_debeRechazarEquipamientoNoEnLimpieza() {
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE, 50);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una limpieza sobre un equipamiento en estado operativo EN_LIMPIEZA");

        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-06: registrarLimpiezaEquipamiento pasa el equipamiento a DISPONIBLE cuando la cantidad de usos queda por debajo del máximo, sin mantenimiento previo")
    void registrar_debePasarADisponibleSinMantenimientoPrevio() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaLimpieza = LocalDateTime.of(2026, 1, 20, 9, 0);
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, fechaLimpieza, "Limpieza CIP estándar");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, null)).thenReturn(10L);
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocation -> {
            LimpiezaEquipamientoEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).save(macerador);

        ArgumentCaptor<LimpiezaEquipamientoEntity> captor = ArgumentCaptor.forClass(LimpiezaEquipamientoEntity.class);
        verify(limpiezaEquipamientoRepository).save(captor.capture());
        LimpiezaEquipamientoEntity limpiezaGuardada = captor.getValue();
        assertThat(limpiezaGuardada.getFecha()).isEqualTo(fechaLimpieza);
        assertThat(limpiezaGuardada.getObservaciones()).isEqualTo("Limpieza CIP estándar");
        assertThat(limpiezaGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(limpiezaGuardada.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(limpiezaGuardada.getEquipamiento()).isSameAs(macerador);

        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-RLE-07: registrarLimpiezaEquipamiento pasa el equipamiento a EN_MANTENIMIENTO cuando la cantidad de usos alcanza el máximo (límite), sin mantenimiento previo")
    void registrar_debePasarAEnMantenimientoAlAlcanzarElMaximo() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, null)).thenReturn(49L);
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);

        ArgumentCaptor<LimpiezaEquipamientoEntity> captor = ArgumentCaptor.forClass(LimpiezaEquipamientoEntity.class);
        verify(limpiezaEquipamientoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);

        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-RLE-08: registrarLimpiezaEquipamiento acota el conteo de usos a partir del último mantenimiento registrado")
    void registrar_debeAcotarConteoAlUltimoMantenimiento() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaUltimoMantenimiento = LocalDateTime.of(2026, 1, 10, 0, 0);
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.of(fechaUltimoMantenimiento));
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, fechaUltimoMantenimiento)).thenReturn(5L);
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        verify(limpiezaEquipamientoRepository).contarLimpiezasRegistradasDesde(1L, fechaUltimoMantenimiento);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-RLE-09: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando existe una participación en etapa de lote posterior")
    void registrar_debeRechazarSiExisteParticipacionEnEtapaPosterior() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(macerador));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este equipamiento");

        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-10: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha de limpieza es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaLimpiezaFutura() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = limpiezaEquipamientoFormDTO(1L, LocalDateTime.now().plusDays(1), "Limpieza CIP estándar");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual");

        verifyNoInteractions(equipamientoRepository, limpiezaEquipamientoRepository);
    }

    // ==================== anularLimpiezaEquipamiento ====================

    @Test
    @DisplayName("CP-ALE-01: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(limpiezaEquipamientoRepository, equipamientoRepository, mantenimientoEquipamientoRepository, fallaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-ALE-02: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(limpiezaEquipamientoRepository, equipamientoRepository, mantenimientoEquipamientoRepository, fallaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-ALE-03: anularLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando la limpieza no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de equipamiento con ID: 99");

        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-04: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando la limpieza no se encuentra en estado REGISTRADO")
    void anular_debeRechazarLimpiezaNoRegistrada() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular limpiezas de equipamiento en estado REGISTRADO");

        verifyNoInteractions(equipamientoRepository, mantenimientoEquipamientoRepository, fallaEquipamientoRepository);
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-05: anularLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento asociado no existe")
    void anular_debeRechazarEquipamientoAsociadoInexistente() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 2");

        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-06: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado no está en el estado operativo que dejó la limpieza")
    void anular_debeRechazarEquipamientoEnOtroEstado() {
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));

        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una limpieza cuyo equipamiento asociado se encuentre en estado operativo DISPONIBLE");

        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-07: anularLimpiezaEquipamiento anula la limpieza cuando dejó el equipamiento DISPONIBLE (camino feliz)")
    void anular_debeAnularCuandoResultanteEsDisponible() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(limpiezaEquipamientoRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        assertThat(limpieza.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(limpieza.getFechaAnulacion()).isNotNull();
        assertThat(limpieza.getMotivoAnulacion()).isEqualTo("Limpieza cargada por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(equipamientoRepository).save(macerador);
        verify(limpiezaEquipamientoRepository).save(limpieza);
    }

    @Test
    @DisplayName("CP-ALE-08: anularLimpiezaEquipamiento anula la limpieza cuando dejó el equipamiento EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularCuandoResultanteEsEnMantenimiento() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.EN_MANTENIMIENTO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(macerador));
        when(equipamientoRepository.save(macerador)).thenReturn(macerador);
        when(limpiezaEquipamientoRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        assertThat(limpieza.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(equipamientoRepository).save(macerador);
        verify(limpiezaEquipamientoRepository).save(limpieza);
    }

    @Test
    @DisplayName("CP-ALE-09: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando existe una limpieza posterior del mismo tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteLimpiezaPosterior() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-10: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el equipamiento")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE, 50);
        LimpiezaEquipamientoEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativo.DISPONIBLE, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(limpieza));
        when(limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este equipamiento");

        verifyNoInteractions(equipamientoRepository);
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static LimpiezaEquipamientoFormDTO limpiezaEquipamientoFormDTO(Long idEquipamiento, LocalDateTime fechaLimpieza, String observaciones) {
        return LimpiezaEquipamientoFormDTO.builder()
                .idEquipamiento(idEquipamiento)
                .fechaLimpieza(fechaLimpieza)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionLimpiezaEquipamientoFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionLimpiezaEquipamientoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, EstadoOperativo estadoOperativo, Integer usosMaximosAntesMantenimiento) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno("MAC-" + id)
                .descripcion("Macerador de prueba")
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .estado(Estado.ACTIVO)
                .capacidadTotal(500.0)
                .capacidadUtil(450.0)
                .espacioMuerto(10.0)
                .eficienciaMaceracion(0.75)
                .build();
    }

    private static LimpiezaEquipamientoEntity crearLimpiezaEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaLimpieza, String observaciones, EstadoOperativo estadoOperativoResultante, EquipamientoEntity equipamiento) {
        return LimpiezaEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaLimpieza)
                .observaciones(observaciones)
                .estado(estado)
                .estadoOperativoResultante(estadoOperativoResultante)
                .equipamiento(equipamiento)
                .build();
    }
}
