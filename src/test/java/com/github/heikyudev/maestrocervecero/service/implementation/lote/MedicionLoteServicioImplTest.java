package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.MedicionLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IMedicionLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IDetalleParametroControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularMedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.MedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.MedicionLoteResponseDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicionLoteServicioImplTest {

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;
    @Mock
    private IDetalleParametroControlRepository detalleParametroControlRepository;
    @Mock
    private IMedicionLoteRepository medicionLoteRepository;

    @InjectMocks
    private MedicionLoteServicioImpl medicionLoteServicio;

    private static final LocalDateTime FECHA_MEDICION = LocalDateTime.now().minusMinutes(5);

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de mediciones correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MedicionLoteEntity medicion1 = crearMedicionLoteBase(1L, 5.5);
        MedicionLoteEntity medicion2 = crearMedicionLoteBase(2L, 5.6);
        when(medicionLoteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(medicion1, medicion2), pageable, 2));

        // === EJECUCION ===
        Page<MedicionLoteResponseDTO> resultado = medicionLoteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getValorMedido()).isEqualTo(5.5);
        assertThat(resultado.getContent().get(1).getValorMedido()).isEqualTo(5.6);
        verify(medicionLoteRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay mediciones registradas")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(medicionLoteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MedicionLoteResponseDTO> resultado = medicionLoteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(medicionLoteRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la medición cuando el ID existe")
    void buscarPorId_debeRetornarMedicionExistente() {
        // === PREPARACION DE DATOS ===
        MedicionLoteEntity medicion = crearMedicionLoteBase(1L, 5.5);
        when(medicionLoteRepository.findById(1L)).thenReturn(Optional.of(medicion));

        // === EJECUCION ===
        MedicionLoteResponseDTO resultado = medicionLoteServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getValorMedido()).isEqualTo(5.5);
        verify(medicionLoteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(medicionLoteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la medición con ID: 99");
        verify(medicionLoteRepository).findById(99L);
    }

    // ==================== registrarMedicion ====================

    @Test
    @DisplayName("CP-RM-01: registrarMedicion lanza RecursoNoEncontradoException si la etapa de lote no existe")
    void registrarMedicion_debeLanzarExcepcionSiEtapaLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(etapaLoteRepository.findById(99L)).thenReturn(Optional.empty());
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(99L, 1L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(detalleParametroControlRepository, medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-02: registrarMedicion lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void registrarMedicion_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControl)));
        LoteEntity lote = crearLote(EstadoLote.PENDIENTE, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 1L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(detalleParametroControlRepository, medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-03: registrarMedicion lanza ReglaNegocioException si la etapa no admite mediciones (Molienda)")
    void registrarMedicion_debeLanzarExcepcionSiEtapaNoPermiteMediciones() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MOLIENDA);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControl)));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MOLIENDA, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 1L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(detalleParametroControlRepository, medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-04: registrarMedicion lanza ReglaNegocioException si la receta no tiene plan de monitoreo para esa etapa")
    void registrarMedicion_debeLanzarExcepcionSiNoHayPlanDeMonitoreoParaLaEtapa() {
        // === PREPARACION DE DATOS ===
        // El único plan de monitoreo configurado es para Hervido, no para Maceración (la etapa actual)
        EtapaControlEntity etapaControlHervido = crearEtapaControl(TipoEtapa.HERVIDO);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControlHervido)));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 1L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(detalleParametroControlRepository, medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-05: registrarMedicion lanza RecursoNoEncontradoException si el detalle de parámetro de control no existe")
    void registrarMedicion_debeLanzarExcepcionSiDetalleParametroControlNoExiste() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControl)));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        when(detalleParametroControlRepository.findById(99L)).thenReturn(Optional.empty());
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 99L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-06: registrarMedicion lanza ReglaNegocioException si el detalle de parámetro de control pertenece a otra etapa")
    void registrarMedicion_debeLanzarExcepcionSiDetalleParametroControlEsDeOtraEtapa() {
        // === PREPARACION DE DATOS ===
        // La etapa actual del lote es Maceración, pero el plan de monitoreo elegido es de Hervido
        EtapaControlEntity etapaControlMaceracion = crearEtapaControl(TipoEtapa.MACERACION);
        EtapaControlEntity etapaControlHervido = crearEtapaControl(TipoEtapa.HERVIDO);
        PlanMonitoreoEtapaEntity planHervido = crearPlanMonitoreoEtapa(etapaControlHervido);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControlMaceracion), planHervido));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalleDeHervido = crearDetalleParametroControl(5L, planHervido, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalleDeHervido));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-07: registrarMedicion lanza ReglaNegocioException si la fecha de medición es posterior a la fecha actual")
    void registrarMedicion_debeLanzarExcepcionSiFechaMedicionEsFutura() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        MedicionLoteFormDTO formDTO = MedicionLoteFormDTO.builder()
                .idEtapaLote(1L)
                .idDetalleParametroControl(5L)
                .valorMedido(5.5)
                .fechaMedicion(LocalDateTime.now().plusHours(1))
                .build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-RM-08: registrarMedicion lanza ReglaNegocioException si ya existe una medición del mismo parámetro en la misma fecha y hora")
    void registrarMedicion_debeLanzarExcepcionSiHayMedicionDuplicada() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(true);
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RM-09: registrarMedicion permite dos parámetros distintos con la misma fecha y hora exacta")
    void registrarMedicion_debePermitirDosParametrosDistintosEnLaMismaFechaYHora() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        // Parámetro 7 (distinto del 5) a la misma fecha: no hay duplicado sobre el parámetro 7
        DetalleParametroControlEntity detalle7 = crearDetalleParametroControl(7L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(7L)).thenReturn(Optional.of(detalle7));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(7L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        lenient().when(medicionLoteRepository.save(any(MedicionLoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 7L);

        // === EJECUCION ===
        MedicionLoteResponseDTO resultado = medicionLoteServicio.registrarMedicion(formDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        verify(medicionLoteRepository).save(any(MedicionLoteEntity.class));
    }

    @Test
    @DisplayName("CP-RM-10: registrarMedicion — camino feliz con valor dentro del rango: se registra sin alerta")
    void registrarMedicion_debeRegistrarSinAlertaCuandoElValorEstaDentroDelRango() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        ArgumentCaptor<MedicionLoteEntity> captor = ArgumentCaptor.forClass(MedicionLoteEntity.class);
        when(medicionLoteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L, 5.5);

        // === EJECUCION ===
        medicionLoteServicio.registrarMedicion(formDTO);

        // === ASSERTS ===
        MedicionLoteEntity guardada = captor.getValue();
        assertThat(guardada.isHayAlerta()).isFalse();
        assertThat(guardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(guardada.getValorMedido()).isEqualTo(5.5);
        assertThat(guardada.getFechaMedicion()).isEqualTo(FECHA_MEDICION);
        assertThat(guardada.getDetalleParametroControl()).isEqualTo(detalle);
        assertThat(guardada.getEtapaLote()).isEqualTo(etapaLote);
    }

    @Test
    @DisplayName("CP-RM-11: registrarMedicion — valor por debajo del mínimo: se registra con alerta")
    void registrarMedicion_debeRegistrarConAlertaCuandoElValorEstaPorDebajoDelMinimo() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        ArgumentCaptor<MedicionLoteEntity> captor = ArgumentCaptor.forClass(MedicionLoteEntity.class);
        when(medicionLoteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L, 4.5);

        // === EJECUCION ===
        medicionLoteServicio.registrarMedicion(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().isHayAlerta()).isTrue();
    }

    @Test
    @DisplayName("CP-RM-12: registrarMedicion — valor por encima del máximo: se registra con alerta")
    void registrarMedicion_debeRegistrarConAlertaCuandoElValorEstaPorEncimaDelMaximo() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        ArgumentCaptor<MedicionLoteEntity> captor = ArgumentCaptor.forClass(MedicionLoteEntity.class);
        when(medicionLoteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L, 6.5);

        // === EJECUCION ===
        medicionLoteServicio.registrarMedicion(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().isHayAlerta()).isTrue();
    }

    @Test
    @DisplayName("CP-RM-13: registrarMedicion lanza ReglaNegocioException si el valor medido está por debajo del mínimo real del parámetro de control")
    void registrarMedicion_debeLanzarExcepcionSiValorMedidoEsMenorAlMinimoReal() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        // Rango real del parámetro de control: 0.0 - 14.0 (ver crearDetalleParametroControl)
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L, -1.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RM-14: registrarMedicion lanza ReglaNegocioException si el valor medido está por encima del máximo real del parámetro de control")
    void registrarMedicion_debeLanzarExcepcionSiValorMedidoEsMayorAlMaximoReal() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        // Rango real del parámetro de control: 0.0 - 14.0 (ver crearDetalleParametroControl)
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        when(detalleParametroControlRepository.findById(5L)).thenReturn(Optional.of(detalle));
        when(medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(5L, FECHA_MEDICION, EstadoTransaccion.REGISTRADO)).thenReturn(false);
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 5L, 15.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RM-15: registrarMedicion lanza ReglaNegocioException si la etapa no es la etapa actual (no está EN_CURSO)")
    void registrarMedicion_debeLanzarExcepcionSiEtapaNoEstaEnCurso() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(crearPlanMonitoreoEtapa(etapaControl)));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        // Etapa Maceración ya avanzó y quedó PENDIENTE/FINALIZADA — el lote ya pasó a otra etapa
        EtapaLoteEntity etapaLote = EtapaLoteEntity.builder()
                .id(1L)
                .etapa(TipoEtapa.MACERACION)
                .estado(EstadoEtapaLote.PENDIENTE)
                .lote(lote)
                .build();
        when(etapaLoteRepository.findById(1L)).thenReturn(Optional.of(etapaLote));
        MedicionLoteFormDTO formDTO = medicionFormDTOBase(1L, 1L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.registrarMedicion(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(detalleParametroControlRepository, medicionLoteRepository);
    }

    // ==================== anularMedicion ====================

    @Test
    @DisplayName("CP-AM-01: anularMedicion lanza ReglaNegocioException si el motivo de anulación es nulo")
    void anularMedicion_debeLanzarExcepcionSiMotivoEsNulo() {
        // === PREPARACION DE DATOS ===
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion(null).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.anularMedicion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-AM-02: anularMedicion lanza ReglaNegocioException si el motivo de anulación está vacío")
    void anularMedicion_debeLanzarExcepcionSiMotivoEstaVacio() {
        // === PREPARACION DE DATOS ===
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion("   ").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.anularMedicion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(medicionLoteRepository);
    }

    @Test
    @DisplayName("CP-AM-03: anularMedicion lanza RecursoNoEncontradoException si la medición no existe")
    void anularMedicion_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(medicionLoteRepository.findById(99L)).thenReturn(Optional.empty());
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.anularMedicion(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-04: anularMedicion lanza ReglaNegocioException si la medición ya está ANULADA")
    void anularMedicion_debeLanzarExcepcionSiYaEstaAnulada() {
        // === PREPARACION DE DATOS ===
        MedicionLoteEntity medicion = crearMedicionLoteBase(1L, 5.5);
        medicion.setEstado(EstadoTransaccion.ANULADO);
        when(medicionLoteRepository.findById(1L)).thenReturn(Optional.of(medicion));
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.anularMedicion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-05: anularMedicion lanza ReglaNegocioException si el lote asociado no está EN_EJECUCION")
    void anularMedicion_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        MedicionLoteEntity medicion = crearMedicionLoteBase(1L, 5.5);
        medicion.getEtapaLote().getLote().setEstado(EstadoLote.FINALIZADO);
        when(medicionLoteRepository.findById(1L)).thenReturn(Optional.of(medicion));
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion("Error de tipeo").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> medicionLoteServicio.anularMedicion(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verify(medicionLoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-06: anularMedicion — camino feliz: la medición queda ANULADA con fechaAnulacion y motivoAnulacion seteados")
    void anularMedicion_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        MedicionLoteEntity medicion = crearMedicionLoteBase(1L, 5.5);
        when(medicionLoteRepository.findById(1L)).thenReturn(Optional.of(medicion));
        when(medicionLoteRepository.save(any(MedicionLoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        AnularMedicionLoteFormDTO formDTO = AnularMedicionLoteFormDTO.builder().motivoAnulacion("Error de tipeo").build();
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        MedicionLoteResponseDTO resultado = medicionLoteServicio.anularMedicion(1L, formDTO);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(medicion.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(medicion.getFechaAnulacion()).isNotNull().isBetween(antes, despues);
        assertThat(medicion.getMotivoAnulacion()).isEqualTo("Error de tipeo");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de tipeo");
        verify(medicionLoteRepository).save(medicion);
    }

    // ==================== helpers ====================

    private static MedicionLoteFormDTO medicionFormDTOBase(Long idEtapaLote, Long idDetalleParametroControl) {
        return medicionFormDTOBase(idEtapaLote, idDetalleParametroControl, 5.5);
    }

    private static MedicionLoteFormDTO medicionFormDTOBase(Long idEtapaLote, Long idDetalleParametroControl, double valorMedido) {
        return MedicionLoteFormDTO.builder()
                .idEtapaLote(idEtapaLote)
                .idDetalleParametroControl(idDetalleParametroControl)
                .valorMedido(valorMedido)
                .fechaMedicion(FECHA_MEDICION)
                .build();
    }

    private static MedicionLoteEntity crearMedicionLoteBase(Long id, double valorMedido) {
        EtapaControlEntity etapaControl = crearEtapaControl(TipoEtapa.MACERACION);
        PlanMonitoreoEtapaEntity plan = crearPlanMonitoreoEtapa(etapaControl);
        VersionRecetaEntity versionReceta = crearVersionReceta(List.of(plan));
        LoteEntity lote = crearLote(EstadoLote.EN_EJECUCION, versionReceta);
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, TipoEtapa.MACERACION, lote);
        DetalleParametroControlEntity detalle = crearDetalleParametroControl(5L, plan, 5.0, 6.0, 5.5);
        return MedicionLoteEntity.builder()
                .id(id)
                .valorMedido(valorMedido)
                .fechaMedicion(FECHA_MEDICION)
                .estado(EstadoTransaccion.REGISTRADO)
                .hayAlerta(false)
                .detalleParametroControl(detalle)
                .etapaLote(etapaLote)
                .build();
    }

    private static EtapaLoteEntity crearEtapaLote(Long id, TipoEtapa tipo, LoteEntity lote) {
        return EtapaLoteEntity.builder()
                .id(id)
                .etapa(tipo)
                .estado(EstadoEtapaLote.EN_CURSO)
                .lote(lote)
                .build();
    }

    private static LoteEntity crearLote(EstadoLote estado, VersionRecetaEntity versionReceta) {
        PlanificacionProduccionEntity planificacion = PlanificacionProduccionEntity.builder()
                .id(1L)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(30))
                .cantidadAProducir(100.0)
                .versionReceta(versionReceta)
                .build();
        return LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-1")
                .volumenObjetivo(20.0)
                .estado(estado)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(20))
                .planificacionProduccion(planificacion)
                .build();
    }

    private static VersionRecetaEntity crearVersionReceta(List<PlanMonitoreoEtapaEntity> planesMonitoreo) {
        RecetaEntity receta = RecetaEntity.builder().id(1L).contadorLotes(1L).estado(Estado.ACTIVO).build();
        return VersionRecetaEntity.builder()
                .id(1L)
                .nombre("IPA Test")
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.050)
                .fgObjetivo(1.010)
                .ibuObjetivo(35)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(10)
                .duracionMaduracion(5)
                .esUltimaVersion(true)
                .receta(receta)
                .detallesMalta(List.of())
                .detallesLupulo(List.of())
                .detallesLevadura(List.of())
                .planesMonitoreo(planesMonitoreo)
                .build();
    }

    private static EtapaControlEntity crearEtapaControl(TipoEtapa etapaAControlar) {
        return EtapaControlEntity.builder()
                .id(1L)
                .nombre("Control " + etapaAControlar)
                .etapaAControlar(etapaAControlar)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static PlanMonitoreoEtapaEntity crearPlanMonitoreoEtapa(EtapaControlEntity etapaControl) {
        return PlanMonitoreoEtapaEntity.builder()
                .id(1L)
                .etapaControl(etapaControl)
                .build();
    }

    private static DetalleParametroControlEntity crearDetalleParametroControl(Long id, PlanMonitoreoEtapaEntity plan, double valorMinimo, double valorMaximo, double valorIdeal) {
        // El rango real del ParametroControlEntity es deliberadamente más amplio que el rango ideal
        // del detalle (valorMinimo/valorMaximo): representan cosas distintas (límite físico real vs.
        // rango deseado para esta receta), y una medición puede caer fuera del ideal (dispara alerta)
        // sin violar el límite real.
        ParametroControlEntity parametroControl = ParametroControlEntity.builder()
                .id(1L)
                .nombre("pH")
                .valorMinimo(0.0)
                .valorMaximo(14.0)
                .estado(Estado.ACTIVO)
                .build();
        return DetalleParametroControlEntity.builder()
                .id(id)
                .valorMinimo(valorMinimo)
                .valorMaximo(valorMaximo)
                .valorIdeal(valorIdeal)
                .planMonitoreoEtapa(plan)
                .parametroControl(parametroControl)
                .build();
    }
}
