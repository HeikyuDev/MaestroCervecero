package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
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
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FraccionamientoBarrilResponseDTO;
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
class FraccionamientoBarrilServicioImplTest {

    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
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
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private FraccionamientoBarrilServicioImpl fraccionamientoBarrilServicio;

    // ==================== filtrarFraccionamientosBarril ====================

    @Test
    @DisplayName("CP-FFRB-01: filtrarFraccionamientosBarril filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        when(fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(fraccionamiento)));

        // === EJECUCION ===
        Page<FraccionamientoBarrilResponseDTO> resultado = fraccionamientoBarrilServicio.filtrarFraccionamientosBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(fraccionamientoBarrilRepository).filtrarFraccionamientosBarril(EstadoTransaccion.REGISTRADO, 1L, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FFRB-02: filtrarFraccionamientosBarril con los 4 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCuatroParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        FraccionamientoBarrilEntity registrado = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        FraccionamientoBarrilEntity anulado = crearFraccionamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Fraccionamiento cargado por error", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        when(fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado)));

        // === EJECUCION ===
        Page<FraccionamientoBarrilResponseDTO> resultado = fraccionamientoBarrilServicio.filtrarFraccionamientosBarril(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fraccionamientoBarrilRepository).filtrarFraccionamientosBarril(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFRB-03: filtrarFraccionamientosBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        FraccionamientoBarrilEntity anulado = crearFraccionamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Fraccionamiento cargado por error", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        when(fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(EstadoTransaccion.ANULADO, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<FraccionamientoBarrilResponseDTO> resultado = fraccionamientoBarrilServicio.filtrarFraccionamientosBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(fraccionamientoBarrilRepository).filtrarFraccionamientosBarril(EstadoTransaccion.ANULADO, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFRB-04: filtrarFraccionamientosBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(null, 99L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<FraccionamientoBarrilResponseDTO> resultado = fraccionamientoBarrilServicio.filtrarFraccionamientosBarril(null, 99L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fraccionamientoBarrilRepository).filtrarFraccionamientosBarril(null, 99L, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFRB-05: filtrarFraccionamientosBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        FraccionamientoBarrilEntity fraccionamiento1 = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        FraccionamientoBarrilEntity fraccionamiento2 = crearFraccionamientoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Fraccionamiento cargado por error", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        when(fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(fraccionamiento1, fraccionamiento2)));

        // === EJECUCION ===
        Page<FraccionamientoBarrilResponseDTO> resultado = fraccionamientoBarrilServicio.filtrarFraccionamientosBarril(null, 1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(fraccionamientoBarrilRepository).filtrarFraccionamientosBarril(null, 1L, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del fraccionamiento de barril cuando el ID existe")
    void buscarPorId_debeRetornarFraccionamientoExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(fraccionamientoBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(fraccionamientoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fraccionamiento de barril con ID: 99");

        verify(fraccionamientoBarrilRepository).findById(99L);
    }

    // ==================== registrarFraccionamientoBarril ====================

    @Test
    @DisplayName("CP-RFRB-01: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha es nula")
    void registrar_debeRechazarFechaNula() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, null, 5.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha es obligatoria");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFRB-02: registrarFraccionamientoBarril lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, null);

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFRB-03: registrarFraccionamientoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "   ");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFRB-04: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la cantidad a extraer es nula")
    void registrar_debeRechazarCantidadNula() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a cero");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFRB-05: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la cantidad a extraer es igual a cero")
    void registrar_debeRechazarCantidadIgualACero() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a cero");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RFRB-06: registrarFraccionamientoBarril lanza RecursoNoEncontradoException cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-07: registrarFraccionamientoBarril lanza ReglaNegocioException cuando el barril no está en estado operativo CON_CERVEZA")
    void registrar_debeRechazarBarrilNoConCerveza() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un fraccionamiento sobre un barril en estado operativo CON_CERVEZA");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-08: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la cantidad a extraer supera el contenido actual del barril")
    void registrar_debeRechazarCantidadQueSuperaElContenidoActual() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 15.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 10.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer no puede superar el contenido actual del barril");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-09: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha no es posterior a un fraccionamiento anterior del mismo barril")
    void registrar_debeRechazarSiFechaNoEsPosteriorAFraccionamientoAnterior() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-10: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha no es posterior a un despacho anterior del mismo barril")
    void registrar_debeRechazarSiFechaNoEsPosteriorADespachoAnterior() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-11: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha no es posterior al último envasado del barril")
    void registrar_debeRechazarSiFechaNoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-12: registrarFraccionamientoBarril vacía por completo el barril y lo deja EN_LIMPIEZA (camino feliz)")
    void registrar_debeDejarEnLimpiezaCuandoVaciaPorCompleto() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Embotellado del remanente");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 5.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(any(FraccionamientoBarrilEntity.class))).thenAnswer(invocation -> {
            FraccionamientoBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<FraccionamientoBarrilEntity> captor = ArgumentCaptor.forClass(FraccionamientoBarrilEntity.class);
        verify(fraccionamientoBarrilRepository).save(captor.capture());
        FraccionamientoBarrilEntity fraccionamientoGuardado = captor.getValue();
        assertThat(fraccionamientoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(fraccionamientoGuardado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);

        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-RFRB-13: registrarFraccionamientoBarril deja contenido restante en el barril y lo mantiene CON_CERVEZA (camino feliz)")
    void registrar_debeMantenerConCervezaCuandoQuedaContenidoRestante() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Toma de muestra para testeo");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(any(FraccionamientoBarrilEntity.class))).thenAnswer(invocation -> {
            FraccionamientoBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(15.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(resultado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-14: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaFutura() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), 5.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository);
    }

