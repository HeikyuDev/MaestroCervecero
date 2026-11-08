package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
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
class FraccionamientoBarrilServicioImplTest {

    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
    @DisplayName("CP-RFRB-01: registrarFraccionamientoBarril lanza ReglaNegocioException con campo fecha cuando la fecha es nula")
    void registrar_debeRechazarFechaNula() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, null, 5.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de fraccionamiento es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-02: registrarFraccionamientoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, null);

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-03: registrarFraccionamientoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "   ");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-04: registrarFraccionamientoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "a".repeat(256));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-05: registrarFraccionamientoBarril lanza ReglaNegocioException con campo cantidadExtraida cuando la cantidad a extraer es nula")
    void registrar_debeRechazarCantidadNula() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), null, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a 0")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidadExtraida"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-06: registrarFraccionamientoBarril lanza ReglaNegocioException con campo cantidadExtraida cuando la cantidad a extraer es igual a cero")
    void registrar_debeRechazarCantidadCero() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a 0")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidadExtraida"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-07: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la cantidad a extraer es negativa")
    void registrar_debeRechazarCantidadNegativa() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), -1.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a 0");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-08: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la cantidad a extraer no es un número finito")
    void registrar_debeRechazarCantidadNoFinita() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), Double.NaN, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer debe ser mayor a 0");

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-09: registrarFraccionamientoBarril lanza ReglaNegocioException con campo cantidadExtraida cuando la cantidad a extraer tiene más de 4 decimales")
    void registrar_debeRechazarCantidadConMasDeCuatroDecimales() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 1.00001, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer no puede tener más de 4 decimales")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidadExtraida"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-10: registrarFraccionamientoBarril acepta una cantidad a extraer con exactamente 4 decimales (límite, camino feliz)")
    void registrar_debeAceptarCantidadConCuatroDecimales() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.0001, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 10.0);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(9.9999);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-11: registrarFraccionamientoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(99L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-12: registrarFraccionamientoBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo CON_CERVEZA")
    void registrar_debeRechazarBarrilNoConCerveza() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE, 0.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un fraccionamiento sobre un barril en estado operativo Con cerveza")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-13: registrarFraccionamientoBarril lanza ReglaNegocioException con campo cantidadExtraida cuando la cantidad a extraer supera el contenido actual del barril")
    void registrar_debeRechazarCantidadMayorAlContenido() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 20.0001, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a extraer no puede superar el contenido actual del barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidadExtraida"));

        assertThat(barril.getContenidoActual()).isEqualTo(20.0);
        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-14: registrarFraccionamientoBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-15: registrarFraccionamientoBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 21, 9, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-16: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la del último envasado")
    void registrar_debeRechazarFechaIgualALaDelUltimoEnvasado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, fecha, 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fecha));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-17: registrarFraccionamientoBarril lanza ReglaNegocioException con campo fecha cuando la fecha es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaFutura() {
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.now().plusDays(1), 5.0, "Merma");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-18: registrarFraccionamientoBarril vacía por completo el barril y lo deja EN_LIMPIEZA (camino feliz)")
    void registrar_debeVaciarElBarrilYDejarloEnLimpieza() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, fecha, 20.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<FraccionamientoBarrilEntity> captor = ArgumentCaptor.forClass(FraccionamientoBarrilEntity.class);
        verify(fraccionamientoBarrilRepository).save(captor.capture());
        FraccionamientoBarrilEntity guardado = captor.getValue();
        assertThat(guardado.getCantidadFraccionada()).isEqualTo(20.0);
        assertThat(guardado.getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
        assertThat(guardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(guardado.getBarril()).isSameAs(barril);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RFRB-19: registrarFraccionamientoBarril deja contenido restante en el barril y lo mantiene CON_CERVEZA (camino feliz)")
    void registrar_debeDejarContenidoRestanteYMantenerConCerveza() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(15.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);

        ArgumentCaptor<FraccionamientoBarrilEntity> captor = ArgumentCaptor.forClass(FraccionamientoBarrilEntity.class);
        verify(fraccionamientoBarrilRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoOperativoResultante()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-20: registrarFraccionamientoBarril redondea el contenido restante a 4 decimales: 20 menos 19,9 deja exactamente 0,1 y el barril sigue CON_CERVEZA")
    void registrar_debeRedondearElContenidoRestante() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 19.9, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.1);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-21: registrarFraccionamientoBarril deja el barril EN_LIMPIEZA cuando el resto crudo es un residuo de punto flotante positivo que redondea a cero")
    void registrar_debeVaciarCuandoElRestoEsUnResiduoPositivo() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.3, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 0.1 + 0.2);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-RFRB-22: registrarFraccionamientoBarril acepta y vacía el barril cuando el resto crudo es un residuo de punto flotante negativo que redondea a cero")
    void registrar_debeVaciarCuandoElRestoEsUnResiduoNegativo() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.3, "Merma");
        // 0.7 - 0.4 vale 0.29999999999999993 (menor que 0.3): el resto crudo es negativo (aprox. -5.5e-17) y redondea a 0
        double contenido = 0.7 - 0.4;
        assertThat(contenido).isLessThan(0.3);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, contenido);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-RFRB-23: registrarFraccionamientoBarril deja el barril EN_LIMPIEZA cuando queda un resto de 1e-10 que redondea a cero")
    void registrar_debeVaciarCuandoElRestoEsDeUnaDiezMilMillonesima() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 5.0000000001);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-RFRB-24: registrarFraccionamientoBarril vacía el barril cuando la cantidad con decimales iguala al contenido (20,3 menos 20,3)")
    void registrar_debeVaciarCuandoLaCantidadConDecimalesIgualaAlContenido() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 20.3, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.3);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-RFRB-25: registrarFraccionamientoBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
        orden.verify(envasadoLoteRepository).buscarFechaFinalizacionDelLoteDelUltimoEnvasadoRegistrado(1L);
    }

    @Test
    @DisplayName("CP-RFRB-26: registrarFraccionamientoBarril extrae 0,1 de 0,3 y deja exactamente 0,2 (el resto crudo es 0,19999999999999998) con el barril CON_CERVEZA")
    void registrar_debeRedondearElRestoDeUnaExtraccionDeUnDecimo() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.1, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 0.3);
        assertThat(0.3 - 0.1).isNotEqualTo(0.2);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.2);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-27: registrarFraccionamientoBarril extrae 0,2 de un contenido de 0,1 más 0,2 y deja exactamente 0,1 con el barril CON_CERVEZA")
    void registrar_debeRedondearElRestoDeUnContenidoConResiduo() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.2, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 0.1 + 0.2);
        assertThat(barril.getContenidoActual() - 0.2).isNotEqualTo(0.1);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.1);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-28: registrarFraccionamientoBarril acepta una extracción de 3 decimales y deja el resto redondeado (0,3 menos 0,125 es 0,175)")
    void registrar_debeAceptarExtraccionDeTresDecimalesConResiduo() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 0.125, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 0.3);
        stubRegistroPermitido(barril);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.175);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
    }

    @Test
    @DisplayName("CP-RFRB-29: registrarFraccionamientoBarril lanza ReglaNegocioException con campo cantidadExtraida cuando la cantidad es NaN o infinita (positiva o negativa)")
    void registrar_debeRechazarNaNEInfinitosConCampo() {
        for (Double invalida : new Double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), invalida, "Merma");

            assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cantidad a extraer debe ser mayor a 0")
                    .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("cantidadExtraida"));
        }

        verifyNoInteractions(barrilRepository, fraccionamientoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RFRB-30: registrarFraccionamientoBarril lanza ReglaNegocioException con campo fecha cuando la fecha es anterior a la finalización del lote del último envasado")
    void registrar_debeRechazarFechaAnteriorALaFinalizacionDelLote() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 8, 0), 5.0, "Merma");
        stubRegistroConLoteFinalizado(LocalDateTime.of(2026, 1, 20, 9, 0));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la finalización del lote que envasó este barril (20/01/2026 09:00:00)")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-31: registrarFraccionamientoBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la finalización del lote del último envasado")
    void registrar_debeRechazarFechaIgualALaFinalizacionDelLote() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        stubRegistroConLoteFinalizado(LocalDateTime.of(2026, 1, 20, 9, 0));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la finalización del lote que envasó este barril (20/01/2026 09:00:00)");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RFRB-32: registrarFraccionamientoBarril registra el fraccionamiento cuando la fecha es posterior a la finalización del lote del último envasado")
    void registrar_debeRegistrarSiLaFechaEsPosteriorALaFinalizacionDelLote() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 1), 5.0, "Merma");
        BarrilEntity barril = stubRegistroConLoteFinalizado(LocalDateTime.of(2026, 1, 20, 9, 0));
        when(fraccionamientoBarrilRepository.save(any(FraccionamientoBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(15.0);
        verify(fraccionamientoBarrilRepository).save(any(FraccionamientoBarrilEntity.class));
    }

    @Test
    @DisplayName("CP-RFRB-33: registrarFraccionamientoBarril no restringe la fecha cuando el lote del último envasado no tiene fecha de finalización")
    void registrar_noDebeRestringirSiElLoteNoTieneFechaDeFinalizacion() {
        // === PREPARACION DE DATOS ===
        FraccionamientoBarrilFormDTO formDTO = fraccionamientoBarrilFormDTO(1L, LocalDateTime.of(2026, 1, 20, 9, 0), 5.0, "Merma");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.buscarFechaFinalizacionDelLoteDelUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.save(any(FraccionamientoBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fraccionamientoBarrilServicio.registrarFraccionamientoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(15.0);
        verify(fraccionamientoBarrilRepository).save(any(FraccionamientoBarrilEntity.class));
    }

    // ==================== anularFraccionamientoBarril ====================

    @Test
    @DisplayName("CP-AFRB-01: anularFraccionamientoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(fraccionamientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFRB-02: anularFraccionamientoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(fraccionamientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFRB-03: anularFraccionamientoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(fraccionamientoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-AFRB-04: anularFraccionamientoBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando el fraccionamiento no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fraccionamiento de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-05: anularFraccionamientoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(fraccionamientoBarrilRepository, never()).findById(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-06: anularFraccionamientoBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(fraccionamientoBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fraccionamiento de barril con ID: 7");

        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-07: anularFraccionamientoBarril lanza ReglaNegocioException cuando el fraccionamiento no se encuentra en estado REGISTRADO")
    void anular_debeRechazarFraccionamientoNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);

        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular fraccionamientos de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-08: anularFraccionamientoBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-09: anularFraccionamientoBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-10: anularFraccionamientoBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior al fraccionamiento")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getContenidoActual()).isEqualTo(15.0);
        assertThat(fraccionamiento.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-11: anularFraccionamientoBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la del fraccionamiento")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFechaDelFraccionamiento() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-12: anularFraccionamientoBarril lanza ReglaNegocioException por el uso, sin consultar el último envasado, cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verifyNoInteractions(envasadoLoteRepository);

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-13: anularFraccionamientoBarril lanza ReglaNegocioException cuando el barril asociado no está en el estado operativo que dejó el fraccionamiento")
    void anular_debeRechazarBarrilFueraDelEstadoResultante() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un fraccionamiento cuyo barril asociado se encuentre en estado operativo Con cerveza");

        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-14: anularFraccionamientoBarril revierte un fraccionamiento que dejó el barril CON_CERVEZA (camino feliz)")
    void anular_debeRevertirFraccionamientoQueDejoConCerveza() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
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
    @DisplayName("CP-AFRB-15: anularFraccionamientoBarril revierte un fraccionamiento que dejó el barril EN_LIMPIEZA (camino feliz)")
    void anular_debeRevertirFraccionamientoQueDejoEnLimpieza() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA, 0.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.EN_LIMPIEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
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

    @Test
    @DisplayName("CP-AFRB-16: anularFraccionamientoBarril anula el fraccionamiento cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 15, 9, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        FraccionamientoBarrilResponseDTO resultado = fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(20.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-AFRB-17: anularFraccionamientoBarril restituye el contenido redondeado a 4 decimales: 0,1 más 19,9 es exactamente 20")
    void anular_debeRedondearElContenidoRestituido() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 0.1);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 19.9, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("CP-AFRB-18: anularFraccionamientoBarril valida en orden: bloqueo del barril, lectura del fraccionamiento, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, fraccionamientoBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(fraccionamientoBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-AFRB-19: anularFraccionamientoBarril lanza IllegalStateException sin modificar nada cuando el barril no tiene ninguna operación registrada")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA, 15.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 5.0, EstadoOperativoBarril.CON_CERVEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(fraccionamientoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFRB-20: anularFraccionamientoBarril restituye una cantidad de 4 decimales a un barril vacío: 0 más 0,1234 deja 0,1234 y el barril CON_CERVEZA")
    void anular_debeRestituirUnaCantidadDeCuatroDecimalesAUnBarrilVacio() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_LIMPIEZA, 0.0);
        FraccionamientoBarrilEntity fraccionamiento = crearFraccionamientoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Merma", 0.1234, EstadoOperativoBarril.EN_LIMPIEZA, barril);
        AnulacionFraccionamientoBarrilFormDTO formDTO = anulacionFormDTO("Fraccionamiento cargado por error");
        stubBarrilBloqueadoYFraccionamiento(barril, fraccionamiento);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(fraccionamientoBarrilRepository.save(fraccionamiento)).thenReturn(fraccionamiento);

        // === EJECUCION ===
        fraccionamientoBarrilServicio.anularFraccionamientoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getContenidoActual()).isEqualTo(0.1234);
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(fraccionamiento.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    // ==================== helpers de construcción ====================

    private BarrilEntity stubRegistroConLoteFinalizado(LocalDateTime fechaFinalizacionLote) {
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA, 20.0);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.buscarFechaFinalizacionDelLoteDelUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fechaFinalizacionLote));
        return barril;
    }

    private void stubRegistroPermitido(BarrilEntity barril) {
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(barril.getId())).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(barril.getId())).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.save(any(FraccionamientoBarrilEntity.class))).thenAnswer(invocation -> {
            FraccionamientoBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });
    }

    private void stubBarrilBloqueadoYFraccionamiento(BarrilEntity barril, FraccionamientoBarrilEntity fraccionamiento) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(fraccionamiento.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(fraccionamientoBarrilRepository.findById(fraccionamiento.getId())).thenReturn(Optional.of(fraccionamiento));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idFraccionamiento, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idFraccionamiento));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

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
                .nombre("Fabricante Test SA")
                .estado(Estado.ACTIVO)
                .build();

        return BarrilEntity.builder()
                .id(id)
                .identificadorInterno("BAR-" + id)
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
