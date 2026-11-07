package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
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
class FallaBarrilServicioImplTest {

    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
    @DisplayName("CP-RFB-01: registrarFallaBarril lanza ReglaNegocioException con campo fecha cuando la fecha de falla es nula")
    void registrar_debeRechazarFechaNula() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, null, "Pérdida de presión");

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de falla es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFB-02: registrarFallaBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), null);

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFB-03: registrarFallaBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "   ");

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFB-04: registrarFallaBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "a".repeat(256));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFB-05: registrarFallaBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-06: registrarFallaBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo DISPONIBLE")
    void registrar_debeRechazarBarrilEnEstadoDeOrigenIncorrecto() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una falla sobre un barril en estado operativo Disponible")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-07: registrarFallaBarril registra la falla y pasa el barril a EN_MANTENIMIENTO (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 15, 10, 0);
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, fecha, "Pérdida de presión");
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
        FallaBarrilEntity guardada = captor.getValue();
        assertThat(guardada.getFecha()).isEqualTo(fecha);
        assertThat(guardada.getObservaciones()).isEqualTo("Pérdida de presión");
        assertThat(guardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(guardada.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RFB-08: registrarFallaBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-09: registrarFallaBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-10: registrarFallaBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la del último envasado")
    void registrar_debeRechazarFechaIgualALaDelUltimoEnvasado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 15, 10, 0);
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, fecha, "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fecha));

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFB-11: registrarFallaBarril lanza ReglaNegocioException con campo fecha cuando la fecha de falla es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaFutura() {
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), "Pérdida de presión");

        assertThatThrownBy(() -> fallaBarrilServicio.registrarFallaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFB-12: registrarFallaBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        FallaBarrilFormDTO formDTO = fallaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.save(any(FallaBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fallaBarrilServicio.registrarFallaBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
    }

    // ==================== anularFallaBarril ====================

    @Test
    @DisplayName("CP-AFB-01: anularFallaBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(fallaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFB-02: anularFallaBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fallaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFB-03: anularFallaBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(fallaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFB-04: anularFallaBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando la falla no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-05: anularFallaBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(fallaBarrilRepository, never()).findById(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-06: anularFallaBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la falla de barril con ID: 7");

        // el ID existe en la raíz de operaciones pero es de otro tipo: no se valida nada más
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-07: anularFallaBarril lanza ReglaNegocioException cuando la falla no se encuentra en estado REGISTRADO")
    void anular_debeRechazarNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);

        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fallas de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-08: anularFallaBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-09: anularFallaBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-10: anularFallaBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior a la falla")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 20, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        assertThat(operacion.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-11: anularFallaBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la de la falla")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFecha() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 15, 10, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-12: anularFallaBarril lanza ReglaNegocioException por el uso cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verify(envasadoLoteRepository, never()).buscarFechaUltimoEnvasadoRegistrado(any());
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-13: anularFallaBarril lanza ReglaNegocioException cuando el barril asociado no se encuentra en estado operativo EN_MANTENIMIENTO")
    void anular_debeRechazarBarrilFueraDelEstadoEsperado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una falla cuyo barril asociado se encuentre en estado operativo En mantenimiento");

        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-14: anularFallaBarril anula la falla y restablece el barril a DISPONIBLE (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fallaBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        FallaBarrilResponseDTO resultado = fallaBarrilServicio.anularFallaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(operacion.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(operacion.getFechaAnulacion()).isNotNull();
        assertThat(operacion.getMotivoAnulacion()).isEqualTo("Falla cargada por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(fallaBarrilRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-AFB-15: anularFallaBarril anula la falla cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 10, 10, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fallaBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        FallaBarrilResponseDTO resultado = fallaBarrilServicio.anularFallaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-AFB-16: anularFallaBarril valida en orden: bloqueo del barril, lectura de la falla, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fallaBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        fallaBarrilServicio.anularFallaBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, fallaBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(fallaBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-AFB-17: anularFallaBarril lanza IllegalStateException sin modificar nada cuando no hay ninguna operación registrada para el barril")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        FallaBarrilEntity operacion = crearFallaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 15, 10, 0), "Pérdida de presión", barril);
        AnulacionFallaBarrilFormDTO formDTO = anulacionFormDTO("Falla cargada por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fallaBarrilServicio.anularFallaBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fallaBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private void stubBarrilBloqueadoYOperacion(BarrilEntity barril, FallaBarrilEntity operacion) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(operacion.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.findById(operacion.getId())).thenReturn(Optional.of(operacion));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idOperacion, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idOperacion));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

    private static FallaBarrilFormDTO fallaBarrilFormDTO(Long idBarril, LocalDateTime fecha, String observaciones) {
        return FallaBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fecha(fecha)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionFallaBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionFallaBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, EstadoOperativoBarril estadoOperativo) {
        FabricanteBarrilEntity fabricante = FabricanteBarrilEntity.builder()
                .id(1L)
                .nombre("Fabricante Test SA")
                .estado(Estado.ACTIVO)
                .build();

        return BarrilEntity.builder()
                .id(id)
                .identificadorInterno("BAR-" + id)
                .capacidad(50.0)
                .contenidoActual(0.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static FallaBarrilEntity crearFallaEntity(Long id, EstadoTransaccion estado, LocalDateTime fecha, String observaciones, BarrilEntity barril) {
        return FallaBarrilEntity.builder()
                .id(id)
                .fecha(fecha)
                .observaciones(observaciones)
                .estado(estado)
                .barril(barril)
                .build();
    }
}
