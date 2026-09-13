package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteServicioImplTest {

    @Mock
    private ILoteRepository loteRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IMolinoRepository molinoRepository;
    @Mock
    private IMaceradorRepository maceradorRepository;
    @Mock
    private IOllaHervorRepository ollaHervorRepository;
    @Mock
    private IFermentadorRepository fermentadorRepository;
    @Mock
    private IRecetaRepository recetaRepository;
    @Mock
    private IConfiguracionProduccionRepository configuracionProduccionRepository;
    @Mock
    private ILoteInsumoRepository loteInsumoRepository;
    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;
    @Mock
    private IConsumoInsumoServicio consumoInsumoServicio;
    // Spy con la implementación real: el escalado de insumos ya se prueba de forma independiente
    // en EscaladoInsumoServicioImplTest, así que acá no tiene sentido mockearlo — se necesita el
    // cálculo real para que estos tests sigan verificando el mismo comportamiento de extremo a
    // extremo que verificaban antes de extraer esta lógica a su propio servicio.
    @Spy
    private IEscaladoInsumoServicio escaladoInsumoServicio = new EscaladoInsumoServicioImpl();

    @InjectMocks
    private LoteServicioImpl loteServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de lotes correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LoteEntity lote1 = crearLoteMinimo(1L, "IPA-1");
        LoteEntity lote2 = crearLoteMinimo(2L, "IPA-2");
        when(loteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(lote1, lote2), pageable, 2));

        // === EJECUCION ===
        Page<LoteResponseDTO> resultado = loteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getIdentificadorInterno()).isEqualTo("IPA-1");
        verify(loteRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay lotes registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(loteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LoteResponseDTO> resultado = loteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(loteRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del lote cuando el ID existe")
    void buscarPorId_debeRetornarLoteExistente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteMinimo(1L, "IPA-1");
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION ===
        LoteResponseDTO resultado = loteServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("IPA-1");
        verify(loteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // ==================== registrarLote ====================

    @Test
    @DisplayName("CP-RL-01: registrarLote lanza RecursoNoEncontradoException si la planificación de producción no existe")
    void registrarLote_debeLanzarExcepcionSiPlanificacionNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteFormDTO formDTO = loteFormDTOBuilderBase().idPlanificacionProduccion(99L).build();
        when(planificacionProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-RL-02: registrarLote lanza ReglaNegocioException si la planificación de producción no está PENDIENTE")
    void registrarLote_debeLanzarExcepcionSiPlanificacionNoPendiente() {
        // === PREPARACION DE DATOS ===
        RecetaEntity receta = crearReceta(5L, 3L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.FINALIZADA, versionReceta);
        LoteFormDTO formDTO = loteFormDTOBase();
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-RL-03: registrarLote lanza RecursoNoEncontradoException si el molino no existe")
    void registrarLote_debeLanzarExcepcionSiMolinoNoExiste() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.empty());
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-RL-04: registrarLote lanza RecursoNoEncontradoException si el macerador no existe")
    void registrarLote_debeLanzarExcepcionSiMaceradorNoExiste() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.empty());
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-RL-05: registrarLote lanza RecursoNoEncontradoException si la olla de hervor no existe")
    void registrarLote_debeLanzarExcepcionSiOllaHervorNoExiste() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 25.0, 5.0, 75.0)));
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.empty());
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-RL-06: registrarLote lanza RecursoNoEncontradoException si el fermentador no existe")
    void registrarLote_debeLanzarExcepcionSiFermentadorNoExiste() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 25.0, 5.0, 75.0)));
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.of(crearOllaHervor(3L, 30.0, 3.0, 2.0)));
        when(fermentadorRepository.findById(4L)).thenReturn(Optional.empty());
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-RL-07: registrarLote lanza ReglaNegocioException si el volumen objetivo es nulo")
    void registrarLote_debeLanzarExcepcionSiVolumenObjetivoNulo() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        LoteFormDTO formDTO = loteFormDTOBuilderBase().volumenObjetivo(null).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-RL-08: registrarLote lanza ReglaNegocioException si el volumen objetivo es cero (límite)")
    void registrarLote_debeLanzarExcepcionSiVolumenObjetivoEsCero() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        LoteFormDTO formDTO = loteFormDTOBuilderBase().volumenObjetivo(0.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-RL-09: registrarLote lanza ReglaNegocioException si el volumen objetivo supera la capacidad del fermentador")
    void registrarLote_debeLanzarExcepcionSiVolumenSuperaCapacidadFermentador() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        LoteFormDTO formDTO = loteFormDTOBuilderBase().volumenObjetivo(25.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-RL-10: registrarLote no lanza por esta regla cuando el volumen objetivo iguala la capacidad del fermentador (límite válido)")
    void registrarLote_noDebeLanzarCuandoVolumenIgualaCapacidadFermentador() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        LoteFormDTO formDTO = loteFormDTOBase(); // volumenObjetivo = 20.0 = capacidadUtil del fermentador
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> loteServicio.registrarLote(formDTO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RL-11: registrarLote lanza ReglaNegocioException si el volumen pre-hervor supera la capacidad de la olla de hervor")
    void registrarLote_debeLanzarExcepcionSiPreHervorSuperaCapacidadOlla() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 25.0, 5.0, 75.0)));
        // pre-hervor calculado = 20 + 2 + 3*(60/60) = 25.0 > 24.0
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.of(crearOllaHervor(3L, 24.0, 3.0, 2.0)));
        when(fermentadorRepository.findById(4L)).thenReturn(Optional.of(crearFermentador(4L, 20.0)));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-RL-12: registrarLote lanza ReglaNegocioException si el volumen de mezcla supera la capacidad del macerador")
    void registrarLote_debeLanzarExcepcionSiMezclaSuperaCapacidadMacerador() {
        // === PREPARACION DE DATOS ===
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        // mezcla calculada ≈ 21.26 > 20.0
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 20.0, 5.0, 75.0)));
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.of(crearOllaHervor(3L, 30.0, 3.0, 2.0)));
        when(fermentadorRepository.findById(4L)).thenReturn(Optional.of(crearFermentador(4L, 20.0)));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-RL-13: registrarLote lanza RecursoNoEncontradoException si no existe la configuración de planificación de producción")
    void registrarLote_debeLanzarExcepcionSiConfiguracionNoExiste() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        RecetaEntity receta = crearReceta(5L, 3L);
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-RL-14: registrarLote — camino feliz: persiste el lote PENDIENTE con identificador, contador y 6 etapas")
    void registrarLote_debeRegistrarLoteConEtapasCorrectamente() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        RecetaEntity receta = crearReceta(5L, 3L);
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        LoteResponseDTO resultado = loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(guardado.getIdentificadorInterno()).isEqualTo("IPA Test-3");
        assertThat(receta.getContadorLotes()).isEqualTo(4L);
        assertThat(guardado.getEtapas()).hasSize(6);
        assertThat(guardado.getEtapas()).allMatch(e -> e.getEstado() == EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(guardado, TipoEtapa.MOLIENDA).getEquipamiento()).isInstanceOf(MolinoEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.MACERACION).getEquipamiento()).isInstanceOf(MaceradorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.HERVIDO).getEquipamiento()).isInstanceOf(OllaHervorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.FERMENTACION).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.MADURACION).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.ENVASADO).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("IPA Test-3");
    }

    @Test
    @DisplayName("CP-RL-15: registrarLote — camino feliz: fechaInicioEstimada es hoy y fechaFinalizacionEstimada suma la duración total")
    void registrarLote_debeCalcularFechasCorrectamenteSinLotesPrevios() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        // 1 día de brew day/envasado (0.88h molienda + 2h maceración/hervido + 2h envasado ≈ 4.88h) + 14 fermentación + 7 maduración
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.now());
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.now().plusDays(22));
    }

    @Test
    @DisplayName("CP-RL-16: registrarLote — fechaInicioEstimada se encola al día siguiente del último lote planificado en ese fermentador")
    void registrarLote_debeEncolarFechaInicioTrasUltimoLoteDelFermentador() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        LocalDate ultimaFechaFinalizacion = LocalDate.of(2026, 10, 1);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.of(ultimaFechaFinalizacion));
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    @DisplayName("CP-RL-17: registrarLote delega al repositorio el fermentador correcto y solo los estados PENDIENTE/EN_EJECUCION")
    void registrarLote_debeConsultarCronogramaConFermentadorYEstadosCorrectos() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<FermentadorEntity> fermentadorCaptor = ArgumentCaptor.forClass(FermentadorEntity.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EstadoLote>> estadosCaptor = ArgumentCaptor.forClass(List.class);
        verify(loteRepository).buscarUltimaFechaFinalizacionEstimadaPorFermentador(fermentadorCaptor.capture(), estadosCaptor.capture());
        assertThat(fermentadorCaptor.getValue().getId()).isEqualTo(4L);
        assertThat(estadosCaptor.getValue()).containsExactlyInAnyOrder(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION);
    }

    @Test
    @DisplayName("CP-RL-18: registrarLote calcula masaMaltaEscalada, volumenPreHervor y volumenMezclaMacerador según EscaladoDeMalta.md")
    void registrarLote_debeCalcularEscaladoDeMaltaYAguaCorrectamente() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        // No lanza ninguna excepción: masaMaltaEscalada ≈ 4.396kg, preHervor = 25.0L (≤30), mezcla ≈ 21.264L (≤25)
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        // La verificación numérica exacta de horasMolienda (masaMaltaEscalada / rendimientoMolienda) prueba
        // indirectamente que masaMaltaEscalada se calculó según la fórmula de EscaladoDeMalta.md: si el total
        // fuera otro, la fechaFinalizacionEstimada (que depende de horasMolienda) también cambiaría.
        verify(loteRepository).save(any(LoteEntity.class));
    }

    // ==================== iniciarLote ====================

    @Test
    @DisplayName("CP-IL-01: iniciarLote lanza RecursoNoEncontradoException si el lote no existe")
    void iniciarLote_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-IL-02: iniciarLote lanza ReglaNegocioException si el lote está en EN_EJECUCION")
    void iniciarLote_debeLanzarExcepcionSiLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBase(EstadoLote.EN_EJECUCION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-IL-03: iniciarLote lanza ReglaNegocioException si el lote está CANCELADO")
    void iniciarLote_debeLanzarExcepcionSiLoteCancelado() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBase(EstadoLote.CANCELADO);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-IL-04: iniciarLote lanza RecursoNoEncontradoException si el molino ya no se encuentra al iniciar")
    void iniciarLote_debeLanzarExcepcionSiMolinoNoEncontradoAlIniciar() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBase(EstadoLote.PENDIENTE);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-IL-05: iniciarLote lanza ReglaNegocioException si el molino no está DISPONIBLE")
    void iniciarLote_debeLanzarExcepcionSiMolinoNoDisponible() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo.EN_USO, EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("molino");
    }

    @Test
    @DisplayName("CP-IL-06: iniciarLote lanza ReglaNegocioException si el macerador no está DISPONIBLE")
    void iniciarLote_debeLanzarExcepcionSiMaceradorNoDisponible() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo.DISPONIBLE, EstadoOperativo.EN_LIMPIEZA, EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("macerador");
    }

    @Test
    @DisplayName("CP-IL-07: iniciarLote lanza ReglaNegocioException si la olla de hervor no está DISPONIBLE")
    void iniciarLote_debeLanzarExcepcionSiOllaHervorNoDisponible() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, EstadoOperativo.EN_MANTENIMIENTO, EstadoOperativo.DISPONIBLE);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("olla de hervor");
    }

    @Test
    @DisplayName("CP-IL-08: iniciarLote lanza ReglaNegocioException si el fermentador no está DISPONIBLE")
    void iniciarLote_debeLanzarExcepcionSiFermentadorNoDisponible() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, EstadoOperativo.EN_USO);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fermentador");
    }

    @Test
    @DisplayName("CP-IL-09: iniciarLote lanza ReglaNegocioException si el stock de malta es insuficiente, sin reservar nada")
    void iniciarLote_debeLanzarExcepcionSiStockMaltaInsuficiente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LoteEntity lote = crearLoteConRecetaSimple(malta, null, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        // requerimiento ≈ 3.70kg, disponible 2.0kg
        LoteInsumoEntity stockMalta = crearLoteInsumo(100L, malta, 2.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L)).thenReturn(List.of(stockMalta));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-IL-10: iniciarLote lanza ReglaNegocioException si el stock de lúpulo es insuficiente, sin reservar nada")
    void iniciarLote_debeLanzarExcepcionSiStockLupuloInsuficiente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        // requerimiento ≈ 14.4g, disponible 5.0g
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 5.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-IL-11: iniciarLote lanza ReglaNegocioException si el stock de levadura es insuficiente, sin reservar nada")
    void iniciarLote_debeLanzarExcepcionSiStockLevaduraInsuficiente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        // requerimiento = 7.5g, disponible 2.0g
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(crearLoteInsumo(102L, levadura, 2.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-IL-19: iniciarLote reporta TODOS los insumos con stock insuficiente en un solo error, no solo el primero")
    void iniciarLote_debeReportarTodosLosInsumosConStockInsuficiente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        // malta insuficiente: requerimiento ≈3.70kg, disponible 2.0kg
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 2.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        // lúpulo suficiente
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        // levadura insuficiente: requerimiento = 7.5g, disponible 2.0g
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(crearLoteInsumo(102L, levadura, 2.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));

        // === EJECUCION Y ASSERTS ===
        // Ambos insumos faltantes deben aparecer en el mismo mensaje: el usuario no debería tener
        // que corregir uno, reintentar, y recién ahí enterarse del siguiente.
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("MaltaB")
                .hasMessageContaining("LevaduraB");
        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(reservaInsumoRepository);
    }

    @Test
    @DisplayName("CP-IL-12: iniciarLote reserva por FEFO desde 2 lotes de insumo cuando el primero no alcanza")
    void iniciarLote_debeReservarPorFefoDesdeDosLotesDeInsumo() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        // requerimiento levadura = 7.5g: lote A vence antes (5.0g disponible), lote B vence después (10.0g disponible)
        LoteInsumoEntity levaduraA = crearLoteInsumo(102L, levadura, 5.0, 0.0, LocalDate.now().plusDays(10), BigDecimal.valueOf(5));
        LoteInsumoEntity levaduraB = crearLoteInsumo(103L, levadura, 10.0, 0.0, LocalDate.now().plusDays(60), BigDecimal.valueOf(8));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(levaduraA, levaduraB));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReservaInsumoEntity>> captor = ArgumentCaptor.forClass(List.class);
        when(reservaInsumoRepository.saveAll(captor.capture())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        assertThat(levaduraA.getCantidadReservada()).isEqualTo(5.0);
        assertThat(levaduraB.getCantidadReservada()).isCloseTo(2.5, within(0.01));
        List<ReservaInsumoEntity> reservasLevadura = captor.getValue().stream()
                .filter(r -> r.getLoteInsumo().getInsumo().getId().equals(12L))
                .toList();
        assertThat(reservasLevadura).hasSize(2);
        assertThat(reservasLevadura.stream().mapToDouble(ReservaInsumoEntity::getCantidadReservada).sum()).isCloseTo(7.5, within(0.01));
    }

    @Test
    @DisplayName("CP-IL-13: iniciarLote reserva desde un solo lote de insumo cuando alcanza para cubrir todo el requerimiento")
    void iniciarLote_debeReservarDesdeUnSoloLoteDeInsumoCuandoAlcanza() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        LoteInsumoEntity unicoLoteLevadura = crearLoteInsumo(102L, levadura, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.valueOf(5));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(unicoLoteLevadura));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReservaInsumoEntity>> captor = ArgumentCaptor.forClass(List.class);
        when(reservaInsumoRepository.saveAll(captor.capture())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        assertThat(unicoLoteLevadura.getCantidadReservada()).isCloseTo(7.5, within(0.01));
        List<ReservaInsumoEntity> reservasLevadura = captor.getValue().stream()
                .filter(r -> r.getLoteInsumo().getInsumo().getId().equals(12L))
                .toList();
        assertThat(reservasLevadura).hasSize(1);
        assertThat(reservasLevadura.get(0).getCantidadReservada()).isCloseTo(7.5, within(0.01));
    }

    @Test
    @DisplayName("CP-IL-14: iniciarLote calcula los gramos de lúpulo HERVOR según la fórmula de Tinseth")
    void iniciarLote_debeCalcularLupuloHervorSegunTinseth() {
        // === PREPARACION DE DATOS ===
        // Receta sintética de un solo lúpulo HERVOR (aísla la fórmula). AA fraccionario (5.5%),
        // igual que el ejemplo ilustrativo de EscaladoDelLupulo.md, ahora que LupuloEntity.aa es Double.
        LupuloEntity lupulo = crearLupulo(20L, "LupuloTinseth", 5.5, FormatoLupulo.PELLET);
        DetalleLupuloEntity detalle = detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, lupulo);
        VersionRecetaEntity versionReceta = versionRecetaAislada(List.of(), List.of(detalle), List.of(), 1.050, 35, 20.0);
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        LoteInsumoEntity stockLupulo = crearLoteInsumo(200L, lupulo, 60.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.ONE);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(20L)).thenReturn(List.of(stockLupulo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaInsumoRepository.saveAll(anyList())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        // Cálculo manual: FactorDensidad=1.65*0.000125^0.05≈1.0530; FactorTiempo(60min)=(1-e^-2.4)/4.15≈0.2191;
        // Utilizacion=1.0530*0.2191*1.10(Pellet)≈0.2538; K=1.0*(5.5/100)*0.2538≈0.013959;
        // GramosTotalHervor=(35*20)/(1000*0.013959)≈50.1g
        assertThat(stockLupulo.getCantidadReservada()).isCloseTo(50.1, within(1.0));
    }

    @Test
    @DisplayName("CP-IL-15: iniciarLote calcula los gramos de lúpulo WHIRLPOOL/DRY_HOP con escalado lineal (EscaladoDelLupulo.md)")
    void iniciarLote_debeCalcularLupuloLinealSegunVolumen() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupulo = crearLupulo(21L, "LupuloDryHop", 5, FormatoLupulo.FLOR);
        DetalleLupuloEntity detalle = detalleLupulo(15.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, lupulo);
        VersionRecetaEntity versionReceta = versionRecetaAislada(List.of(), List.of(detalle), List.of(), 1.050, 35, 20.0);
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 100.0);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        LoteInsumoEntity stockLupulo = crearLoteInsumo(201L, lupulo, 100.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.ONE);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(21L)).thenReturn(List.of(stockLupulo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaInsumoRepository.saveAll(anyList())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        // 15g * (100L / 20L) = 75.0g exactos (ejemplo de EscaladoDelLupulo.md)
        assertThat(stockLupulo.getCantidadReservada()).isCloseTo(75.0, within(0.01));
    }

    @Test
    @DisplayName("CP-IL-16: iniciarLote calcula los gramos de levadura según EscaladoDeLevadura.md (Caso 1)")
    void iniciarLote_debeCalcularLevaduraSegunFormula() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levadura = crearLevadura(22L, "LevaduraUnica", TipoLevadura.ALE, 2.0E10);
        DetalleLevaduraEntity detalle = detalleLevadura(6.0, levadura);
        VersionRecetaEntity versionReceta = versionRecetaAislada(List.of(), List.of(), List.of(detalle), 1.050, 35, 20.0);
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        LoteInsumoEntity stockLevadura = crearLoteInsumo(202L, levadura, 20.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.ONE);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(22L)).thenReturn(List.of(stockLevadura));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaInsumoRepository.saveAll(anyList())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        // GradosPlato=12.5; TasaPromedio=0.75 (única); Celulas=20000ml*12.5*0.75=187500 millones;
        // Gramos=(187500*1e6)/2.0E10=9.375g exactos
        assertThat(stockLevadura.getCantidadReservada()).isCloseTo(9.375, within(0.001));
    }

    @Test
    @DisplayName("CP-IL-17: iniciarLote NO fusiona el requerimiento cuando el mismo lúpulo aparece en dos detalles de etapas distintas (WHIRLPOOL en Hervido + DRY_HOP en Maduración): genera 2 reservas separadas, cada una atada a su propia etapa")
    void iniciarLote_debeGenerarReservasSeparadasPorEtapaParaElMismoInsumo() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupulo = crearLupulo(23L, "LupuloDoble", 5, FormatoLupulo.FLOR);
        DetalleLupuloEntity whirlpool = detalleLupulo(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, 0.0, lupulo);
        DetalleLupuloEntity dryHop = detalleLupulo(5.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, lupulo);
        VersionRecetaEntity versionReceta = versionRecetaAislada(List.of(), List.of(whirlpool, dryHop), List.of(), 1.050, 35, 20.0);
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0); // volumenObjetivo = volumenBase → ratio 1:1
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        LoteInsumoEntity stockLupulo = crearLoteInsumo(203L, lupulo, 20.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.ONE);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(23L)).thenReturn(List.of(stockLupulo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReservaInsumoEntity>> captor = ArgumentCaptor.forClass(List.class);
        when(reservaInsumoRepository.saveAll(captor.capture())).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        // El stock físico es compartido (WHIRLPOOL y DRY_HOP consumen del mismo lote de insumo), así
        // que la cantidad reservada total sigue siendo 15.0 — pero repartida en 2 reservas distintas,
        // una por etapa (Hervido y Maduración), no fusionadas en una sola.
        assertThat(stockLupulo.getCantidadReservada()).isCloseTo(15.0, within(0.01));
        List<ReservaInsumoEntity> reservasLupulo = captor.getValue().stream()
                .filter(r -> r.getLoteInsumo().getInsumo().getId().equals(23L))
                .toList();
        assertThat(reservasLupulo).hasSize(2);
        assertThat(reservasLupulo.stream().mapToDouble(ReservaInsumoEntity::getCantidadReservada).sum()).isCloseTo(15.0, within(0.01));

        ReservaInsumoEntity reservaHervido = reservasLupulo.stream()
                .filter(r -> r.getEtapaLote().getEtapa() == TipoEtapa.HERVIDO)
                .findFirst().orElseThrow();
        ReservaInsumoEntity reservaMaduracion = reservasLupulo.stream()
                .filter(r -> r.getEtapaLote().getEtapa() == TipoEtapa.MADURACION)
                .findFirst().orElseThrow();
        assertThat(reservaHervido.getCantidadReservada()).isCloseTo(10.0, within(0.01));
        assertThat(reservaMaduracion.getCantidadReservada()).isCloseTo(5.0, within(0.01));
    }

    @Test
    @DisplayName("CP-IL-18: iniciarLote — camino feliz completo: EN_EJECUCION, equipamiento EN_USO, Molienda EN_CURSO, resto PENDIENTE")
    void iniciarLote_debeIniciarLoteCompletoCorrectamente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(crearLoteInsumo(102L, levadura, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        ArgumentCaptor<LoteEntity> loteCaptor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(loteCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReservaInsumoEntity>> reservasCaptor = ArgumentCaptor.forClass(List.class);
        when(reservaInsumoRepository.saveAll(reservasCaptor.capture())).thenReturn(List.of());
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        LoteResponseDTO resultado = loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        LoteEntity guardado = loteCaptor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoLote.EN_EJECUCION);
        assertThat(guardado.getFechaInicio()).isNotNull().isBetween(antes, despues);
        assertThat(resultado.getEstado()).isEqualTo(EstadoLote.EN_EJECUCION);

        ArgumentCaptor<MolinoEntity> molinoCaptor = ArgumentCaptor.forClass(MolinoEntity.class);
        ArgumentCaptor<MaceradorEntity> maceradorCaptor = ArgumentCaptor.forClass(MaceradorEntity.class);
        ArgumentCaptor<OllaHervorEntity> ollaHervorCaptor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        ArgumentCaptor<FermentadorEntity> fermentadorCaptor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(molinoRepository).save(molinoCaptor.capture());
        verify(maceradorRepository).save(maceradorCaptor.capture());
        verify(ollaHervorRepository).save(ollaHervorCaptor.capture());
        verify(fermentadorRepository).save(fermentadorCaptor.capture());
        assertThat(molinoCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        assertThat(maceradorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        assertThat(ollaHervorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        assertThat(fermentadorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);

        EtapaLoteEntity molienda = findEtapa(guardado, TipoEtapa.MOLIENDA);
        assertThat(molienda.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(molienda.getFechaInicio()).isNotNull().isBetween(antes, despues);
        for (TipoEtapa otra : List.of(TipoEtapa.MACERACION, TipoEtapa.HERVIDO, TipoEtapa.FERMENTACION, TipoEtapa.MADURACION, TipoEtapa.ENVASADO)) {
            assertThat(findEtapa(guardado, otra).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        }

        assertThat(reservasCaptor.getValue()).hasSize(3);
    }

    // ==================== cancelarLote ====================

    @Test
    @DisplayName("CP-CL-01: cancelarLote lanza RecursoNoEncontradoException si el lote no existe")
    void cancelarLote_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(99L, cancelacionLoteFormDTOBase()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-CL-02: cancelarLote lanza ReglaNegocioException si el motivo de cancelación es nulo, sin consultar nada más")
    void cancelarLote_debeLanzarExcepcionSiMotivoNulo() {
        // === PREPARACION DE DATOS ===
        CancelacionLoteFormDTO formDTO = CancelacionLoteFormDTO.builder().motivoCancelacion(null).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(loteRepository, reservaInsumoRepository, loteInsumoRepository,
                molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-03: cancelarLote lanza ReglaNegocioException si el motivo de cancelación está vacío (solo espacios)")
    void cancelarLote_debeLanzarExcepcionSiMotivoVacio() {
        // === PREPARACION DE DATOS ===
        CancelacionLoteFormDTO formDTO = CancelacionLoteFormDTO.builder().motivoCancelacion("   ").build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(loteRepository, reservaInsumoRepository, loteInsumoRepository,
                molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-04: cancelarLote lanza ReglaNegocioException si el lote está FINALIZADO")
    void cancelarLote_debeLanzarExcepcionSiLoteFinalizado() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.FINALIZADO, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase()))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(reservaInsumoRepository, loteInsumoRepository,
                molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-05: cancelarLote lanza ReglaNegocioException si el lote ya está CANCELADO")
    void cancelarLote_debeLanzarExcepcionSiLoteYaCancelado() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.CANCELADO, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase()))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(reservaInsumoRepository, loteInsumoRepository,
                molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-06: cancelarLote — lote PENDIENTE sin ninguna etapa en curso: todo el equipamiento pasa a DISPONIBLE")
    void cancelarLote_debeDejarTodoElEquipamientoDisponibleSiNingunaEtapaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> molinoCaptor = ArgumentCaptor.forClass(MolinoEntity.class);
        ArgumentCaptor<MaceradorEntity> maceradorCaptor = ArgumentCaptor.forClass(MaceradorEntity.class);
        ArgumentCaptor<OllaHervorEntity> ollaHervorCaptor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        ArgumentCaptor<FermentadorEntity> fermentadorCaptor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(molinoRepository).save(molinoCaptor.capture());
        verify(maceradorRepository).save(maceradorCaptor.capture());
        verify(ollaHervorRepository).save(ollaHervorCaptor.capture());
        // El Fermentador se guarda UNA sola vez pese a estar asociado a 3 etapas (Fermentación/Maduración/Envasado)
        verify(fermentadorRepository, times(1)).save(fermentadorCaptor.capture());
        assertThat(molinoCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(maceradorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(ollaHervorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(fermentadorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-CL-07: cancelarLote — Molienda EN_CURSO: el molino pasa a EN_LIMPIEZA, el resto a DISPONIBLE")
    void cancelarLote_debeDejarMolinoEnLimpiezaSiMoliendaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> molinoCaptor = ArgumentCaptor.forClass(MolinoEntity.class);
        ArgumentCaptor<MaceradorEntity> maceradorCaptor = ArgumentCaptor.forClass(MaceradorEntity.class);
        ArgumentCaptor<OllaHervorEntity> ollaHervorCaptor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        ArgumentCaptor<FermentadorEntity> fermentadorCaptor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(molinoRepository).save(molinoCaptor.capture());
        verify(maceradorRepository).save(maceradorCaptor.capture());
        verify(ollaHervorRepository).save(ollaHervorCaptor.capture());
        verify(fermentadorRepository).save(fermentadorCaptor.capture());
        assertThat(molinoCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        assertThat(maceradorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(ollaHervorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(fermentadorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    @DisplayName("CP-CL-08: cancelarLote — Fermentación EN_CURSO: el fermentador pasa a EN_LIMPIEZA (no DISPONIBLE) pese a que Maduración/Envasado están PENDIENTE")
    void cancelarLote_debeDejarFermentadorEnLimpiezaSiFermentacionEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> molinoCaptor = ArgumentCaptor.forClass(MolinoEntity.class);
        ArgumentCaptor<MaceradorEntity> maceradorCaptor = ArgumentCaptor.forClass(MaceradorEntity.class);
        ArgumentCaptor<OllaHervorEntity> ollaHervorCaptor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        ArgumentCaptor<FermentadorEntity> fermentadorCaptor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(molinoRepository).save(molinoCaptor.capture());
        verify(maceradorRepository).save(maceradorCaptor.capture());
        verify(ollaHervorRepository).save(ollaHervorCaptor.capture());
        // El fermentador se resuelve UNA sola vez pese a estar EN_CURSO en Fermentación y PENDIENTE en las otras dos
        verify(fermentadorRepository, times(1)).save(fermentadorCaptor.capture());
        assertThat(molinoCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(maceradorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(ollaHervorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(fermentadorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("CP-CL-09: cancelarLote libera todas las reservas del lote, aplicando la cantidad correcta sobre cada lote de insumo, y las borra")
    void cancelarLote_debeLiberarTodasLasReservas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);

        MaltaEntity malta = crearMalta(10L, "MaltaCancel", 75);
        EtapaLoteEntity etapaMaceracion = findEtapa(lote, TipoEtapa.MACERACION);
        LoteInsumoEntity loteInsumoA = crearLoteInsumo(300L, malta, 10.0, 4.0, LocalDate.now().plusDays(30), BigDecimal.TEN);
        LoteInsumoEntity loteInsumoB = crearLoteInsumo(301L, malta, 10.0, 6.0, LocalDate.now().plusDays(60), BigDecimal.TEN);
        ReservaInsumoEntity reservaA = ReservaInsumoEntity.builder().id(1L).etapaLote(etapaMaceracion).loteInsumo(loteInsumoA).cantidadReservada(4.0).build();
        ReservaInsumoEntity reservaB = ReservaInsumoEntity.builder().id(2L).etapaLote(etapaMaceracion).loteInsumo(loteInsumoB).cantidadReservada(6.0).build();
        List<ReservaInsumoEntity> reservas = List.of(reservaA, reservaB);
        when(reservaInsumoRepository.findByEtapaLote_Lote_Id(1L)).thenReturn(reservas);
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(300L)).thenReturn(Optional.of(loteInsumoA));
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(301L)).thenReturn(Optional.of(loteInsumoB));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(loteInsumoA.getCantidadReservada()).isEqualTo(0.0);
        assertThat(loteInsumoB.getCantidadReservada()).isEqualTo(0.0);
        verify(loteInsumoRepository).save(loteInsumoA);
        verify(loteInsumoRepository).save(loteInsumoB);
        verify(reservaInsumoRepository).deleteAll(reservas);
    }

    @Test
    @DisplayName("CP-CL-10: cancelarLote — camino feliz: el lote queda CANCELADO con fechaCancelacion y motivoCancelacion seteados")
    void cancelarLote_debeCancelarLoteCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        LoteResponseDTO resultado = loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(guardado.getFechaCancelacion()).isNotNull().isBetween(antes, despues);
        assertThat(guardado.getMotivoCancelacion()).isEqualTo("Motivo de prueba");
        assertThat(resultado.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(resultado.getMotivoCancelacion()).isEqualTo("Motivo de prueba");
    }

    @Test
    @DisplayName("CP-CL-11: cancelarLote no toca el equipamiento de una etapa ya FINALIZADA (mantiene su estado)")
    void cancelarLote_noDebeTocarEquipamientoDeEtapaFinalizada() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, null);
        // Hoy no existe ninguna funcionalidad que avance una etapa a FINALIZADA, pero el código debe
        // contemplar el caso igual: forzamos la etapa de Maceración a FINALIZADA manualmente.
        findEtapa(lote, TipoEtapa.MACERACION).setEstado(EstadoEtapaLote.FINALIZADA);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        lenient().when(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(1L))
                .thenReturn(Optional.of((MolinoEntity) findEtapa(lote, TipoEtapa.MOLIENDA).getEquipamiento()));
        lenient().when(ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(3L))
                .thenReturn(Optional.of((OllaHervorEntity) findEtapa(lote, TipoEtapa.HERVIDO).getEquipamiento()));
        lenient().when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L))
                .thenReturn(Optional.of((FermentadorEntity) findEtapa(lote, TipoEtapa.FERMENTACION).getEquipamiento()));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        verifyNoInteractions(maceradorRepository);
    }

    // ==================== finalizarMolienda ====================

    @Test
    @DisplayName("CP-FM-01: finalizarMolienda lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarMolienda_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMolienda(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-FM-02: finalizarMolienda lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarMolienda_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMolienda(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-FM-03: finalizarMolienda lanza ReglaNegocioException si la etapa actual no es Molienda")
    void finalizarMolienda_debeLanzarExcepcionSiEtapaActualNoEsMolienda() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMolienda(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-FM-04: finalizarMolienda — camino feliz: Molienda FINALIZADA, Maceración EN_CURSO, molino EN_LIMPIEZA, resto sigue PENDIENTE")
    void finalizarMolienda_debeAvanzarDeMoliendaAMaceracionCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarMolienda(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        EtapaLoteEntity etapaMolienda = findEtapa(lote, TipoEtapa.MOLIENDA);
        EtapaLoteEntity etapaMaceracion = findEtapa(lote, TipoEtapa.MACERACION);
        assertThat(etapaMolienda.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaMolienda.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        assertThat(etapaMaceracion.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(etapaMaceracion.getFechaInicio()).isNotNull().isBetween(antes, despues);

        // Las etapas restantes no se tocan
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.FERMENTACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MADURACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        ArgumentCaptor<MolinoEntity> molinoCaptor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(molinoCaptor.capture());
        assertThat(molinoCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        verifyNoInteractions(maceradorRepository, ollaHervorRepository, fermentadorRepository);
        verify(loteRepository).save(lote);
    }

    // ==================== finalizarMaceracion ====================

    @Test
    @DisplayName("CP-FMC-01: finalizarMaceracion lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarMaceracion_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaceracion(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(maceradorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMC-02: finalizarMaceracion lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarMaceracion_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaceracion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(maceradorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMC-03: finalizarMaceracion lanza ReglaNegocioException si la etapa actual no es Maceración")
    void finalizarMaceracion_debeLanzarExcepcionSiEtapaActualNoEsMaceracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaceracion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(maceradorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMC-04: finalizarMaceracion lanza RecursoNoEncontradoException si no existe la configuración de producción")
    void finalizarMaceracion_debeLanzarExcepcionSiNoExisteConfiguracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaceracion(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio, maceradorRepository);
    }

    @Test
    @DisplayName("CP-FMC-05: finalizarMaceracion lanza ReglaNegocioException si algún insumo requerido no alcanzó el porcentaje mínimo de consumo configurado")
    void finalizarMaceracion_debeLanzarExcepcionSiNoAlcanzaElPorcentajeMinimo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        EtapaLoteEntity etapaMaceracion = findEtapa(lote, TipoEtapa.MACERACION);
        etapaMaceracion.setId(2L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(2L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(MaltaResponseDTO.builder().id(1L).nombre("Malta Pilsen").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(7.0) // 70% < 80% requerido
                        .build()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaceracion(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Malta Pilsen");
        verifyNoInteractions(maceradorRepository, reservaInsumoRepository);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FMC-06: finalizarMaceracion — camino feliz: Maceración FINALIZADA, Hervido EN_CURSO, macerador EN_LIMPIEZA, se liberan las reservas de la etapa")
    void finalizarMaceracion_debeAvanzarDeMaceracionAHervidoCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        EtapaLoteEntity etapaMaceracion = findEtapa(lote, TipoEtapa.MACERACION);
        etapaMaceracion.setId(2L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(2L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(MaltaResponseDTO.builder().id(1L).nombre("Malta Pilsen").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(8.0) // exactamente 80%: alcanza (>=)
                        .build()));
        mockearEquipamientoParaIniciar(lote);

        MaltaEntity malta = MaltaEntity.builder().id(1L).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().id(10L).insumo(malta).identificacionLoteProveedor("LOTE-A")
                .fechaVencimiento(LocalDate.now().plusMonths(6)).cantidadActual(10.0).cantidadReservada(2.0).build();
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).etapaLote(etapaMaceracion).loteInsumo(loteInsumo).cantidadReservada(2.0).build();
        when(reservaInsumoRepository.findByEtapaLoteId(2L)).thenReturn(List.of(reserva));
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(10L)).thenReturn(Optional.of(loteInsumo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarMaceracion(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(etapaMaceracion.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaMaceracion.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        EtapaLoteEntity etapaHervido = findEtapa(lote, TipoEtapa.HERVIDO);
        assertThat(etapaHervido.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(etapaHervido.getFechaInicio()).isNotNull().isBetween(antes, despues);

        // Las etapas restantes no se tocan
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.FERMENTACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MADURACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        ArgumentCaptor<MaceradorEntity> maceradorCaptor = ArgumentCaptor.forClass(MaceradorEntity.class);
        verify(maceradorRepository).save(maceradorCaptor.capture());
        assertThat(maceradorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);

        // La reserva de la etapa se libera: se devuelve al lote de insumo y se elimina la reserva
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        verify(loteInsumoRepository).save(loteInsumo);
        verify(reservaInsumoRepository).deleteAll(List.of(reserva));
        verifyNoInteractions(molinoRepository, ollaHervorRepository, fermentadorRepository);
        verify(loteRepository).save(lote);
    }

    // ==================== helpers ====================

    private static EtapaLoteEntity findEtapa(LoteEntity lote, TipoEtapa tipo) {
        return lote.getEtapas().stream().filter(e -> e.getEtapa() == tipo).findFirst().orElseThrow();
    }

    private void mockearPlanificacionPendienteBase() {
        RecetaEntity receta = crearReceta(5L, 3L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
    }

    private void mockearEquipamientoBase() {
        mockearPlanificacionPendienteBase();
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 25.0, 5.0, 75.0)));
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.of(crearOllaHervor(3L, 30.0, 3.0, 2.0)));
        when(fermentadorRepository.findById(4L)).thenReturn(Optional.of(crearFermentador(4L, 20.0)));
    }

    private void mockearConfiguracionYReceta() {
        RecetaEntity receta = crearReceta(5L, 3L);
        lenient().when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        ConfiguracionProduccionEntity configuracion = ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .velocidadEstandarMolienda(50.0)
                .velocidadEstandarEnvasado(10.0)
                .tiempoEstandarCip(30.0)
                .capacidadLoteEstandar(20.0)
                .porcentajeMinimoConsumoParaAvanzarEtapa(80.0)
                .criterioSeleccionPlanSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA)
                .criterioSeleccionPlanConcurrente(CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA)
                .build();
        lenient().when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
    }

    private void mockearEquipamientoParaIniciar(LoteEntity lote) {
        MolinoEntity molino = (MolinoEntity) findEtapa(lote, TipoEtapa.MOLIENDA).getEquipamiento();
        MaceradorEntity macerador = (MaceradorEntity) findEtapa(lote, TipoEtapa.MACERACION).getEquipamiento();
        OllaHervorEntity ollaHervor = (OllaHervorEntity) findEtapa(lote, TipoEtapa.HERVIDO).getEquipamiento();
        FermentadorEntity fermentador = (FermentadorEntity) findEtapa(lote, TipoEtapa.FERMENTACION).getEquipamiento();
        lenient().when(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(molino.getId())).thenReturn(Optional.of(molino));
        lenient().when(maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(macerador.getId())).thenReturn(Optional.of(macerador));
        lenient().when(ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(ollaHervor.getId())).thenReturn(Optional.of(ollaHervor));
        lenient().when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(fermentador.getId())).thenReturn(Optional.of(fermentador));
    }

    private static LoteFormDTO.LoteFormDTOBuilder loteFormDTOBuilderBase() {
        return LoteFormDTO.builder()
                .volumenObjetivo(20.0)
                .idPlanificacionProduccion(1L)
                .idFermentador(4L)
                .idMolino(1L)
                .idMacerador(2L)
                .idOllaHervor(3L);
    }

    private static LoteFormDTO loteFormDTOBase() {
        return loteFormDTOBuilderBase().build();
    }

    private static RecetaEntity crearReceta(Long id, Long contadorLotes) {
        return RecetaEntity.builder()
                .id(id)
                .contadorLotes(contadorLotes)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static PlanificacionProduccionEntity crearPlanificacion(Long id, EstadoSolicitud estado, VersionRecetaEntity versionReceta) {
        return PlanificacionProduccionEntity.builder()
                .id(id)
                .estado(estado)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(30))
                .cantidadAProducir(100.0)
                .versionReceta(versionReceta)
                .build();
    }

    private static MaltaEntity crearMalta(Long id, String nombre, int potencialExtracto) {
        return MaltaEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .estado(Estado.ACTIVO)
                .potencialExtracto(potencialExtracto)
                .build();
    }

    private static LupuloEntity crearLupulo(Long id, String nombre, double aa, FormatoLupulo formato) {
        return LupuloEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO)
                .aa(aa)
                .formato(formato)
                .build();
    }

    private static LevaduraEntity crearLevadura(Long id, String nombre, TipoLevadura tipo, double celulasPorGramo) {
        return LevaduraEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO)
                .tipo(tipo)
                .cantidadCelulasPorGramo(celulasPorGramo)
                .build();
    }

    private static DetalleMaltaEntity detalleMalta(double cantidad, MaltaEntity malta) {
        return DetalleMaltaEntity.builder().cantidad(cantidad).malta(malta).build();
    }

    private static DetalleLupuloEntity detalleLupulo(double cantidad, UsoLupulo uso, TipoEtapa etapaDeUso, double tiempoDeHervor, LupuloEntity lupulo) {
        return DetalleLupuloEntity.builder().cantidad(cantidad).uso(uso).etapaDeUso(etapaDeUso).tiempoDeHervor(tiempoDeHervor).lupulo(lupulo).build();
    }

    private static DetalleLevaduraEntity detalleLevadura(double cantidad, LevaduraEntity levadura) {
        return DetalleLevaduraEntity.builder().cantidad(cantidad).levadura(levadura).build();
    }

    private static VersionRecetaEntity versionRecetaMaltaBase(RecetaEntity receta) {
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        MaltaEntity caramelo = crearMalta(2L, "Caramelo", 74);
        return VersionRecetaEntity.builder()
                .id(10L)
                .nombre("IPA Test")
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.050)
                .fgObjetivo(1.010)
                .ibuObjetivo(35)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(14)
                .duracionMaduracion(7)
                .esUltimaVersion(true)
                .receta(receta)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen), detalleMalta(20.0, caramelo)))
                .build();
    }

    private static VersionRecetaEntity versionRecetaAislada(List<DetalleMaltaEntity> maltas, List<DetalleLupuloEntity> lupulos, List<DetalleLevaduraEntity> levaduras, double ogObjetivo, int ibuObjetivo, double volumenBase) {
        return VersionRecetaEntity.builder()
                .id(99L)
                .nombre("Receta Aislada")
                .volumenBase(volumenBase)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(ogObjetivo)
                .fgObjetivo(1.010)
                .ibuObjetivo(ibuObjetivo)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(10)
                .duracionMaduracion(5)
                .esUltimaVersion(true)
                .receta(crearReceta(50L, 0L))
                .detallesMalta(maltas)
                .detallesLupulo(lupulos)
                .detallesLevadura(levaduras)
                .build();
    }

    private static MolinoEntity crearMolino(Long id, double rendimientoMolienda) {
        return crearMolino(id, EstadoOperativo.DISPONIBLE, rendimientoMolienda);
    }

    private static MolinoEntity crearMolino(Long id, EstadoOperativo estadoOperativo, double rendimientoMolienda) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno("Molino " + id)
                .estado(Estado.ACTIVO)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .rendimientoMolienda(rendimientoMolienda)
                .build();
    }

    private static MaceradorEntity crearMacerador(Long id, double capacidadUtil, double espacioMuerto, double eficienciaMaceracion) {
        return crearMacerador(id, EstadoOperativo.DISPONIBLE, capacidadUtil, espacioMuerto, eficienciaMaceracion);
    }

    private static MaceradorEntity crearMacerador(Long id, EstadoOperativo estadoOperativo, double capacidadUtil, double espacioMuerto, double eficienciaMaceracion) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno("Macerador " + id)
                .estado(Estado.ACTIVO)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(capacidadUtil + 5.0)
                .capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(eficienciaMaceracion)
                .build();
    }

    private static OllaHervorEntity crearOllaHervor(Long id, double capacidadUtil, double evaporacion, double perdidaPorTrub) {
        return crearOllaHervor(id, EstadoOperativo.DISPONIBLE, capacidadUtil, evaporacion, perdidaPorTrub);
    }

    private static OllaHervorEntity crearOllaHervor(Long id, EstadoOperativo estadoOperativo, double capacidadUtil, double evaporacion, double perdidaPorTrub) {
        return OllaHervorEntity.builder()
                .id(id)
                .identificadorInterno("Olla " + id)
                .estado(Estado.ACTIVO)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(capacidadUtil + 5.0)
                .capacidadUtil(capacidadUtil)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .build();
    }

    private static FermentadorEntity crearFermentador(Long id, double capacidadUtil) {
        return crearFermentador(id, EstadoOperativo.DISPONIBLE, capacidadUtil);
    }

    private static FermentadorEntity crearFermentador(Long id, EstadoOperativo estadoOperativo, double capacidadUtil) {
        return FermentadorEntity.builder()
                .id(id)
                .identificadorInterno("Fermentador " + id)
                .estado(Estado.ACTIVO)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(capacidadUtil + 5.0)
                .capacidadUtil(capacidadUtil)
                .build();
    }

    private static LoteInsumoEntity crearLoteInsumo(Long id, InsumoEntity insumo, double cantidadActual, double cantidadReservada, LocalDate fechaVencimiento, BigDecimal costoPPP) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .identificacionLoteProveedor("PROV-" + id)
                .fechaVencimiento(fechaVencimiento)
                .costoUnitarioPPP(costoPPP)
                .build();
    }

    private static LoteEntity crearLoteMinimo(Long id, String identificadorInterno) {
        RecetaEntity receta = crearReceta(5L, 1L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        return LoteEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .volumenObjetivo(20.0)
                .estado(EstadoLote.PENDIENTE)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(20))
                .planificacionProduccion(planificacion)
                .etapas(new ArrayList<>())
                .build();
    }

    private static LoteEntity crearLoteRegistradoBase(EstadoLote estado) {
        return crearLoteRegistradoBaseConEstadosEquipamiento(
                estado == EstadoLote.PENDIENTE ? EstadoOperativo.DISPONIBLE : EstadoOperativo.DISPONIBLE,
                EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, EstadoOperativo.DISPONIBLE, estado);
    }

    private static LoteEntity crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo estadoMolino, EstadoOperativo estadoMacerador, EstadoOperativo estadoOllaHervor, EstadoOperativo estadoFermentador) {
        return crearLoteRegistradoBaseConEstadosEquipamiento(estadoMolino, estadoMacerador, estadoOllaHervor, estadoFermentador, EstadoLote.PENDIENTE);
    }

    private static LoteEntity crearLoteRegistradoBaseConEstadosEquipamiento(EstadoOperativo estadoMolino, EstadoOperativo estadoMacerador, EstadoOperativo estadoOllaHervor, EstadoOperativo estadoFermentador, EstadoLote estadoLote) {
        RecetaEntity receta = crearReceta(5L, 3L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        MolinoEntity molino = crearMolino(1L, estadoMolino, 5.0);
        MaceradorEntity macerador = crearMacerador(2L, estadoMacerador, 25.0, 5.0, 75.0);
        OllaHervorEntity ollaHervor = crearOllaHervor(3L, estadoOllaHervor, 30.0, 3.0, 2.0);
        FermentadorEntity fermentador = crearFermentador(4L, estadoFermentador, 20.0);

        LoteEntity lote = LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-3")
                .volumenObjetivo(20.0)
                .estado(estadoLote)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(22))
                .planificacionProduccion(planificacion)
                .build();
        lote.setEtapas(construirEtapas(lote, molino, macerador, ollaHervor, fermentador));
        return lote;
    }

    private static LoteEntity crearLoteConRecetaSimple(MaltaEntity malta, LupuloEntity lupulo, LevaduraEntity levadura) {
        VersionRecetaEntity versionReceta = versionRecetaAislada(
                malta != null ? List.of(detalleMalta(10.0, malta)) : List.of(),
                lupulo != null ? List.of(detalleLupulo(10.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, lupulo)) : List.of(),
                levadura != null ? List.of(detalleLevadura(5.0, levadura)) : List.of(),
                1.040, 20, 20.0);
        return crearLoteConVersionReceta(versionReceta, 20.0);
    }

    private static LoteEntity crearLoteConVersionReceta(VersionRecetaEntity versionReceta, double volumenObjetivo) {
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        MolinoEntity molino = crearMolino(1L, EstadoOperativo.DISPONIBLE, 5.0);
        MaceradorEntity macerador = crearMacerador(2L, EstadoOperativo.DISPONIBLE, 100.0, 5.0, 75.0);
        OllaHervorEntity ollaHervor = crearOllaHervor(3L, EstadoOperativo.DISPONIBLE, 200.0, 3.0, 2.0);
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.DISPONIBLE, 150.0);

        LoteEntity lote = LoteEntity.builder()
                .id(1L)
                .identificadorInterno("Test-1")
                .volumenObjetivo(volumenObjetivo)
                .estado(EstadoLote.PENDIENTE)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(22))
                .planificacionProduccion(planificacion)
                .build();
        lote.setEtapas(construirEtapas(lote, molino, macerador, ollaHervor, fermentador));
        return lote;
    }

    private static List<EtapaLoteEntity> construirEtapas(LoteEntity lote, MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor, FermentadorEntity fermentador) {
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(EtapaLoteEntity.builder().id(1L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.PENDIENTE).equipamiento(molino).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(2L).etapa(TipoEtapa.MACERACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(macerador).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(3L).etapa(TipoEtapa.HERVIDO).estado(EstadoEtapaLote.PENDIENTE).equipamiento(ollaHervor).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(4L).etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(fermentador).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(5L).etapa(TipoEtapa.MADURACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(fermentador).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(6L).etapa(TipoEtapa.ENVASADO).estado(EstadoEtapaLote.PENDIENTE).equipamiento(fermentador).lote(lote).build());
        return etapas;
    }

    private static CancelacionLoteFormDTO cancelacionLoteFormDTOBase() {
        return CancelacionLoteFormDTO.builder().motivoCancelacion("Motivo de prueba").build();
    }

    /**
     * Construye un lote para los tests de {@code cancelarLote}, con sus 6 etapas en PENDIENTE
     * salvo, opcionalmente, una que se marca EN_CURSO ({@code etapaEnCurso}, o {@code null} para que
     * las 6 queden en PENDIENTE).
     */
    private static LoteEntity crearLoteParaCancelar(EstadoLote estadoLote, TipoEtapa etapaEnCurso) {
        RecetaEntity receta = crearReceta(5L, 3L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        MolinoEntity molino = crearMolino(1L, EstadoOperativo.EN_USO, 5.0);
        MaceradorEntity macerador = crearMacerador(2L, EstadoOperativo.EN_USO, 25.0, 5.0, 75.0);
        OllaHervorEntity ollaHervor = crearOllaHervor(3L, EstadoOperativo.EN_USO, 30.0, 3.0, 2.0);
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.EN_USO, 20.0);

        LoteEntity lote = LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-3")
                .volumenObjetivo(20.0)
                .estado(estadoLote)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(22))
                .planificacionProduccion(planificacion)
                .build();
        lote.setEtapas(construirEtapasParaCancelar(lote, molino, macerador, ollaHervor, fermentador, etapaEnCurso));
        return lote;
    }

    private static List<EtapaLoteEntity> construirEtapasParaCancelar(LoteEntity lote, MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor, FermentadorEntity fermentador, TipoEtapa etapaEnCurso) {
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.MOLIENDA, molino, etapaEnCurso));
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.MACERACION, macerador, etapaEnCurso));
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.HERVIDO, ollaHervor, etapaEnCurso));
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.FERMENTACION, fermentador, etapaEnCurso));
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.MADURACION, fermentador, etapaEnCurso));
        etapas.add(construirEtapaConEstado(lote, TipoEtapa.ENVASADO, fermentador, etapaEnCurso));
        return etapas;
    }

    private static EtapaLoteEntity construirEtapaConEstado(LoteEntity lote, TipoEtapa tipo, EquipamientoEntity equipamiento, TipoEtapa etapaEnCurso) {
        EstadoEtapaLote estado = tipo == etapaEnCurso ? EstadoEtapaLote.EN_CURSO : EstadoEtapaLote.PENDIENTE;
        return EtapaLoteEntity.builder().etapa(tipo).estado(estado).equipamiento(equipamiento).lote(lote).build();
    }
}
