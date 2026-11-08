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
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
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
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
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
    private IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
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
    @DisplayName("CP-RLE-01: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha es nula")
    void registrar_debeRechazarFechaInicioNula() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, null, "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha y hora de la operación es obligatoria")
                .extracting("campo").isEqualTo("fecha");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-04: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha es posterior a la fecha y hora actual")
    void registrar_debeRechazarFinFuturo() {
        // === PREPARACION DE DATOS ===
        LocalDateTime manana = LocalDateTime.now().plusDays(1);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, manana, "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación no puede ser posterior a la fecha y hora actual")
                .extracting("campo").isEqualTo("fecha");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-05: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .extracting("campo").isEqualTo("observaciones");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-06: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "   ");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-07: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "a".repeat(256));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .extracting("campo").isEqualTo("observaciones");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RLE-08: registrarLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void registrar_debeLanzarExcepcionSiElEquipamientoNoExiste() {
        // === PREPARACION DE DATOS ===
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(99L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-09: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento está dado de baja")
    void registrar_debeRechazarEquipamientoDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        macerador.setEstado(Estado.BAJA);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El equipamiento está dado de baja");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-10: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento está en uso")
    void registrar_debeRechazarEquipamientoEnUso() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_USO);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar una limpieza porque el equipamiento está en uso");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-11: registrarLimpiezaEquipamiento orienta con la operación previa cuando el equipamiento está disponible")
    void registrar_debeOrientarSegunElEstadoOperativo1() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar una limpieza porque el equipamiento está disponible");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-12: registrarLimpiezaEquipamiento orienta con la operación previa cuando el equipamiento está en mantenimiento")
    void registrar_debeOrientarSegunElEstadoOperativo2() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Para registrar una limpieza, primero registre el mantenimiento del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-13: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha coincide con la de la última operación registrada")
    void registrar_debeRechazarInicioIgualAlFinDeLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento")
                .extracting("campo").isEqualTo("fecha");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-14: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha es anterior a la de la última operación registrada")
    void registrar_debeRechazarInicioAnteriorAlFinDeLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO.plusHours(1)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-15: registrarLimpiezaEquipamiento lanza ReglaNegocioException cuando la fecha no es posterior al fin de la última participación en un lote")
    void registrar_debeRechazarInicioNoPosteriorALaUltimaParticipacionEnLote() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(INICIO));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLE-17: registrarLimpiezaEquipamiento registra la operación, deja el equipamiento en el estado operativo esperado y bloquea el equipamiento antes de consultar la última operación (camino feliz)")
    void registrar_debeRegistrarYBloquearElEquipamientoPrimero() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "  ruido anormal en el motor  ");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusDays(2)));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusDays(1)));
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(resultado.getFecha()).isEqualTo(INICIO);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).save(macerador);

        ArgumentCaptor<LimpiezaEquipamientoEntity> captor = ArgumentCaptor.forClass(LimpiezaEquipamientoEntity.class);
        verify(limpiezaEquipamientoRepository).save(captor.capture());
        assertThat(captor.getValue().getFecha()).isEqualTo(INICIO);
        assertThat(captor.getValue().getObservaciones()).isEqualTo("ruido anormal en el motor");
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(captor.getValue().getEquipamiento()).isSameAs(macerador);

        InOrder orden = inOrder(equipamientoRepository, operacionCicloVidaEquipamientoRepository, etapaLoteRepository);
        orden.verify(equipamientoRepository).buscarPorIdParaOperacionCicloVida(1L);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(etapaLoteRepository).buscarFechaUltimaParticipacionRegistrada(1L);
    }

    @Test
    @DisplayName("CP-RLE-18: registrarLimpiezaEquipamiento pasa el equipamiento a DISPONIBLE cuando la cantidad de usos queda por debajo del máximo, sin mantenimiento previo")
    void registrar_debePasarADisponibleCuandoLosUsosQuedanPorDebajoDelMaximo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Limpieza CIP estándar");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, null)).thenReturn(48L);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-RLE-19: registrarLimpiezaEquipamiento pasa el equipamiento a EN_MANTENIMIENTO cuando la cantidad de usos alcanza el máximo (límite), sin mantenimiento previo")
    void registrar_debePasarAEnMantenimientoCuandoLosUsosAlcanzanElMaximo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Limpieza CIP estándar");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, null)).thenReturn(49L);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-RLE-20: registrarLimpiezaEquipamiento acota el conteo de usos a partir de la fecha del último mantenimiento registrado")
    void registrar_debeAcotarElConteoDeUsosAlFinDelUltimoMantenimiento() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA, 50);
        LocalDateTime finUltimoMantenimiento = INICIO.minusDays(10);
        LimpiezaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, "Limpieza CIP estándar");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.of(finUltimoMantenimiento));
        when(limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(1L, finUltimoMantenimiento)).thenReturn(3L);
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        limpiezaEquipamientoServicio.registrarLimpiezaEquipamiento(formDTO);

        // === ASSERTS ===
        verify(limpiezaEquipamientoRepository).contarLimpiezasRegistradasDesde(1L, finUltimoMantenimiento);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    // ==================== anularLimpiezaEquipamiento ====================

    @Test
    @DisplayName("CP-ALE-01: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        // === PREPARACION DE DATOS ===
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO(null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .extracting("campo").isEqualTo("motivoAnulacion");
        verifyNoInteractions(limpiezaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-ALE-02: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        // === PREPARACION DE DATOS ===
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("   ");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");
        verifyNoInteractions(limpiezaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-ALE-03: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .extracting("campo").isEqualTo("motivoAnulacion");
        verifyNoInteractions(limpiezaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-ALE-04: anularLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando la operación no existe")
    void anular_debeLanzarExcepcionSiLaOperacionNoExiste() {
        // === PREPARACION DE DATOS ===
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de equipamiento con ID: 99");
        verifyNoInteractions(equipamientoRepository);
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-05: anularLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento asociado no existe")
    void anular_debeLanzarExcepcionSiElEquipamientoNoExiste() {
        // === PREPARACION DE DATOS ===
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 2");
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-06: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado está dado de baja")
    void anular_debeRechazarEquipamientoDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        macerador.setEstado(Estado.BAJA);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El equipamiento está dado de baja");
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-07: anularLimpiezaEquipamiento lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeLanzarExcepcionSiLaOperacionEsDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de equipamiento con ID: 1");
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-08: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando la operación no se encuentra en estado REGISTRADO")
    void anular_debeRechazarOperacionNoRegistrada() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.ANULADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular limpiezas de equipamiento en estado Registrado");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-09: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando la última operación registrada es otra")
    void anular_debeRechazarSiNoEsLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(99L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-10: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado no está en el estado operativo que dejó la operación")
    void anular_debeRechazarEstadoOperativoIncorrecto() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una limpieza cuyo equipamiento asociado se encuentre en estado operativo Disponible");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-11: anularLimpiezaEquipamiento anula la operación y restablece el equipamiento (camino feliz)")
    void anular_debeAnularYRestablecerElEstadoOperativo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("  carga por error  ");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("carga por error");
        assertThat(resultado.getFechaAnulacion()).isNotNull();
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        verify(equipamientoRepository).save(macerador);
        verify(limpiezaEquipamientoRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-ALE-12: anularLimpiezaEquipamiento bloquea el equipamiento antes de leer la operación y de verificar que sea la última")
    void anular_debeBloquearElEquipamientoAntesDeVerificarLaUltima() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaEquipamientoRepository, equipamientoRepository, limpiezaEquipamientoRepository);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdEquipamientoDeLaOperacion(1L);
        orden.verify(equipamientoRepository).buscarPorIdParaOperacionCicloVida(2L);
        orden.verify(limpiezaEquipamientoRepository).findById(1L);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdUltimaOperacionRegistrada(2L);
    }


    @Test
    @DisplayName("CP-ALE-13: anularLimpiezaEquipamiento anula la limpieza cuando dejó el equipamiento EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularLimpiezaQueDejoEnMantenimiento() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        operacion.setEstadoOperativoResultante(EstadoOperativo.EN_MANTENIMIENTO);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        LimpiezaEquipamientoResponseDTO resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-ALE-14: anularLimpiezaEquipamiento exige el estado operativo exacto que dejó la limpieza, no cualquiera de los dos posibles")
    void anular_debeExigirElEstadoOperativoExactoQueDejoLaLimpieza() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        operacion.setEstadoOperativoResultante(EstadoOperativo.EN_MANTENIMIENTO);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una limpieza cuyo equipamiento asociado se encuentre en estado operativo En mantenimiento");
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-15: anularLimpiezaEquipamiento lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarSiLaUltimaEsOtraConIgualFecha() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(2L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-16: anularLimpiezaEquipamiento lanza ReglaNegocioException sin consultar el estado operativo ni modificar nada cuando el equipamiento participó en un lote posterior a la operación")
    void anular_debeRechazarSiElEquipamientoParticipoEnUnLotePosterior() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.of(INICIO.plusHours(3)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el equipamiento participó en un lote posterior");
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-17: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el fin de la última participación en un lote coincide con la fecha de la operación")
    void anular_debeRechazarSiElLoteTerminaEnLaMismaFechaDeLaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.of(INICIO));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el equipamiento participó en un lote posterior");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-18: anularLimpiezaEquipamiento anula la operación cuando es posterior al fin de la última participación en un lote")
    void anular_debePermitirSiLaOperacionEsPosteriorAlUltimoLote() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.of(INICIO.minusHours(1)));
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        var resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        verify(limpiezaEquipamientoRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-ALE-19: anularLimpiezaEquipamiento anula la operación cuando el equipamiento nunca participó en un lote")
    void anular_debePermitirSiElEquipamientoNuncaParticipoEnUnLote() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        var resultado = limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(etapaLoteRepository).buscarFechaUltimaParticipacionRegistrada(2L);
        verify(limpiezaEquipamientoRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-ALE-20: anularLimpiezaEquipamiento valida en orden: bloqueo del equipamiento, última operación, participación en un lote y estado operativo")
    void anular_debeValidarElLoteDespuesDeLaUltimaOperacionYAntesDelEstado() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.empty());
        when(limpiezaEquipamientoRepository.save(any(LimpiezaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(equipamientoRepository, operacionCicloVidaEquipamientoRepository, etapaLoteRepository);
        orden.verify(equipamientoRepository).buscarPorIdParaOperacionCicloVida(2L);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(etapaLoteRepository).buscarFechaUltimaParticipacionRegistrada(2L);
        orden.verify(equipamientoRepository).save(macerador);
    }

    @Test
    @DisplayName("CP-ALE-21: anularLimpiezaEquipamiento lanza ReglaNegocioException cuando el equipamiento está en uso por un lote")
    void anular_debeRechazarEquipamientoEnUso() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_USO);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el equipamiento está en uso por un lote");
        InOrder orden = inOrder(operacionCicloVidaEquipamientoRepository, etapaLoteRepository);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(etapaLoteRepository).buscarFechaUltimaParticipacionRegistrada(2L);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALE-22: anularLimpiezaEquipamiento con el equipamiento en otro estado que no es el exigido rechaza por el estado operativo y no por el uso")
    void anular_debeRechazarPorEstadoOperativoCuandoNoEstaEnUso() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_LIMPIEZA);
        LimpiezaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionLimpiezaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(limpiezaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaEquipamientoServicio.anularLimpiezaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una limpieza cuyo equipamiento asociado se encuentre en estado operativo Disponible");
        verify(equipamientoRepository, never()).save(any());
        verify(limpiezaEquipamientoRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 15, 12, 0);

    private static LimpiezaEquipamientoFormDTO registroFormDTO(Long idEquipamiento, LocalDateTime fecha, String observaciones) {
        return LimpiezaEquipamientoFormDTO.builder()
                .idEquipamiento(idEquipamiento)
                .fecha(fecha)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionLimpiezaEquipamientoFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionLimpiezaEquipamientoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, EstadoOperativo estadoOperativo) {
        return crearMaceradorEntity(id, estadoOperativo, 100);
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, EstadoOperativo estadoOperativo, Integer usosMaximosAntesMantenimiento) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno("MAC-" + id)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .estado(Estado.ACTIVO)
                .capacidadTotal(500.0)
                .capacidadUtil(450.0)
                .espacioMuerto(10.0)
                .eficienciaMaceracion(0.75)
                .build();
    }

    private static LimpiezaEquipamientoEntity nuevaOperacion(Long id, EstadoTransaccion estado, EquipamientoEntity equipamiento) {
        return LimpiezaEquipamientoEntity.builder()
                .id(id)
                .fecha(INICIO)
                .observaciones("Observaciones de prueba")
                .estado(estado)
                .estadoOperativoResultante(EstadoOperativo.DISPONIBLE)
                .equipamiento(equipamiento)
                .build();
    }

    private static LimpiezaEquipamientoEntity crearLimpiezaEntity(Long id, EstadoTransaccion estado, LocalDateTime fecha, String observaciones, EstadoOperativo estadoOperativoResultante, EquipamientoEntity equipamiento) {
        return LimpiezaEquipamientoEntity.builder()
                .id(id)
                .fecha(fecha)
                .observaciones(observaciones)
                .estado(estado)
                .estadoOperativoResultante(estadoOperativoResultante)
                .equipamiento(equipamiento)
                .build();
    }
}
