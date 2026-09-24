package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionMantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.MantenimientoBarrilResponseDTO;
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
class MantenimientoBarrilServicioImplTest {

    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
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
    @DisplayName("CP-RMB-01: registrarMantenimientoBarril lanza ReglaNegocioException cuando la fecha de mantenimiento es nula")
    void registrar_debeRechazarFechaMantenimientoNula() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, null, "Se reemplazó la válvula de presión");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de mantenimiento es obligatoria");

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RMB-02: registrarMantenimientoBarril lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null);

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RMB-03: registrarMantenimientoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "   ");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RMB-04: registrarMantenimientoBarril lanza RecursoNoEncontradoException cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-05: registrarMantenimientoBarril lanza ReglaNegocioException cuando el barril no está en estado operativo EN_MANTENIMIENTO")
    void registrar_debeRechazarBarrilNoEnMantenimiento() {
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.registrarMantenimientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un mantenimiento sobre un barril en estado operativo EN_MANTENIMIENTO");

        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RMB-06: registrarMantenimientoBarril registra el mantenimiento y pasa el barril a DISPONIBLE (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaMantenimiento = LocalDateTime.of(2026, 1, 20, 9, 0);
        MantenimientoBarrilFormDTO formDTO = mantenimientoBarrilFormDTO(1L, fechaMantenimiento, "Se reemplazó la válvula de presión y se verificó el sellado");
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
        MantenimientoBarrilEntity mantenimientoGuardado = captor.getValue();
        assertThat(mantenimientoGuardado.getFecha()).isEqualTo(fechaMantenimiento);
        assertThat(mantenimientoGuardado.getObservaciones()).isEqualTo("Se reemplazó la válvula de presión y se verificó el sellado");
        assertThat(mantenimientoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(mantenimientoGuardado.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularMantenimientoBarril ====================

    @Test
    @DisplayName("CP-AMB-01: anularMantenimientoBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(mantenimientoBarrilRepository, fallaBarrilRepository, limpiezaBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AMB-02: anularMantenimientoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(mantenimientoBarrilRepository, fallaBarrilRepository, limpiezaBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AMB-03: anularMantenimientoBarril lanza RecursoNoEncontradoException cuando el mantenimiento no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el mantenimiento de barril con ID: 99");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-04: anularMantenimientoBarril lanza ReglaNegocioException cuando el mantenimiento no se encuentra en estado REGISTRADO")
    void anular_debeRechazarMantenimientoNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular mantenimientos de barril en estado REGISTRADO");

        verifyNoInteractions(fallaBarrilRepository, limpiezaBarrilRepository, barrilRepository);
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-05: anularMantenimientoBarril lanza RecursoNoEncontradoException cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2");

        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-06: anularMantenimientoBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo DISPONIBLE")
    void anular_debeRechazarBarrilNoDisponible() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un mantenimiento cuyo barril asociado se encuentre en estado operativo DISPONIBLE");

        verify(barrilRepository, never()).save(any());
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-07: anularMantenimientoBarril anula el mantenimiento y restablece el barril a EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(mantenimientoBarrilRepository.save(mantenimiento)).thenReturn(mantenimiento);

        // === EJECUCION ===
        MantenimientoBarrilResponseDTO resultado = mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        assertThat(mantenimiento.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(mantenimiento.getFechaAnulacion()).isNotNull();
        assertThat(mantenimiento.getMotivoAnulacion()).isEqualTo("Mantenimiento cargado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(mantenimientoBarrilRepository).save(mantenimiento);
    }

    @Test
    @DisplayName("CP-AMB-08: anularMantenimientoBarril lanza ReglaNegocioException cuando existe un mantenimiento posterior del mismo tipo sobre el barril")
    void anular_debeRechazarSiExisteMantenimientoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMB-09: anularMantenimientoBarril lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el barril")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE);
        MantenimientoBarrilEntity mantenimiento = crearMantenimientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Se reemplazó la válvula de presión", barril);
        AnulacionMantenimientoBarrilFormDTO formDTO = anulacionFormDTO("Mantenimiento cargado por error");
        when(mantenimientoBarrilRepository.findById(1L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> mantenimientoBarrilServicio.anularMantenimientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(mantenimientoBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO(Long idBarril, LocalDateTime fechaMantenimiento, String observaciones) {
        return MantenimientoBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fechaMantenimiento(fechaMantenimiento)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionMantenimientoBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionMantenimientoBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
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

    private static MantenimientoBarrilEntity crearMantenimientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaMantenimiento, String observaciones, BarrilEntity barril) {
        return MantenimientoBarrilEntity.builder()
                .id(id)
                .fecha(fechaMantenimiento)
                .observaciones(observaciones)
                .estado(estado)
                .barril(barril)
                .build();
    }
}
