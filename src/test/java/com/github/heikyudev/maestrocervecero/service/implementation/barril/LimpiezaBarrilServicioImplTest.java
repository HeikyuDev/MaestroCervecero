package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionLimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.LimpiezaBarrilResponseDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimpiezaBarrilServicioImplTest {

    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private LimpiezaBarrilServicioImpl limpiezaBarrilServicio;

    // ==================== filtrarLimpiezasBarril ====================

    @Test
    @DisplayName("CP-FLB-01: filtrarLimpiezasBarril filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        when(limpiezaBarrilRepository.filtrarLimpiezasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(limpieza)));

        // === EJECUCION ===
        Page<LimpiezaBarrilResponseDTO> resultado = limpiezaBarrilServicio.filtrarLimpiezasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(limpiezaBarrilRepository).filtrarLimpiezasBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FLB-02: filtrarLimpiezasBarril con los 4 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity registrada = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        LimpiezaBarrilEntity anulada = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Limpieza cargada por error", EstadoOperativoBarril.DISPONIBLE, barril);
        when(limpiezaBarrilRepository.filtrarLimpiezasBarril(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada)));

        // === EJECUCION ===
        Page<LimpiezaBarrilResponseDTO> resultado = limpiezaBarrilServicio.filtrarLimpiezasBarril(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(limpiezaBarrilRepository).filtrarLimpiezasBarril(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLB-03: filtrarLimpiezasBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        LimpiezaBarrilEntity anulada = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Limpieza cargada por error", EstadoOperativoBarril.DISPONIBLE, barril);
        when(limpiezaBarrilRepository.filtrarLimpiezasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada)));

        // === EJECUCION ===
        Page<LimpiezaBarrilResponseDTO> resultado = limpiezaBarrilServicio.filtrarLimpiezasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(limpiezaBarrilRepository).filtrarLimpiezasBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLB-04: filtrarLimpiezasBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(limpiezaBarrilRepository.filtrarLimpiezasBarril(null, 99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<LimpiezaBarrilResponseDTO> resultado = limpiezaBarrilServicio.filtrarLimpiezasBarril(null, 99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(limpiezaBarrilRepository).filtrarLimpiezasBarril(null, 99L, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLB-05: filtrarLimpiezasBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza1 = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        LimpiezaBarrilEntity limpieza2 = crearLimpiezaEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Limpieza cargada por error", EstadoOperativoBarril.DISPONIBLE, barril);
        when(limpiezaBarrilRepository.filtrarLimpiezasBarril(null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(limpieza1, limpieza2)));

        // === EJECUCION ===
        Page<LimpiezaBarrilResponseDTO> resultado = limpiezaBarrilServicio.filtrarLimpiezasBarril(null, 1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(limpiezaBarrilRepository).filtrarLimpiezasBarril(null, 1L, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la limpieza de barril cuando el ID existe")
    void buscarPorId_debeRetornarLimpiezaExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        when(limpiezaBarrilRepository.findById(1L)).thenReturn(Optional.of(limpieza));

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(limpiezaBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(limpiezaBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de barril con ID: 99");

        verify(limpiezaBarrilRepository).findById(99L);
    }

    // ==================== registrarLimpiezaBarril ====================

    @Test
    @DisplayName("CP-RLB-01: registrarLimpiezaBarril lanza ReglaNegocioException con campo fecha cuando la fecha de limpieza es nula")
    void registrar_debeRechazarFechaLimpiezaNula() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, null, "Limpieza CIP estándar");

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha y hora de la operación es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RLB-02: registrarLimpiezaBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null);

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RLB-03: registrarLimpiezaBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "   ");

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RLB-04: registrarLimpiezaBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "a".repeat(256));

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RLB-05: registrarLimpiezaBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLB-06: registrarLimpiezaBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo EN_LIMPIEZA")
    void registrar_debeRechazarBarrilNoEnLimpieza() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una limpieza sobre un barril en estado operativo En limpieza")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(mantenimientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLB-07: registrarLimpiezaBarril deja el barril DISPONIBLE cuando la cantidad de usos está por debajo del máximo, sin mantenimiento previo (camino feliz)")
    void registrar_debeDejarDisponibleCuandoUsosPorDebajoDelMaximo() {
        // === PREPARACION DE DATOS ===
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(eq(1L), isNull())).thenReturn(10L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(any(LimpiezaBarrilEntity.class))).thenAnswer(invocation -> {
            LimpiezaBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<LimpiezaBarrilEntity> captor = ArgumentCaptor.forClass(LimpiezaBarrilEntity.class);
        verify(limpiezaBarrilRepository).save(captor.capture());
        LimpiezaBarrilEntity limpiezaGuardada = captor.getValue();
        assertThat(limpiezaGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(limpiezaGuardada.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);

        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-RLB-08: registrarLimpiezaBarril deja el barril EN_MANTENIMIENTO cuando la cantidad de usos alcanza el máximo, sin mantenimiento previo (límite, camino feliz)")
    void registrar_debeDejarEnMantenimientoCuandoUsosAlcanzanElMaximo() {
        // === PREPARACION DE DATOS ===
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(eq(1L), isNull())).thenReturn(49L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(any(LimpiezaBarrilEntity.class))).thenAnswer(invocation -> {
            LimpiezaBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.EN_MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-RLB-09: registrarLimpiezaBarril acota el conteo a partir del último mantenimiento registrado (camino feliz)")
    void registrar_debeAcotarConteoAlUltimoMantenimiento() {
        // === PREPARACION DE DATOS ===
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        LocalDateTime fechaUltimoMantenimiento = LocalDateTime.of(2026, 1, 10, 0, 0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.of(fechaUltimoMantenimiento));
        when(limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(1L, fechaUltimoMantenimiento)).thenReturn(5L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(any(LimpiezaBarrilEntity.class))).thenAnswer(invocation -> {
            LimpiezaBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO);

        // === ASSERTS ===
        verify(limpiezaBarrilRepository).contarLimpiezasRegistradasDesde(1L, fechaUltimoMantenimiento);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-RLB-10: registrarLimpiezaBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLB-11: registrarLimpiezaBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 21, 9, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLB-12: registrarLimpiezaBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la del último envasado")
    void registrar_debeRechazarFechaIgualALaDelUltimoEnvasado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, fecha, "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fecha));

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada sobre este barril");

        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RLB-13: registrarLimpiezaBarril lanza ReglaNegocioException con campo fecha cuando la fecha de limpieza es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaLimpiezaFutura() {
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), "Limpieza CIP estándar");

        assertThatThrownBy(() -> limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, limpiezaBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RLB-14: registrarLimpiezaBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        LimpiezaBarrilFormDTO formDTO = limpiezaBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.EN_LIMPIEZA, 50);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(limpiezaBarrilRepository.save(any(LimpiezaBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        limpiezaBarrilServicio.registrarLimpiezaBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
    }

    // ==================== anularLimpiezaBarril ====================

    @Test
    @DisplayName("CP-ALB-01: anularLimpiezaBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(limpiezaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ALB-02: anularLimpiezaBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(limpiezaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ALB-03: anularLimpiezaBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(limpiezaBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ALB-04: anularLimpiezaBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando la limpieza no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-05: anularLimpiezaBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(limpiezaBarrilRepository, never()).findById(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-06: anularLimpiezaBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(limpiezaBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la limpieza de barril con ID: 7");

        // el ID existe en la raíz de operaciones pero es de otro tipo: no se valida nada más
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-07: anularLimpiezaBarril lanza ReglaNegocioException cuando la limpieza no se encuentra en estado REGISTRADO")
    void anular_debeRechazarLimpiezaNoRegistrada() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);

        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular limpiezas de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-08: anularLimpiezaBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-09: anularLimpiezaBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(limpiezaBarrilRepository.findById(7L)).thenReturn(Optional.of(limpieza));
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-10: anularLimpiezaBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior a la limpieza")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(limpieza.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-11: anularLimpiezaBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la de la limpieza")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFechaDeLaLimpieza() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-12: anularLimpiezaBarril lanza ReglaNegocioException por el uso cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verify(envasadoLoteRepository, never()).buscarFechaUltimoEnvasadoRegistrado(any());
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-13: anularLimpiezaBarril lanza ReglaNegocioException cuando el barril asociado no está en el estado operativo que dejó la limpieza")
    void anular_debeRechazarBarrilFueraDelEstadoResultante() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular una limpieza cuyo barril asociado se encuentre en estado operativo Disponible");

        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ALB-14: anularLimpiezaBarril anula la limpieza cuando dejó el barril DISPONIBLE (camino feliz)")
    void anular_debeAnularCuandoDejoDisponible() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(limpieza.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(limpieza.getFechaAnulacion()).isNotNull();
        assertThat(limpieza.getMotivoAnulacion()).isEqualTo("Limpieza cargada por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(limpiezaBarrilRepository).save(limpieza);
    }

    @Test
    @DisplayName("CP-ALB-15: anularLimpiezaBarril anula la limpieza cuando dejó el barril EN_MANTENIMIENTO (camino feliz)")
    void anular_debeAnularCuandoDejoEnMantenimiento() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_MANTENIMIENTO, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.EN_MANTENIMIENTO, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(limpieza.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-ALB-16: anularLimpiezaBarril anula la limpieza cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 15, 9, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        LimpiezaBarrilResponseDTO resultado = limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-ALB-17: anularLimpiezaBarril valida en orden: bloqueo del barril, lectura de la limpieza, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity limpieza = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, limpieza);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(limpiezaBarrilRepository.save(limpieza)).thenReturn(limpieza);

        // === EJECUCION ===
        limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, limpiezaBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(limpiezaBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-ALB-18: anularLimpiezaBarril lanza IllegalStateException sin modificar nada cuando no hay ninguna operación registrada para el barril")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DISPONIBLE, 50);
        LimpiezaBarrilEntity operacion = crearLimpiezaEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Limpieza CIP estándar", EstadoOperativoBarril.DISPONIBLE, barril);
        AnulacionLimpiezaBarrilFormDTO formDTO = anulacionFormDTO("Limpieza cargada por error");
        stubBarrilBloqueadoYLimpieza(barril, operacion);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> limpiezaBarrilServicio.anularLimpiezaBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(limpiezaBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private void stubBarrilBloqueadoYLimpieza(BarrilEntity barril, LimpiezaBarrilEntity limpieza) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(limpieza.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(limpiezaBarrilRepository.findById(limpieza.getId())).thenReturn(Optional.of(limpieza));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idLimpieza, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idLimpieza));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

    private static LimpiezaBarrilFormDTO limpiezaBarrilFormDTO(Long idBarril, LocalDateTime fecha, String observaciones) {
        return LimpiezaBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fecha(fecha)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionLimpiezaBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionLimpiezaBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, EstadoOperativoBarril estadoOperativo, Integer usosMaximosAntesMantenimiento) {
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
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static LimpiezaBarrilEntity crearLimpiezaEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaLimpieza, String observaciones, EstadoOperativoBarril estadoOperativoResultante, BarrilEntity barril) {
        return LimpiezaBarrilEntity.builder()
                .id(id)
                .fecha(fechaLimpieza)
                .observaciones(observaciones)
                .estado(estado)
                .estadoOperativoResultante(estadoOperativoResultante)
                .barril(barril)
                .build();
    }
}
