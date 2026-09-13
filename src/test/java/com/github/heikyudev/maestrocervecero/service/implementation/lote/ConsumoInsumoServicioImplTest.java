package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ConsumoInsumoServicioImplTest {

    @Mock
    private IConsumoInsumoRepository consumoInsumoRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;
    @Mock
    private ILoteInsumoRepository loteInsumoRepository;
    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private IEscaladoInsumoServicio escaladoInsumoServicio;

    @InjectMocks
    private ConsumoInsumoServicioImpl consumoInsumoServicio;

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
    @DisplayName("CP-FCI-02: filtrarConsumosInsumo con estado nulo asume REGISTRADO por defecto, propagando tipoConsumo e idInsumo nulos tal cual")
    void filtrarConsumosInsumo_debeAsumirRegistradoPorDefecto() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(consumoInsumoRepository.filtrarConsumosInsumo(1L, null, null, EstadoTransaccion.REGISTRADO, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ConsumoInsumoResponseDTO> resultado = consumoInsumoServicio.filtrarConsumosInsumo(1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(consumoInsumoRepository).filtrarConsumosInsumo(1L, null, null, EstadoTransaccion.REGISTRADO, pageable);
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
        ConsumoInsumoFormDTO formDTO = formDTOBase(99L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-02: registrarConsumoInsumoReservado lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void registrarReservado_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.PENDIENTE);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe estar en ejecución para registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-03: registrarConsumoInsumoReservado lanza ReglaNegocioException si la etapa no está EN_CURSO")
    void registrarReservado_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.PENDIENTE, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-04: registrarConsumoInsumoReservado lanza ReglaNegocioException si el tipo de etapa no permite registrar consumo (Envasado)")
    void registrarReservado_debeLanzarExcepcionSiTipoEtapaNoPermiteConsumo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de tipo ENVASADO no permite registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-05: registrarConsumoInsumoReservado lanza RecursoNoEncontradoException si el lote de insumo no existe")
    void registrarReservado_debeLanzarExcepcionSiLoteInsumoNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 99L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
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
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, VersionRecetaEntity.builder().build());
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, maltaNoUsada, 10.0, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-07: registrarConsumoInsumoReservado lanza ReglaNegocioException si no existe una reserva de ese lote de insumo para esa etapa")
    void registrarReservado_debeLanzarExcepcionSiNoExisteReserva() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No existe una reserva de este lote de insumo para esta etapa; utilice el registro de consumo directo");
        verifyNoInteractions(consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-08: registrarConsumoInsumoReservado lanza ReglaNegocioException si la cantidad consumida es nula")
    void registrarReservado_debeRechazarCantidadNula() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verifyNoInteractions(consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-09: registrarConsumoInsumoReservado lanza ReglaNegocioException si la cantidad consumida es cero (límite)")
    void registrarReservado_debeRechazarCantidadEnCero() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 0.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-10: registrarConsumoInsumoReservado lanza ReglaNegocioException si la cantidad consumida es negativa")
    void registrarReservado_debeRechazarCantidadNegativa() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, -3.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CR-11: registrarConsumoInsumoReservado lanza ReglaNegocioException si la cantidad consumida supera la cantidad reservada")
    void registrarReservado_debeRechazarCantidadQueSuperaLaReservada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 6.5);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad reservada del lote de insumo");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CR-12: registrarConsumoInsumoReservado permite una cantidad igual a la reservada (límite): la reserva queda en 0")
    void registrarReservado_debePermitirCantidadIgualALaReservada() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 6.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(1L, etapaLote, loteInsumo, 6.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(1L, 1L)).thenReturn(Optional.of(reserva));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 6.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO);

        // === ASSERTS ===
        assertThat(reserva.getCantidadReservada()).isEqualTo(0.0);
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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
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
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 4.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoReservado(formDTO);

        // === ASSERTS ===
        assertThat(reserva.getCantidadReservada()).isEqualTo(2.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(2.0);
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(6.0);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(4.0);
        assertThat(resultado.getCostoUnitarioPPP()).isEqualByComparingTo(BigDecimal.valueOf(12.5));
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== registrarConsumoInsumoDirecto ====================

    @Test
    @DisplayName("CP-CD-01: registrarConsumoInsumoDirecto lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void registrarDirecto_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(99L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de lote con ID: 99");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-02: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void registrarDirecto_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.FINALIZADO);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote debe estar en ejecución para registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-03: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la etapa no está EN_CURSO")
    void registrarDirecto_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.FINALIZADA, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa debe estar en curso para registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-04: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el tipo de etapa no permite registrar consumo (Envasado)")
    void registrarDirecto_debeLanzarExcepcionSiTipoEtapaNoPermiteConsumo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.ENVASADO, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de tipo ENVASADO no permite registrar consumos de insumo");
        verifyNoInteractions(loteInsumoRepository, reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-05: registrarConsumoInsumoDirecto lanza RecursoNoEncontradoException si el lote de insumo no existe")
    void registrarDirecto_debeLanzarExcepcionSiLoteInsumoNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(99L)).thenReturn(Optional.empty());
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 99L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-06: registrarConsumoInsumoDirecto lanza ReglaNegocioException si el insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta")
    void registrarDirecto_debeRechazarInsumoNoRequeridoPorLaEtapa() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaNoUsada = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, VersionRecetaEntity.builder().build());
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, maltaNoUsada, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 5.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        verifyNoInteractions(reservaInsumoRepository, consumoInsumoRepository);
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-07: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la cantidad consumida es nula")
    void registrarDirecto_debeRechazarCantidadNula() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, null);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-08: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la cantidad consumida es cero (límite)")
    void registrarDirecto_debeRechazarCantidadEnCero() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 0.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-09: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la cantidad consumida es negativa")
    void registrarDirecto_debeRechazarCantidadNegativa() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, -2.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida debe ser mayor a cero");
        verify(loteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CD-10: registrarConsumoInsumoDirecto lanza ReglaNegocioException si la cantidad consumida supera la cantidad disponible")
    void registrarDirecto_debeRechazarCantidadQueSuperaLaDisponible() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 10.5);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad consumida no puede superar la cantidad disponible del lote de insumo");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(consumoInsumoRepository);
    }

    @Test
    @DisplayName("CP-CD-11: registrarConsumoInsumoDirecto permite una cantidad igual a la disponible (límite)")
    void registrarDirecto_debePermitirCantidadIgualALaDisponible() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, crearVersionRecetaConMalta(malta));
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(1L, malta, 10.0, 0.0);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(loteInsumoRepository.buscarPorIdParaConsumir(1L)).thenReturn(Optional.of(loteInsumo));
        when(consumoInsumoRepository.save(any(ConsumoInsumoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 10.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO);

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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
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
        ConsumoInsumoFormDTO formDTO = formDTOBase(1L, 1L, 3.0);

        // === EJECUCION ===
        ConsumoInsumoResponseDTO resultado = consumoInsumoServicio.registrarConsumoInsumoDirecto(formDTO);

        // === ASSERTS ===
        assertThat(loteInsumo.getCantidadActual()).isEqualTo(4.0);
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(4.0);
        assertThat(resultado.getCantidadConsumida()).isEqualTo(3.0);
        assertThat(resultado.getCostoUnitarioPPP()).isEqualByComparingTo(BigDecimal.valueOf(8.25));
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verifyNoInteractions(reservaInsumoRepository);
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
        // acá solo se verifica que el resultado de ambos se mapee y sume correctamente.
        MaltaEntity malta = maltaEntity(1L);
        LevaduraEntity levadura = levaduraEntity(2L);
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION);
        EtapaLoteEntity etapaMaceracion = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaMaceracion));
        when(escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMaceracion)).thenReturn(List.of(
                new RequerimientoInsumo(malta, etapaMaceracion, 500.0),
                new RequerimientoInsumo(levadura, etapaMaceracion, 10.0)));

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
                .hasMessage("Solo se pueden anular consumos de insumo en estado REGISTRADO");
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
                .hasMessage("El lote debe encontrarse en estado EN_EJECUCION para poder anular un consumo de insumo");
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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, malta, 6.0, 2.0);
        ReservaInsumoEntity reserva = reservaInsumoEntity(100L, etapaLote, loteInsumo, 2.0);
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
        assertThat(reserva.getCantidadReservada()).isEqualTo(6.0);
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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
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

    // ==================== helpers de construcción ====================

    private static ConsumoInsumoFormDTO formDTOBase(Long idEtapaLote, Long idLoteInsumo, Double cantidadConsumida) {
        return ConsumoInsumoFormDTO.builder()
                .idEtapaLote(idEtapaLote)
                .idLoteInsumo(idLoteInsumo)
                .cantidadConsumida(cantidadConsumida)
                .build();
    }

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static LevaduraEntity levaduraEntity(Long id) {
        return LevaduraEntity.builder().id(id).nombre("Levadura Ale").estado(Estado.ACTIVO).build();
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

    private static LoteInsumoEntity loteInsumoEntity(Long id, MaltaEntity insumo, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor("LOTE-" + id)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }

    private static ReservaInsumoEntity reservaInsumoEntity(Long id, EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo, double cantidadReservada) {
        return ReservaInsumoEntity.builder()
                .id(id)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .cantidadReservada(cantidadReservada)
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
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO, lote);
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
