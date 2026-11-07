package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionMantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.MantenimientoBarrilResponseDTO;
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
class MantenimientoBarrilServicioImplTest {

    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private MantenimientoBarrilServicioImpl mantenimientoBarrilServicio;

    // ==================== filtrarMantenimientosBarril ====================

    @Test
    @DisplayName("CP-FMB-01: filtrarMantenimientosBarril filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        when(mantenimientoBarrilRepository.filtrarMantenimientosBarril(1L, EstadoTransaccion.REGISTRADO, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(mantenimiento)));

        // === EJECUCION ===
        Page<MantenimientoBarrilResponseDTO> resultado = mantenimientoBarrilServicio.filtrarMantenimientosBarril(1L, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(mantenimientoBarrilRepository).filtrarMantenimientosBarril(1L, EstadoTransaccion.REGISTRADO, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FMB-02: filtrarMantenimientosBarril con los 4 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity registrado = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        MantenimientoBarrilEntity anulado = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Mantenimiento cargado por error", barril);
        when(mantenimientoBarrilRepository.filtrarMantenimientosBarril(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado)));

        // === EJECUCION ===
        Page<MantenimientoBarrilResponseDTO> resultado = mantenimientoBarrilServicio.filtrarMantenimientosBarril(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(mantenimientoBarrilRepository).filtrarMantenimientosBarril(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMB-03: filtrarMantenimientosBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        MantenimientoBarrilEntity anulado = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Mantenimiento cargado por error", barril);
        when(mantenimientoBarrilRepository.filtrarMantenimientosBarril(null, EstadoTransaccion.ANULADO, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<MantenimientoBarrilResponseDTO> resultado = mantenimientoBarrilServicio.filtrarMantenimientosBarril(null, EstadoTransaccion.ANULADO, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(mantenimientoBarrilRepository).filtrarMantenimientosBarril(null, EstadoTransaccion.ANULADO, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMB-04: filtrarMantenimientosBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(mantenimientoBarrilRepository.filtrarMantenimientosBarril(99L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<MantenimientoBarrilResponseDTO> resultado = mantenimientoBarrilServicio.filtrarMantenimientosBarril(99L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(mantenimientoBarrilRepository).filtrarMantenimientosBarril(99L, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMB-05: filtrarMantenimientosBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento1 = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        MantenimientoBarrilEntity mantenimiento2 = crearMantenimientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Mantenimiento cargado por error", barril);
        when(mantenimientoBarrilRepository.filtrarMantenimientosBarril(1L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(mantenimiento1, mantenimiento2)));

        // === EJECUCION ===
        Page<MantenimientoBarrilResponseDTO> resultado = mantenimientoBarrilServicio.filtrarMantenimientosBarril(1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(mantenimientoBarrilRepository).filtrarMantenimientosBarril(1L, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del mantenimiento de barril cuando el ID existe")
    void buscarPorId_debeRetornarMantenimientoExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));

        // === EJECUCION ===
        MantenimientoBarrilResponseDTO resultado = mantenimientoBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(mantenimientoBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(mantenimientoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de barril con ID: 99");

        verify(mantenimientoBarrilRepository).findById(99L);
    }

    // ==================== registrarMantenimientoBarril ====================

    @Test
    @DisplayName("CP-RMB-01: registrarMantenimientoBarril lanza ReglaNegocioException con campo fecha cuando la fecha de mantenimiento es nula")
    void registrar_debeRechazarFechaNula() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, null, "Se reemplazó la válvula de presión");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de mantenimiento es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RMB-02: registrarMantenimientoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null);

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RMB-03: registrarMantenimientoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "   ");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RMB-04: registrarMantenimientoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "a".repeat(256));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RMB-05: registrarMantenimientoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-06: registrarMantenimientoBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo EN_MANTENIMIENTO")
    void registrar_debeRechazarBarrilEnEstadoDeOrigenIncorrecto() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un mantenimiento sobre un barril en estado operativo En mantenimiento")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-07: registrarMantenimientoBarril registra el mantenimiento y pasa el barril a DISPONIBLE (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, fecha, "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(mantenimientoBarrilRepository.save(any(MantenimientoBarrilEntity.class))).thenAnswer(invocation -> {
            MantenimientoBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        MantenimientoBarrilResponseDTO resultado = mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<MantenimientoBarrilEntity> captor = ArgumentCaptor.forClass(MantenimientoBarrilEntity.class);
        verify(mantenimientoBarrilRepository).save(captor.capture());
        MantenimientoBarrilEntity guardada = captor.getValue();
        assertThat(guardada.getFecha()).isEqualTo(fecha);
        assertThat(guardada.getObservaciones()).isEqualTo("Se reemplazó la válvula de presión");
        assertThat(guardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(guardada.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RMB-08: registrarMantenimientoBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-09: registrarMantenimientoBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-10: registrarMantenimientoBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la del último envasado")
    void registrar_debeRechazarFechaIgualALaDelUltimoEnvasado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, fecha, "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fecha));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-11: registrarMantenimientoBarril lanza ReglaNegocioException con campo fecha cuando la fecha de mantenimiento es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaFutura() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), "Se reemplazó la válvula de presión");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RMB-12: registrarMantenimientoBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.save(any(MantenimientoBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
    }

    // ==================== anularMantenimientoBarril ====================

    @Test
    @DisplayName("CP-AMB-01: anularMantenimientoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(mantenimientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AMB-02: anularMantenimientoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(mantenimientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AMB-03: anularMantenimientoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(mantenimientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AMB-04: anularMantenimientoBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando el mantenimiento no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-05: anularMantenimientoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(mantenimientoBarrilRepository, never()).findById(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-06: anularMantenimientoBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de barril con ID: 7");

        // El ID 7 es de otra operación del barril (por ejemplo una falla): no se lee la última operación ni el envasado
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-07: anularMantenimientoBarril lanza ReglaNegocioException cuando el mantenimiento no se encuentra en estado REGISTRADO")
    void anular_debeRechazarNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular mantenimientos de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-08: anularMantenimientoBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-09: anularMantenimientoBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-10: anularMantenimientoBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior a el mantenimiento")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(operacion.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-11: anularMantenimientoBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la de el mantenimiento")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFecha() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-12: anularMantenimientoBarril lanza ReglaNegocioException por el uso, sin consultar el último envasado, cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-13: anularMantenimientoBarril lanza ReglaNegocioException cuando el barril asociado no se encuentra en estado operativo DISPONIBLE")
    void anular_debeRechazarBarrilFueraDelEstadoEsperado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un mantenimiento cuyo barril asociado se encuentre en estado operativo Disponible");

        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-14: anularMantenimientoBarril anula el mantenimiento y restablece el barril a EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(mantenimientoBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        MantenimientoBarrilResponseDTO resultado = mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        assertThat(operacion.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(operacion.getFechaAnulacion()).isNotNull();
        assertThat(operacion.getMotivoAnulacion()).isEqualTo("Mantenimiento cargado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(mantenimientoBarrilRepository).save(operacion);
    }

    @Test
    @DisplayName("CP-AMB-15: anularMantenimientoBarril anula el mantenimiento cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 15, 9, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(mantenimientoBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        MantenimientoBarrilResponseDTO resultado = mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-AMB-16: anularMantenimientoBarril valida en orden: bloqueo del barril, lectura de el mantenimiento, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(mantenimientoBarrilRepository.save(operacion)).thenReturn(operacion);

        // === EJECUCION ===
        mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, mantenimientoBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(mantenimientoBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-AMB-17: anularMantenimientoBarril lanza IllegalStateException sin modificar nada cuando no se encuentra ninguna operación registrada del barril")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity operacion = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        stubBarrilBloqueadoYOperacion(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private void stubBarrilBloqueadoYOperacion(BarrilEntity barril, MantenimientoBarrilEntity operacion) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(operacion.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.findById(operacion.getId())).thenReturn(Optional.of(operacion));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idOperacion, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idOperacion));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

    private static MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO(Long idBarril, LocalDateTime fecha, String observaciones) {
        return MantenimientoBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fecha(fecha)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionMantenimientoBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionMantenimientoBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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

    private static MantenimientoBarrilEntity crearMantenimientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fecha, String observaciones, BarrilEntity barril) {
        return MantenimientoBarrilEntity.builder()
                .id(id)
                .fecha(fecha)
                .observaciones(observaciones)
                .estado(estado)
                .barril(barril)
                .build();
    }
}
