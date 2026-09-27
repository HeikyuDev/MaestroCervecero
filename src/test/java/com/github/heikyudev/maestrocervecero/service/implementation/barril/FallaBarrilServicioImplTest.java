package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FallaBarrilResponseDTO;
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
class FallaBarrilServicioImplTest {

    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IDevolucionBarrilRepository devolucionBarrilRepository;
    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private FallaBarrilServicioImpl fallaBarrilServicio;

    // ==================== filtrarFallasBarril ====================

    @Test
    @DisplayName("CP-FFB-01: filtrarFallasBarril filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        when(fallaBarrilRepository.filtrarFallasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(falla)));

        // === EJECUCION ===
        Page<FallaBarrilResponseDTO> resultado = fallaBarrilServicio.filtrarFallasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(fallaBarrilRepository).filtrarFallasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FFB-02: filtrarFallasBarril con los 4 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity registrada = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        FallaBarrilEntity anulada = crearFallaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 16, 10, 0), "Error de carga", barril);
        when(fallaBarrilRepository.filtrarFallasBarril(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada)));

        // === EJECUCION ===
        Page<FallaBarrilResponseDTO> resultado = fallaBarrilServicio.filtrarFallasBarril(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fallaBarrilRepository).filtrarFallasBarril(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFB-03: filtrarFallasBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity anulada = crearFallaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 16, 10, 0), "Error de carga", barril);
        when(fallaBarrilRepository.filtrarFallasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada)));

        // === EJECUCION ===
        Page<FallaBarrilResponseDTO> resultado = fallaBarrilServicio.filtrarFallasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(fallaBarrilRepository).filtrarFallasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFB-04: filtrarFallasBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fallaBarrilRepository.filtrarFallasBarril(null, 99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<FallaBarrilResponseDTO> resultado = fallaBarrilServicio.filtrarFallasBarril(null, 99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fallaBarrilRepository).filtrarFallasBarril(null, 99L, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFB-05: filtrarFallasBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity falla1 = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        FallaBarrilEntity falla2 = crearFallaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 10, 0), "Error de carga", barril);
        when(fallaBarrilRepository.filtrarFallasBarril(null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(falla1, falla2)));

        // === EJECUCION ===
        Page<FallaBarrilResponseDTO> resultado = fallaBarrilServicio.filtrarFallasBarril(null, 1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fallaBarrilRepository).filtrarFallasBarril(null, 1L, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la falla de barril cuando el ID existe")
    void buscarPorId_debeRetornarFallaExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));

        // === EJECUCION ===
        FallaBarrilResponseDTO resultado = fallaBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(fallaBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(fallaBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de barril con ID: 99");

        verify(fallaBarrilRepository).findById(99L);
    }

    // ==================== registrarFallaBarril ====================

    @Test
    @DisplayName("CP-RFB-01: registrarFallaBarril lanza ReglaNegocioException cuando la fecha de falla es nula")
    void registrar_debeRechazarFechaFallaNula() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, null, "Pérdida de presión");

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de falla es obligatoria");

        verifyNoInteractions(barrilRepository, fallaBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFB-02: registrarFallaBarril lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), null);

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fallaBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFB-03: registrarFallaBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "   ");

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fallaBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFB-04: registrarFallaBarril lanza RecursoNoEncontradoException cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-05: registrarFallaBarril lanza ReglaNegocioException cuando el barril no está en estado operativo DISPONIBLE")
    void registrar_debeRechazarBarrilNoDisponible() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una falla sobre un barril en estado operativo DISPONIBLE");

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-06: registrarFallaBarril registra la falla y pasa el barril a EN_MANTENIMIENTO (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaFalla = LocalDateTime.of(2026, 1, 15, 10, 0);
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, fechaFalla, "Pérdida de presión en la válvula");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fallaBarrilRepository.save(any(FallaBarrilEntity.class))).thenAnswer(invocation -> {
            FallaBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        FallaBarrilResponseDTO resultado = fallaBarrilServicio.registrarFallaBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<FallaBarrilEntity> captor = ArgumentCaptor.forClass(FallaBarrilEntity.class);
        verify(fallaBarrilRepository).save(captor.capture());
        FallaBarrilEntity fallaGuardada = captor.getValue();
        assertThat(fallaGuardada.getFecha()).isEqualTo(fechaFalla);
        assertThat(fallaGuardada.getObservaciones()).isEqualTo("Pérdida de presión en la válvula");
        assertThat(fallaGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(fallaGuardada.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RFB-07: registrarFallaBarril lanza ReglaNegocioException cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 18, 10, 0)));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    // ==================== anularFallaBarril ====================

    @Test
    @DisplayName("CP-AFB-01: anularFallaBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AFB-02: anularFallaBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AFB-03: anularFallaBarril lanza RecursoNoEncontradoException cuando la falla no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de barril con ID: 99");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-04: anularFallaBarril lanza ReglaNegocioException cuando la falla no se encuentra en estado REGISTRADO")
    void anular_debeRechazarFallaNoRegistrada() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fallas de barril en estado REGISTRADO");

        verifyNoInteractions(mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-05: anularFallaBarril lanza RecursoNoEncontradoException cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-06: anularFallaBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo EN_MANTENIMIENTO")
    void anular_debeRechazarBarrilNoEnMantenimiento() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una falla cuyo barril asociado se encuentre en estado operativo EN_MANTENIMIENTO");

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-07: anularFallaBarril anula la falla y restablece el barril a DISPONIBLE (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fallaBarrilRepository.save(falla)).thenReturn(falla);

        // === EJECUCION ===
        FallaBarrilResponseDTO resultado = fallaBarrilServicio.anularFallaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(falla.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(falla.getFechaAnulacion()).isNotNull();
        assertThat(falla.getMotivoAnulacion()).isEqualTo("Falla resuelta por error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(fallaBarrilRepository).save(falla);
    }

    @Test
    @DisplayName("CP-AFB-08: anularFallaBarril lanza ReglaNegocioException cuando existe una falla posterior del mismo tipo sobre el barril")
    void anular_debeRechazarSiExisteFallaPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-09: anularFallaBarril lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el barril")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 18, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-10: anularFallaBarril lanza ReglaNegocioException cuando existe un despacho posterior sobre el barril")
    void anular_debeRechazarSiExisteDespachoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-11: anularFallaBarril lanza ReglaNegocioException cuando existe una devolución posterior sobre el barril")
    void anular_debeRechazarSiExisteDevolucionPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-12: anularFallaBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior sobre el barril")
    void anular_debeRechazarSiExisteFraccionamientoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity falla = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla resuelta por error de carga");
        when(fallaBarrilRepository.findById(1L)).thenReturn(Optional.of(falla));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static FallaBarrilFormDTO fallaBarrilFormDTO(Long idBarril, LocalDateTime fechaFalla, String observaciones) {
        return FallaBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fechaFalla(fechaFalla)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionFallaBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionFallaBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, EstadoOperativoBarril estadoOperativo) {
        FabricanteBarrilEntity fabricante = FabricanteBarrilEntity.builder()
                .id(1L)
                .razonSocial("Fabricante Test SA")
                .nombreComercial("Fabricante Test")
                .cuit("30-12345678-9")
                .telefono("11-2233-4455")
                .email("contacto@fabricante.com")
                .estado(Estado.ACTIVO)
                .build();

        return BarrilEntity.builder()
                .id(id)
                .identificador("BAR-" + id)
                .capacidad(50.0)
                .contenidoActual(0.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static FallaBarrilEntity crearFallaEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaFalla, String observaciones, BarrilEntity barril) {
        return FallaBarrilEntity.builder()
                .id(id)
                .fecha(fechaFalla)
                .observaciones(observaciones)
                .estado(estado)
                .barril(barril)
                .build();
    }
}