    // ==================== anularFraccionamientoBarril ====================

    @Test
    @DisplayName("CP-AFRB-01: anularFraccionamientoBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fraccionamientoBarrilRepository, barrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository);
    }

    @Test
    @DisplayName("CP-AFRB-02: anularFraccionamientoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fraccionamientoBarrilRepository, barrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository);
    }

    @Test
    @DisplayName("CP-AFRB-03: anularFraccionamientoBarril lanza RecursoNoEncontradoException cuando el fraccionamiento no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fraccionamiento de barril con ID: 99");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-04: anularFraccionamientoBarril lanza ReglaNegocioException cuando el fraccionamiento no se encuentra en estado REGISTRADO")
    void anular_debeRechazarFraccionamientoNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fraccionamientos de barril en estado REGISTRADO");

        verifyNoInteractions(barrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, despachoBarrilRepository, devolucionBarrilRepository);
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-05: anularFraccionamientoBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior del mismo tipo sobre el barril")
    void anular_debeRechazarSiExisteFraccionamientoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-06: anularFraccionamientoBarril lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el barril")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-07: anularFraccionamientoBarril lanza RecursoNoEncontradoException cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-08: anularFraccionamientoBarril lanza ReglaNegocioException cuando el barril asociado no está en el estado operativo que dejó el fraccionamiento")
    void anular_debeRechazarBarrilFueraDelEstadoResultante() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA, 0.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un fraccionamiento cuyo barril asociado se encuentre en estado operativo CON_CERVEZA");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-09: anularFraccionamientoBarril revierte un fraccionamiento que dejó el barril CON_CERVEZA (camino feliz)")
    void anular_debeRevertirCuandoDejoConCerveza() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Toma de muestra para testeo", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(20.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(fraccionamiento.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(fraccionamiento.getFechaAnulacion()).isNotNull();
        assertThat(fraccionamiento.getMotivoAnulacion()).isEqualTo("Fraccionamiento cargado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(fraccionamientoBarrilRepository).save(fraccionamiento);
    }

    @Test
    @DisplayName("CP-AFRB-10: anularFraccionamientoBarril revierte un fraccionamiento que dejó el barril EN_LIMPIEZA (camino feliz)")
    void anular_debeRevertirCuandoDejoEnLimpieza() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA, 0.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Embotellado del remanente", 5.0, EstadoOperativoBarril.EN_LIMPIEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(fraccionamientoBarrilRepository.findById(1L)).thenReturn(Optional.of(fraccionamiento));
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(5.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(fraccionamiento.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    // ==================== helpers de construcción ====================

    private static FraccionamientoBarrilFormDTO fraccionamientoBarrilFormDTO(Long idBarril, LocalDateTime fecha, Double cantidadExtraida, String observaciones) {
        return FraccionamientoBarrilFormDTO.builder()
                .idBarril(idBarril)
                .fecha(fecha)
                .cantidadExtraida(cantidadExtraida)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionFraccionamientoBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionFraccionamientoBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, EstadoOperativoBarril estadoOperativo, Double contenidoActual) {
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
                .contenidoActual(contenidoActual)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(50)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static FraccionamientoBarrilEntity crearFraccionamientoEntity(Long id, EstadoTransaccion estado, LocalDateTime fecha, String observaciones, Double cantidadFraccionada, EstadoOperativoBarril estadoOperativoResultante, BarrilEntity barril) {
        return FraccionamientoBarrilEntity.builder()
                .id(id)
                .fecha(fecha)
                .observaciones(observaciones)
                .estado(estado)
                .cantidadFraccionada(cantidadFraccionada)
                .estadoOperativoResultante(estadoOperativoResultante)
                .barril(barril)
                .build();
    }
}
