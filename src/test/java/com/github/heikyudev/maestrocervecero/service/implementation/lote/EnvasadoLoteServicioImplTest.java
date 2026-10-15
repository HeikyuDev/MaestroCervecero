package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
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
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ResumenEnvasadoResponseDTO;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class EnvasadoLoteServicioImplTest {

    private static final Integer USOS_MAXIMOS_ANTES_MANTENIMIENTO = 200;
    private static final double CAPACIDAD_UTIL_FERMENTADOR = 100.0;

    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @Mock
    private ILoteRepository loteRepository;

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
    @DisplayName("CP-FEL-02: filtrarEnvasadosLote con estado nulo no asume REGISTRADO por defecto, lo propaga tal cual junto con idBarril nulo sin restringir")
    void filtrarEnvasadosLote_debePropagarEstadoNuloSinDefault() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(3L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity registrado = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        EnvasadoLoteEntity anulado = crearEnvasadoLoteEntity(2L, etapaLote, barril, 20.0, EstadoTransaccion.ANULADO);
        when(envasadoLoteRepository.filtrarEnvasadosLote(1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado), pageable, 2));

        // === EJECUCION ===
        Page<EnvasadoLoteResponseDTO> resultado = envasadoLoteServicio.filtrarEnvasadosLote(1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(envasadoLoteRepository).filtrarEnvasadosLote(1L, null, null, pageable);
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

    @Test
    @DisplayName("CP-FEL-04: filtrarEnvasadosLote lanza ReglaNegocioException y no consulta nada cuando la etapa de lote es nula")
    void filtrarEnvasadosLote_debeRechazarEtapaLoteNula() {
        assertThatThrownBy(() -> envasadoLoteServicio.filtrarEnvasadosLote(null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa del lote es obligatoria");

        verifyNoInteractions(envasadoLoteRepository);
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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(1L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));

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
        when(etapaLoteRepository.buscarIdLoteDeLaEtapa(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(99L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");

        verifyNoInteractions(loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-02: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando el lote no está EN_EJECUCION")
    void registrarEnvasadoLote_debeRechazarLoteNoEnEjecucion() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.PENDIENTE), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder registrar un envasado");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-03: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando la etapa no está EN_CURSO")
    void registrarEnvasadoLote_debeRechazarEtapaNoEnCurso() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.PENDIENTE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para poder registrar un envasado");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-04: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando el tipo de etapa no admite envasado")
    void registrarEnvasadoLote_debeRechazarTipoEtapaNoPermiteEnvasado() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.MADURACION, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Maduración no admite el registro de envasados");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-05: registrarEnvasadoLote lanza RecursoNoEncontradoException y no persiste cuando el barril no existe")
    void registrarEnvasadoLote_debeRechazarBarrilInexistente() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El barril debe encontrarse en estado operativo Disponible para poder envasar en él");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-07: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando la cantidad a envasar es nula")
    void registrarEnvasadoLote_debeRechazarCantidadNula() {
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar es obligatoria");

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-08: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando la cantidad a envasar es igual a cero (valor límite)")
    void registrarEnvasadoLote_debeRechazarCantidadIgualACero() {
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 0.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar debe ser mayor a 0");

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-09: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando la cantidad a envasar es negativa")
    void registrarEnvasadoLote_debeRechazarCantidadNegativa() {
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, -5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar debe ser mayor a 0");

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-10: registrarEnvasadoLote lanza ReglaNegocioException y no persiste cuando la cantidad a envasar supera la capacidad del barril")
    void registrarEnvasadoLote_debeRechazarCantidadSuperaCapacidad() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 60.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar no puede superar la capacidad del barril seleccionado (50 L)");

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
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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

    @Test
    @DisplayName("CP-REL-13: registrarEnvasadoLote lanza ReglaNegocioException cuando la cantidad supera la capacidad útil del fermentador (sin envasados previos)")
    void registrarEnvasadoLote_debeRechazarCantidadQueSuperaLaCapacidadUtilDelFermentador() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 300.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 120.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar supera la capacidad útil del fermentador (quedan 100 L disponibles para envasar)");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-14: registrarEnvasadoLote lanza ReglaNegocioException cuando lo ya envasado más esta cantidad supera la capacidad útil del fermentador")
    void registrarEnvasadoLote_debeRechazarCantidadQueSumadaALoYaEnvasadoSuperaLaCapacidadUtil() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 300.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(80.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar supera la capacidad útil del fermentador (quedan 20 L disponibles para envasar)");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-15: registrarEnvasadoLote persiste cuando lo ya envasado más esta cantidad es exactamente igual a la capacidad útil del fermentador (valor límite, permitido)")
    void registrarEnvasadoLote_debePermitirCantidadQueCompletaExactoLaCapacidadUtil() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 300.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(70.0);
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0));

        // === ASSERTS ===
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(30.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository).save(barril);
        verify(envasadoLoteRepository).save(any(EnvasadoLoteEntity.class));
    }

    @Test
    @DisplayName("CP-REL-16: registrarEnvasadoLote recupera el lote bloqueado antes de validarlo y de consultar el barril")
    void registrarEnvasadoLote_debeBloquearElLoteAntesDeValidarlo() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0));

        // === ASSERTS ===
        InOrder orden = inOrder(loteRepository, barrilRepository);
        orden.verify(loteRepository).buscarPorIdParaActualizar(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        verify(loteRepository, times(1)).buscarPorIdParaActualizar(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-REL-29: registrarEnvasadoLote bloquea el lote antes de leer la etapa, de modo que la validación ve el estado más reciente de la etapa")
    void registrarEnvasadoLote_debeBloquearElLoteAntesDeLeerLaEtapa() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0));

        // === ASSERTS ===
        InOrder orden = inOrder(etapaLoteRepository, loteRepository);
        orden.verify(etapaLoteRepository).buscarIdLoteDeLaEtapa(1L);
        orden.verify(loteRepository).buscarPorIdParaActualizar(1L);
        orden.verify(etapaLoteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-AEL-12: anularEnvasadoLote bloquea el lote antes de leer el envasado, de modo que dos anulaciones simultáneas no pasan ambas la validación de estado")
    void anularEnvasadoLote_debeBloquearElLoteAntesDeLeerElEnvasado() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.ANULADO);
        when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(envasadoLoteRepository.findById(10L)).thenReturn(Optional.of(envasadoLote));

        // === EJECUCION ===
        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class);

        // === ASSERTS ===
        InOrder orden = inOrder(envasadoLoteRepository, loteRepository);
        orden.verify(envasadoLoteRepository).buscarIdLoteDelEnvasado(10L);
        orden.verify(loteRepository).buscarPorIdParaActualizar(1L);
        orden.verify(envasadoLoteRepository).findById(10L);
    }

    @Test
    @DisplayName("CP-REL-17: registrarEnvasadoLote lanza RecursoNoEncontradoException y no consulta nada más cuando el lote a bloquear no existe")
    void registrarEnvasadoLote_debeRechazarLoteInexistenteAlBloquear() {
        LoteEntity lote = crearLoteEntity(EstadoLote.EN_EJECUCION);
        lote.setId(7L);
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, lote, TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 7");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-18: registrarEnvasadoLote lanza ReglaNegocioException y no consulta el barril cuando el lote está FINALIZADO")
    void registrarEnvasadoLote_debeRechazarLoteFinalizado() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.FINALIZADO), TipoEtapa.ENVASADO, EstadoEtapaLote.FINALIZADA);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));

        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder registrar un envasado");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-19: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando no se informó el barril")
    void registrarEnvasadoLote_debeRechazarBarrilNulo() {
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(null, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El barril es obligatorio");

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-20: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando la cantidad a envasar es NaN o infinita")
    void registrarEnvasadoLote_debeRechazarCantidadNoFinita() {
        for (double noFinita : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, noFinita)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cantidad a envasar debe ser un número mayor a 0");
        }

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-21: registrarEnvasadoLote lanza ReglaNegocioException sin consultar nada cuando la cantidad a envasar supera el máximo técnico (100.000 L)")
    void registrarEnvasadoLote_debeRechazarCantidadQueSuperaElMaximoTecnico() {
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 100_000.5)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar no puede superar 100.000 litros");

        verifyNoInteractions(etapaLoteRepository, loteRepository, barrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-REL-22: registrarEnvasadoLote permite envasar por encima del volumen objetivo del lote mientras no supere la capacidad útil del fermentador")
    void registrarEnvasadoLote_debePermitirEnvasarMasQueElVolumenObjetivo() {
        // === PREPARACION DE DATOS ===
        // Volumen objetivo 50 L, capacidad útil del fermentador 100 L: ya hay 45 L envasados y se envasan 20 L más (65 L)
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(45.0);
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 20.0));

        // === ASSERTS ===
        assertThat(etapaLote.getLote().getVolumenObjetivo()).isEqualTo(50.0);
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(20.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-REL-23: registrarEnvasadoLote compara los acumulados decimales redondeados a 4 decimales (10,2 + 89,9 contra 100,1)")
    void registrarEnvasadoLote_debePermitirAcumuladoDecimalIgualALaCapacidadUtil() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.1);
        BarrilEntity barril = crearBarrilEntity(1L, 100.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(10.2);
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 89.9));

        // === ASSERTS ===
        assertThat(10.2 + 89.9).isGreaterThan(100.1);
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(89.9);
        verify(envasadoLoteRepository).save(any(EnvasadoLoteEntity.class));
    }

    @Test
    @DisplayName("CP-REL-24: registrarEnvasadoLote solo cuenta lo envasado REGISTRADO: un envasado anulado libera su capacidad")
    void registrarEnvasadoLote_noDebeContarLosEnvasadosAnulados() {
        // === PREPARACION DE DATOS ===
        // El total previo lo informa solo la suma de los envasados REGISTRADO: un envasado anulado de 80 L ya no figura en ella
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 100.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(20.0);
        when(envasadoLoteRepository.save(any(EnvasadoLoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 80.0));

        // === ASSERTS ===
        assertThat(resultado.getCantidadEnvasada()).isEqualTo(80.0);
        verify(envasadoLoteRepository).sumarCantidadEnvasadaRegistrada(1L);
    }

    @Test
    @DisplayName("CP-REL-25: registrarEnvasadoLote informa 0 L disponibles (nunca negativo) cuando la capacidad útil del fermentador ya está agotada")
    void registrarEnvasadoLote_debeInformarCeroDisponiblesSiLaCapacidadEstaAgotada() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 100.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(100.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 1.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar supera la capacidad útil del fermentador (quedan 0 L disponibles para envasar)");

        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-REL-26: registrarEnvasadoLote informa 0 L disponibles (nunca negativo) cuando lo ya envasado supera la capacidad útil del fermentador")
    void registrarEnvasadoLote_debeInformarCeroDisponiblesSiLoEnvasadoSuperaLaCapacidad() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        BarrilEntity barril = crearBarrilEntity(1L, 100.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(105.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 1.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar supera la capacidad útil del fermentador (quedan 0 L disponibles para envasar)");
    }

    @Test
    @DisplayName("CP-REL-27: registrarEnvasadoLote resuelve la capacidad útil del fermentador desenvolviendo el proxy de Hibernate del equipamiento de la etapa")
    void registrarEnvasadoLote_debeResolverElFermentadorDesdeUnProxy() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        EquipamientoEntity proxy = proxyDe(crearFermentador(100.0));
        etapaLote.setEquipamiento(proxy);
        BarrilEntity barril = crearBarrilEntity(1L, 300.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);

        // === EJECUCION Y ASSERTS ===
        assertThat(proxy).isNotInstanceOf(FermentadorEntity.class);
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 120.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a envasar supera la capacidad útil del fermentador (quedan 100 L disponibles para envasar)");
    }

    @Test
    @DisplayName("CP-REL-28: registrarEnvasadoLote lanza ReglaNegocioException cuando el equipamiento de la etapa no es un fermentador")
    void registrarEnvasadoLote_debeRechazarEquipamientoQueNoEsFermentador() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 100.0);
        etapaLote.setEquipamiento(MolinoEntity.builder().id(9L).build());
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        prepararRegistro(etapaLote, barril);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.registrarEnvasadoLote(1L, registrarFormDTO(1L, 30.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de Envasado del lote 1 no tiene un fermentador asociado como equipamiento");

        verify(barrilRepository, never()).save(any());
        verify(envasadoLoteRepository, never()).save(any());
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
    @DisplayName("CP-AEL-12: anularEnvasadoLote lanza ReglaNegocioException cuando el motivo de anulación tiene 256 caracteres")
    void anularEnvasadoLote_debeRechazarMotivoDe256Caracteres() {
        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("a".repeat(256))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");

        verifyNoInteractions(envasadoLoteRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-AEL-03: anularEnvasadoLote lanza RecursoNoEncontradoException y no persiste cuando el envasado no existe")
    void anularEnvasadoLote_debeRechazarEnvasadoInexistente() {
        when(envasadoLoteRepository.buscarIdLoteDelEnvasado(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(99L, anularFormDTO("Error de carga")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el envasado de lote con ID: 99");

        verifyNoInteractions(loteRepository, barrilRepository);
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AEL-04: anularEnvasadoLote lanza ReglaNegocioException y no persiste cuando el envasado ya está ANULADO")
    void anularEnvasadoLote_debeRechazarEnvasadoYaAnulado() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 0.0, EstadoOperativoBarril.DISPONIBLE);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, barril, 30.0, EstadoTransaccion.ANULADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(envasadoLoteRepository.findById(1L)).thenReturn(Optional.of(envasadoLote));
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(1L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular envasados en estado Registrado");

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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(1L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(1L, anularFormDTO("Error de carga")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder anular un envasado");

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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(1L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
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

    @Test
    @DisplayName("CP-AEL-10: anularEnvasadoLote recupera el lote bloqueado antes de validarlo y de consultar el barril")
    void anularEnvasadoLote_debeBloquearElLoteAntesDeValidarlo() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.findById(10L)).thenReturn(Optional.of(envasadoLote));
        lenient().when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(etapaLote.getLote()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(envasadoLoteRepository.save(envasadoLote)).thenReturn(envasadoLote);

        // === EJECUCION ===
        EnvasadoLoteResponseDTO resultado = envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga"));

        // === ASSERTS ===
        InOrder orden = inOrder(loteRepository, barrilRepository);
        orden.verify(loteRepository).buscarPorIdParaActualizar(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        verify(loteRepository, times(1)).buscarPorIdParaActualizar(1L);
        assertThat(envasadoLote.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-AEL-11: anularEnvasadoLote lanza RecursoNoEncontradoException y no consulta el barril ni persiste cuando el lote a bloquear no existe")
    void anularEnvasadoLote_debeRechazarLoteInexistenteAlBloquear() {
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        etapaLote.getLote().setId(7L);
        BarrilEntity barril = crearBarrilEntity(1L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(10L, etapaLote, barril, 30.0, EstadoTransaccion.REGISTRADO);
        when(envasadoLoteRepository.buscarIdLoteDelEnvasado(10L)).thenReturn(Optional.of(envasadoLote.getEtapaLote().getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envasadoLoteServicio.anularEnvasadoLote(10L, anularFormDTO("Error de carga")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 7");

        verifyNoInteractions(barrilRepository);
        verify(envasadoLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FEL-05: filtrarEnvasadosLote informa la fecha de registro (fecha de creación de la auditoría) de cada envasado")
    void filtrarEnvasadosLote_debeInformarLaFechaDeRegistro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EtapaLoteEntity etapaLote = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        EnvasadoLoteEntity envasadoLote = crearEnvasadoLoteEntity(1L, etapaLote, crearBarrilEntity(3L, 50.0, 30.0, EstadoOperativoBarril.CON_CERVEZA), 30.0, EstadoTransaccion.REGISTRADO);
        envasadoLote.setCreatedDate(java.time.LocalDateTime.of(2027, 7, 5, 14, 20));
        when(envasadoLoteRepository.filtrarEnvasadosLote(1L, null, null, pageable)).thenReturn(new PageImpl<>(List.of(envasadoLote), pageable, 1));

        // === EJECUCION ===
        Page<EnvasadoLoteResponseDTO> resultado = envasadoLoteServicio.filtrarEnvasadosLote(1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent().get(0).getFechaRegistro()).isEqualTo(java.time.LocalDateTime.of(2027, 7, 5, 14, 20));
    }

    // ==================== resumirEnvasado ====================

    @Test
    @DisplayName("CP-RSE-01: resumirEnvasado lanza ReglaNegocioException y no consulta nada cuando la etapa de lote es nula")
    void resumirEnvasado_debeRechazarEtapaLoteNula() {
        assertThatThrownBy(() -> envasadoLoteServicio.resumirEnvasado(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa del lote es obligatoria");
        verifyNoInteractions(etapaLoteRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RSE-02: resumirEnvasado lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void resumirEnvasado_debeLanzarExcepcionSiLaEtapaNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.resumirEnvasado(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RSE-03: resumirEnvasado lanza ReglaNegocioException si la etapa no admite envasados")
    void resumirEnvasado_debeRechazarUnaEtapaQueNoAdmiteEnvasados() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaFermentacion = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.FERMENTACION, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaFermentacion));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaFermentacion.getLote().getId()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.resumirEnvasado(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Fermentación no admite el registro de envasados");
        verifyNoInteractions(envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RSE-04: resumirEnvasado — camino feliz: total envasado, capacidad útil del fermentador, lo que resta, el fermentador y el volumen objetivo")
    void resumirEnvasado_debeResumirLoEnvasadoFrenteALaCapacidadDelFermentador() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaEnvasado = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaEnvasado));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaEnvasado.getLote().getId()));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(30.0);

        // === EJECUCION ===
        ResumenEnvasadoResponseDTO resultado = envasadoLoteServicio.resumirEnvasado(1L);

        // === ASSERTS ===
        assertThat(resultado.getTotalEnvasado()).isEqualTo(30.0);
        assertThat(resultado.getCapacidadUtilFermentador()).isEqualTo(100.0);
        assertThat(resultado.getRestante()).isEqualTo(70.0);
        assertThat(resultado.getExcesoSobreVolumenObjetivo()).isEqualTo(0.0);
        assertThat(resultado.getIdentificadorFermentador()).isEqualTo("FER-01");
        assertThat(resultado.getVolumenObjetivo()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-RSE-05: resumirEnvasado sin envasados registrados informa total 0 y todo el fermentador por llenar")
    void resumirEnvasado_debeInformarTotalCeroSinEnvasados() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaEnvasado = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaEnvasado));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaEnvasado.getLote().getId()));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(0.0);

        // === EJECUCION ===
        ResumenEnvasadoResponseDTO resultado = envasadoLoteServicio.resumirEnvasado(1L);

        // === ASSERTS ===
        assertThat(resultado.getTotalEnvasado()).isEqualTo(0.0);
        assertThat(resultado.getRestante()).isEqualTo(100.0);
        assertThat(resultado.getExcesoSobreVolumenObjetivo()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-RSE-06: resumirEnvasado con la capacidad del fermentador agotada informa restante 0, y con envasado por encima del volumen objetivo informa el exceso")
    void resumirEnvasado_debeInformarRestanteCeroConLaCapacidadAgotadaYElExceso() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaEnvasado = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaEnvasado));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaEnvasado.getLote().getId()));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(100.0);

        // === EJECUCION ===
        ResumenEnvasadoResponseDTO resultado = envasadoLoteServicio.resumirEnvasado(1L);

        // === ASSERTS ===
        assertThat(resultado.getTotalEnvasado()).isEqualTo(100.0);
        assertThat(resultado.getRestante()).isEqualTo(0.0);
        assertThat(resultado.getExcesoSobreVolumenObjetivo()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-RSE-07: resumirEnvasado nunca informa un restante negativo aunque lo envasado supere la capacidad útil (por ejemplo, tras bajar la capacidad del fermentador)")
    void resumirEnvasado_nuncaDebeInformarRestanteNegativo() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaEnvasado = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, 40.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaEnvasado));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaEnvasado.getLote().getId()));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(45.5);

        // === EJECUCION ===
        ResumenEnvasadoResponseDTO resultado = envasadoLoteServicio.resumirEnvasado(1L);

        // === ASSERTS ===
        assertThat(resultado.getRestante()).isEqualTo(0.0);
        assertThat(resultado.getCapacidadUtilFermentador()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("CP-RSE-08: resumirEnvasado resuelve el fermentador desde un proxy de Hibernate de EquipamientoEntity")
    void resumirEnvasado_debeResolverElFermentadorDesdeUnProxyDeHibernate() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaEnvasado = crearEtapaLoteEntity(1L, crearLoteEntity(EstadoLote.EN_EJECUCION), TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO);
        etapaEnvasado.setEquipamiento(proxyDe(crearFermentador(80.0)));
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaEnvasado));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaEnvasado.getLote().getId()));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(1L)).thenReturn(20.0);

        // === EJECUCION ===
        ResumenEnvasadoResponseDTO resultado = envasadoLoteServicio.resumirEnvasado(1L);

        // === ASSERTS ===
        assertThat(resultado.getCapacidadUtilFermentador()).isEqualTo(80.0);
        assertThat(resultado.getRestante()).isEqualTo(60.0);
        assertThat(resultado.getIdentificadorFermentador()).isEqualTo("FER-01");
    }

    // ==================== existenEnvasadosRegistrados ====================

    @Test
    @DisplayName("CP-EER-01: existenEnvasadosRegistrados lanza ReglaNegocioException si no se informó el lote y RecursoNoEncontradoException si no existe")
    void existenEnvasadosRegistrados_debeValidarElLote() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.existsById(99L)).thenReturn(false);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> envasadoLoteServicio.existenEnvasadosRegistrados(null))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El lote es obligatorio");
        assertThatThrownBy(() -> envasadoLoteServicio.existenEnvasadosRegistrados(99L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("No se encontró el lote con ID: 99");
        verifyNoInteractions(envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-EER-02: existenEnvasadosRegistrados devuelve true con envasados registrados y false sin ninguno (solo cuentan los REGISTRADO)")
    void existenEnvasadosRegistrados_debeConsultarSoloLosEnvasadosRegistrados() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.existsById(1L)).thenReturn(true);
        when(envasadoLoteRepository.existsByEtapaLote_Lote_IdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(true, false);

        // === EJECUCION Y ASSERTS ===
        assertThat(envasadoLoteServicio.existenEnvasadosRegistrados(1L)).isTrue();
        assertThat(envasadoLoteServicio.existenEnvasadosRegistrados(1L)).isFalse();
    }

    // ==================== helpers ====================

    private static LoteEntity crearLoteEntity(EstadoLote estado) {
        return LoteEntity.builder()
                .id(1L)
                .estado(estado)
                .volumenObjetivo(50.0)
                .build();
    }

    private static EtapaLoteEntity crearEtapaLoteEntity(Long id, LoteEntity lote, TipoEtapa etapa, EstadoEtapaLote estado) {
        return crearEtapaLoteEntity(id, lote, etapa, estado, CAPACIDAD_UTIL_FERMENTADOR);
    }

    private static EtapaLoteEntity crearEtapaLoteEntity(Long id, LoteEntity lote, TipoEtapa etapa, EstadoEtapaLote estado, double capacidadUtilFermentador) {
        return EtapaLoteEntity.builder()
                .id(id)
                .lote(lote)
                .etapa(etapa)
                .estado(estado)
                .equipamiento(crearFermentador(capacidadUtilFermentador))
                .build();
    }

    private static FermentadorEntity crearFermentador(double capacidadUtil) {
        return FermentadorEntity.builder()
                .id(30L)
                .identificadorInterno("FER-01")
                .capacidadTotal(capacidadUtil + 10.0)
                .capacidadUtil(capacidadUtil)
                .build();
    }

    /**
     * Simula el proxy perezoso de Hibernate que llega desde {@code EtapaLoteEntity.getEquipamiento()}: una instancia
     * de la clase abstracta {@code EquipamientoEntity} (no de {@code FermentadorEntity}) que delega en el real.
     */
    private static EquipamientoEntity proxyDe(FermentadorEntity real) {
        EquipamientoEntity proxy = mock(EquipamientoEntity.class, withSettings().extraInterfaces(HibernateProxy.class));
        HibernateProxy proxyHibernate = (HibernateProxy) proxy;
        LazyInitializer inicializador = mock(LazyInitializer.class);
        when(proxyHibernate.asHibernateProxy()).thenReturn(proxyHibernate);
        when(proxyHibernate.getHibernateLazyInitializer()).thenReturn(inicializador);
        when(inicializador.getImplementation()).thenReturn(real);
        return proxy;
    }

    private void prepararRegistro(EtapaLoteEntity etapaLote, BarrilEntity barril) {
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(1L)).thenReturn(Optional.of(etapaLote.getLote().getId()));
        when(loteRepository.buscarPorIdParaActualizar(etapaLote.getLote().getId())).thenReturn(Optional.of(etapaLote.getLote()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
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
