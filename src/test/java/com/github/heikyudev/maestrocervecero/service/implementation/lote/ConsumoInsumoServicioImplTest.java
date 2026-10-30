package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EscaladoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.AdicionLupulo;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AdicionLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
class ConsumoInsumoServicioImplTest {

    @Mock
    private IConsumoInsumoRepository consumoInsumoRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;
    @Mock
    private ILoteRepository loteRepository;
    @Mock
    private ILoteInsumoRepository loteInsumoRepository;
    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private IEscaladoInsumoServicio escaladoInsumoServicio;

    @InjectMocks
    private ConsumoInsumoServicioImpl consumoInsumoServicio;

    private static final Long ID_LOTE_BLOQUEADO = 500L;

    /** Todo consumo bloquea primero el lote de su etapa: por defecto existe y se puede bloquear. */
    @BeforeEach
    void bloquearElLoteDeLaEtapaPorDefecto() {
        lenient().when(etapaLoteRepository.buscarIdLoteDeLaEtapa(anyLong())).thenReturn(Optional.of(ID_LOTE_BLOQUEADO));
        lenient().when(consumoInsumoRepository.buscarIdLoteDelConsumo(anyLong())).thenReturn(Optional.of(ID_LOTE_BLOQUEADO));
        lenient().when(loteRepository.buscarPorIdParaActualizar(ID_LOTE_BLOQUEADO)).thenReturn(Optional.of(new LoteEntity()));
    }

    // ==================== bloqueo del lote ====================

    @Test
    @DisplayName("CP-BLC-01: registrarConsumoInsumoReservado bloquea el lote de la etapa antes de leer la etapa y el lote de insumo")
    void registrarReservado_debeBloquearElLoteAntesDeLeerLaEtapa() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = ConsumoInsumoFormDTO.builder().idInsumo(1L).idLoteInsumo(10L).cantidadConsumida(1.0).build();

        // === EJECUCION ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        // === ASSERTS ===
        InOrder orden = inOrder(etapaLoteRepository, loteRepository);
        orden.verify(etapaLoteRepository).buscarIdLoteDeLaEtapa(1L);
        orden.verify(loteRepository).buscarPorIdParaActualizar(ID_LOTE_BLOQUEADO);
        orden.verify(etapaLoteRepository).findById(1L);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-BLC-02: registrarConsumoInsumoDirecto bloquea el lote de la etapa antes de leer la etapa y el lote de insumo")
    void registrarDirecto_debeBloquearElLoteAntesDeLeerLaEtapa() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = ConsumoInsumoFormDTO.builder().idInsumo(1L).idLoteInsumo(10L).cantidadConsumida(1.0).build();

        // === EJECUCION ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        // === ASSERTS ===
        InOrder orden = inOrder(etapaLoteRepository, loteRepository);
        orden.verify(etapaLoteRepository).buscarIdLoteDeLaEtapa(1L);
        orden.verify(loteRepository).buscarPorIdParaActualizar(ID_LOTE_BLOQUEADO);
        orden.verify(etapaLoteRepository).findById(1L);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-BLC-03: anularConsumoInsumo bloquea el lote del consumo antes de leer el consumo")
    void anularConsumoInsumo_debeBloquearElLoteAntesDeLeerElConsumo() {
        // === PREPARACION DE DATOS ===
        when(consumoInsumoRepository.findById(7L)).thenReturn(Optional.empty());
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        // === ASSERTS ===
        InOrder orden = inOrder(consumoInsumoRepository, loteRepository);
        orden.verify(consumoInsumoRepository).buscarIdLoteDelConsumo(7L);
        orden.verify(loteRepository).buscarPorIdParaActualizar(ID_LOTE_BLOQUEADO);
        orden.verify(consumoInsumoRepository).findById(7L);
    }

    @Test
    @DisplayName("CP-BLC-04: registrarConsumoInsumoReservado lanza RecursoNoEncontradoException sin bloquear nada cuando la etapa no existe")
    void registrarReservado_debeRechazarEtapaInexistenteSinBloquear() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.buscarIdLoteDeLaEtapa(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = ConsumoInsumoFormDTO.builder().idInsumo(1L).idLoteInsumo(10L).cantidadConsumida(1.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(loteRepository, loteInsumoRepository);
    }

    // ==================== listarConsumosDelLote ====================

    @Test
    @DisplayName("CP-LCL-01: listarConsumosDelLote retorna, en una sola consulta, todos los consumos del lote (registrados y anulados) mapeados a DTO y con el usuario que los registró")
    void listarConsumosDelLote_debeRetornarTodosLosConsumosMapeados() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity registrado = crearConsumoBase(1L, 5.0);
        registrado.setCreatedBy("operario");
        ConsumoInsumoEntity anulado = crearConsumoBase(2L, 3.0);
        anulado.setEstado(EstadoTransaccion.ANULADO);
        when(consumoInsumoRepository.listarDelLote(7L)).thenReturn(List.of(registrado, anulado));

        // === EJECUCION ===
        List<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.listarConsumosDelLote(7L);

        // === ASSERTS ===
        assertThat(resultado).extracting(ConsumoInsumoResponseDTO::getCantidadConsumida).containsExactly(5.0, 3.0);
        assertThat(resultado).extracting(ConsumoInsumoResponseDTO::getEstado).containsExactly(EstadoTransaccion.REGISTRADO, EstadoTransaccion.ANULADO);
        assertThat(resultado.get(0).getUsuarioRegistro()).isEqualTo("operario");
        verify(consumoInsumoRepository).listarDelLote(7L);
    }

    @Test
    @DisplayName("CP-LCL-02: listarConsumosDelLote retorna una lista vacía cuando el lote no tiene consumos")
    void listarConsumosDelLote_debeRetornarListaVaciaSinConsumos() {
        // === PREPARACION DE DATOS ===
        when(consumoInsumoRepository.listarDelLote(7L)).thenReturn(List.of());

        // === EJECUCION + ASSERTS ===
        assertThat(consumoInsumoServicio.listarConsumosDelLote(7L)).isEmpty();
    }

    // ==================== filtrarConsumosInsumo ====================

    @Test
    @DisplayName("CP-FCI-01: filtrarConsumosInsumo con los 3 criterios opcionales informados retorna una página mapeada")
    void filtrarConsumosInsumo_debeRetornarPaginaMapeadaConCriteriosInformados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ConsumoInsumoEntity consumo1 = crearConsumoBase(1L, 5.0);
        ConsumoInsumoEntity consumo2 = crearConsumoBase(2L, 3.0);
        when(consumoInsumoRepository.filtrarConsumosInsumo(1L, TipoConsumo.RESERVADO, 7L, EstadoTransaccion.REGISTRADO, pageable))
                .thenReturn(new PageImpl<>(List.of(consumo1, consumo2), pageable, 2));

        // === EJECUCION ===
        Page<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.filtrarConsumosInsumo(1L, TipoConsumo.RESERVADO, 7L, EstadoTransaccion.REGISTRADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        verify(consumoInsumoRepository).filtrarConsumosInsumo(1L, TipoConsumo.RESERVADO, 7L, EstadoTransaccion.REGISTRADO, pageable);
    }

