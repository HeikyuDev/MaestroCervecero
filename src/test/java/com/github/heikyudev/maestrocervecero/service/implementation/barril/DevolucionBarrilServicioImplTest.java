package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
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
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;
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
class DevolucionBarrilServicioImplTest {

    @Mock
    private IDevolucionBarrilRepository devolucionBarrilRepository;
    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
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
    @DisplayName("CP-RDVB-01: registrarDevolucionBarril lanza ReglaNegocioException cuando la fecha de devolución es nula")
    void registrar_debeRechazarFechaDevolucionNula() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, null, "Barril recibido en depósito");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de devolución es obligatoria");

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDVB-02: registrarDevolucionBarril lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), null);

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDVB-03: registrarDevolucionBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "   ");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDVB-04: registrarDevolucionBarril lanza RecursoNoEncontradoException cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(99L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-05: registrarDevolucionBarril lanza ReglaNegocioException cuando el barril no está en estado operativo DESPACHADO")
    void registrar_debeRechazarBarrilNoDespachado() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una devolución sobre un barril en estado operativo DESPACHADO");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-06: registrarDevolucionBarril lanza ReglaNegocioException cuando existe una devolución posterior del mismo barril")
    void registrar_debeRechazarSiExisteDevolucionPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-07: registrarDevolucionBarril lanza ReglaNegocioException cuando existe un despacho posterior del mismo barril")
    void registrar_debeRechazarSiExisteDespachoPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-08: registrarDevolucionBarril lanza ReglaNegocioException cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDVB-10: registrarDevolucionBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior del mismo barril")
    void registrar_debeRechazarSiExisteFraccionamientoPosterior() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

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
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.empty());
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
        verify(barrilRepository).save(barril);

        ArgumentCaptor<DevolucionBarrilEntity> captor = ArgumentCaptor.forClass(DevolucionBarrilEntity.class);
        verify(devolucionBarrilRepository).save(captor.capture());
        DevolucionBarrilEntity devolucionGuardada = captor.getValue();
        assertThat(devolucionGuardada.getFecha()).isEqualTo(fechaDevolucion);
        assertThat(devolucionGuardada.getObservaciones()).isEqualTo("Barril recibido en depósito, sin daños visibles");
        assertThat(devolucionGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(devolucionGuardada.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RDVB-11: registrarDevolucionBarril lanza ReglaNegocioException cuando la fecha de devolución es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaDevolucionFutura() {
        DevolucionBarrilFormDTO formDTO = devolucionBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), "Barril recibido en depósito");

        assertThatThrownBy(() -> devolucionBarrilServicio.registrarDevolucionBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual");

        verifyNoInteractions(barrilRepository, devolucionBarrilRepository);
    }

    // ==================== anularDevolucionBarril ====================

    @Test
    @DisplayName("CP-ADVB-01: anularDevolucionBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(devolucionBarrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-ADVB-02: anularDevolucionBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(devolucionBarrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-ADVB-03: anularDevolucionBarril lanza RecursoNoEncontradoException cuando la devolución no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la devolución de barril con ID: 99");

        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-04: anularDevolucionBarril lanza ReglaNegocioException cuando la devolución no se encuentra en estado REGISTRADO")
    void anular_debeRechazarDevolucionNoRegistrada() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular devoluciones de barril en estado REGISTRADO");

        verifyNoInteractions(fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-05: anularDevolucionBarril lanza ReglaNegocioException cuando existe una devolución posterior del mismo tipo sobre el barril")
    void anular_debeRechazarSiExisteDevolucionPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-06: anularDevolucionBarril lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el barril")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-07: anularDevolucionBarril lanza RecursoNoEncontradoException cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2");

        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-08: anularDevolucionBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo EN_LIMPIEZA")
    void anular_debeRechazarBarrilNoEnLimpieza() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una devolución cuyo barril asociado se encuentre en estado operativo EN_LIMPIEZA");

        verify(barrilRepository, never()).save(any());
        verify(devolucionBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADVB-09: anularDevolucionBarril anula la devolución y restablece el barril a DESPACHADO (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(devolucionBarrilRepository.save(devolucion)).thenReturn(devolucion);

        // === EJECUCION ===
        DevolucionBarrilResponseDTO resultado = devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
        assertThat(devolucion.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(devolucion.getFechaAnulacion()).isNotNull();
        assertThat(devolucion.getMotivoAnulacion()).isEqualTo("Devolución cargada por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(devolucionBarrilRepository).save(devolucion);
    }

    @Test
    @DisplayName("CP-ADVB-10: anularDevolucionBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior sobre el barril")
    void anular_debeRechazarSiExisteFraccionamientoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA);
        DevolucionBarrilEntity devolucion = crearDevolucionEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 2, 5, 10, 0), "Barril recibido en depósito", barril);
        AnulacionDevolucionBarrilFormDTO formDTO = anulacionFormDTO("Devolución cargada por error");
        when(devolucionBarrilRepository.findById(1L)).thenReturn(Optional.of(devolucion));
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 2, 6, 10, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> devolucionBarrilServicio.anularDevolucionBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(devolucionBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static DevolucionBarrilFormDTO devolucionBarrilFormDTO(Long idBarril, LocalDateTime fechaDevolucion, String observaciones) {
        return DevolucionBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fechaDevolucion(fechaDevolucion)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionDevolucionBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionDevolucionBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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
