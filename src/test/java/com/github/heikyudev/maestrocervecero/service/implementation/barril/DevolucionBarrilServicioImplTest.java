package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;
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
class DevolucionBarrilServicioImplTest {

    @Mock
    private IDevolucionBarrilRepository devolucionBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private DevolucionBarrilServicioImpl devolucionBarrilServicio;

    // ==================== filtrarDevolucionesBarril ====================

    @Test
    @DisplayName("CP-FDVB-01: filtrarDevolucionesBarril filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Barril recibido en depósito", barril);
        when(devolucionBarrilRepository.filtrarDevolucionesBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(devolucion)));

        // === EJECUCION ===
        Page<DevolucionBarrilResponseDTO> resultado = devolucionBarrilServicio.filtrarDevolucionesBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(devolucionBarrilRepository).filtrarDevolucionesBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FDVB-02: filtrarDevolucionesBarril con los 4 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity registrada = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Barril recibido en depósito", barril);
        DevolucionBarrilEntity anulada = crearDevolucionEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Devolución cargada por error", barril);
        when(devolucionBarrilRepository.filtrarDevolucionesBarril(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada)));

        // === EJECUCION ===
        Page<DevolucionBarrilResponseDTO> resultado = devolucionBarrilServicio.filtrarDevolucionesBarril(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(devolucionBarrilRepository).filtrarDevolucionesBarril(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDVB-03: filtrarDevolucionesBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        DevolucionBarrilEntity anulada = crearDevolucionEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Devolución cargada por error", barril);
        when(devolucionBarrilRepository.filtrarDevolucionesBarril(EstadoTransaccion.ANULADO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada)));

        // === EJECUCION ===
        Page<DevolucionBarrilResponseDTO> resultado = devolucionBarrilServicio.filtrarDevolucionesBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(devolucionBarrilRepository).filtrarDevolucionesBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDVB-04: filtrarDevolucionesBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(devolucionBarrilRepository.filtrarDevolucionesBarril(null, 99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<DevolucionBarrilResponseDTO> resultado = devolucionBarrilServicio.filtrarDevolucionesBarril(null, 99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(devolucionBarrilRepository).filtrarDevolucionesBarril(null, 99L, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDVB-05: filtrarDevolucionesBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion1 = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Barril recibido en depósito", barril);
        DevolucionBarrilEntity devolucion2 = crearDevolucionEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Devolución cargada por error", barril);
        when(devolucionBarrilRepository.filtrarDevolucionesBarril(null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(devolucion1, devolucion2)));

        // === EJECUCION ===
        Page<DevolucionBarrilResponseDTO> resultado = devolucionBarrilServicio.filtrarDevolucionesBarril(null, 1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(devolucionBarrilRepository).filtrarDevolucionesBarril(null, 1L, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la devolución de barril cuando el ID existe")
    void buscarPorId_debeRetornarDevolucionExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Barril recibido en depósito", barril);
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(devolucionBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(devolucionBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la devolución de barril con ID: 99");

        verify(devolucionBarrilRepository).findById(99L);
    }

    // ==================== registrarDevolucionBarril ====================

    @Test
    @DisplayName("CP-RDVB-01: registrarDevolucionBarril lanza ReglaNegocioException con campo fecha cuando la fecha de devolución es nula")
    void registrar_debeRechazarFechaDevolucionNula() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, null, "Barril recibido en depósito");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha y hora de la operación es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDVB-02: registrarDevolucionBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), null);

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDVB-03: registrarDevolucionBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "   ");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDVB-04: registrarDevolucionBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "a".repeat(256));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDVB-05: registrarDevolucionBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(99L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-06: registrarDevolucionBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo DESPACHADO")
    void registrar_debeRechazarBarrilNoDespachado() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una devolución sobre un barril en estado operativo Despachado")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-07: registrarDevolucionBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-08: registrarDevolucionBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-09: registrarDevolucionBarril registra la devolución y pasa el barril a EN_LIMPIEZA (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaDevolucion = LocalDateTime.of(2026, 2, 5, 10, 0);
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, fechaDevolucion, "Barril recibido en depósito, sin daños visibles");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(devolucionBarrilRepository.save(any(DevolucionBarrilEntity.class))).thenAnswer(invocation -> {
            DevolucionBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.registrarDevolucionBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<DevolucionBarrilEntity> captor = ArgumentCaptor.forClass(DevolucionBarrilEntity.class);
        verify(devolucionBarrilRepository).save(captor.capture());
        DevolucionBarrilEntity devolucionGuardada = captor.getValue();
        assertThat(devolucionGuardada.getFecha()).isEqualTo(fechaDevolucion);
        assertThat(devolucionGuardada.getObservaciones()).isEqualTo("Barril recibido en depósito, sin daños visibles");
        assertThat(devolucionGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(devolucionGuardada.getBarril()).isSameAs(barril);
        assertThat(devolucionGuardada.getLitrosDescartados()).isEqualTo(50.0);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(resultado.getLitrosDescartados()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-RDVB-10: registrarDevolucionBarril guarda 0 litros descartados cuando el barril volvió vacío")
    void registrar_debeGuardarCeroLitrosDescartadosConBarrilVacio() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaDevolucion = LocalDateTime.of(2026, 2, 5, 10, 0);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        barril.setContenidoActual(0.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.save(any(DevolucionBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.registrarDevolucionBarril(devolucionBarrilFormDTO(1L, fechaDevolucion, "Barril recibido vacío"));

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(resultado.getLitrosDescartados()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-RDVB-11: registrarDevolucionBarril lanza ReglaNegocioException con campo fecha cuando la fecha de devolución es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaDevolucionFutura() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), "Barril recibido en depósito");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDVB-12: registrarDevolucionBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.save(any(DevolucionBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        devolucionBarrilServicio.registrarDevolucionBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
    }

    // ==================== anularDevolucionBarril ====================

    @Test
    @DisplayName("CP-ADVB-01: anularDevolucionBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(devolucionBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADVB-02: anularDevolucionBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(devolucionBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADVB-03: anularDevolucionBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(devolucionBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADVB-04: anularDevolucionBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando la devolución no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la devolución de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-05: anularDevolucionBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(devolucionBarrilRepository, never()).findById(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-06: anularDevolucionBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la devolución de barril con ID: 7");

        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-07: anularDevolucionBarril lanza ReglaNegocioException cuando la devolución no se encuentra en estado REGISTRADO")
    void anular_debeRechazarDevolucionNoRegistrada() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular devoluciones de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-08: anularDevolucionBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-09: anularDevolucionBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-10: anularDevolucionBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior a la devolución")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        barril.setContenidoActual(0.0);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        devolucion.setLitrosDescartados(35.0);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 10, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(devolucion.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-11: anularDevolucionBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la de la devolución")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFechaDeLaDevolucion() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 2, 5, 10, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-12: anularDevolucionBarril lanza ReglaNegocioException por el uso, sin consultar el último envasado, cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verifyNoInteractions(envasadoLoteRepository);

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-13: anularDevolucionBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo EN_LIMPIEZA")
    void anular_debeRechazarBarrilNoEnLimpieza() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una devolución cuyo barril asociado se encuentre en estado operativo En limpieza");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-14: anularDevolucionBarril anula la devolución y restablece el barril a DESPACHADO (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        barril.setContenidoActual(0.0);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        devolucion.setLitrosDescartados(35.0);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(devolucionBarrilRepository.save(devolucion)).thenReturn(devolucion);

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
        assertThat(barril.getContenidoActual()).isEqualTo(35.0);
        assertThat(devolucion.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(devolucion.getFechaAnulacion()).isNotNull();
        assertThat(devolucion.getMotivoAnulacion()).isEqualTo("Devolución cargada por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(devolucionBarrilRepository).save(devolucion);
    }

    @Test
    @DisplayName("CP-ADVB-15: anularDevolucionBarril no altera el contenido del barril cuando la devolución no descartó litros")
    void anular_noDebeAlterarElContenidoSinLitrosDescartados() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        barril.setContenidoActual(12.0);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(devolucionBarrilRepository.save(devolucion)).thenReturn(devolucion);

        // === EJECUCION ===
        devolucionBarrilServicio.anularDevolucionBarril(1L, anulacionFormDTO("Devolución cargada por error"));

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(12.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
    }

    @Test
    @DisplayName("CP-ADVB-16: anularDevolucionBarril anula la devolución cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 1, 10, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(devolucionBarrilRepository.save(devolucion)).thenReturn(devolucion);

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-ADVB-17: anularDevolucionBarril valida en orden: bloqueo del barril, lectura de la devolución, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(devolucionBarrilRepository.save(devolucion)).thenReturn(devolucion);

        // === EJECUCION ===
        devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, devolucionBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(devolucionBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-ADVB-18: anularDevolucionBarril lanza IllegalStateException sin modificar nada cuando el barril no tiene ninguna operación registrada")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        stubBarrilBloqueadoYDevolucion(barril, devolucion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private void stubBarrilBloqueadoYDevolucion(BarrilEntity barril, DevolucionBarrilEntity devolucion) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(devolucion.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.findById(devolucion.getId())).thenReturn(Optional.of(devolucion));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idDevolucion, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idDevolucion));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

    private static DevolucionBarrilFormDTO devolucionBarrilFormDTO(Long idBarril, LocalDateTime fecha, String observaciones) {
        return DevolucionBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fecha(fecha)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionDevolucionBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionDevolucionBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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
                .contenidoActual(50.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static DevolucionBarrilEntity crearDevolucionEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaDevolucion, String observaciones, BarrilEntity barril) {
        return DevolucionBarrilEntity.builder()
                .id(id)
                .fecha(fechaDevolucion)
                .observaciones(observaciones)
                .estado(estado)
                .barril(barril)
                .build();
    }
}