    @Test
    @DisplayName("CP-FCI-02: filtrarConsumosInsumo con estado nulo no asume REGISTRADO por defecto, lo propaga tal cual junto con tipoConsumo e idInsumo nulos")
    void filtrarConsumosInsumo_debePropagarEstadoNuloSinDefault() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ConsumoInsumoEntity registrado = crearConsumoBase(1L, 5.0);
        ConsumoInsumoEntity anulado = crearConsumoBase(2L, 3.0);
        anulado.setEstado(EstadoTransaccion.ANULADO);
        when(consumoInsumoRepository.filtrarConsumosInsumo(1L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado), pageable, 2));

        // === EJECUCION ===
        Page<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.filtrarConsumosInsumo(1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(consumoInsumoRepository).filtrarConsumosInsumo(1L, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FCI-03: filtrarConsumosInsumo permite al usuario elegir explícitamente ver los consumos ANULADOs")
    void filtrarConsumosInsumo_debePermitirElegirAnulados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ConsumoInsumoEntity consumoAnulado = crearConsumoBase(1L, 5.0);
        consumoAnulado.setEstado(EstadoTransaccion.ANULADO);
        when(consumoInsumoRepository.filtrarConsumosInsumo(1L, null, null, EstadoTransaccion.ANULADO, pageable))
                .thenReturn(new PageImpl<>(List.of(consumoAnulado), pageable, 1));

        // === EJECUCION ===
        Page<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.filtrarConsumosInsumo(1L, null, null, EstadoTransaccion.ANULADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(consumoInsumoRepository).filtrarConsumosInsumo(1L, null, null, EstadoTransaccion.ANULADO, pageable);
    }

    @Test
    @DisplayName("CP-FCI-04: filtrarConsumosInsumo lanza ReglaNegocioException y no consulta nada cuando la etapa de lote es nula")
    void filtrarConsumosInsumo_debeRechazarEtapaLoteNula() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.filtrarConsumosInsumo(null, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa del lote es obligatoria");
        verifyNoInteractions(consumoInsumoRepository);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del consumo de insumo cuando el ID existe")
    void buscarPorId_debeRetornarConsumoExistente() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(5.0);
        assertThat(resultado.getAdvertencias()).isEmpty();
        verify(consumoInsumoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(consumoInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el consumo de insumo con ID: 99");
        verify(consumoInsumoRepository).findById(99L);
    }

    // ==================== registrarConsumoInsumoReservado ====================

    @Test
    @DisplayName("CP-CR-01: registrarConsumoInsumoReservado lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void registrarReservado_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-02: registrarConsumoInsumoReservado lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void registrarReservado_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.PENDIENTE);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-03: registrarConsumoInsumoReservado lanza ReglaNegocioException si la etapa no está EN_CURSO")
    void registrarReservado_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.PENDIENTE, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para poder registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-04: registrarConsumoInsumoReservado lanza ReglaNegocioException si el tipo de etapa no permite registrar consumo (Envasado)")
    void registrarReservado_debeLanzarExcepcionSiTipoEtapaNoPermiteConsumo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Envasado no admite el registro de consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-05: registrarConsumoInsumoReservado lanza RecursoNoEncontradoException si el lote de insumo no existe")
    void registrarReservado_debeLanzarExcepcionSiLoteInsumoNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(99L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-06: registrarConsumoInsumoReservado lanza ReglaNegocioException si el insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta")
    void registrarReservado_debeRechazarInsumoNoRequeridoPorLaEtapa() {
        // === PREPARACION DE DATOS ===
        // La receta no tiene ningún detalle de malta que use esta malta en esta etapa
        MaltaEntity maltaNoUsada = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(maltaEntity(2L)));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, maltaNoUsada, 10.0, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-07: registrarConsumoInsumoReservado lanza ReglaNegocioException si no existe una reserva de ese lote de insumo para esa etapa")
    void registrarReservado_debeLanzarExcepcionSiNoExisteReserva() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No existe una reserva de este lote de insumo para esta etapa: utilice el registro de consumo directo");
        verifyNoInteractions(consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-08: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida es nula")
    void registrarReservado_debeRechazarCantidadNula() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida es obligatoria");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-09: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida es cero (límite)")
    void registrarReservado_debeRechazarCantidadEnCero() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 0.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a 0");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-10: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida es negativa")
    void registrarReservado_debeRechazarCantidadNegativa() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, -3.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a 0");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-11: registrarConsumoInsumoReservado lanza ReglaNegocioException si la cantidad consumida supera la cantidad reservada")
    void registrarReservado_debeRechazarCantidadQueSuperaLaReservada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 6.5);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad reservada del lote de insumo (6 kg)");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-12: registrarConsumoInsumoReservado permite una cantidad igual a la reservada (límite): la reserva queda en 0")
    void registrarReservado_debePermitirCantidadIgualALaReservada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 6.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO);

        // === ASSERTS ===
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(6.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(6.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.CONSUMIDA);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(4.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(reservaInsumoRepository).save(reserva);
        verify(loteInsumoRepository).save(loteInsumo);
    }

    @Test
    @DisplayName("CP-CR-13: registrarConsumoInsumoReservado — camino feliz con cantidad parcial: descuenta reserva, lote de insumo, y congela el costo PPP vigente")
    void registrarReservado_debeRegistrarCorrectamenteConCantidadParcial() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        loteInsumo.setCostoUnitarioPPP(BigDecimal.valueOf(12.5));
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> {
            ConsumoInsumoEntity guardado = inv.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 4.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO);

        // === ASSERTS ===
        assertThat(reserva.getCantidadPendiente()).isEqualTo(2.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(4.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(6.0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(0.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(2.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(4.0);
        assertThat(resultado.getCostoUnitarioPPP()).isEqualByComparingTo(BigDecimal.valueOf(12.5));
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CR-14: registrarConsumoInsumoReservado lanza ReglaNegocioException si el lote está FINALIZADO (lote inmutable)")
    void registrarReservado_debeLanzarExcepcionSiLoteFinalizado() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.FINALIZADO);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-15: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si no se informó el lote de insumo")
    void registrarReservado_debeRechazarLoteInsumoNulo() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(null, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo es obligatorio");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-16: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida es NaN o infinita")
    void registrarReservado_debeRechazarCantidadNoFinita() {
        // === EJECUCION Y ASSERTS ===
        for (double noFinita : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, noFinita)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cantidad consumida debe ser mayor a 0");
        }
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-17: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida supera el máximo técnico")
    void registrarReservado_debeRechazarCantidadQueSuperaElMaximoTecnico() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 1_000_000.5)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar 1.000.000 kg o g, según el insumo");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-18: registrarConsumoInsumoReservado lanza ReglaNegocioException sin consultar nada si la cantidad consumida redondeada a 4 decimales es cero")
    void registrarReservado_debeRechazarCantidadQueSeRedondeaACero() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 0.00001)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a 0");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-19: registrarConsumoInsumoReservado permite consumir una cantidad con residuo decimal igual a la reservada (0,1 + 0,2 contra 0,3) y deja la reserva y el lote de insumo en 0")
    void registrarReservado_debePermitirCantidadConResiduoDecimalIgualALaReservada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 0.3, 0.3);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 0.3);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        double cantidadConResiduo = 0.1 + 0.2;

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, cantidadConResiduo));

        // === ASSERTS ===
        assertThat(cantidadConResiduo).isNotEqualTo(0.3);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(0.3);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-CR-20: registrarConsumoInsumoReservado muestra en el mensaje la cantidad reservada formateada, sin residuos decimales")
    void registrarReservado_debeMostrarLaCantidadReservadaFormateada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.30000000000000004);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 0.30000000000000004);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 0.5)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad reservada del lote de insumo (0,3 kg)");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-21: registrarConsumoInsumoReservado reconoce la malta requerida por ID cuando el insumo del lote de insumo es un proxy que no es instancia de MaltaEntity")
    void registrarReservado_debeReconocerElInsumoPorIdCuandoEsUnProxy() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        InsumoEntity insumoProxy = insumoProxy(malta);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, insumoProxy, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(insumoProxy).isNotInstanceOf(MaltaEntity.class);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        verify(loteInsumoRepository).save(loteInsumo);
    }

    @Test
    @DisplayName("CP-CR-22: registrarConsumoInsumoReservado rechaza el insumo cuando su ID no figura en los detalles de la receta, aunque el insumo sea un proxy")
    void registrarReservado_debeRechazarElInsumoProxyCuyoIdNoEstaEnLaReceta() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, insumoProxy(maltaEntity(2L)), 10.0, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOConInsumo(2L, 1L, 4.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-23: registrarConsumoInsumoReservado lanza ReglaNegocioException si la etapa es Maceración, que no admite consumos de insumo")
    void registrarReservado_debeLanzarExcepcionSiLaEtapaEsMaceracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaMaceracion = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMaceracion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Maceración no admite el registro de consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-24: registrarConsumoInsumoReservado — la reserva de un lote de insumo vencido se mantiene: el consumo se registra con la advertencia del vencimiento")
    void registrarReservado_debeAdvertirElLoteDeInsumoVencido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        LocalDate vencimiento = LocalDate.now().minusDays(3);
        loteInsumo.setFechaVencimiento(vencimiento);
        ReservaInsumoEntity reserva = prepararConsumoReservado(malta, loteInsumo, 6.0, 100.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly("Este lote de insumo se encuentra vencido (venció el " + formatearFecha(vencimiento) + ")");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(2.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        verify(consumoInsumoRepository).save(any(ConsumoInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-CR-25: registrarConsumoInsumoReservado — el total consumido del insumo en la etapa (consumos registrados más este) supera lo requerido: advierte el exceso y no bloquea")
    void registrarReservado_debeAdvertirElExcesoSobreLoRequeridoSinBloquear() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        LoteInsumoEntity otroLoteInsumo = loteInsumoEntity(2L, malta, 5.0, 0.0);
        ConsumoInsumoEntity consumoPrevio = consumoInsumoEntity(otroLoteInsumo, 8.0, EstadoTransaccion.REGISTRADO);
        ReservaInsumoEntity reserva = prepararConsumoReservado(malta, loteInsumo, 6.0, 10.0, consumoPrevio);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly("El total consumido de Malta Pilsen en esta etapa (12 kg) supera la cantidad requerida (10 kg)");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(2.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        verify(consumoInsumoRepository).save(any(ConsumoInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-CR-26: registrarConsumoInsumoReservado — lote de insumo vencido y exceso a la vez: devuelve ambas advertencias, primero la del vencimiento")
    void registrarReservado_debeAdvertirElVencimientoYElExcesoALaVez() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        LocalDate vencimiento = LocalDate.now().minusDays(1);
        loteInsumo.setFechaVencimiento(vencimiento);
        prepararConsumoReservado(malta, loteInsumo, 6.0, 3.5);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly(
                "Este lote de insumo se encuentra vencido (venció el " + formatearFecha(vencimiento) + ")",
                "El total consumido de Malta Pilsen en esta etapa (4 kg) supera la cantidad requerida (3,5 kg)");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CR-27: registrarConsumoInsumoReservado — lote de insumo vigente y total consumido igual a lo requerido (límite): sin advertencias")
    void registrarReservado_noDebeAdvertirSiElTotalConsumidoIgualaLoRequerido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        LoteInsumoEntity otroLoteInsumo = loteInsumoEntity(2L, malta, 5.0, 0.0);
        ConsumoInsumoEntity consumoPrevio = consumoInsumoEntity(otroLoteInsumo, 6.0, EstadoTransaccion.REGISTRADO);
        prepararConsumoReservado(malta, loteInsumo, 6.0, 10.0, consumoPrevio);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).isEmpty();
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CR-28: registrarConsumoInsumoReservado — un lote de insumo que vence hoy todavía es válido (límite): sin advertencia de vencimiento")
    void registrarReservado_noDebeAdvertirUnLoteDeInsumoQueVenceHoy() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        loteInsumo.setFechaVencimiento(LocalDate.now());
        prepararConsumoReservado(malta, loteInsumo, 6.0, 100.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-CR-29: registrarConsumoInsumoReservado — el exceso solo suma los consumos del mismo insumo y compara con redondeo a 4 decimales: 0,1 + 0,2 contra 0,3 no advierte")
    void registrarReservado_elExcesoSoloSumaElMismoInsumoYRedondea() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LevaduraEntity levadura = levaduraEntity(2L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.2);
        LoteInsumoEntity loteInsumoMalta = loteInsumoEntity(3L, malta, 5.0, 0.0);
        LoteInsumoEntity loteInsumoLevadura = loteInsumoEntity(4L, levadura, 500.0, 0.0);
        ConsumoInsumoEntity consumoMalta = consumoInsumoEntity(loteInsumoMalta, 0.1, EstadoTransaccion.REGISTRADO);
        ConsumoInsumoEntity consumoLevadura = consumoInsumoEntity(loteInsumoLevadura, 50.0, EstadoTransaccion.REGISTRADO);
        prepararConsumoReservado(malta, loteInsumo, 0.2, 0.3, consumoMalta, consumoLevadura);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 0.2));

        // === ASSERTS ===
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
        assertThat(resultado.getAdvertencias()).isEmpty();
    }

    // ==================== registrarConsumoInsumoDirecto ====================

    @Test
    @DisplayName("CP-CD-01: registrarConsumoInsumoDirecto lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void registrarDirecto_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-02: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void registrarDirecto_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.FINALIZADO);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-03: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la etapa no está EN_CURSO")
    void registrarDirecto_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para poder registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-04: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el tipo de etapa no permite registrar consumo (Envasado)")
    void registrarDirecto_debeLanzarExcepcionSiTipoEtapaNoPermiteConsumo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Envasado no admite el registro de consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-05: registrarConsumoInsumoDirecto lanza RecursoNoEncontradoException si el lote de insumo no existe")
    void registrarDirecto_debeLanzarExcepcionSiLoteInsumoNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(99L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-06: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta")
    void registrarDirecto_debeRechazarInsumoNoRequeridoPorLaEtapa() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaNoUsada = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(maltaEntity(2L)));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, maltaNoUsada, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-07: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si la cantidad consumida es nula")
    void registrarDirecto_debeRechazarCantidadNula() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida es obligatoria");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-08: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si la cantidad consumida es cero (límite)")
    void registrarDirecto_debeRechazarCantidadEnCero() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 0.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a 0");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-09: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si la cantidad consumida es negativa")
    void registrarDirecto_debeRechazarCantidadNegativa() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, -2.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a 0");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-10: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la cantidad consumida supera la cantidad disponible")
    void registrarDirecto_debeRechazarCantidadQueSuperaLaDisponible() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 10.5);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad disponible del lote de insumo (10 kg)");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-11: registrarConsumoInsumoDirecto permite una cantidad igual a la disponible (límite)")
    void registrarDirecto_debePermitirCantidadIgualALaDisponible() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 10.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(loteInsumoRepository).save(loteInsumo);
    }

    @Test
    @DisplayName("CP-CD-12: registrarConsumoInsumoDirecto — camino feliz: permite consumir aunque todavía quede cantidad reservada de ese mismo insumo en esa misma etapa, descontando solo la cantidad actual sin tocar la reservada")
    void registrarDirecto_debeRegistrarCorrectamenteAunConReservaViva() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        // Este lote de insumo tiene 4.0 reservados para ESTA MISMA etapa (a través de una reserva
        // todavía activa) y 3.0 disponibles; el consumo directo no exige que esa reserva esté agotada
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 7.0, 4.0);
        loteInsumo.setCostoUnitarioPPP(BigDecimal.valueOf(8.25));
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> {
            ConsumoInsumoEntity guardado = inv.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 3.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(4.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(4.0);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(3.0);
        assertThat(resultado.getCostoUnitarioPPP()).isEqualByComparingTo(BigDecimal.valueOf(8.25));
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-13: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si no se informó el lote de insumo")
    void registrarDirecto_debeRechazarLoteInsumoNulo() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(null, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo es obligatorio");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-14: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si la cantidad consumida es NaN o infinita")
    void registrarDirecto_debeRechazarCantidadNoFinita() {
        // === EJECUCION Y ASSERTS ===
        for (double noFinita : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, noFinita)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La cantidad consumida debe ser mayor a 0");
        }
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-15: registrarConsumoInsumoDirecto lanza ReglaNegocioException sin consultar nada si la cantidad consumida supera el máximo técnico")
    void registrarDirecto_debeRechazarCantidadQueSuperaElMaximoTecnico() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 1_000_000.5)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar 1.000.000 kg o g, según el insumo");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-16: registrarConsumoInsumoDirecto con stock 0,3 permite tres consumos de 0,1 y deja el lote de insumo en 0")
    void registrarDirecto_debePermitirTresConsumosDeUnDecimoSobreUnStockDeTresDecimos() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 0.3, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 0.1));
        consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 0.1));
        ConsumoInsumoResponseDTO tercero = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 0.1));

        // === ASSERTS ===
        assertThat(tercero.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.0);
        assertThat(loteInsumo.getCantidadDisponible()).isEqualTo(0.0);
        verify(consumoInsumoRepository, times(3)).save(any(ConsumoInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-CD-17: registrarConsumoInsumoDirecto muestra en el mensaje la cantidad disponible formateada, sin residuos decimales")
    void registrarDirecto_debeMostrarLaCantidadDisponibleFormateada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 0.7, 0.4);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));

        // === EJECUCION Y ASSERTS ===
        assertThat(0.7 - 0.4).isNotEqualTo(0.3);
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 0.5)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad disponible del lote de insumo (0,3 kg)");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-18: registrarConsumoInsumoDirecto reconoce la levadura requerida por ID cuando el insumo del lote de insumo es un proxy que no es instancia de LevaduraEntity")
    void registrarDirecto_debeReconocerLaLevaduraPorIdCuandoEsUnProxy() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levadura = levaduraEntity(5L);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .detallesLevadura(List.of(DetalleLevaduraEntity.builder().levadura(levadura).cantidad(10.0).build()))
                .build();
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaFermentacion = crearEtapaLote(1L, TipoEtapa.FERMENTACION, EstadoEtapaLote.EN_CURSO, lote);
        InsumoEntity insumoProxy = insumoProxy(levadura);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, insumoProxy, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaFermentacion));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOConInsumo(5L, 1L, 4.0));

        // === ASSERTS ===
        assertThat(insumoProxy).isNotInstanceOf(LevaduraEntity.class);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("CP-CD-19: registrarConsumoInsumoDirecto rechaza la levadura en una etapa distinta de Fermentación, aunque el insumo sea un proxy")
    void registrarDirecto_debeRechazarLaLevaduraEnUnaEtapaDistintaDeFermentacion() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levadura = levaduraEntity(5L);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .detallesMalta(List.of(DetalleMaltaEntity.builder().malta(maltaEntity(1L)).cantidad(10.0).build()))
                .detallesLevadura(List.of(DetalleLevaduraEntity.builder().levadura(levadura).cantidad(10.0).build()))
                .build();
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaMolienda = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, insumoProxy(levadura), 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMolienda));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOConInsumo(5L, 1L, 4.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-20: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la etapa es Maceración, que no admite consumos de insumo")
    void registrarDirecto_debeLanzarExcepcionSiLaEtapaEsMaceracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaMaceracion = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMaceracion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa Maceración no admite el registro de consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-21: registrarConsumoInsumoDirecto — el consumo directo sobre un lote de insumo vencido se permite, con la advertencia del vencimiento")
    void registrarDirecto_debeAdvertirElLoteDeInsumoVencido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        LocalDate vencimiento = LocalDate.now().minusDays(10);
        loteInsumo.setFechaVencimiento(vencimiento);
        prepararConsumoDirecto(malta, loteInsumo, 100.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly("Este lote de insumo se encuentra vencido (venció el " + formatearFecha(vencimiento) + ")");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        verify(consumoInsumoRepository).save(any(ConsumoInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-CD-22: registrarConsumoInsumoDirecto — el total consumido del insumo en la etapa supera lo requerido: advierte el exceso y no bloquea")
    void registrarDirecto_debeAdvertirElExcesoSobreLoRequeridoSinBloquear() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        LoteInsumoEntity otroLoteInsumo = loteInsumoEntity(2L, malta, 5.0, 0.0);
        ConsumoInsumoEntity consumoPrevio = consumoInsumoEntity(otroLoteInsumo, 8.0, EstadoTransaccion.REGISTRADO);
        prepararConsumoDirecto(malta, loteInsumo, 10.0, consumoPrevio);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly("El total consumido de Malta Pilsen en esta etapa (12 kg) supera la cantidad requerida (10 kg)");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        verify(consumoInsumoRepository).save(any(ConsumoInsumoEntity.class));
    }

    @Test
    @DisplayName("CP-CD-23: registrarConsumoInsumoDirecto — lote de insumo vencido y exceso a la vez: devuelve ambas advertencias, primero la del vencimiento")
    void registrarDirecto_debeAdvertirElVencimientoYElExcesoALaVez() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        LocalDate vencimiento = LocalDate.now().minusDays(1);
        loteInsumo.setFechaVencimiento(vencimiento);
        prepararConsumoDirecto(malta, loteInsumo, 3.5);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).containsExactly(
                "Este lote de insumo se encuentra vencido (venció el " + formatearFecha(vencimiento) + ")",
                "El total consumido de Malta Pilsen en esta etapa (4 kg) supera la cantidad requerida (3,5 kg)");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CD-24: registrarConsumoInsumoDirecto — lote de insumo vigente y total consumido igual a lo requerido (límite): sin advertencias")
    void registrarDirecto_noDebeAdvertirSiElTotalConsumidoIgualaLoRequerido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        LoteInsumoEntity otroLoteInsumo = loteInsumoEntity(2L, malta, 5.0, 0.0);
        ConsumoInsumoEntity consumoPrevio = consumoInsumoEntity(otroLoteInsumo, 6.0, EstadoTransaccion.REGISTRADO);
        prepararConsumoDirecto(malta, loteInsumo, 10.0, consumoPrevio);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).isEmpty();
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CD-25: registrarConsumoInsumoDirecto — un lote de insumo que vence hoy todavía es válido (límite): sin advertencia de vencimiento")
    void registrarDirecto_noDebeAdvertirUnLoteDeInsumoQueVenceHoy() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        loteInsumo.setFechaVencimiento(LocalDate.now());
        prepararConsumoDirecto(malta, loteInsumo, 100.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 4.0));

        // === ASSERTS ===
        assertThat(resultado.getAdvertencias()).isEmpty();
    }

    @Test
    @DisplayName("CP-FCI-05: filtrarConsumosInsumo informa la fecha de registro (fecha de creación de la auditoría) de cada consumo")
    void filtrarConsumosInsumo_debeInformarLaFechaDeRegistro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        consumo.setCreatedDate(LocalDateTime.of(2027, 7, 1, 9, 15));
        when(consumoInsumoRepository.filtrarConsumosInsumo(1L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(consumo), pageable, 1));

        // === EJECUCION ===
        Page<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.filtrarConsumosInsumo(1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent().get(0).getFechaRegistro()).isEqualTo(LocalDateTime.of(2027, 7, 1, 9, 15));
    }

    // ==================== filtrarAdicionesLupulo ====================

    @Test
    @DisplayName("CP-FAL-01: filtrarAdicionesLupulo lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void filtrarAdicionesLupulo_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.filtrarAdicionesLupulo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(escaladoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FAL-02: filtrarAdicionesLupulo mapea cada adición del servicio de escalado, sin fusionar las del mismo lúpulo")
    void filtrarAdicionesLupulo_debeMapearCadaAdicion() {
        // === PREPARACION DE DATOS ===
        MaltaEntity insumo = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaHervido = crearEtapaLote(3L, TipoEtapa.HERVIDO, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(3L)).thenReturn(Optional.of(etapaHervido));
        when(escaladoInsumoServicio.calcularAdicionesLupulo(etapaHervido)).thenReturn(List.of(
                new AdicionLupulo(insumo, UsoLupulo.HERVOR, 60.0, 215.0),
                new AdicionLupulo(insumo, UsoLupulo.HERVOR, 15.0, 25.5),
                new AdicionLupulo(insumo, UsoLupulo.WHIRLPOOL, 0.0, 10.0)));

        // === EJECUCION ===
        List<AdicionLupuloResponseDTO> resultado = consumoInsumoServicio.filtrarAdicionesLupulo(3L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(3);
        assertThat(resultado).extracting(AdicionLupuloResponseDTO::getCantidad).containsExactly(215.0, 25.5, 10.0);
        assertThat(resultado).extracting(AdicionLupuloResponseDTO::getTiempoDeHervor).containsExactly(60.0, 15.0, 0.0);
        assertThat(resultado).extracting(AdicionLupuloResponseDTO::getUso).containsExactly(UsoLupulo.HERVOR, UsoLupulo.HERVOR, UsoLupulo.WHIRLPOOL);
        assertThat(resultado.get(0).getNombreInsumo()).isEqualTo(insumo.getNombre());
    }

    // ==================== filtrarInsumosRequeridos ====================

    @Test
    @DisplayName("CP-FIR-01: filtrarInsumosRequeridos lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void filtrarInsumosRequeridos_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.filtrarInsumosRequeridos(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(escaladoInsumoServicio, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-FIR-02: filtrarInsumosRequeridos — camino feliz: mapea los requerimientos de la etapa (ya acotados por el servicio de escalado) junto con su cantidad consumida (ya acotada por el repositorio a REGISTRADO)")
    void filtrarInsumosRequeridos_debeMapearLosRequerimientosConSuCantidadConsumida() {
        // === PREPARACION DE DATOS ===
        // El acotado por etapa (calcularRequerimientosEtapa) y por estado REGISTRADO
        // (findByEtapaLoteIdAndEstado) es responsabilidad de los colaboradores, no de este método:
        // aquí solo se verifica que el resultado de ambos se mapee y sume correctamente.
        MaltaEntity malta = maltaEntity(1L);
        LevaduraEntity levadura = levaduraEntity(2L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaMolienda = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMolienda));
        when(escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMolienda)).thenReturn(List.of(
                new RequerimientoInsumo(malta, etapaMolienda, 500.0),
                new RequerimientoInsumo(levadura, etapaMolienda, 10.0)));

        LoteInsumoEntity loteInsumoMalta1 = loteInsumoEntity(10L, malta, 300.0, 0.0);
        LoteInsumoEntity loteInsumoMalta2 = loteInsumoEntity(11L, malta, 300.0, 0.0);
        ConsumoInsumoEntity consumoMalta1 = consumoInsumoEntity(loteInsumoMalta1, 200.0, EstadoTransaccion.REGISTRADO);
        ConsumoInsumoEntity consumoMalta2 = consumoInsumoEntity(loteInsumoMalta2, 50.0, EstadoTransaccion.REGISTRADO);
        when(consumoInsumoRepository.findByEtapaLoteIdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(consumoMalta1, consumoMalta2));

        // === EJECUCION ===
        List<InsumoRequeridoResponseDTO> resultado = consumoInsumoServicio.filtrarInsumosRequeridos(1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        InsumoRequeridoResponseDTO dtoMalta = resultado.stream().filter(dto -> dto.getInsumo().getId().equals(1L)).findFirst().orElseThrow();
        InsumoRequeridoResponseDTO dtoLevadura = resultado.stream().filter(dto -> dto.getInsumo().getId().equals(2L)).findFirst().orElseThrow();
        assertThat(dtoMalta.getCantidadRequerida()).isEqualTo(500.0);
        assertThat(dtoMalta.getCantidadConsumida()).isEqualTo(250.0);
        assertThat(dtoLevadura.getCantidadRequerida()).isEqualTo(10.0);
        assertThat(dtoLevadura.getCantidadConsumida()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-FIR-03: filtrarInsumosRequeridos lanza ReglaNegocioException y no consulta nada cuando la etapa de lote es nula")
    void filtrarInsumosRequeridos_debeRechazarEtapaLoteNula() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.filtrarInsumosRequeridos(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa del lote es obligatoria");
        verifyNoInteractions(etapaLoteRepository, escaladoInsumoServicio, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-FIR-04: filtrarInsumosRequeridos informa por insumo el saldo reservado (una sola consulta agrupada), la unidad de medida y el porcentaje consumido")
    void filtrarInsumosRequeridos_debeInformarReservadoUnidadYPorcentajeConsumido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LevaduraEntity levadura = levaduraEntity(2L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapa = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(escaladoInsumoServicio.calcularRequerimientosEtapa(etapa)).thenReturn(List.of(
                new RequerimientoInsumo(malta, etapa, 500.0),
                new RequerimientoInsumo(levadura, etapa, 10.0)));
        LoteInsumoEntity loteInsumoMalta = loteInsumoEntity(10L, malta, 300.0, 0.0);
        when(consumoInsumoRepository.findByEtapaLoteIdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(
                consumoInsumoEntity(loteInsumoMalta, 200.0, EstadoTransaccion.REGISTRADO),
                consumoInsumoEntity(loteInsumoMalta, 50.0, EstadoTransaccion.REGISTRADO)));
        when(reservaInsumoRepository.sumarSaldoReservadoPorInsumoDeEtapa(1L)).thenReturn(List.<Object[]>of(new Object[]{1L, 250.00004}));

        // === EJECUCION ===
        List<InsumoRequeridoResponseDTO> resultado = consumoInsumoServicio.filtrarInsumosRequeridos(1L);

        // === ASSERTS ===
        InsumoRequeridoResponseDTO dtoMalta = resultado.stream().filter(dto -> dto.getInsumo().getId().equals(1L)).findFirst().orElseThrow();
        InsumoRequeridoResponseDTO dtoLevadura = resultado.stream().filter(dto -> dto.getInsumo().getId().equals(2L)).findFirst().orElseThrow();
        assertThat(dtoMalta.getCantidadReservada()).isEqualTo(250.0);
        assertThat(dtoMalta.getUnidadDeMedida()).isEqualTo(malta.getUnidadDeMedida());
        assertThat(dtoMalta.getPorcentajeConsumido()).isEqualTo(50.0);
        assertThat(dtoLevadura.getCantidadReservada()).isEqualTo(0.0);
        assertThat(dtoLevadura.getPorcentajeConsumido()).isEqualTo(0.0);
        verify(reservaInsumoRepository, times(1)).sumarSaldoReservadoPorInsumoDeEtapa(1L);
    }

    @Test
    @DisplayName("CP-FIR-05: filtrarInsumosRequeridos redondea el porcentaje consumido a 4 decimales, no lo topa en 100 y devuelve 0 si lo requerido es 0")
    void filtrarInsumosRequeridos_debeRedondearElPorcentajeSinTopeYManejarRequeridoCero() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LevaduraEntity levadura = levaduraEntity(2L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapa = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(escaladoInsumoServicio.calcularRequerimientosEtapa(etapa)).thenReturn(List.of(
                new RequerimientoInsumo(malta, etapa, 3.0),
                new RequerimientoInsumo(levadura, etapa, 0.0)));
        when(consumoInsumoRepository.findByEtapaLoteIdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(
                consumoInsumoEntity(loteInsumoEntity(10L, malta, 300.0, 0.0), 4.0, EstadoTransaccion.REGISTRADO)));

        // === EJECUCION ===
        List<InsumoRequeridoResponseDTO> resultado = consumoInsumoServicio.filtrarInsumosRequeridos(1L);

        // === ASSERTS ===
        assertThat(resultado.get(0).getPorcentajeConsumido()).isEqualTo(133.3333);
        assertThat(resultado.get(1).getPorcentajeConsumido()).isEqualTo(0.0);
    }

    // ==================== insumo elegido y etapa sin insumos ====================

    @Test
    @DisplayName("CP-CR-30: registrarConsumoInsumoReservado lanza ReglaNegocioException y no consulta nada si no se informó el insumo")
    void registrarReservado_debeRechazarSinInsumoElegido() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = ConsumoInsumoFormDTO.builder().idLoteInsumo(1L).cantidadConsumida(5.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El insumo es obligatorio");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-31: registrarConsumoInsumoReservado lanza ReglaNegocioException si la etapa no consume insumos según la receta (Maduración sin lúpulo de maduración)")
    void registrarReservado_debeRechazarUnaEtapaSinInsumos() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaMaduracion = crearEtapaLote(1L, TipoEtapa.MADURACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMaduracion));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, malta, 10.0, 6.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Esta etapa no consume insumos");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-32: registrarConsumoInsumoReservado lanza ReglaNegocioException si el lote de insumo no es del insumo elegido")
    void registrarReservado_debeRechazarUnLoteQueNoEsDelInsumoElegido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, malta, 10.0, 6.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOConInsumo(2L, 1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde al insumo elegido");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-26: registrarConsumoInsumoDirecto lanza ReglaNegocioException y no consulta nada si no se informó el insumo")
    void registrarDirecto_debeRechazarSinInsumoElegido() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoFormDTO formDTO = ConsumoInsumoFormDTO.builder().idLoteInsumo(1L).cantidadConsumida(5.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El insumo es obligatorio");
        verifyNoInteractions(etapaLoteRepository, loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-27: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la etapa no consume insumos según la receta (Maduración sin lúpulo de maduración)")
    void registrarDirecto_debeRechazarUnaEtapaSinInsumos() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaMaduracion = crearEtapaLote(1L, TipoEtapa.MADURACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMaduracion));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, malta, 10.0, 0.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Esta etapa no consume insumos");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-28: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el lote de insumo no es del insumo elegido")
    void registrarDirecto_debeRechazarUnLoteQueNoEsDelInsumoElegido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, malta, 10.0, 0.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOConInsumo(2L, 1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde al insumo elegido");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-29: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el insumo elegido, aunque sea el del lote, no lo requiere la etapa según la receta")
    void registrarDirecto_debeRechazarUnInsumoElegidoQueLaEtapaNoRequiere() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaRequerida = maltaEntity(1L);
        MaltaEntity maltaElegida = maltaEntity(2L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(maltaRequerida));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, maltaElegida, 10.0, 0.0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOConInsumo(2L, 1L, 5.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(consumoInsumoRepository);
    }

    // ==================== anularConsumoInsumo ====================

    @Test
    @DisplayName("CP-AC-01: anularConsumoInsumo lanza ReglaNegocioException si el motivo de anulación es nulo")
    void anularConsumoInsumo_debeLanzarExcepcionSiMotivoEsNulo() {
        // === PREPARACION DE DATOS ===
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion(null).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");
        verifyNoInteractions(consumoInsumoRepository, loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-02: anularConsumoInsumo lanza ReglaNegocioException si el motivo de anulación está vacío")
    void anularConsumoInsumo_debeLanzarExcepcionSiMotivoEstaVacio() {
        // === PREPARACION DE DATOS ===
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("   ").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");
        verifyNoInteractions(consumoInsumoRepository, loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-11: anularConsumoInsumo lanza ReglaNegocioException si el motivo de anulación tiene 256 caracteres")
    void anularConsumoInsumo_debeLanzarExcepcionSiMotivoDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("a".repeat(256)).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres");
        verifyNoInteractions(consumoInsumoRepository, loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-03: anularConsumoInsumo lanza RecursoNoEncontradoException si el consumo no existe")
    void anularConsumoInsumo_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(consumoInsumoRepository.findById(99L)).thenReturn(Optional.empty());
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el consumo de insumo con ID: 99");
        verify(consumoInsumoRepository, never()).save(any());
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-04: anularConsumoInsumo lanza ReglaNegocioException si el consumo ya está ANULADO")
    void anularConsumoInsumo_debeLanzarExcepcionSiYaEstaAnulado() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        consumo.setEstado(EstadoTransaccion.ANULADO);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular consumos de insumo en estado Registrado");
        verify(consumoInsumoRepository, never()).save(any());
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-05: anularConsumoInsumo lanza ReglaNegocioException si el lote asociado no está EN_EJECUCION")
    void anularConsumoInsumo_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        consumo.getEtapaLote().getLote().setEstado(EstadoLote.FINALIZADO);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe encontrarse en estado En ejecución para poder anular un consumo de insumo");
        verify(consumoInsumoRepository, never()).save(any());
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-06: anularConsumoInsumo lanza ReglaNegocioException si la etapa no está EN_CURSO")
    void anularConsumoInsumo_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        consumo.getEtapaLote().setEstado(EstadoEtapaLote.FINALIZADA);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para poder anular un consumo de insumo");
        verify(consumoInsumoRepository, never()).save(any());
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-07: anularConsumoInsumo lanza RecursoNoEncontradoException si el lote de insumo ya no existe")
    void anularConsumoInsumo_debeLanzarExcepcionSiLoteInsumoNoExiste() {
        // === PREPARACION DE DATOS ===
        ConsumoInsumoEntity consumo = crearConsumoBase(1L, 5.0);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(consumo.getLoteInsumo().getId())).thenReturn(Optional.empty());
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: " + consumo.getLoteInsumo().getId());
        verify(consumoInsumoRepository, never()).save(any());
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-08: anularConsumoInsumo (RESERVADO) lanza RecursoNoEncontradoException si ya no existe la reserva puntual de esa etapa para ese lote de insumo")
    void anularConsumoInsumo_debeLanzarExcepcionSiNoExisteLaReservaParaUnConsumoReservado() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 6.0, 2.0);
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 4.0, TipoConsumo.RESERVADO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 10L)).thenReturn(Optional.empty());
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.anularConsumoInsumo(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la reserva de insumo de esta etapa para este lote de insumo");
        verify(loteInsumoRepository, never()).save(any());
        verify(consumoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-09: anularConsumoInsumo (RESERVADO) — camino feliz: devuelve la cantidad a la reserva puntual y a la cantidad actual y reservada del lote de insumo")
    void anularConsumoInsumo_debeAnularCorrectamenteUnConsumoReservado() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 6.0, 2.0);
        // Se reservaron 6, se consumieron 4 y quedan 2 pendientes
        ReservaInsumoEntity reserva = reservaInsumoEntity(100L, etapaLote, loteInsumo, 6.0, 4.0, 2.0, EstadoReservaInsumo.ACTIVA);
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 4.0, TipoConsumo.RESERVADO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 10L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Registrado por error").build();
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.anularConsumoInsumo(1L, formDTO);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(6.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(6.0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(10.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(6.0);
        assertThat(consumo.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(consumo.getFechaAnulacion()).isNotNull().isBetween(antes, despues);
        assertThat(consumo.getMotivoAnulacion()).isEqualTo("Registrado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Registrado por error");
        verify(reservaInsumoRepository).save(reserva);
        verify(loteInsumoRepository).save(loteInsumo);
    }

    @Test
    @DisplayName("CP-AC-10: anularConsumoInsumo (DIRECTO) — camino feliz: devuelve la cantidad únicamente a la cantidad actual del lote de insumo, sin interactuar con reservas")
    void anularConsumoInsumo_debeAnularCorrectamenteUnConsumoDirecto() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 6.0, 0.0);
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 4.0, TipoConsumo.DIRECTO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        AnularConsumoInsumoFormDTO formDTO = AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Registrado por error").build();

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.anularConsumoInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(10.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        assertThat(consumo.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(loteInsumoRepository).save(loteInsumo);
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-AC-12: anularConsumoInsumo (RESERVADO) redondea a 4 decimales lo que devuelve a la reserva y al lote de insumo")
    void anularConsumoInsumo_debeRedondearLoQueDevuelveALaReservaYAlLoteDeInsumo() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 0.1, 0.1);
        ReservaInsumoEntity reserva = reservaInsumoEntity(100L, etapaLote, loteInsumo, 0.1);
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 0.2, TipoConsumo.RESERVADO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 10L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        consumoInsumoServicio.anularConsumoInsumo(1L, AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Registrado por error").build());

        // === ASSERTS ===
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(0.3);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-AC-13: anularConsumoInsumo (RESERVADO) sobre una reserva CONSUMIDA la devuelve a ACTIVA con el saldo pendiente restituido")
    void anularConsumoInsumo_debeReactivarUnaReservaConsumida() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 4.0, 0.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(100L, etapaLote, loteInsumo, 6.0, 6.0, 0.0, EstadoReservaInsumo.CONSUMIDA);
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 6.0, TipoConsumo.RESERVADO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 10L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        consumoInsumoServicio.anularConsumoInsumo(1L, AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Registrado por error").build());

        // === ASSERTS ===
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(6.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(6.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("CP-AC-14: anularConsumoInsumo (RESERVADO) sobre una reserva LIBERADA por una merma la devuelve a ACTIVA y conserva lo reducido por ajuste")
    void anularConsumoInsumo_debeReactivarUnaReservaLiberadaPorMerma() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 6.0, 0.0);
        // Se reservaron 10, se consumieron 6 y una merma quitó los 4 pendientes: la reserva quedó Liberada
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).etapaLote(etapaLote).loteInsumo(loteInsumo)
                .cantidadReservada(10.0).cantidadConsumida(6.0).cantidadReducidaPorAjuste(4.0).cantidadPendiente(0.0)
                .estado(EstadoReservaInsumo.LIBERADA).build();
        ConsumoInsumoEntity consumo = crearConsumoParaAnular(1L, 6.0, TipoConsumo.RESERVADO, loteInsumo, etapaLote);
        when(consumoInsumoRepository.findById(1L)).thenReturn(Optional.of(consumo));
        when(loteInsumoRepository.buscarPorIdParaConsumir(10L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 10L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        consumoInsumoServicio.anularConsumoInsumo(1L, AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Registrado por error").build());

        // === ASSERTS ===
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        assertThat(reserva.getCantidadPendiente()).isEqualTo(6.0);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0.0);
        assertThat(reserva.getCantidadReducidaPorAjuste()).isEqualTo(4.0);
        assertThat(reserva.getCantidadReservada()).isEqualTo(10.0);
    }

    // ==================== helpers de construcción ====================

    /**
     * Prepara el registro de un consumo reservado de malta en una etapa de Molienda en curso: devuelve la reserva y
     * deja stubbeados los colaboradores, incluidos lo requerido de la malta en la etapa y los consumos ya registrados.
     */
    private ReservaInsumoEntity prepararConsumoReservado(MaltaEntity malta, LoteInsumoEntity loteInsumo, double cantidadReservada, double cantidadRequerida, ConsumoInsumoEntity... consumosPrevios) {
        EtapaLoteEntity etapaLote = prepararEtapaConMaltaRequerida(malta, loteInsumo, cantidadRequerida, consumosPrevios);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, cantidadReservada);
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, loteInsumo.getId())).thenReturn(Optional.of(reserva));
        return reserva;
    }

    /**
     * Prepara el registro de un consumo directo de malta en una etapa de Molienda en curso, con los colaboradores
     * stubbeados, incluidos lo requerido de la malta en la etapa y los consumos ya registrados.
     */
    private void prepararConsumoDirecto(MaltaEntity malta, LoteInsumoEntity loteInsumo, double cantidadRequerida, ConsumoInsumoEntity... consumosPrevios) {
        prepararEtapaConMaltaRequerida(malta, loteInsumo, cantidadRequerida, consumosPrevios);
    }

    private EtapaLoteEntity prepararEtapaConMaltaRequerida(MaltaEntity malta, LoteInsumoEntity loteInsumo, double cantidadRequerida, ConsumoInsumoEntity... consumosPrevios) {
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(loteInsumo.getId())).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escaladoInsumoServicio.calcularRequerimientosEtapa(etapaLote)).thenReturn(List.of(new RequerimientoInsumo(malta, etapaLote, cantidadRequerida)));
        when(consumoInsumoRepository.findByEtapaLoteIdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(consumosPrevios));
        return etapaLote;
    }

    private static String formatearFecha(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static ConsumoInsumoFormDTO formDTOBase(Long idLoteInsumo, Double cantidadConsumida) {
        return ConsumoInsumoFormDTO.builder()
                .idInsumo(1L)
                .idLoteInsumo(idLoteInsumo)
                .cantidadConsumida(cantidadConsumida)
                .build();
    }

    private static ConsumoInsumoFormDTO formDTOConInsumo(Long idInsumo, Long idLoteInsumo, Double cantidadConsumida) {
        return ConsumoInsumoFormDTO.builder()
                .idInsumo(idInsumo)
                .idLoteInsumo(idLoteInsumo)
                .cantidadConsumida(cantidadConsumida)
                .build();
    }

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
    }

    private static LevaduraEntity levaduraEntity(Long id) {
        return LevaduraEntity.builder().id(id).nombre("Levadura Ale").unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).build();
    }

    /**
     * Simula el proxy perezoso de Hibernate que llega desde {@code LoteInsumoEntity.getInsumo()}: una instancia
     * de la clase abstracta {@code InsumoEntity} que no es instancia de ninguna subclase concreta, que responde su
     * ID sin inicializarse y que delega el resto en el insumo real al desenvolverse.
     */
    private static InsumoEntity insumoProxy(InsumoEntity real) {
        InsumoEntity proxy = mock(InsumoEntity.class, withSettings().extraInterfaces(HibernateProxy.class));
        HibernateProxy proxyHibernate = (HibernateProxy) proxy;
        LazyInitializer inicializador = mock(LazyInitializer.class);
        lenient().when(proxy.getId()).thenReturn(real.getId());
        lenient().when(proxyHibernate.asHibernateProxy()).thenReturn(proxyHibernate);
        lenient().when(proxyHibernate.getHibernateLazyInitializer()).thenReturn(inicializador);
        lenient().when(inicializador.getImplementation()).thenReturn(real);
        return proxy;
    }

    private static ConsumoInsumoEntity consumoInsumoEntity(LoteInsumoEntity loteInsumo, double cantidadConsumida, EstadoTransaccion estado) {
        return ConsumoInsumoEntity.builder()
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(BigDecimal.TEN)
                .estado(estado)
                .loteInsumo(loteInsumo)
                .build();
    }

    private static VersionRecetaEntity crearVersionRecetaConMalta(MaltaEntity malta) {
        return VersionRecetaEntity.builder()
                .detallesMalta(List.of(DetalleMaltaEntity.builder().malta(malta).cantidad(10.0).build()))
                .build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, InsumoEntity insumo, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor("LOTE-" + id)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }

    private static ReservaInsumoEntity reservaInsumoEntity(Long id, EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo,
                                                           double original, double consumida, double pendiente, EstadoReservaInsumo estado) {
        return ReservaInsumoEntity.builder()
                .id(id)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .cantidadReservada(original)
                .cantidadConsumida(consumida)
                .cantidadPendiente(pendiente)
                .estado(estado)
                .build();
    }

    private static ReservaInsumoEntity reservaInsumoEntity(Long id, EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo, double cantidadReservada) {
        return ReservaInsumoEntity.builder()
                .id(id)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .cantidadReservada(cantidadReservada).cantidadPendiente(cantidadReservada)
                .build();
    }

    private static LoteEntity crearLote(EstadoLote estado) {
        return LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-1")
                .volumenObjetivo(20.0)
                .estado(estado)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(20))
                .build();
    }

    private static LoteEntity crearLote(EstadoLote estado, VersionRecetaEntity versionReceta) {
        PlanificacionProduccionEntity planificacionProduccion = PlanificacionProduccionEntity.builder()
                .versionReceta(versionReceta)
                .build();
        return LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-1")
                .volumenObjetivo(20.0)
                .estado(estado)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(20))
                .planificacionProduccion(planificacionProduccion)
                .build();
    }

    @Test
    @DisplayName("CP-CR-33: registrarConsumoInsumoReservado rechaza una malta en la Molienda cuya mezcla de agua y grano (100000 kg) desborda el macerador de la instantánea de escalado")
    void registrarReservado_debeRechazarMaltaQueDesbordaElMacerador() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        prepararMaceradorDeLaInstantanea(lote, 5.0, 100.0);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 200000.0, 200000.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 100000.0);

        // === EJECUCION Y ASSERTS ===
        // Mezcla = 100000 * 3 + 5 + 100000 * 0,7 = 370005 L > 100 L
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageStartingWith("La malta molida (")
                .hasMessageContaining(" kg en total) requiere una mezcla de ")
                .hasMessageEndingWith(" L que supera la capacidad útil del macerador (100 L)");
        verify(loteInsumoRepository, never()).save(any());
        verify(consumoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-34: registrarConsumoInsumoReservado suma la malta ya molida (consumos registrados) al consumo nuevo para validar el macerador, y permite la malta que entra")
    void registrarReservado_debeSumarLaMaltaYaMolidaParaValidarElMacerador() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        prepararMaceradorDeLaInstantanea(lote, 5.0, 100.0);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 100.0, 100.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 100.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.findByEtapaLoteIdAndEstado(1L, EstadoTransaccion.REGISTRADO)).thenReturn(List.of(crearConsumoBase(1L, 25.0)));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        // 25 + 10 = 35 kg: mezcla = 35 * 3 + 5 + 35 * 0,7 = 134,5 L > 100 L; 25 + 5 = 30 kg: 90 + 5 + 21 = 116 L > 100 L; 25 + 1 = 26 kg: 78 + 5 + 18,2 = 101,2 L > 100 L
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 10.0)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("requiere una mezcla de ");
        // 25 + 0,5 = 25,5 kg: 76,5 + 5 + 17,85 = 99,35 L <= 100 L
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(1L, formDTOBase(1L, 0.5));
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-CD-30: registrarConsumoInsumoDirecto rechaza una malta en la Molienda cuya mezcla de agua y grano (100000 kg) desborda el macerador de la instantánea de escalado")
    void registrarDirecto_debeRechazarMaltaQueDesbordaElMacerador() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        prepararMaceradorDeLaInstantanea(lote, 5.0, 100.0);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 200000.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 100000.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageStartingWith("La malta molida (")
                .hasMessageEndingWith(" L que supera la capacidad útil del macerador (100 L)");
        verify(loteInsumoRepository, never()).save(any());
        verify(consumoInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-31: registrarConsumoInsumoDirecto no valida el macerador cuando la malta cabe en él")
    void registrarDirecto_debePermitirMaltaQueCabeEnElMacerador() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        prepararMaceradorDeLaInstantanea(lote, 5.0, 100.0);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 50.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        // 10 kg: mezcla = 30 + 5 + 7 = 42 L <= 100 L
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(1L, formDTOBase(1L, 10.0));

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    private static void prepararMaceradorDeLaInstantanea(LoteEntity lote, double espacioMuerto, double capacidadUtil) {
        lote.getPlanificacionProduccion().getVersionReceta().setRelacionDeEmpaste(3.0);
        lote.setEscaladoLote(EscaladoLoteEntity.builder().espacioMuertoMacerador(espacioMuerto).capacidadUtilMacerador(capacidadUtil).build());
    }

    private static EtapaLoteEntity crearEtapaLote(Long id, TipoEtapa tipo, EstadoEtapaLote estado, LoteEntity lote) {
        return EtapaLoteEntity.builder()
                .id(id)
                .etapa(tipo)
                .estado(estado)
                .lote(lote)
                .build();
    }

    private static ConsumoInsumoEntity crearConsumoParaAnular(Long id, double cantidadConsumida, TipoConsumo tipoConsumo, LoteInsumoEntity loteInsumo, EtapaLoteEntity etapaLote) {
        return ConsumoInsumoEntity.builder()
                .id(id)
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(BigDecimal.TEN)
                .estado(EstadoTransaccion.REGISTRADO)
                .tipoConsumo(tipoConsumo)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();
    }

    private static ConsumoInsumoEntity crearConsumoBase(Long id, double cantidadConsumida) {
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, EstadoEtapaLote.EN_CURSO, lote);
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        return ConsumoInsumoEntity.builder()
                .id(id)
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(BigDecimal.TEN)
                .estado(EstadoTransaccion.REGISTRADO)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();
    }
}
