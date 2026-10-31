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
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;
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
class FallaEquipamientoServicioImplTest {

    @Mock
    private IFallaEquipamientoRepository fallaEquipamientoRepository;
    @Mock
    private IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    @Mock
    private IEquipamientoRepository equipamientoRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

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
    @DisplayName("CP-RFE-01: registrarFallaEquipamiento lanza ReglaNegocioException cuando la fecha de inicio es nula")
    void registrar_debeRechazarFechaInicioNula() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, null, FIN, "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio es obligatoria")
                .extracting("campo").isEqualTo("fechaInicio");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-02: registrarFallaEquipamiento lanza ReglaNegocioException cuando la fecha de fin es nula")
    void registrar_debeRechazarFechaFinNula() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, null, "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de fin es obligatoria")
                .extracting("campo").isEqualTo("fechaFin");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-03: registrarFallaEquipamiento lanza ReglaNegocioException cuando la fecha de fin es anterior a la de inicio")
    void registrar_debeRechazarFinAnteriorAlInicio() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, INICIO.minusMinutes(1), "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de fin no puede ser anterior a la fecha de inicio")
                .extracting("campo").isEqualTo("fechaFin");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-04: registrarFallaEquipamiento lanza ReglaNegocioException cuando la fecha de fin es posterior a la fecha y hora actual")
    void registrar_debeRechazarFinFuturo() {
        // === PREPARACION DE DATOS ===
        LocalDateTime manana = LocalDateTime.now().plusDays(1);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, manana, "Ruido anormal");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de fin no puede ser posterior a la fecha y hora actual")
                .extracting("campo").isEqualTo("fechaFin");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-05: registrarFallaEquipamiento lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .extracting("campo").isEqualTo("observaciones");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-06: registrarFallaEquipamiento lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "   ");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-07: registrarFallaEquipamiento lanza ReglaNegocioException cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "a".repeat(256));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El texto de las observaciones no puede superar 255 caracteres")
                .extracting("campo").isEqualTo("observaciones");
        verifyNoInteractions(equipamientoRepository, operacionCicloVidaEquipamientoRepository);
    }

    @Test
    @DisplayName("CP-RFE-08: registrarFallaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void registrar_debeLanzarExcepcionSiElEquipamientoNoExiste() {
        // === PREPARACION DE DATOS ===
        FallaEquipamientoFormDTO formDTO = registroFormDTO(99L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-09: registrarFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento está dado de baja")
    void registrar_debeRechazarEquipamientoDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        macerador.setEstado(Estado.BAJA);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El equipamiento está dado de baja");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-10: registrarFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento está en uso")
    void registrar_debeRechazarEquipamientoEnUso() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_USO);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar una falla porque el equipamiento está en uso");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-11: registrarFallaEquipamiento orienta con la operación previa cuando el equipamiento está en limpieza")
    void registrar_debeOrientarSegunElEstadoOperativo1() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_LIMPIEZA);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Para registrar una falla, primero registre la limpieza del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-12: registrarFallaEquipamiento orienta con la operación previa cuando el equipamiento está en mantenimiento")
    void registrar_debeOrientarSegunElEstadoOperativo2() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Para registrar una falla, primero registre el mantenimiento del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-13: registrarFallaEquipamiento lanza ReglaNegocioException cuando el inicio coincide con el fin de la última operación registrada")
    void registrar_debeRechazarInicioIgualAlFinDeLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaFinUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento")
                .extracting("campo").isEqualTo("fechaInicio");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-14: registrarFallaEquipamiento lanza ReglaNegocioException cuando el inicio es anterior al fin de la última operación registrada")
    void registrar_debeRechazarInicioAnteriorAlFinDeLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaFinUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO.plusHours(1)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-15: registrarFallaEquipamiento lanza ReglaNegocioException cuando el inicio no es posterior al fin de la última participación en un lote")
    void registrar_debeRechazarInicioNoPosteriorALaUltimaParticipacionEnLote() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(INICIO));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de inicio debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFE-16: registrarFallaEquipamiento acepta una operación instantánea (fin igual al inicio) posterior a la actividad anterior")
    void registrar_debeAceptarFinIgualAlInicio() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, INICIO, "Ruido anormal");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaFinUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusSeconds(1)));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusDays(1)));
        when(fallaEquipamientoRepository.save(any(FallaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getFechaInicio()).isEqualTo(INICIO);
        assertThat(resultado.getFechaFin()).isEqualTo(INICIO);
    }

    @Test
    @DisplayName("CP-RFE-17: registrarFallaEquipamiento registra la operación, deja el equipamiento en el estado operativo esperado y bloquea el equipamiento antes de consultar la última operación (camino feliz)")
    void registrar_debeRegistrarYBloquearElEquipamientoPrimero() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(1L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoFormDTO formDTO = registroFormDTO(1L, INICIO, FIN, "  ruido anormal en el motor  ");
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(1L)).thenReturn(Optional.of(macerador));
        when(operacionCicloVidaEquipamientoRepository.buscarFechaFinUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusDays(2)));
        when(etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(1L)).thenReturn(Optional.of(INICIO.minusDays(1)));
        when(fallaEquipamientoRepository.save(any(FallaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.registrarFallaEquipamiento(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(resultado.getFechaInicio()).isEqualTo(INICIO);
        assertThat(resultado.getFechaFin()).isEqualTo(FIN);
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_MANTENIMIENTO);
        verify(equipamientoRepository).save(macerador);

        ArgumentCaptor<FallaEquipamientoEntity> captor = ArgumentCaptor.forClass(FallaEquipamientoEntity.class);
        verify(fallaEquipamientoRepository).save(captor.capture());
        assertThat(captor.getValue().getFecha()).isEqualTo(INICIO);
        assertThat(captor.getValue().getFechaFin()).isEqualTo(FIN);
        assertThat(captor.getValue().getObservaciones()).isEqualTo("ruido anormal en el motor");
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(captor.getValue().getEquipamiento()).isSameAs(macerador);

        InOrder orden = inOrder(equipamientoRepository, operacionCicloVidaEquipamientoRepository, etapaLoteRepository);
        orden.verify(equipamientoRepository).buscarPorIdParaOperacionCicloVida(1L);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarFechaFinUltimaOperacionRegistrada(1L);
        orden.verify(etapaLoteRepository).buscarFechaUltimaParticipacionRegistrada(1L);
    }


    // ==================== anularFallaEquipamiento ====================

    @Test
    @DisplayName("CP-AFE-01: anularFallaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        // === PREPARACION DE DATOS ===
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO(null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .extracting("campo").isEqualTo("motivoAnulacion");
        verifyNoInteractions(fallaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AFE-02: anularFallaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        // === PREPARACION DE DATOS ===
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("   ");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");
        verifyNoInteractions(fallaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AFE-03: anularFallaEquipamiento lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .extracting("campo").isEqualTo("motivoAnulacion");
        verifyNoInteractions(fallaEquipamientoRepository, operacionCicloVidaEquipamientoRepository, equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AFE-04: anularFallaEquipamiento lanza RecursoNoEncontradoException cuando la operación no existe")
    void anular_debeLanzarExcepcionSiLaOperacionNoExiste() {
        // === PREPARACION DE DATOS ===
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de equipamiento con ID: 99");
        verifyNoInteractions(equipamientoRepository);
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-05: anularFallaEquipamiento lanza RecursoNoEncontradoException cuando el equipamiento asociado no existe")
    void anular_debeLanzarExcepcionSiElEquipamientoNoExiste() {
        // === PREPARACION DE DATOS ===
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 2");
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-06: anularFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado está dado de baja")
    void anular_debeRechazarEquipamientoDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        macerador.setEstado(Estado.BAJA);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El equipamiento está dado de baja");
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-07: anularFallaEquipamiento lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeLanzarExcepcionSiLaOperacionEsDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de equipamiento con ID: 1");
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-08: anularFallaEquipamiento lanza ReglaNegocioException cuando la operación no se encuentra en estado REGISTRADO")
    void anular_debeRechazarOperacionNoRegistrada() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.ANULADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fallas de equipamiento en estado Registrado");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-09: anularFallaEquipamiento lanza ReglaNegocioException cuando la última operación registrada es otra")
    void anular_debeRechazarSiNoEsLaUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(99L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-10: anularFallaEquipamiento lanza ReglaNegocioException cuando el equipamiento asociado no está en el estado operativo que dejó la operación")
    void anular_debeRechazarEstadoOperativoIncorrecto() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.DISPONIBLE);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una falla cuyo equipamiento asociado se encuentre en estado operativo En mantenimiento");
        verify(equipamientoRepository, never()).save(any());
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFE-11: anularFallaEquipamiento anula la operación y restablece el equipamiento (camino feliz)")
    void anular_debeAnularYRestablecerElEstadoOperativo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("  carga por error  ");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(fallaEquipamientoRepository.save(any(FallaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        FallaEquipamientoResponseDTO resultado = fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("carga por error");
        assertThat(resultado.getFechaAnulacion()).isNotNull();
        assertThat(macerador.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).save(macerador);
        verify(fallaEquipamientoRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-AFE-12: anularFallaEquipamiento bloquea el equipamiento antes de leer la operación y de verificar que sea la última")
    void anular_debeBloquearElEquipamientoAntesDeVerificarLaUltima() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(fallaEquipamientoRepository.save(any(FallaEquipamientoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // === EJECUCION ===
        fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaEquipamientoRepository, equipamientoRepository, fallaEquipamientoRepository);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdEquipamientoDeLaOperacion(1L);
        orden.verify(equipamientoRepository).buscarPorIdParaOperacionCicloVida(2L);
        orden.verify(fallaEquipamientoRepository).findById(1L);
        orden.verify(operacionCicloVidaEquipamientoRepository).buscarIdUltimaOperacionRegistrada(2L);
    }


    @Test
    @DisplayName("CP-AFE-13: anularFallaEquipamiento lanza ReglaNegocioException ante un empate de fecha de fin cuando la última es la de mayor ID")
    void anular_debeRechazarSiLaUltimaEsOtraConIgualFechaDeFin() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = crearMaceradorEntity(2L, EstadoOperativo.EN_MANTENIMIENTO);
        FallaEquipamientoEntity operacion = nuevaOperacion(1L, EstadoTransaccion.REGISTRADO, macerador);
        AnulacionFallaEquipamientoFormDTO formDTO = anulacionFormDTO("Carga por error");
        when(operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(equipamientoRepository.buscarPorIdParaOperacionCicloVida(2L)).thenReturn(Optional.of(macerador));
        when(fallaEquipamientoRepository.findById(1L)).thenReturn(Optional.of(operacion));
        when(operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(2L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaEquipamientoServicio.anularFallaEquipamiento(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
        verify(fallaEquipamientoRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 15, 12, 0);

    private static FallaEquipamientoFormDTO registroFormDTO(Long idEquipamiento, LocalDateTime fechaInicio, LocalDateTime fechaFin, String observaciones) {
        return FallaEquipamientoFormDTO.builder()
                .idEquipamiento(idEquipamiento)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionFallaEquipamientoFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionFallaEquipamientoFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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

    private static FallaEquipamientoEntity nuevaOperacion(Long id, EstadoTransaccion estado, EquipamientoEntity equipamiento) {
        return FallaEquipamientoEntity.builder()
                .id(id)
                .fecha(INICIO)
                .fechaFin(FIN)
                .observaciones("Observaciones de prueba")
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }

    private static FallaEquipamientoEntity crearFallaEquipamientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaInicio, String observaciones, EquipamientoEntity equipamiento) {
        return FallaEquipamientoEntity.builder()
                .id(id)
                .fecha(fechaInicio)
                .fechaFin(fechaInicio.plusHours(2))
                .observaciones(observaciones)
                .estado(estado)
                .equipamiento(equipamiento)
                .build();
    }
}
