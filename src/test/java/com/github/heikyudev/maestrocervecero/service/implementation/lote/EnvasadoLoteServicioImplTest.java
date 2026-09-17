package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
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
class EnvasadoLoteServicioImplTest {

    private static final Integer USOS_MAXIMOS_ANTES_MANTENIMIENTO = 200;

    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private EnvasadoLoteServicioImpl envasadoLoteServicio;

    // ==================== filtrarEnvasadosLote ====================

    @Test
    @DisplayName("CP-FEL-01: filtrarEnvasadosLote retorna una página de envasados correctamente mapeada a DTO cuando se filtra por barril y estado")
    void filtrarEnvasadosLote_debeRetornarPaginaMapeadaFiltrandoPorBarrilYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(3L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        EnvasadoLoteEntity otroEnvasadoLote = crearEnvasadoLoteEntity(2L, etapaLote, barril, 20.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.filtrarEnvasadosLote(1L, 3L, EstadoTransaccion.REGISTRADO, pageable))
                .thenReturn(new PageImpl<>(List.of(envasadoLote, otroEnvasadoLote), pageable, 2));

        // === EJECUCION ===
        Page<EnvasadoLoteResponseDTO> resultado = envasadoLoteServicio.filtrarEnvasadosLote(1L, 3L, EstadoTransaccion.REGISTRADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertEnvasadoLoteDTO(envasadoLote, resultado.getContent().get(0));
        assertEnvasadoLoteDTO(otroEnvasadoLote, resultado.getContent().get(1));
        verify(envasadoLoteRepository).filtrarEnvasadosLote(1L, 3L, EstadoTransaccion.REGISTRADO, pageable);
    }

    @Test
    @DisplayName("CP-FEL-02: filtrarEnvasadosLote asume REGISTRADO cuando el estado es nulo y propaga idBarril nulo sin restringir")
    void filtrarEnvasadosLote_debeAsumirRegistradoYPropagarIdBarrilNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(envasadoLoteRepository.filtrarEnvasadosLote(1L, null, EstadoTransaccion.REGISTRADO, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<EnvasadoLoteResponseDTO> resultado = envasadoLoteServicio.filtrarEnvasadosLote(1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(envasadoLoteRepository).filtrarEnvasadosLote(1L, null, EstadoTransaccion.REGISTRADO, pageable);
    }

    @Test
    @DisplayName("CP-FEL-03: filtrarEnvasadosLote permite ver explícitamente los envasados anulados")
    void filtrarEnvasadosLote_debePermitirVerAnulados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.FINALIZADA);
        BarrilEntity barril = crearBarrilEntity(3L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        EnvasadoLoteEntity envasadoAnulado = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.ANULADO);
        when(envasadoLoteRepository.filtrarEnvasadosLote(1L, null, EstadoTransaccion.ANULADO, pageable))
                .thenReturn(new PageImpl<>(List.of(envasadoAnulado), pageable, 1));

        // === EJECUCION ===
        Page<EnvasadoLoteResponseDTO> resultado = envasadoLoteServicio.filtrarEnvasadosLote(1L, null, EstadoTransaccion.ANULADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(envasadoLoteRepository).filtrarEnvasadosLote(1L, null, EstadoTransaccion.ANULADO, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del envasado, incluyendo la etapa y el barril, cuando el ID existe")
    void buscarPorId_debeRetornarEnvasadoExistente() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(3L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(1L)).thenReturn(Optional.of(envasadoLote));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertEnvasadoLoteDTO(envasadoLote, resultado);
        verify(envasadoLoteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(envasadoLoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el envasado de lote con ID: 99");
        verify(envasadoLoteRepository).findById(99L);
    }

    // ==================== registrarEnvasadoLote ====================

    @Test
    @DisplayName("CP-REL-01: registrarEnvasadoLote lanza RecursoNoEncontradoException y no consulta nada más cuando la etapa de lote no existe")
    void registrarEnvasadoLote_debeRechazarEtapaInexistente() {
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(99L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-02: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando el lote no está EN_EJECUCION")
    void registrarEnvasadoLote_debeRechazarLoteNoEnEjecucion() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.PENDIENTE), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado EN_EJECUCION para poder registrar un envasado");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-03: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando la etapa no está EN_CURSO")
    void registrarEnvasadoLote_debeRechazarEtapaNoEnCurso() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.PENDIENTE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden registrar envasados sobre la etapa actualmente en curso del lote");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-04: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando el tipo de etapa no admite envasado")
    void registrarEnvasadoLote_debeRechazarTipoEtapaNoPermiteEnvasado() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.MADURACION, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa MADURACION no admite el registro de envasados");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-05: registrarEnvasadoLote lanza RecursoNoEncontradoException y no persiste cuando el barril no existe")
    void registrarEnvasadoLote_debeRechazarBarrilInexistente() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(99L, 30.0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-06: registrarEnvasadoLote lanza ReglaNegocioException y no persiste cuando el barril no está en estado operativo DISPONIBLE")
    void registrarEnvasadoLote_debeRechazarBarrilNoDisponible() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.EN_LIMPIEZA);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El barril debe encontrarse en estado operativo DISPONIBLE para poder envasar en él");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-07: registrarEnvasadoLote lanza ReglaNegocioException y no persiste cuando la cantidad a envasar es nula")
    void registrarEnvasadoLote_debeRechazarCantidadNula() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar debe ser mayor a cero");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-08: registrarEnvasadoLote lanza ReglaNegocioException cuando la cantidad a envasar es igual a cero (valor límite)")
    void registrarEnvasadoLote_debeRechazarCantidadIgualACero() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 0.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar debe ser mayor a cero");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-09: registrarEnvasadoLote lanza ReglaNegocioException cuando la cantidad a envasar es negativa")
    void registrarEnvasadoLote_debeRechazarCantidadNegativa() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, -5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar debe ser mayor a cero");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-10: registrarEnvasadoLote lanza ReglaNegocioException y no persiste cuando la cantidad a envasar supera la capacidad del barril")
    void registrarEnvasadoLote_debeRechazarCantidadSuperaCapacidad() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 60.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar no puede superar la capacidad del barril seleccionado");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-11: registrarEnvasadoLote persiste cuando la cantidad a envasar es igual a la capacidad del barril (valor límite, permitido)")
    void registrarEnvasadoLote_debePermitirCantidadIgualALaCapacidad() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> {
            EnvasadoLoteEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 50.0));

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(barril.getContenidoActual()).isEqualTo(50.0);
        verify(barrilRepository).save(barril);
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(50.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-REL-12: registrarEnvasadoLote persiste el envasado y actualiza el barril cuando los datos son válidos (camino feliz)")
    void registrarEnvasadoLote_debePersistirYActualizarBarrilCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> {
            EnvasadoLoteEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(10L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0));

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> barrilCaptor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(barrilCaptor.capture());
        assertThat(barrilCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(barrilCaptor.getValue().getContenidoActual()).isEqualTo(30.0);

        ArgumentCaptor<EnvasadoLoteEntity> envasadoCaptor = ArgumentCaptor.forClass(EnvasadoLoteEntity.class);
        verify(envasadoLoteRepository).save(envasadoCaptor.capture());
        EnvasadoLoteEntity entidadCapturada = envasadoCaptor.getValue();
        assertThat(entidadCapturada.getCantidadEnvasada()).isEqualTo(30.0);
        assertThat(entidadCapturada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(entidadCapturada.getEtapaLote()).isEqualTo(etapaLote);
        assertThat(entidadCapturada.getBarril()).isEqualTo(barril);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(30.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularEnvasadoLote ====================

    @Test
    @DisplayName("CP-AEL-01: anularEnvasadoLote lanza ReglaNegocioException y no consulta nada cuando el motivo de anulación es nulo")
    void anularEnvasadoLote_debeRechazarMotivoNulo() {
        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO(null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(envasadoLoteRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AEL-02: anularEnvasadoLote lanza ReglaNegocioException cuando el motivo de anulación está vacío (solo espacios)")
    void anularEnvasadoLote_debeRechazarMotivoEnBlanco() {
        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("   ")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(envasadoLoteRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AEL-03: anularEnvasadoLote lanza RecursoNoEncontradoException y no persiste cuando el envasado no existe")
    void anularEnvasadoLote_debeRechazarEnvasadoInexistente() {
        when(envasadoLoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(99L, anularFormDTO("Error de carga")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el envasado de lote con ID: 99");

        verifyNoInteractions(barrilRepository);
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-04: anularEnvasadoLote lanza ReglaNegocioException y no persiste cuando el envasado ya está ANULADO")
    void anularEnvasadoLote_debeRechazarEnvasadoYaAnulado() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.ANULADO);
        when(envasadoLoteRepository.findById(1L)).thenReturn(Optional.of(envasadoLote));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular envasados en estado REGISTRADO");

        verifyNoInteractions(barrilRepository);
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-05: anularEnvasadoLote lanza ReglaNegocioException y no persiste cuando el lote asociado no está EN_EJECUCION")
    void anularEnvasadoLote_debeRechazarLoteNoEnEjecucion() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.FINALIZADO), TipoEtapa.ENVASADO, EstadoEtapaLote.FINALIZADA);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(1L)).thenReturn(Optional.of(envasadoLote));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado EN_EJECUCION para poder anular un envasado");

        verifyNoInteractions(barrilRepository);
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-06: anularEnvasadoLote lanza RecursoNoEncontradoException y no persiste cuando el barril asociado ya no existe")
    void anularEnvasadoLote_debeRechazarBarrilInexistente() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(5L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(1L)).thenReturn(Optional.of(envasadoLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 5");

        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-07: anularEnvasadoLote lanza ReglaNegocioException y no persiste cuando el barril ya no está en estado operativo CON_CERVEZA")
    void anularEnvasadoLote_debeRechazarBarrilYaNoConCerveza() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.DESPACHADO);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(10L)).thenReturn(Optional.of(envasadoLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-08: anularEnvasadoLote lanza ReglaNegocioException cuando el contenido actual del barril ya no coincide con la cantidad envasada")
    void anularEnvasadoLote_debeRechazarContenidoNoCoincide() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 45.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(10L)).thenReturn(Optional.of(envasadoLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-09: anularEnvasadoLote revierte el barril y anula el envasado cuando todas las validaciones pasan (camino feliz)")
    void anularEnvasadoLote_debeRevertirBarrilYAnularEnvasado() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(10L)).thenReturn(Optional.of(envasadoLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(envasadoLote)).thenReturn(envasadoLote);

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(barril.getContenidoActual()).isEqualTo(0.0);
        verify(barrilRepository).save(barril);

        assertThat(envasadoLote.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(envasadoLote.getFechaAnulacion()).isNotNull();
        assertThat(envasadoLote.getMotivoAnulacion()).isEqualTo("Error de carga");
        verify(envasadoLoteRepository).save(envasadoLote);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de carga");
    }

    // ==================== helpers ====================

    private static LoteEntity crearLoteEntity(EstadoLote estado) {
        return LoteEntity.builder()
                .id(1L)
                .estado(estado)
                .build();
    }

    private static EtapaLoteEntity crearEtapaLoteEntity(Long id, LoteEntity lote, TipoEtapa etapa, EstadoEtapaLote estado) {
        return EtapaLoteEntity.builder()
                .id(id)
                .lote(lote)
                .etapa(etapa)
                .estado(estado)
                .build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, Double capacidad, Double contenidoActual, EstadoOperativoBarril estadoOperativo) {
        return BarrilEntity.builder()
                .id(id)
                .identificador("BAR-01")
                .capacidad(capacidad)
                .contenidoActual(contenidoActual)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static EnvasadoLoteEntity crearEnvasadoLoteEntity(Long id, EtapaLoteEntity etapaLote, BarrilEntity barril, Double cantidadEnvasada, EstadoTransaccion estado) {
        return EnvasadoLoteEntity.builder()
                .id(id)
                .cantidadEnvasada(cantidadEnvasada)
                .estado(estado)
                .etapaLote(etapaLote)
                .barril(barril)
                .build();
    }

    private static RegistrarEnvasadoLoteFormDTO registrarFormDTO(Long idBarril, Double cantidad) {
        return RegistrarEnvasadoLoteFormDTO.builder()
                .idBarril(idBarril)
                .cantidad(cantidad)
                .build();
    }

    private static AnularEnvasadoLoteFormDTO anularFormDTO(String motivoAnulacion) {
        return AnularEnvasadoLoteFormDTO.builder()
                .motivoAnulacion(motivoAnulacion)
                .build();
    }

    private static void assertEnvasadoLoteDTO(EnvasadoLoteEntity entidad, EnvasadoLoteResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getCantidadEnvasada()).isEqualTo(entidad.getCantidadEnvasada());
        assertThat(dto.getFechaAnulacion()).isEqualTo(entidad.getFechaAnulacion());
        assertThat(dto.getMotivoAnulacion()).isEqualTo(entidad.getMotivoAnulacion());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
        assertThat(dto.getEtapaLote().getId()).isEqualTo(entidad.getEtapaLote().getId());
        assertThat(dto.getBarril().getId()).isEqualTo(entidad.getBarril().getId());
    }
}
