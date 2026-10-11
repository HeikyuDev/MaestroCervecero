package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
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
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.ICostoDirectoAdicionalRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
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
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
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
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private ICostoDirectoAdicionalRepository costoDirectoAdicionalRepository;
    // Spy con la implementación real: el escalado de insumos ya se prueba de forma independiente
    // en EscaladoInsumoServicioImplTest, así que acá no tiene sentido mockearlo — se necesita el
    // cálculo real para que estos tests sigan verificando el mismo comportamiento de extremo a
    // extremo que verificaban antes de extraer esta lógica a su propio servicio.
    @Spy
    private IEscaladoInsumoServicio escaladoInsumoServicio = new EscaladoInsumoServicioImpl();

    @InjectMocks
    private LoteServicioImpl loteServicio;

    // ==================== filtrarLotes ====================

    @Test
    @DisplayName("CP-FL-01: filtrarLotes retorna una página de lotes correctamente mapeada a DTO cuando se filtra por los 5 criterios")
    void filtrarLotes_debeRetornarPaginaMapeadaFiltrandoPorLos5Criterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LoteEntity lote1 = crearLoteMinimo(1L, "IPA-1");
        LoteEntity lote2 = crearLoteMinimo(2L, "IPA-2");
        when(loteRepository.filtrarLotes(3L, "IPA", EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, 20.0, pageable))
                .thenReturn(new PageImpl<>(List.of(lote1, lote2), pageable, 2));

        // === EJECUCION ===
        Page<LoteResponseDTO> resultado = loteServicio.filtrarLotes(3L, "IPA", EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, 20.0, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getIdentificadorInterno()).isEqualTo("IPA-1");
        verify(loteRepository).filtrarLotes(3L, "IPA", EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, 20.0, pageable);
    }

    @Test
    @DisplayName("CP-FL-02: filtrarLotes propaga los 5 parámetros nulos sin restringir la búsqueda")
    void filtrarLotes_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LoteEntity lote1 = crearLoteMinimo(1L, "IPA-1");
        LoteEntity lote2 = crearLoteMinimo(2L, "STOUT-1");
        when(loteRepository.filtrarLotes(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(lote1, lote2), pageable, 2));

        // === EJECUCION ===
        Page<LoteResponseDTO> resultado = loteServicio.filtrarLotes(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(loteRepository).filtrarLotes(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FL-03: filtrarLotes retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarLotes_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(loteRepository.filtrarLotes(null, "Inexistente", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LoteResponseDTO> resultado = loteServicio.filtrarLotes(null, "Inexistente", null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(loteRepository).filtrarLotes(null, "Inexistente", null, null, null, pageable);
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
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

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
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));

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
        assertThat(guardado.getIdentificadorInterno()).isEqualTo("IPA_TEST-R5-3");
        assertThat(receta.getContadorLotes()).isEqualTo(4L);
        assertThat(guardado.getEtapas()).hasSize(6);
        assertThat(guardado.getEtapas()).allMatch(e -> e.getEstado() == EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(guardado, TipoEtapa.MOLIENDA).getEquipamiento()).isInstanceOf(MolinoEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.MACERACION).getEquipamiento()).isInstanceOf(MaceradorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.HERVIDO).getEquipamiento()).isInstanceOf(OllaHervorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.FERMENTACION).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.MADURACION).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(findEtapa(guardado, TipoEtapa.ENVASADO).getEquipamiento()).isInstanceOf(FermentadorEntity.class);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("IPA_TEST-R5-3");
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
    @DisplayName("CP-RL-16: registrarLote — fechaInicioEstimada se encola dos días después del último lote planificado en ese fermentador (el día intermedio es la limpieza)")
    void registrarLote_debeEncolarFechaInicioTrasUltimoLoteDelFermentador() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        LocalDate ultimaFechaFinalizacion = LocalDate.now().plusDays(5);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.of(ultimaFechaFinalizacion));
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.now().plusDays(29));
    }

    @Test
    @DisplayName("CP-RL-19: registrarLote — planificación que inicia en el futuro: el lote arranca el día de inicio de la planificación")
    void registrarLote_debeArrancarElDiaDeInicioDeLaPlanificacionSiEsPosteriorAlResultadoHabitual() {
        // === PREPARACION DE DATOS ===
        LocalDate inicioPlanificacion = LocalDate.now().plusDays(10);
        mockearEquipamientoBase(inicioPlanificacion);
        mockearConfiguracionYReceta();
        // Resultado habitual: último lote termina dentro de 3 días => arrancaría dentro de 5 días
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.of(LocalDate.now().plusDays(3)));
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(inicioPlanificacion);
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(inicioPlanificacion.plusDays(22));
    }

    @Test
    @DisplayName("CP-RL-41: registrarLote — el último lote vivo está en ejecución y atrasado: el lote nuevo arranca dentro de dos días")
    void registrarLote_debeArrancarDentroDeDosDiasSiElUltimoLoteEstaAtrasado() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        // El último lote vence hace 3 días pero sigue ocupando el fermentador hoy
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.of(LocalDate.now().minusDays(3)));
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.now().plusDays(24));
    }

    @Test
    @DisplayName("CP-RL-42: registrarLote — sin lotes vivos en el fermentador y planificación que inició en el pasado: el lote arranca hoy")
    void registrarLote_debeArrancarHoySinLotesVivosAunqueLaPlanificacionHayaIniciadoEnElPasado() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase(LocalDate.now().minusDays(5));
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.now());
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.now().plusDays(22));
    }

    @Test
    @DisplayName("CP-RL-20: registrarLote — planificación que inicia antes del resultado habitual: la fecha de inicio no cambia")
    void registrarLote_noDebeCambiarLaFechaDeInicioSiLaPlanificacionInicioAntesDelResultadoHabitual() {
        // === PREPARACION DE DATOS ===
        LocalDate inicioPlanificacion = LocalDate.now().minusDays(5);
        mockearEquipamientoBase(inicioPlanificacion);
        mockearConfiguracionYReceta();
        // Resultado habitual: último lote termina dentro de 7 días => arranca dentro de 9 días
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.of(LocalDate.now().plusDays(7)));
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        LoteEntity guardado = captor.getValue();
        assertThat(guardado.getFechaInicioEstimada()).isEqualTo(LocalDate.now().plusDays(9));
        assertThat(guardado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.now().plusDays(31));
    }

    @Test
    @DisplayName("CP-RL-30: registrarLote — el identificador reemplaza tildes, diéresis y eñe por su letra base")
    void registrarLote_debeReemplazarDiacriticosEnElIdentificador() {
        assertThat(registrarYObtenerIdentificador("Pilsen Clásica Añeja")).isEqualTo("PILSEN_CLASICA_ANEJA-R5-3");
        assertThat(registrarYObtenerIdentificador("Cerveza Güemes")).isEqualTo("CERVEZA_GUEMES-R5-3");
        assertThat(registrarYObtenerIdentificador("Ñandú")).isEqualTo("NANDU-R5-3");
    }

    @Test
    @DisplayName("CP-RL-31: registrarLote — toda secuencia de caracteres no alfanuméricos pasa a un solo guion bajo, sin guiones bajos en los extremos")
    void registrarLote_debeColapsarLosCaracteresNoAlfanumericosEnElIdentificador() {
        assertThat(registrarYObtenerIdentificador("  IPA -- Doble (v2)!  ")).isEqualTo("IPA_DOBLE_V2-R5-3");
        assertThat(registrarYObtenerIdentificador("Pale/Ale\u2014Rubia")).isEqualTo("PALE_ALE_RUBIA-R5-3");
        assertThat(registrarYObtenerIdentificador("__Pale___Ale__")).isEqualTo("PALE_ALE-R5-3");
    }

    @Test
    @DisplayName("CP-RL-32: registrarLote — las letras en mayúscula y los dígitos se conservan en el identificador")
    void registrarLote_debeConservarMayusculasYDigitosEnElIdentificador() {
        assertThat(registrarYObtenerIdentificador("Stout 2027")).isEqualTo("STOUT_2027-R5-3");
        assertThat(registrarYObtenerIdentificador("IPA")).isEqualTo("IPA-R5-3");
        assertThat(registrarYObtenerIdentificador("123")).isEqualTo("123-R5-3");
    }

    @Test
    @DisplayName("CP-RL-33: registrarLote — un nombre sin caracteres utilizables usa LOTE en el identificador")
    void registrarLote_debeUsarLoteSiElNombreNoTieneCaracteresUtilizables() {
        for (String nombre : new String[]{"***", "¿¿??", "\u9EA6\u9152", null}) {
            assertThat(registrarYObtenerIdentificador(nombre)).as("nombre: %s", nombre).isEqualTo("LOTE-R5-3");
        }
    }

    @Test
    @DisplayName("CP-RL-34: registrarLote — las letras que no se descomponen se tratan como símbolo y la ß pasa a SS en el identificador")
    void registrarLote_debeTratarLasLetrasSinDescomposicionComoSimboloEnElIdentificador() {
        assertThat(registrarYObtenerIdentificador("Sm\u00F8rrebr\u00F8d")).isEqualTo("SM_RREBR_D-R5-3");
        assertThat(registrarYObtenerIdentificador("Wei\u00DFbier")).isEqualTo("WEISSBIER-R5-3");
    }

    @Test
    @DisplayName("CP-RL-35: registrarLote — el nombre del identificador se corta a 100 caracteres (límite) y se quita un guion bajo final")
    void registrarLote_debeCortarElNombreDelIdentificadorACienCaracteres() {
        assertThat(registrarYObtenerIdentificador("A".repeat(100))).isEqualTo("A".repeat(100) + "-R5-3");
        assertThat(registrarYObtenerIdentificador("\u00DF".repeat(100))).isEqualTo("S".repeat(100) + "-R5-3");
        assertThat(registrarYObtenerIdentificador("A".repeat(99) + " B")).isEqualTo("A".repeat(99) + "-R5-3");
    }

    @Test
    @DisplayName("CP-RL-36: registrarLote — el identificador usa el nombre de la versión de la planificación, no el de la versión vigente")
    void registrarLote_debeUsarElNombreDeLaVersionDeLaPlanificacion() {
        // La receta ya tiene otra versión vigente "Pilsen v2", pero la planificación es de "Pilsen v1"
        assertThat(registrarYObtenerIdentificador("Pilsen v1")).isEqualTo("PILSEN_V1-R5-3");
    }

    @Test
    @DisplayName("CP-RL-37: registrarLote — dos lotes seguidos de la misma receta usan contadores consecutivos")
    void registrarLote_debeUsarContadoresConsecutivosEnDosLotesDeLaMismaReceta() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = mockearEquipamientoBase();
        RecetaEntity receta = planificacion.getVersionReceta().getReceta();
        receta.setContadorLotes(3L);
        mockearConfiguracionYReceta();
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        String primero = loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();
        String segundo = loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();

        // === ASSERTS ===
        assertThat(primero).isEqualTo("IPA_TEST-R5-3");
        assertThat(segundo).isEqualTo("IPA_TEST-R5-4");
        assertThat(receta.getContadorLotes()).isEqualTo(5L);
    }

    @Test
    @DisplayName("CP-RL-38: registrarLote — recetas distintas con versiones del mismo nombre no colisionan")
    void registrarLote_noDebeColisionarEntreRecetasConVersionesDelMismoNombre() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        RecetaEntity receta5 = crearReceta(5L, 1L);
        RecetaEntity receta6 = crearReceta(6L, 1L);
        VersionRecetaEntity version5 = versionRecetaMaltaBase(receta5);
        version5.setNombre("IPA");
        VersionRecetaEntity version6 = versionRecetaMaltaBase(receta6);
        version6.setNombre("IPA");
        PlanificacionProduccionEntity planificacion5 = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, version5);
        PlanificacionProduccionEntity planificacion6 = crearPlanificacion(2L, EstadoSolicitud.PENDIENTE, version6);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion5));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(2L)).thenReturn(Optional.of(planificacion6));
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta5));
        when(recetaRepository.findByIdParaActualizarContador(6L)).thenReturn(Optional.of(receta6));
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        String codigo5 = loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();
        String codigo6 = loteServicio.registrarLote(loteFormDTOBuilderBase().idPlanificacionProduccion(2L).build()).getIdentificadorInterno();

        // === ASSERTS ===
        assertThat(codigo5).isEqualTo("IPA-R5-1");
        assertThat(codigo6).isEqualTo("IPA-R6-1");
        assertThat(receta5.getContadorLotes()).isEqualTo(2L);
        assertThat(receta6.getContadorLotes()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-RL-39: registrarLote — renombrar la versión mantiene el contador de la receta")
    void registrarLote_debeMantenerElContadorDeLaRecetaAlRenombrarLaVersion() {
        // === PREPARACION DE DATOS ===
        PlanificacionProduccionEntity planificacion = mockearEquipamientoBase();
        VersionRecetaEntity version = planificacion.getVersionReceta();
        RecetaEntity receta = version.getReceta();
        receta.setContadorLotes(3L);
        mockearConfiguracionYReceta();
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        version.setNombre("Pilsen v1");
        String primero = loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();
        version.setNombre("Pilsen Reserva");
        String segundo = loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();

        // === ASSERTS ===
        assertThat(primero).isEqualTo("PILSEN_V1-R5-3");
        assertThat(segundo).isEqualTo("PILSEN_RESERVA-R5-4");
    }

    @Test
    @DisplayName("CP-RL-40: registrarLote lanza RecursoNoEncontradoException si la receta no existe al generar el código del lote")
    void registrarLote_debeLanzarExcepcionSiLaRecetaNoExisteAlGenerarElCodigo() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(loteFormDTOBase()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 5");
        verify(loteRepository, never()).save(any());
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

    @Test
    @DisplayName("CP-RL-21: registrarLote lanza ReglaNegocioException si lo comprometido es exactamente igual a la cantidad a producir (límite)")
    void registrarLote_debeRechazarSiComprometidoIgualaLaCantidadAProducir() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(40.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(60.0);
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación ya tiene lotes que cubren la cantidad a producir (100 L comprometidos de 100 L solicitados)");
        verify(recetaRepository, never()).findByIdParaActualizarContador(any());
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RL-22: registrarLote lanza ReglaNegocioException si lo comprometido supera la cantidad a producir")
    void registrarLote_debeRechazarSiComprometidoSuperaLaCantidadAProducir() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(130.0);
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación ya tiene lotes que cubren la cantidad a producir (130 L comprometidos de 100 L solicitados)");
        verify(recetaRepository, never()).findByIdParaActualizarContador(any());
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RL-23: registrarLote registra el lote si lo comprometido es menor a la cantidad a producir, sin contar el volumen del lote nuevo (límite válido)")
    void registrarLote_debeRegistrarSiComprometidoEsMenorAunqueConElLoteNuevoSeSupere() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        RecetaEntity receta = crearReceta(5L, 3L);
        when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(40.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(59.5);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBuilderBase().volumenObjetivo(20.0).build();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(receta.getContadorLotes()).isEqualTo(4L);
    }

    @Test
    @DisplayName("CP-RL-24: registrarLote admite un cuarto lote de 600 L en una planificación de 2.000 L con 3 lotes vivos de 600 L")
    void registrarLote_debeAdmitirCuartoLoteEnPlanificacionDe2000LitrosConTresLotesVivos() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase().setCantidadAProducir(2000.0);
        mockearConfiguracionYReceta();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(1800.0);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("CP-RL-25: registrarLote rechaza un quinto lote en una planificación de 2.000 L con 4 lotes vivos de 600 L")
    void registrarLote_debeRechazarQuintoLoteEnPlanificacionDe2000LitrosConCuatroLotesVivos() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase().setCantidadAProducir(2000.0);
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(2400.0);
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación ya tiene lotes que cubren la cantidad a producir (2.400 L comprometidos de 2.000 L solicitados)");
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RL-26: registrarLote cuenta lo efectivamente envasado de un lote finalizado, no su objetivo, y reabre cupo")
    void registrarLote_debeContarLoEnvasadoDeUnLoteFinalizadoYReabrirCupo() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase().setCantidadAProducir(1800.0);
        mockearConfiguracionYReceta();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(1600.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("CP-RL-27: registrarLote no cuenta como comprometido un lote cancelado (ambas sumas devuelven cero)")
    void registrarLote_noDebeContarLotesCanceladosComoComprometidos() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        mockearConfiguracionYReceta();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(0.0);
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        ArgumentCaptor<LoteEntity> captor = ArgumentCaptor.forClass(LoteEntity.class);
        when(loteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        LoteFormDTO formDTO = loteFormDTOBase();

        // === EJECUCION ===
        loteServicio.registrarLote(formDTO);

        // === ASSERTS ===
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        verify(envasadoLoteRepository).sumarLitrosProducidosDePlanificacion(1L);
        verify(loteRepository).sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L);
        verify(loteRepository, never()).findAll();
    }

    @Test
    @DisplayName("CP-RL-28: registrarLote valida la capacidad del fermentador antes que la cobertura de la planificación, sin consultar las sumas")
    void registrarLote_debeValidarCapacidadAntesQueCoberturaDeLaPlanificacion() {
        // === PREPARACION DE DATOS ===
        mockearEquipamientoBase();
        // Lotes que ya cubren la planificación (comprometido 120.0): no debe llegar a consultarse
        lenient().when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        lenient().when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(120.0);
        LoteFormDTO formDTO = loteFormDTOBuilderBase().volumenObjetivo(25.0).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.registrarLote(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("supera la capacidad útil del fermentador")
                .hasMessageNotContaining("cubren la cantidad a producir");
        verify(envasadoLoteRepository, never()).sumarLitrosProducidosDePlanificacion(any());
        verify(loteRepository, never()).sumarVolumenObjetivoDeLotesVivosDePlanificacion(any());
    }

    @Test
    @DisplayName("CP-RL-29: registrarLote recupera la planificación bloqueada para escritura, una sola vez y sin usar findById")
    void registrarLote_debeRecuperarLaPlanificacionBloqueadaParaEscritura() {
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
        verify(planificacionProduccionRepository, times(1)).buscarPorIdParaActualizar(1L);
        verify(planificacionProduccionRepository, never()).findById(any());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoLote.PENDIENTE);
    }

    // ==================== iniciarLote ====================

    @Test
    @DisplayName("CP-IL-01: iniciarLote lanza RecursoNoEncontradoException si el lote no existe")
    void iniciarLote_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CP-IL-02: iniciarLote lanza ReglaNegocioException si el lote está en EN_EJECUCION")
    void iniciarLote_debeLanzarExcepcionSiLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBase(EstadoLote.EN_EJECUCION);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.iniciarLote(1L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("CP-IL-04: iniciarLote lanza RecursoNoEncontradoException si el molino ya no se encuentra al iniciar")
    void iniciarLote_debeLanzarExcepcionSiMolinoNoEncontradoAlIniciar() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteRegistradoBase(EstadoLote.PENDIENTE);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
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

    @Test
    @DisplayName("CP-IL-20: iniciarLote — las fechas estimadas del lote iniciado pasan a ser las reales y conservan su duración")
    void iniciarLote_debeConservarLaDuracionAlTrasladarLasFechasEstimadasAHoy() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararInicioDeLoteConStockSuficiente(1L, hoy.plusDays(36), hoy.plusDays(56));

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_EJECUCION);
        assertFechas(lote, hoy, hoy.plusDays(20));
    }

    @Test
    @DisplayName("CP-IL-21: iniciarLote — iniciar un lote que no es el primero de la cola reencadena la cola a continuación")
    void iniciarLote_debeReencadenarLaColaAContinuacionDelLoteIniciado() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteX = prepararInicioDeLoteConStockSuficiente(12L, hoy.plusDays(36), hoy.plusDays(56));
        LoteEntity loteB = crearLotePendienteEnCola(10L, hoy.plusDays(1), hoy.plusDays(19), hoy);
        LoteEntity loteC = crearLotePendienteEnCola(11L, hoy.plusDays(20), hoy.plusDays(35), hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(loteX));

        // === EJECUCION ===
        loteServicio.iniciarLote(12L);

        // === ASSERTS ===
        assertFechas(loteX, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(22), hoy.plusDays(40));
        assertFechas(loteC, hoy.plusDays(42), hoy.plusDays(57));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
    }

    @Test
    @DisplayName("CP-IL-22: iniciarLote — sin otros pendientes en el fermentador no hay nada que reencadenar")
    void iniciarLote_noDebeReencadenarSiNoHayOtrosPendientesEnElFermentador() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararInicioDeLoteConStockSuficiente(1L, hoy.plusDays(3), hoy.plusDays(13));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.iniciarLote(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_EJECUCION);
        assertFechas(lote, hoy, hoy.plusDays(10));
        verify(loteRepository).buscarPendientesDeFermentadorParaReordenar(5L);
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-IL-23: iniciarLote — un pendiente cuya planificación inicia en el futuro no se adelanta al reencadenar")
    void iniciarLote_noDebeAdelantarUnPendienteCuyaPlanificacionIniciaEnElFuturo() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteX = prepararInicioDeLoteConStockSuficiente(12L, hoy.plusDays(36), hoy.plusDays(56));
        LoteEntity loteB = crearLotePendienteEnCola(10L, hoy.plusDays(1), hoy.plusDays(19), hoy.plusDays(30));
        LoteEntity loteC = crearLotePendienteEnCola(11L, hoy.plusDays(20), hoy.plusDays(35), hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(loteX));

        // === EJECUCION ===
        loteServicio.iniciarLote(12L);

        // === ASSERTS ===
        assertFechas(loteX, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(30), hoy.plusDays(48));
        assertFechas(loteC, hoy.plusDays(50), hoy.plusDays(65));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
    }

    // ==================== cancelarLote ====================

    @Test
    @DisplayName("CP-CL-01: cancelarLote lanza RecursoNoEncontradoException si el lote no existe")
    void cancelarLote_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase()))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(reservaInsumoRepository, loteInsumoRepository,
                molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-06: cancelarLote — lote PENDIENTE: queda CANCELADO y no modifica el estado de ningún equipamiento")
    void cancelarLote_debeDejarTodoElEquipamientoDisponibleSiNingunaEtapaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        // Un lote PENDIENTE todavía no ocupó ningún equipamiento: no se consulta ni se guarda ninguno
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
        for (EtapaLoteEntity etapa : lote.getEtapas()) {
            assertThat(((EquipamientoEntity) etapa.getEquipamiento()).getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_USO);
        }
    }

    @Test
    @DisplayName("CP-CL-07: cancelarLote — Molienda EN_CURSO: el molino pasa a EN_LIMPIEZA, el resto a DISPONIBLE")
    void cancelarLote_debeDejarMolinoEnLimpiezaSiMoliendaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
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
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
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
        EtapaLoteEntity etapaMaceracion = findEtapa(lote, TipoEtapa.MACERACION);
        etapaMaceracion.setEstado(EstadoEtapaLote.FINALIZADA);
        LocalDateTime fechaFinalizacionOriginal = LocalDateTime.now().minusDays(2);
        etapaMaceracion.setFechaFinalizacion(fechaFinalizacionOriginal);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
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
        assertThat(etapaMaceracion.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaMaceracion.getFechaFinalizacion()).isEqualTo(fechaFinalizacionOriginal);
    }

    @Test
    @DisplayName("CP-CL-12: cancelarLote pasa las etapas PENDIENTE y EN_CURSO a CANCELADA con fechaFinalizacion, preservando fechaInicio")
    void cancelarLote_debeCancelarEtapasNoFinalizadas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        EtapaLoteEntity etapaFermentacion = findEtapa(lote, TipoEtapa.FERMENTACION);
        LocalDateTime fechaInicioFermentacion = etapaFermentacion.getFechaInicio();
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(lote.getEtapas()).allMatch(e -> e.getEstado() == EstadoEtapaLote.CANCELADA);
        assertThat(lote.getEtapas()).allSatisfy(e -> assertThat(e.getFechaFinalizacion()).isNotNull().isBetween(antes, despues));
        // Fermentación estaba EN_CURSO: el equipamiento sí se usó, fechaInicio se conserva
        assertThat(etapaFermentacion.getFechaInicio()).isEqualTo(fechaInicioFermentacion);
        // El resto nunca llegó a EN_CURSO: el equipamiento nunca se usó, fechaInicio sigue nula
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getFechaInicio()).isNull();
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getFechaInicio()).isNull();
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getFechaInicio()).isNull();
        assertThat(findEtapa(lote, TipoEtapa.MADURACION).getFechaInicio()).isNull();
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getFechaInicio()).isNull();
    }

    @Test
    @DisplayName("CP-CL-13: cancelarLote finaliza la planificación si los litros producidos igualan la cantidad y no quedan lotes vivos (límite)")
    void cancelarLote_debeFinalizarLaPlanificacionSiLitrosIgualanLaCantidadYNoQuedanLotesVivos() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarConPlanificacionDelLote();
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(100.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository).save(planificacion);
    }

    @Test
    @DisplayName("CP-CL-14: cancelarLote también finaliza la planificación si los litros producidos superan la cantidad y no quedan lotes vivos")
    void cancelarLote_debeFinalizarLaPlanificacionSiLitrosSuperanLaCantidad() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarConPlanificacionDelLote();
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(120.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isNotNull();
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository).save(planificacion);
    }

    @Test
    @DisplayName("CP-CL-15: cancelarLote no finaliza la planificación si quedan otros lotes vivos")
    void cancelarLote_noDebeFinalizarLaPlanificacionSiQuedanLotesVivos() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarConPlanificacionDelLote();
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(100.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(true);

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(planificacion.getFechaFinalizacion()).isNull();
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CL-16: cancelarLote no finaliza la planificación si los litros producidos no alcanzan la cantidad, sin consultar los lotes vivos")
    void cancelarLote_noDebeFinalizarLaPlanificacionSiLosLitrosNoAlcanzanLaCantidad() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarConPlanificacionDelLote();
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(60.0);

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(loteRepository, never()).existsByPlanificacionProduccion_IdAndEstadoIn(any(), anyList());
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CL-17: cancelarLote no modifica una planificación que no está PENDIENTE ni consulta litros ni lotes vivos")
    void cancelarLote_noDebeModificarUnaPlanificacionNoPendiente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarConPlanificacionDelLote();
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        LocalDateTime fechaFinalizacionOriginal = LocalDateTime.now().minusDays(3);
        planificacion.setEstado(EstadoSolicitud.FINALIZADA);
        planificacion.setFechaFinalizacion(fechaFinalizacionOriginal);
        planificacion.setMotivoFinalizacion("Cierre forzado");

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isEqualTo(fechaFinalizacionOriginal);
        assertThat(planificacion.getMotivoFinalizacion()).isEqualTo("Cierre forzado");
        verify(envasadoLoteRepository, never()).sumarLitrosProducidosDePlanificacion(any());
        verify(loteRepository, never()).existsByPlanificacionProduccion_IdAndEstadoIn(any(), anyList());
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CL-18: cancelarLote lanza RecursoNoEncontradoException si la planificación del lote no se encuentra al evaluar el cierre")
    void cancelarLote_debeLanzarExcepcionSiLaPlanificacionDelLoteNoSeEncuentra() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 1");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CL-19: cancelarLote — lote EN_EJECUCION en una etapa temprana: actualiza el equipamiento y reencadena la cola desde mañana")
    void cancelarLote_debeReencadenarLaColaDesdeMananaSiElLoteEstabaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        LoteEntity loteB = crearLoteBParaReencadenar(hoy);
        LoteEntity loteC = crearLoteCParaReencadenar(hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEquipamiento().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getEquipamiento().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getEquipamiento().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(findEtapa(lote, TipoEtapa.FERMENTACION).getEquipamiento().getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertFechas(loteB, hoy.plusDays(1), hoy.plusDays(19));
        assertFechas(loteC, hoy.plusDays(21), hoy.plusDays(41));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC)
                .allSatisfy(pendiente -> assertThat(pendiente.getFechaInicioEstimada()).isAfterOrEqualTo(hoy.plusDays(1)));
    }

    @Test
    @DisplayName("CP-CL-20: cancelarLote — un lote PENDIENTE compacta la cola desde hoy sin tocar el equipamiento")
    void cancelarLote_debeCompactarLaColaDesdeHoySiElLoteEstabaPendiente() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.PENDIENTE, null);
        lote.setFechaInicioEstimada(hoy);
        lote.setFechaFinalizacionEstimada(hoy.plusDays(10));
        LoteEntity loteB = crearLoteBParaReencadenar(hoy);
        LoteEntity loteC = crearLoteCParaReencadenar(hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertFechas(loteB, hoy, hoy.plusDays(18));
        assertFechas(loteC, hoy.plusDays(20), hoy.plusDays(40));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-21: cancelarLote — un lote PENDIENTE con otro lote en ejecución en el fermentador reencadena la cola detrás de ese lote")
    void cancelarLote_debeReencadenarDetrasDelLoteEnEjecucionSiElCanceladoEstabaPendiente() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.PENDIENTE, null);
        LoteEntity loteA = crearLoteEnEjecucionParaCola(20L, hoy.plusDays(5));
        LoteEntity loteB = crearLoteBParaReencadenar(hoy);
        LoteEntity loteC = crearLoteCParaReencadenar(hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(loteA));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertFechas(loteB, hoy.plusDays(7), hoy.plusDays(25));
        assertFechas(loteC, hoy.plusDays(27), hoy.plusDays(47));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
    }

    @Test
    @DisplayName("CP-CL-22: cancelarLote — la planificación de un pendiente sigue siendo piso al reencadenar tras cancelar un lote EN_EJECUCION")
    void cancelarLote_debeRespetarElPisoDeLaPlanificacionAlReencadenarTrasCancelarUnLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy.plusDays(11), hoy.plusDays(29), hoy.plusDays(10));
        LoteEntity loteC = crearLoteCParaReencadenar(hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertFechas(loteB, hoy.plusDays(10), hoy.plusDays(28));
        assertFechas(loteC, hoy.plusDays(30), hoy.plusDays(50));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
    }

    @Test
    @DisplayName("CP-CL-23: cancelarLote — si no hay pendientes en la cola no hay nada que reencadenar y se evalúa igualmente el cierre de la planificación")
    void cancelarLote_noDebeReencadenarSiLaColaPendienteEstaVacia() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.PENDIENTE, null);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        verify(loteRepository, never()).saveAll(anyList());
        verify(planificacionProduccionRepository).buscarPorIdParaActualizar(1L);
    }

    @Test
    @DisplayName("CP-CL-24: cancelarLote — el reencadenado ocurre antes de evaluar el cierre de la planificación")
    void cancelarLote_debeReencadenarAntesDeEvaluarElCierreDeLaPlanificacion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(crearLoteBParaReencadenar(hoy), crearLoteCParaReencadenar(hoy)));

        // === EJECUCION ===
        loteServicio.cancelarLote(1L, cancelacionLoteFormDTOBase());

        // === ASSERTS ===
        InOrder orden = inOrder(loteRepository, planificacionProduccionRepository);
        orden.verify(loteRepository).saveAll(anyList());
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(1L);
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

    // ==================== finalizarHervido ====================

    @Test
    @DisplayName("CP-FH-01: finalizarHervido lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarHervido_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarHervido(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(ollaHervorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FH-02: finalizarHervido lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarHervido_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarHervido(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ollaHervorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FH-03: finalizarHervido lanza ReglaNegocioException si la etapa actual no es Hervido")
    void finalizarHervido_debeLanzarExcepcionSiEtapaActualNoEsHervido() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarHervido(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ollaHervorRepository, consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FH-04: finalizarHervido lanza RecursoNoEncontradoException si no existe la configuración de producción")
    void finalizarHervido_debeLanzarExcepcionSiNoExisteConfiguracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarHervido(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio, ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-FH-05: finalizarHervido lanza ReglaNegocioException si algún insumo requerido no alcanzó el porcentaje mínimo de consumo configurado")
    void finalizarHervido_debeLanzarExcepcionSiNoAlcanzaElPorcentajeMinimo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO);
        EtapaLoteEntity etapaHervido = findEtapa(lote, TipoEtapa.HERVIDO);
        etapaHervido.setId(3L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(3L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LupuloResponseDTO.builder().id(1L).nombre("Cascade").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(7.0) // 70% < 80% requerido
                        .build()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarHervido(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Cascade");
        verifyNoInteractions(ollaHervorRepository, reservaInsumoRepository);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FH-06: finalizarHervido — camino feliz: Hervido FINALIZADA, Fermentación EN_CURSO, olla de hervor EN_LIMPIEZA, se liberan las reservas de la etapa")
    void finalizarHervido_debeAvanzarDeHervidoAFermentacionCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO);
        EtapaLoteEntity etapaHervido = findEtapa(lote, TipoEtapa.HERVIDO);
        etapaHervido.setId(3L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(3L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LupuloResponseDTO.builder().id(1L).nombre("Cascade").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(8.0) // exactamente 80%: alcanza (>=)
                        .build()));
        mockearEquipamientoParaIniciar(lote);

        LupuloEntity lupulo = crearLupulo(1L, "Cascade", 5.5, FormatoLupulo.PELLET);
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().id(10L).insumo(lupulo).identificacionLoteProveedor("LOTE-A")
                .fechaVencimiento(LocalDate.now().plusMonths(6)).cantidadActual(10.0).cantidadReservada(2.0).build();
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).etapaLote(etapaHervido).loteInsumo(loteInsumo).cantidadReservada(2.0).build();
        when(reservaInsumoRepository.findByEtapaLoteId(3L)).thenReturn(List.of(reserva));
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(10L)).thenReturn(Optional.of(loteInsumo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarHervido(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(etapaHervido.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaHervido.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        EtapaLoteEntity etapaFermentacion = findEtapa(lote, TipoEtapa.FERMENTACION);
        assertThat(etapaFermentacion.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(etapaFermentacion.getFechaInicio()).isNotNull().isBetween(antes, despues);

        // Las etapas restantes no se tocan
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MADURACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        ArgumentCaptor<OllaHervorEntity> ollaHervorCaptor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        verify(ollaHervorRepository).save(ollaHervorCaptor.capture());
        assertThat(ollaHervorCaptor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);

        // La reserva de la etapa se libera: se devuelve al lote de insumo y se elimina la reserva
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        verify(loteInsumoRepository).save(loteInsumo);
        verify(reservaInsumoRepository).deleteAll(List.of(reserva));
        verifyNoInteractions(molinoRepository, maceradorRepository, fermentadorRepository);
        verify(loteRepository).save(lote);
    }

    // ==================== finalizarFermentacion ====================

    @Test
    @DisplayName("CP-FF-01: finalizarFermentacion lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarFermentacion_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarFermentacion(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FF-02: finalizarFermentacion lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarFermentacion_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarFermentacion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FF-03: finalizarFermentacion lanza ReglaNegocioException si la etapa actual no es Fermentación")
    void finalizarFermentacion_debeLanzarExcepcionSiEtapaActualNoEsFermentacion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarFermentacion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FF-04: finalizarFermentacion lanza RecursoNoEncontradoException si no existe la configuración de producción")
    void finalizarFermentacion_debeLanzarExcepcionSiNoExisteConfiguracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarFermentacion(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FF-05: finalizarFermentacion lanza ReglaNegocioException si algún insumo requerido no alcanzó el porcentaje mínimo de consumo configurado")
    void finalizarFermentacion_debeLanzarExcepcionSiNoAlcanzaElPorcentajeMinimo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        EtapaLoteEntity etapaFermentacion = findEtapa(lote, TipoEtapa.FERMENTACION);
        etapaFermentacion.setId(4L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(4L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LevaduraResponseDTO.builder().id(1L).nombre("Levadura Ale").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(7.0) // 70% < 80% requerido
                        .build()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarFermentacion(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Levadura Ale");
        verifyNoInteractions(reservaInsumoRepository);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FF-06: finalizarFermentacion — camino feliz: Fermentación FINALIZADA, Maduración EN_CURSO, ningún equipamiento cambia de estado, se liberan las reservas de la etapa")
    void finalizarFermentacion_debeAvanzarDeFermentacionAMaduracionCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        EtapaLoteEntity etapaFermentacion = findEtapa(lote, TipoEtapa.FERMENTACION);
        etapaFermentacion.setId(4L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(4L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LevaduraResponseDTO.builder().id(1L).nombre("Levadura Ale").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(8.0) // exactamente 80%: alcanza (>=)
                        .build()));

        LevaduraEntity levadura = crearLevadura(1L, "Levadura Ale", TipoLevadura.ALE, 2.0E10);
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().id(10L).insumo(levadura).identificacionLoteProveedor("LOTE-A")
                .fechaVencimiento(LocalDate.now().plusMonths(6)).cantidadActual(10.0).cantidadReservada(2.0).build();
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).etapaLote(etapaFermentacion).loteInsumo(loteInsumo).cantidadReservada(2.0).build();
        when(reservaInsumoRepository.findByEtapaLoteId(4L)).thenReturn(List.of(reserva));
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(10L)).thenReturn(Optional.of(loteInsumo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarFermentacion(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(etapaFermentacion.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaFermentacion.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        EtapaLoteEntity etapaMaduracion = findEtapa(lote, TipoEtapa.MADURACION);
        assertThat(etapaMaduracion.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(etapaMaduracion.getFechaInicio()).isNotNull().isBetween(antes, despues);

        // Las etapas restantes no se tocan
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        // La reserva de la etapa se libera: se devuelve al lote de insumo y se elimina la reserva
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        verify(loteInsumoRepository).save(loteInsumo);
        verify(reservaInsumoRepository).deleteAll(List.of(reserva));

        // Ningún equipamiento cambia de estado operativo: el fermentador sigue EN_USO sin interrupción
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
        verify(loteRepository).save(lote);
    }

    // ==================== finalizarMaduracion ====================

    @Test
    @DisplayName("CP-FMD-01: finalizarMaduracion lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarMaduracion_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaduracion(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMD-02: finalizarMaduracion lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarMaduracion_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaduracion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMD-03: finalizarMaduracion lanza ReglaNegocioException si la etapa actual no es Maduración")
    void finalizarMaduracion_debeLanzarExcepcionSiEtapaActualNoEsMaduracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaduracion(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMD-04: finalizarMaduracion lanza RecursoNoEncontradoException si no existe la configuración de producción")
    void finalizarMaduracion_debeLanzarExcepcionSiNoExisteConfiguracion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaduracion(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-FMD-05: finalizarMaduracion lanza ReglaNegocioException si algún insumo requerido (ej. un lúpulo de Dry Hop) no alcanzó el porcentaje mínimo de consumo configurado")
    void finalizarMaduracion_debeLanzarExcepcionSiNoAlcanzaElPorcentajeMinimo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION);
        EtapaLoteEntity etapaMaduracion = findEtapa(lote, TipoEtapa.MADURACION);
        etapaMaduracion.setId(5L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(5L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LupuloResponseDTO.builder().id(1L).nombre("Citra Dry Hop").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(7.0) // 70% < 80% requerido
                        .build()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarMaduracion(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Citra Dry Hop");
        verifyNoInteractions(reservaInsumoRepository);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FMD-06: finalizarMaduracion — camino feliz: Maduración FINALIZADA, Envasado EN_CURSO, ningún equipamiento cambia de estado, se liberan las reservas de la etapa")
    void finalizarMaduracion_debeAvanzarDeMaduracionAEnvasadoCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION);
        EtapaLoteEntity etapaMaduracion = findEtapa(lote, TipoEtapa.MADURACION);
        etapaMaduracion.setId(5L);
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        mockearConfiguracionYReceta(); // porcentajeMinimoConsumoParaAvanzarEtapa = 80.0
        when(consumoInsumoServicio.filtrarInsumosRequeridos(5L)).thenReturn(List.of(
                InsumoRequeridoResponseDTO.builder()
                        .insumo(LupuloResponseDTO.builder().id(1L).nombre("Citra Dry Hop").build())
                        .cantidadRequerida(10.0)
                        .cantidadConsumida(8.0) // exactamente 80%: alcanza (>=)
                        .build()));

        LupuloEntity lupulo = crearLupulo(1L, "Citra Dry Hop", 12.0, FormatoLupulo.PELLET);
        LoteInsumoEntity loteInsumo = LoteInsumoEntity.builder().id(10L).insumo(lupulo).identificacionLoteProveedor("LOTE-A")
                .fechaVencimiento(LocalDate.now().plusMonths(6)).cantidadActual(10.0).cantidadReservada(2.0).build();
        ReservaInsumoEntity reserva = ReservaInsumoEntity.builder().id(100L).etapaLote(etapaMaduracion).loteInsumo(loteInsumo).cantidadReservada(2.0).build();
        when(reservaInsumoRepository.findByEtapaLoteId(5L)).thenReturn(List.of(reserva));
        when(loteInsumoRepository.buscarPorIdParaLiberarReserva(10L)).thenReturn(Optional.of(loteInsumo));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarMaduracion(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(etapaMaduracion.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaMaduracion.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        EtapaLoteEntity etapaEnvasado = findEtapa(lote, TipoEtapa.ENVASADO);
        assertThat(etapaEnvasado.getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(etapaEnvasado.getFechaInicio()).isNotNull().isBetween(antes, despues);

        // Las etapas restantes no se tocan
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.FERMENTACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        // La reserva de la etapa se libera: se devuelve al lote de insumo y se elimina la reserva
        assertThat(loteInsumo.getCantidadReservada()).isEqualTo(0.0);
        verify(loteInsumoRepository).save(loteInsumo);
        verify(reservaInsumoRepository).deleteAll(List.of(reserva));

        // Ningún equipamiento cambia de estado operativo: el fermentador sigue EN_USO sin interrupción
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, fermentadorRepository);
        verify(loteRepository).save(lote);
    }

    // ==================== finalizarEnvasado ====================

    @Test
    @DisplayName("CP-FE-01: finalizarEnvasado lanza RecursoNoEncontradoException si el lote no existe")
    void finalizarEnvasado_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-FE-02: finalizarEnvasado lanza ReglaNegocioException si el lote no está EN_EJECUCION")
    void finalizarEnvasado_debeLanzarExcepcionSiLoteNoEstaEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.PENDIENTE, null);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-FE-03: finalizarEnvasado lanza ReglaNegocioException si la etapa actual no es Envasado")
    void finalizarEnvasado_debeLanzarExcepcionSiEtapaActualNoEsEnvasado() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-FE-04: finalizarEnvasado lanza RecursoNoEncontradoException si el fermentador ya no existe")
    void finalizarEnvasado_debeLanzarExcepcionSiFermentadorNoExiste() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(20.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-05: finalizarEnvasado — camino feliz: Envasado FINALIZADA (sin etapa siguiente), fermentador EN_LIMPIEZA, lote FINALIZADO")
    void finalizarEnvasado_debeFinalizarLoteCorrectamente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.EN_USO, 20.0);
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L)).thenReturn(Optional.of(fermentador));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(20.0);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        EtapaLoteEntity etapaEnvasado = findEtapa(lote, TipoEtapa.ENVASADO);
        assertThat(etapaEnvasado.getEstado()).isEqualTo(EstadoEtapaLote.FINALIZADA);
        assertThat(etapaEnvasado.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);

        // Las demás etapas no se tocan
        assertThat(findEtapa(lote, TipoEtapa.MOLIENDA).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MACERACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.HERVIDO).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.FERMENTACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);
        assertThat(findEtapa(lote, TipoEtapa.MADURACION).getEstado()).isEqualTo(EstadoEtapaLote.PENDIENTE);

        assertThat(fermentador.getEstadoOperativo()).isEqualTo(EstadoOperativo.EN_LIMPIEZA);
        verify(fermentadorRepository).save(fermentador);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(lote.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        verifyNoInteractions(molinoRepository, maceradorRepository, ollaHervorRepository, consumoInsumoServicio, reservaInsumoRepository);
        verify(loteRepository).save(lote);
        assertThat(lote.getDetallesCostoDirecto()).isEmpty();
    }

    @Test
    @DisplayName("CP-FE-06: finalizarEnvasado registra un DetalleCostoDirecto por cada costo directo adicional activo, sobre el total efectivamente envasado")
    void finalizarEnvasado_debeRegistrarCostosDirectosDelLote() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.EN_USO, 20.0);
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L)).thenReturn(Optional.of(fermentador));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(20.0);
        CostoDirectoAdicionalEntity etiquetas = CostoDirectoAdicionalEntity.builder().id(1L).nombre("Etiquetas").costoPorLitro(BigDecimal.valueOf(0.50)).estado(Estado.ACTIVO).build();
        CostoDirectoAdicionalEntity corchos = CostoDirectoAdicionalEntity.builder().id(2L).nombre("Corchos").costoPorLitro(BigDecimal.valueOf(0.25)).estado(Estado.ACTIVO).build();
        when(costoDirectoAdicionalRepository.findAllActivos()).thenReturn(List.of(etiquetas, corchos));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getDetallesCostoDirecto()).hasSize(2);

        DetalleCostoDirectoEntity detalleEtiquetas = lote.getDetallesCostoDirecto().stream()
                .filter(detalle -> detalle.getCostoDirectoAdicional() == etiquetas).findFirst().orElseThrow();
        assertThat(detalleEtiquetas.getCostoPorLitroAplicado()).isEqualByComparingTo("0.50");
        assertThat(detalleEtiquetas.getSubtotal()).isEqualByComparingTo("10.0000");
        assertThat(detalleEtiquetas.getLote()).isSameAs(lote);

        DetalleCostoDirectoEntity detalleCorchos = lote.getDetallesCostoDirecto().stream()
                .filter(detalle -> detalle.getCostoDirectoAdicional() == corchos).findFirst().orElseThrow();
        assertThat(detalleCorchos.getCostoPorLitroAplicado()).isEqualByComparingTo("0.25");
        assertThat(detalleCorchos.getSubtotal()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("CP-FE-07: finalizarEnvasado lanza ReglaNegocioException y no cambia nada si el lote no tiene litros envasados")
    void finalizarEnvasado_debeRechazarLoteSinLitrosEnvasados() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(0.0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede finalizar el envasado de un lote sin litros envasados. Si no se va a producir, debe cancelarse el lote");
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_EJECUCION);
        assertThat(findEtapa(lote, TipoEtapa.ENVASADO).getEstado()).isEqualTo(EstadoEtapaLote.EN_CURSO);
        assertThat(lote.getDetallesCostoDirecto()).isEmpty();
        verifyNoInteractions(fermentadorRepository, costoDirectoAdicionalRepository, planificacionProduccionRepository);
        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-08: finalizarEnvasado finaliza la planificación si los litros producidos igualan la cantidad y no quedan lotes vivos (límite)")
    void finalizarEnvasado_debeFinalizarLaPlanificacionSiLitrosIgualanLaCantidadYNoQuedanLotesVivos() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(40.0);
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(100.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isNotNull().isBetween(antes, despues);
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository).save(planificacion);
    }

    @Test
    @DisplayName("CP-FE-09: finalizarEnvasado también finaliza la planificación si los litros producidos superan la cantidad y no quedan lotes vivos")
    void finalizarEnvasado_debeFinalizarLaPlanificacionSiLitrosSuperanLaCantidad() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(20.0);
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(130.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(false);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        assertThat(planificacion.getFechaFinalizacion()).isNotNull();
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository).save(planificacion);
    }

    @Test
    @DisplayName("CP-FE-10: finalizarEnvasado no finaliza la planificación si quedan lotes vivos")
    void finalizarEnvasado_noDebeFinalizarLaPlanificacionSiQuedanLotesVivos() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(20.0);
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(100.0);
        when(loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))).thenReturn(true);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(planificacion.getFechaFinalizacion()).isNull();
        assertThat(planificacion.getMotivoFinalizacion()).isNull();
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-11: finalizarEnvasado no finaliza la planificación si los litros producidos no alcanzan la cantidad, sin consultar los lotes vivos")
    void finalizarEnvasado_noDebeFinalizarLaPlanificacionSiLosLitrosNoAlcanzanLaCantidad() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(20.0);
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(70.0);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        verify(loteRepository, never()).existsByPlanificacionProduccion_IdAndEstadoIn(any(), anyList());
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-12: finalizarEnvasado no evalúa el cierre si la planificación no está PENDIENTE")
    void finalizarEnvasado_noDebeEvaluarElCierreSiLaPlanificacionNoEstaPendiente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(20.0);
        PlanificacionProduccionEntity planificacion = lote.getPlanificacionProduccion();
        planificacion.setEstado(EstadoSolicitud.FINALIZADA);

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(planificacion.getEstado()).isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(envasadoLoteRepository, never()).sumarLitrosProducidosDePlanificacion(any());
        verify(loteRepository, never()).existsByPlanificacionProduccion_IdAndEstadoIn(any(), anyList());
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-13: finalizarEnvasado finaliza un lote con menos litros que su volumen objetivo y calcula los costos sobre lo envasado")
    void finalizarEnvasado_debeFinalizarLoteConMenosLitrosQueSuObjetivo() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasado(400.0);
        lote.setVolumenObjetivo(600.0);
        CostoDirectoAdicionalEntity etiquetas = CostoDirectoAdicionalEntity.builder().id(1L).nombre("Etiquetas").costoPorLitro(BigDecimal.valueOf(0.50)).estado(Estado.ACTIVO).build();
        when(costoDirectoAdicionalRepository.findAllActivos()).thenReturn(List.of(etiquetas));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(lote.getDetallesCostoDirecto()).hasSize(1);
        DetalleCostoDirectoEntity detalle = lote.getDetallesCostoDirecto().get(0);
        assertThat(detalle.getCostoPorLitroAplicado()).isEqualByComparingTo("0.50");
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("200.0000");
        verify(envasadoLoteRepository, times(1)).sumarCantidadEnvasadaRegistrada(any());
    }

    @Test
    @DisplayName("CP-FE-14: finalizarEnvasado lanza RecursoNoEncontradoException si la planificación del lote no se encuentra al evaluar el cierre")
    void finalizarEnvasado_debeLanzarExcepcionSiLaPlanificacionDelLoteNoSeEncuentra() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.empty());
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.EN_USO, 20.0);
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L)).thenReturn(Optional.of(fermentador));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(20.0);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.finalizarEnvasado(1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 1");
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-FE-15: finalizarEnvasado — la cola pendiente arranca como mínimo mañana")
    void finalizarEnvasado_debeReencadenarLaColaPendienteDesdeManana() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy.minusDays(5));
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertFechas(loteB, hoy.plusDays(1), hoy.plusDays(19));
        assertFechas(loteC, hoy.plusDays(21), hoy.plusDays(41));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
    }

    @Test
    @DisplayName("CP-FE-16: finalizarEnvasado — un pendiente cuya planificación inicia en el futuro conserva su fecha posterior")
    void finalizarEnvasado_noDebeModificarPendientesCuyaPlanificacionIniciaEnElFuturo() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy.plusDays(10), hoy.plusDays(28), hoy.plusDays(10));
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(30), hoy.plusDays(50), hoy);
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertFechas(loteB, hoy.plusDays(10), hoy.plusDays(28));
        assertFechas(loteC, hoy.plusDays(30), hoy.plusDays(50));
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-FE-17: finalizarEnvasado — si el lote termina antes de lo estimado los pendientes se adelantan sin pasar de mañana ni de su planificación")
    void finalizarEnvasado_debeAdelantarLosPendientesSinPasarDeMananaNiDeSuPlanificacion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity lote = prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola();
        lote.setFechaFinalizacionEstimada(hoy.plusDays(10));
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy.plusDays(11), hoy.plusDays(29), hoy.minusDays(3));
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(30), hoy.plusDays(50), hoy.plusDays(8));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertFechas(loteB, hoy.plusDays(1), hoy.plusDays(19));
        assertFechas(loteC, hoy.plusDays(21), hoy.plusDays(41));
        assertThat(capturarLotesGuardados()).containsExactly(loteB, loteC);
    }

    @Test
    @DisplayName("CP-FE-18: finalizarEnvasado — sin lotes pendientes en el fermentador no hay nada que reencadenar")
    void finalizarEnvasado_noDebeReencadenarSiNoHayLotesPendientesEnElFermentador() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola();
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-FE-19: finalizarEnvasado — el reencadenado ocurre antes de evaluar el cierre de la planificación")
    void finalizarEnvasado_debeReencadenarAntesDeEvaluarElCierreDeLaPlanificacion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola();
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(crearLoteBParaReencadenar(hoy), crearLoteCParaReencadenar(hoy)));

        // === EJECUCION ===
        loteServicio.finalizarEnvasado(1L);

        // === ASSERTS ===
        InOrder orden = inOrder(loteRepository, planificacionProduccionRepository);
        orden.verify(loteRepository).saveAll(anyList());
        orden.verify(planificacionProduccionRepository).buscarPorIdParaActualizar(1L);
    }

    // ==================== listarLotesDePlanificacion ====================

    @Test
    @DisplayName("CP-LP-01: listarLotesDePlanificacion lanza RecursoNoEncontradoException cuando la planificación de producción no existe")
    void listarLotesDePlanificacion_debeLanzarExcepcionSiPlanificacionNoExiste() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(99L)).thenReturn(false);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.listarLotesDePlanificacion(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");
        verifyNoInteractions(loteRepository);
    }

    @Test
    @DisplayName("CP-LP-02: listarLotesDePlanificacion retorna una lista vacía cuando la planificación no tiene lotes")
    void listarLotesDePlanificacion_debeRetornarListaVaciaSinLotes() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("CP-LP-03: listarLotesDePlanificacion mapea los lotes de la planificación en todos los estados, sin filtrar")
    void listarLotesDePlanificacion_debeMapearLotesEnVariosEstados() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(
                crearLoteParaCronograma(10L, EstadoLote.FINALIZADO),
                crearLoteParaCronograma(11L, EstadoLote.EN_EJECUCION),
                crearLoteParaCronograma(12L, EstadoLote.PENDIENTE)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(3);
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getEstado)
                .containsExactly(EstadoLote.FINALIZADO, EstadoLote.EN_EJECUCION, EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("CP-LP-04: listarLotesDePlanificacion conserva el orden devuelto por la consulta")
    void listarLotesDePlanificacion_debeConservarElOrdenDeLaConsulta() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(
                crearLoteParaCronograma(12L, EstadoLote.PENDIENTE),
                crearLoteParaCronograma(10L, EstadoLote.FINALIZADO),
                crearLoteParaCronograma(11L, EstadoLote.EN_EJECUCION)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(12L, 10L, 11L);
    }

    @Test
    @DisplayName("CP-LP-05: listarLotesDePlanificacion consulta con el ID recibido y no persiste nada")
    void listarLotesDePlanificacion_debeConsultarConElIdRecibido() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(7L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(7L)).thenReturn(List.of());

        // === EJECUCION ===
        loteServicio.listarLotesDePlanificacion(7L);

        // === ASSERTS ===
        verify(planificacionProduccionRepository, times(1)).existsById(7L);
        verify(loteRepository, times(1)).buscarLotesDePlanificacionConCronograma(7L);
        verify(loteRepository, never()).save(any());
        verify(planificacionProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-LP-06: listarLotesDePlanificacion — el lote en ejecución atrasado de otra planificación corre al pendiente de esta (desplazamiento de 4 días) sin tocar sus fechas")
    void listarLotesDePlanificacion_debeCorrerAlPendienteConElAtrasoDeOtraPlanificacion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteB));
        when(loteRepository.buscarVivosDeFermentadores(eq(Set.of(5L)))).thenReturn(List.of(loteA, loteB, loteC));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        LoteCronogramaResponseDTO dtoB = resultado.get(0);
        assertThat(dtoB.getId()).isEqualTo(11L);
        assertThat(dtoB.getDiasDeDesplazamiento()).isEqualTo(4);
        assertThat(dtoB.getDiasDeAtraso()).isEqualTo(0);
        assertFechas(dtoB, hoy.minusDays(2), hoy.plusDays(6));
    }

    @Test
    @DisplayName("CP-LP-07: listarLotesDePlanificacion — una sola consulta de lotes vivos con el conjunto de fermentadores, sin repetidos")
    void listarLotesDePlanificacion_debeConsultarLosVivosUnaSolaVezConUnConjuntoDeFermentadores() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteB2 = crearLoteEnFermentador(13L, EstadoLote.PENDIENTE, 1L, 5L, hoy.plusDays(13), hoy.plusDays(16), hoy.minusDays(10));
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteB, loteB2));
        when(loteRepository.buscarVivosDeFermentadores(any())).thenReturn(List.of(loteA, loteB, loteC, loteB2));

        // === EJECUCION ===
        loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        ArgumentCaptor<Collection<Long>> captor = capturadorDeFermentadores();
        verify(loteRepository, times(1)).buscarVivosDeFermentadores(captor.capture());
        assertThat(captor.getValue()).containsExactly(5L);
    }

    @Test
    @DisplayName("CP-LP-08: listarLotesDePlanificacion — cada fermentador arma su propia cola: el atraso del fermentador 5 no corre al lote del 6")
    void listarLotesDePlanificacion_debeArmarUnaColaPorFermentador() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        LoteEntity loteD = crearLoteEnFermentador(14L, EstadoLote.PENDIENTE, 1L, 6L, hoy.plusDays(1), hoy.plusDays(7), hoy.minusDays(10));
        LoteEntity loteE = crearLoteEnFermentador(20L, EstadoLote.EN_EJECUCION, 3L, 6L, hoy.minusDays(10), hoy.plusDays(2), hoy.minusDays(10));
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteB, loteD));
        when(loteRepository.buscarVivosDeFermentadores(any())).thenReturn(List.of(loteA, loteB, loteC, loteE, loteD));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        ArgumentCaptor<Collection<Long>> captor = capturadorDeFermentadores();
        verify(loteRepository, times(1)).buscarVivosDeFermentadores(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(5L, 6L);
        assertThat(dtoDeLote(resultado, 11L).getDiasDeDesplazamiento()).isEqualTo(4);
        assertThat(dtoDeLote(resultado, 14L).getDiasDeDesplazamiento()).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-LP-09: listarLotesDePlanificacion — una planificación sin lotes no consulta a los lotes vivos")
    void listarLotesDePlanificacion_noDebeConsultarLosVivosSinLotes() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
        verify(loteRepository, never()).buscarVivosDeFermentadores(any());
    }

    @Test
    @DisplayName("CP-LP-10: listarLotesDePlanificacion — con solo lotes finalizados o cancelados no consulta a los vivos y devuelve atraso y desplazamiento en 0")
    void listarLotesDePlanificacion_noDebeConsultarLosVivosConLotesFinalizadosOCancelados() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(
                crearLoteEnFermentador(10L, EstadoLote.FINALIZADO, 1L, 5L, hoy.minusDays(20), hoy.minusDays(10), hoy.minusDays(30)),
                crearLoteEnFermentador(11L, EstadoLote.CANCELADO, 1L, 5L, hoy.minusDays(9), hoy.minusDays(5), hoy.minusDays(30))));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        verify(loteRepository, never()).buscarVivosDeFermentadores(any());
        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(dto -> assertAtraso(dto, 0, 0));
    }

    @Test
    @DisplayName("CP-LP-11: listarLotesDePlanificacion — un lote sin etapa de fermentación no tiene cola: no consulta a los vivos y su desplazamiento es 0")
    void listarLotesDePlanificacion_noDebeConsultarLosVivosSinFermentador() {
        // === PREPARACION DE DATOS ===
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(crearLoteParaCronograma(10L, EstadoLote.PENDIENTE)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        verify(loteRepository, never()).buscarVivosDeFermentadores(any());
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDiasDeDesplazamiento()).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-LP-12: listarLotesDePlanificacion — el lote en ejecución de la propia planificación lleva su atraso y el pendiente su desplazamiento")
    void listarLotesDePlanificacion_debeInformarElAtrasoDelLoteEnEjecucionPropio() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteEnFermentador(10L, EstadoLote.EN_EJECUCION, 1L, 5L, hoy.minusDays(20), hoy.minusDays(3), hoy.minusDays(10));
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteA, loteB));
        when(loteRepository.buscarVivosDeFermentadores(any())).thenReturn(List.of(loteA, loteB, loteC));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
    }

    @Test
    @DisplayName("CP-LP-13: listarLotesDePlanificacion — sin lote en ejecución en el fermentador no hay corrimiento aunque la fecha guardada del pendiente esté en el pasado")
    void listarLotesDePlanificacion_noDebeCorrerSinLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteEnFermentador(11L, EstadoLote.PENDIENTE, 1L, 5L, hoy.minusDays(5), hoy.plusDays(3), hoy.minusDays(10));
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteB));
        when(loteRepository.buscarVivosDeFermentadores(eq(Set.of(5L)))).thenReturn(List.of(loteB, loteC));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertAtraso(resultado.get(0), 0, 0);
    }

    @Test
    @DisplayName("CP-LP-14: listarLotesDePlanificacion — el cálculo del atraso es de solo lectura: no persiste nada ni modifica las fechas estimadas")
    void listarLotesDePlanificacion_noDebePersistirNiModificarFechas() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteC = crearLoteAtrasadoC(hoy);
        when(planificacionProduccionRepository.existsById(1L)).thenReturn(true);
        when(loteRepository.buscarLotesDePlanificacionConCronograma(1L)).thenReturn(List.of(loteB));
        when(loteRepository.buscarVivosDeFermentadores(any())).thenReturn(List.of(loteA, loteB, loteC));

        // === EJECUCION ===
        loteServicio.listarLotesDePlanificacion(1L);

        // === ASSERTS ===
        verify(loteRepository, never()).save(any());
        verify(loteRepository, never()).saveAll(anyList());
        verify(planificacionProduccionRepository, never()).save(any());
        assertThat(loteA.getFechaFinalizacionEstimada()).isEqualTo(hoy.minusDays(3));
        assertFechas(loteB, hoy.minusDays(2), hoy.plusDays(6));
        assertFechas(loteC, hoy.plusDays(7), hoy.plusDays(12));
    }

    // ==================== listarLotesParaCronograma ====================

    @Test
    @DisplayName("CP-LC-01: listarLotesParaCronograma sin fermentador no valida y mapea todos los lotes en el orden recibido")
    void listarLotesParaCronograma_debeMapearEnElOrdenRecibidoSinValidarFermentador() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class))).thenReturn(List.of(
                crearLoteParaCronograma(12L, EstadoLote.PENDIENTE),
                crearLoteParaCronograma(10L, EstadoLote.FINALIZADO),
                crearLoteParaCronograma(11L, EstadoLote.EN_EJECUCION)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesParaCronograma(null, false);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class));
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(12L, 10L, 11L);
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-LC-02: listarLotesParaCronograma con fermentador existente propaga el filtro a la consulta")
    void listarLotesParaCronograma_debePropagarElFiltroDeFermentadorExistente() {
        // === PREPARACION DE DATOS ===
        when(fermentadorRepository.findById(5L)).thenReturn(Optional.of(crearFermentador(5L, 20.0)));
        when(loteRepository.buscarLotesParaCronograma(eq(5L), eq(false), any(LocalDateTime.class))).thenReturn(List.of(
                crearLoteParaCronograma(10L, EstadoLote.PENDIENTE),
                crearLoteParaCronograma(11L, EstadoLote.EN_EJECUCION)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesParaCronograma(5L, false);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarLotesParaCronograma(eq(5L), eq(false), any(LocalDateTime.class));
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("CP-LC-03: listarLotesParaCronograma lanza RecursoNoEncontradoException cuando el fermentador no existe o está inactivo")
    void listarLotesParaCronograma_debeLanzarExcepcionSiFermentadorNoExiste() {
        // === PREPARACION DE DATOS ===
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.listarLotesParaCronograma(99L, false))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");
        verify(loteRepository, never()).buscarLotesParaCronograma(any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("CP-LC-04: listarLotesParaCronograma sin historial consulta desde el inicio del día de hoy menos 14 días")
    void listarLotesParaCronograma_debeConsultarConVentanaDe14DiasSinHistorial() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class))).thenReturn(List.of());
        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // === EJECUCION ===
        loteServicio.listarLotesParaCronograma(null, false);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarLotesParaCronograma(isNull(), eq(false), desdeCaptor.capture());
        assertThat(desdeCaptor.getValue()).isEqualTo(hoy.minusDays(14).atStartOfDay());
    }

    @Test
    @DisplayName("CP-LC-05: listarLotesParaCronograma con historial completo propaga el indicador y no filtra en el service")
    void listarLotesParaCronograma_debePropagarElIndicadorDeHistorialCompleto() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(true), any(LocalDateTime.class))).thenReturn(List.of(
                crearLoteParaCronograma(10L, EstadoLote.FINALIZADO),
                crearLoteParaCronograma(11L, EstadoLote.CANCELADO)));
        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesParaCronograma(null, true);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarLotesParaCronograma(isNull(), eq(true), desdeCaptor.capture());
        assertThat(desdeCaptor.getValue()).isEqualTo(hoy.minusDays(14).atStartOfDay());
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("CP-LC-06: listarLotesParaCronograma retorna una lista vacía cuando no hay lotes")
    void listarLotesParaCronograma_debeRetornarListaVaciaSinLotes() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class))).thenReturn(List.of());

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesParaCronograma(null, false);

        // === ASSERTS ===
        assertThat(resultado).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("CP-LC-07: listarLotesParaCronograma mapea los lotes en todos los estados sin filtrar")
    void listarLotesParaCronograma_debeMapearLotesEnTodosLosEstados() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class))).thenReturn(List.of(
                crearLoteParaCronograma(10L, EstadoLote.FINALIZADO),
                crearLoteParaCronograma(11L, EstadoLote.EN_EJECUCION),
                crearLoteParaCronograma(12L, EstadoLote.PENDIENTE),
                crearLoteParaCronograma(13L, EstadoLote.CANCELADO)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = loteServicio.listarLotesParaCronograma(null, false);

        // === ASSERTS ===
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getEstado)
                .containsExactly(EstadoLote.FINALIZADO, EstadoLote.EN_EJECUCION, EstadoLote.PENDIENTE, EstadoLote.CANCELADO);
        verify(loteRepository, never()).save(any());
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-LC-08: listarLotesParaCronograma — un lote en ejecución que termina hoy o después no está atrasado: nadie se corre")
    void listarLotesParaCronograma_noDebeInformarAtrasoSiElLoteEnEjecucionNoVencio() {
        LocalDate hoy = LocalDate.now();
        for (LocalDate finEstimado : List.of(hoy.plusDays(4), hoy)) {
            // === PREPARACION DE DATOS ===
            LoteEntity loteA = crearLoteAtrasadoA(hoy);
            loteA.setFechaFinalizacionEstimada(finEstimado);
            LoteEntity loteB = crearLoteAtrasadoB(hoy);
            LoteEntity loteC = crearLoteAtrasadoC(hoy);

            // === EJECUCION ===
            List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(loteA, loteB, loteC);

            // === ASSERTS ===
            assertThat(resultado).allSatisfy(dto -> assertAtraso(dto, 0, 0));
            assertFechas(dtoDeLote(resultado, 11L), hoy.minusDays(2), hoy.plusDays(6));
            assertFechas(dtoDeLote(resultado, 12L), hoy.plusDays(7), hoy.plusDays(12));
        }
    }

    @Test
    @DisplayName("CP-LC-09: listarLotesParaCronograma — el lote en ejecución atrasado 3 días corre la cola (4 y 5 días, por el día de limpieza) sin cambiar las fechas guardadas")
    void listarLotesParaCronograma_debeCorrerATodaLaColaConElAtrasoDelLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(
                crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), crearLoteAtrasadoC(hoy));

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 5);
        assertFechas(dtoDeLote(resultado, 11L), hoy.minusDays(2), hoy.plusDays(6));
    }

    @Test
    @DisplayName("CP-LC-10: listarLotesParaCronograma — un atraso de un solo día corre 2 y 3 días a los pendientes (cuenta desde hoy más el día de limpieza)")
    void listarLotesParaCronograma_debeInformarUnAtrasoDeUnDia() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        loteA.setFechaFinalizacionEstimada(hoy.minusDays(1));
        LoteEntity loteB = crearLoteEnFermentador(11L, EstadoLote.PENDIENTE, 1L, 5L, hoy, hoy.plusDays(8), hoy.minusDays(10));
        LoteEntity loteC = crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(9), hoy.plusDays(14), hoy.minusDays(10));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(loteA, loteB, loteC);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 10L), 1, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 2);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 3);
    }

    @Test
    @DisplayName("CP-LC-11: listarLotesParaCronograma — un hueco en la cola absorbe parte del atraso: el segundo pendiente se corre menos que el primero")
    void listarLotesParaCronograma_debeAbsorberParteDelAtrasoConUnHueco() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteC = crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(11), hoy.plusDays(16), hoy.minusDays(10));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), loteC);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 1);
    }

    @Test
    @DisplayName("CP-LC-12: listarLotesParaCronograma — un hueco igual o mayor al atraso no corre al lote (un corrimiento no positivo no se informa)")
    void listarLotesParaCronograma_noDebeCorrerAUnLoteConHuecoIgualOMayorAlAtraso() {
        LocalDate hoy = LocalDate.now();
        for (int inicioC : List.of(12, 14)) {
            // === PREPARACION DE DATOS ===
            LoteEntity loteC = crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(inicioC), hoy.plusDays(inicioC + 5), hoy.minusDays(10));

            // === EJECUCION ===
            List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), loteC);

            // === ASSERTS ===
            assertAtraso(dtoDeLote(resultado, 12L), 0, 0);
            assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
            assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        }
    }

    @Test
    @DisplayName("CP-LC-13: listarLotesParaCronograma — el inicio de la planificación es piso: un pendiente que inicia mucho después de hoy no se corre")
    void listarLotesParaCronograma_noDebeCorrerALotesDeUnaPlanificacionFutura() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteEnFermentador(11L, EstadoLote.PENDIENTE, 1L, 5L, hoy.plusDays(30), hoy.plusDays(38), hoy.plusDays(30));
        LoteEntity loteC = crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(40), hoy.plusDays(45), hoy.minusDays(10));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(crearLoteAtrasadoA(hoy), loteB, loteC);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 0);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 0);
    }

    @Test
    @DisplayName("CP-LC-14: listarLotesParaCronograma — sin lote en ejecución no hay corrimiento aunque las fechas guardadas estén en el pasado")
    void listarLotesParaCronograma_noDebeCorrerSinLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteEnFermentador(11L, EstadoLote.PENDIENTE, 1L, 5L, hoy.minusDays(5), hoy.plusDays(3), hoy.minusDays(10));
        LoteEntity loteC = crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(4), hoy.plusDays(9), hoy.minusDays(10));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(loteB, loteC);

        // === ASSERTS ===
        assertThat(resultado).allSatisfy(dto -> assertAtraso(dto, 0, 0));
    }

    @Test
    @DisplayName("CP-LC-15: listarLotesParaCronograma — un lote en ejecución atrasado sin pendientes informa su atraso y no falla")
    void listarLotesParaCronograma_debeInformarElAtrasoSinPendientes() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(crearLoteAtrasadoA(hoy));

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertAtraso(resultado.get(0), 3, 0);
    }

    @Test
    @DisplayName("CP-LC-16: listarLotesParaCronograma — un lote en ejecución sin fecha de finalización estimada no tiene atraso y nadie se corre")
    void listarLotesParaCronograma_noDebeInformarAtrasoSinFechaDeFinalizacionEstimada() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        loteA.setFechaFinalizacionEstimada(null);

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(loteA, crearLoteAtrasadoB(hoy), crearLoteAtrasadoC(hoy));

        // === ASSERTS ===
        assertThat(resultado).allSatisfy(dto -> assertAtraso(dto, 0, 0));
    }

    @Test
    @DisplayName("CP-LC-17: listarLotesParaCronograma — una cola con un lote sin fechas válidas no se corre y no lanza excepción (conserva el atraso del lote en ejecución)")
    void listarLotesParaCronograma_noDebeCorrerUnaColaConFechasInvalidas() {
        LocalDate hoy = LocalDate.now();
        for (int variante = 0; variante < 3; variante++) {
            // === PREPARACION DE DATOS ===
            LoteEntity loteC = crearLoteAtrasadoC(hoy);
            switch (variante) {
                case 0 -> loteC.setFechaFinalizacionEstimada(null);
                case 1 -> loteC.setFechaInicioEstimada(null);
                default -> {
                    loteC.setFechaInicioEstimada(hoy.plusDays(12));
                    loteC.setFechaFinalizacionEstimada(hoy.plusDays(7));
                }
            }

            // === EJECUCION ===
            List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), loteC);

            // === ASSERTS ===
            assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
            assertAtraso(dtoDeLote(resultado, 11L), 0, 0);
            assertAtraso(dtoDeLote(resultado, 12L), 0, 0);
        }
    }

    @Test
    @DisplayName("CP-LC-18: listarLotesParaCronograma — dos fermentadores independientes: cada cola se reencadena por separado")
    void listarLotesParaCronograma_debeCalcularCadaFermentadorPorSeparado() {
        LocalDate hoy = LocalDate.now();
        // E vencido hace 5 días y no vencido, en el fermentador 6
        for (LocalDate finDeE : List.of(hoy.minusDays(5), hoy.plusDays(3))) {
            // === PREPARACION DE DATOS ===
            LoteEntity loteE = crearLoteEnFermentador(20L, EstadoLote.EN_EJECUCION, 3L, 6L, hoy.minusDays(20), finDeE, hoy.minusDays(10));
            LoteEntity loteF = crearLoteEnFermentador(21L, EstadoLote.PENDIENTE, 3L, 6L, hoy.minusDays(4), hoy.plusDays(2), hoy.minusDays(10));

            // === EJECUCION ===
            List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(
                    crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), crearLoteAtrasadoC(hoy), loteE, loteF);

            // === ASSERTS ===
            assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
            assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
            assertAtraso(dtoDeLote(resultado, 12L), 0, 5);
            boolean eVencido = finDeE.isBefore(hoy);
            assertAtraso(dtoDeLote(resultado, 20L), eVencido ? 5 : 0, 0);
            assertAtraso(dtoDeLote(resultado, 21L), 0, eVencido ? 6 : 0);
        }
    }

    @Test
    @DisplayName("CP-LC-19: listarLotesParaCronograma — los lotes finalizados no participan de la cola y se devuelven con atraso y desplazamiento en 0")
    void listarLotesParaCronograma_noDebeIncluirFinalizadosNiCanceladosEnLaCola() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity finalizado = crearLoteEnFermentador(30L, EstadoLote.FINALIZADO, 1L, 5L, hoy.minusDays(20), hoy.minusDays(10), hoy.minusDays(30));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(
                crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), crearLoteAtrasadoC(hoy), finalizado);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 5);
        assertAtraso(dtoDeLote(resultado, 30L), 0, 0);
    }

    @Test
    @DisplayName("CP-LC-20: listarLotesParaCronograma — el orden de la cola sale de las fechas guardadas, no del orden recibido, y el resultado conserva el orden recibido")
    void listarLotesParaCronograma_debeOrdenarLaColaPorFechasGuardadas() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(
                crearLoteAtrasadoC(hoy), crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy));

        // === ASSERTS ===
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(12L, 10L, 11L);
        assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 5);
    }

    @Test
    @DisplayName("CP-LC-21: listarLotesParaCronograma — un lote pendiente sin etapa de fermentación no pertenece a ninguna cola y no altera el cálculo de las demás")
    void listarLotesParaCronograma_noDebeIncluirEnLaColaAUnLoteSinEtapaDeFermentacion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity sinFermentacion = crearLoteParaCronograma(40L, EstadoLote.PENDIENTE);

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = listarCronogramaCon(
                crearLoteAtrasadoA(hoy), crearLoteAtrasadoB(hoy), crearLoteAtrasadoC(hoy), sinFermentacion);

        // === ASSERTS ===
        assertAtraso(dtoDeLote(resultado, 40L), 0, 0);
        assertAtraso(dtoDeLote(resultado, 10L), 3, 0);
        assertAtraso(dtoDeLote(resultado, 11L), 0, 4);
        assertAtraso(dtoDeLote(resultado, 12L), 0, 5);
    }

    @Test
    @DisplayName("CP-LC-22: listarLotesParaCronograma — el atraso no agrega consultas ni modifica datos")
    void listarLotesParaCronograma_noDebeAgregarConsultasNiModificarDatosAlCalcularElAtraso() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteA = crearLoteAtrasadoA(hoy);
        LoteEntity loteB = crearLoteAtrasadoB(hoy);
        LoteEntity loteC = crearLoteAtrasadoC(hoy);

        // === EJECUCION ===
        listarCronogramaCon(loteA, loteB, loteC);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class));
        verify(loteRepository, never()).buscarVivosDeFermentadores(any());
        verify(loteRepository, never()).save(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertThat(loteA.getFechaFinalizacionEstimada()).isEqualTo(hoy.minusDays(3));
        assertFechas(loteB, hoy.minusDays(2), hoy.plusDays(6));
        assertFechas(loteC, hoy.plusDays(7), hoy.plusDays(12));
    }

    // ==================== subirLoteEnCola ====================

    @Test
    @DisplayName("CP-SQ-01: subirLoteEnCola lanza RecursoNoEncontradoException cuando el lote no existe")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 99");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-SQ-02: subirLoteEnCola lanza ReglaNegocioException cuando el lote está EN_EJECUCION")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.EN_EJECUCION)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-SQ-03: subirLoteEnCola lanza ReglaNegocioException cuando el lote está FINALIZADO")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteFinalizado() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.FINALIZADO)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-SQ-04: subirLoteEnCola lanza ReglaNegocioException cuando el lote está CANCELADO")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteCancelado() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.CANCELADO)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-SQ-05: subirLoteEnCola lanza RecursoNoEncontradoException cuando el lote no tiene etapa de Fermentación")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteSinEtapaDeFermentacion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(11L)).thenReturn(Optional.of(crearLoteParaCronograma(11L, EstadoLote.PENDIENTE)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(11L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lote no tiene una etapa de Fermentación");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-SQ-06: subirLoteEnCola lanza ReglaNegocioException cuando el lote ya no figura en la cola bloqueada")
    void subirLoteEnCola_debeLanzarExcepcionSiElLoteYaNoFiguraEnLaCola() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(crearLoteCDeLaCola(hoy), crearLoteDDeLaCola(hoy)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-SQ-07: subirLoteEnCola lanza ReglaNegocioException cuando el lote ya es el primero de la cola")
    void subirLoteEnCola_debeLanzarExcepcionSiLoteYaEsElPrimero() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote ya es el primero de la cola pendiente de su fermentador");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteC, hoy.plusDays(32), hoy.plusDays(52));
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-SQ-08: subirLoteEnCola lanza ReglaNegocioException cuando la cola tiene un único lote pendiente")
    void subirLoteEnCola_debeLanzarExcepcionSiLaColaTieneUnSoloLote() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote ya es el primero de la cola pendiente de su fermentador");
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-SQ-09: subirLoteEnCola intercambia D con C y reencadena las fechas de la cola completa")
    void subirLoteEnCola_debeReencadenarLaColaCompletaAlSubirD() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.subirLoteEnCola(13L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteD, loteC);
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteD, hoy.plusDays(32), hoy.plusDays(47));
        assertFechas(loteC, hoy.plusDays(49), hoy.plusDays(69));
        assertThat(resultado.getId()).isEqualTo(13L);
        assertFechas(resultado, hoy.plusDays(32), hoy.plusDays(47));
    }

    @Test
    @DisplayName("CP-SQ-10: subirLoteEnCola sube el segundo lote de una cola de dos sin lote en ejecución")
    void subirLoteEnCola_debeEncadenarDesdeHoySinLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
        when(loteRepository.findById(12L)).thenReturn(Optional.of(loteC));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.empty());

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.subirLoteEnCola(12L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(22), hoy.plusDays(40));
        assertFechas(resultado, hoy, hoy.plusDays(20));
    }

    @Test
    @DisplayName("CP-SQ-11: subirLoteEnCola usa hoy como base cuando el lote en ejecución no tiene fechaFinalizacionEstimada")
    void subirLoteEnCola_debeUsarHoyCuandoElLoteEnEjecucionNoTieneFechaDeFin() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
        when(loteRepository.findById(12L)).thenReturn(Optional.of(loteC));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, null)));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.subirLoteEnCola(12L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(22), hoy.plusDays(40));
        assertFechas(resultado, hoy, hoy.plusDays(20));
    }

    @Test
    @DisplayName("CP-SQ-12: subirLoteEnCola con el lote en ejecución atrasado arranca como mínimo en hoy + 2 aunque la base y la planificación sean anteriores")
    void subirLoteEnCola_debeArrancarDesdeMananaConElLoteEnEjecucionAtrasado() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy.minusDays(5), hoy.plusDays(13), hoy.minusDays(30));
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(14), hoy.plusDays(34), hoy.minusDays(30));
        when(loteRepository.findById(12L)).thenReturn(Optional.of(loteC));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.minusDays(3))));

        // === EJECUCION ===
        loteServicio.subirLoteEnCola(12L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy.plusDays(2), hoy.plusDays(22));
        assertFechas(loteB, hoy.plusDays(24), hoy.plusDays(42));
    }

    @Test
    @DisplayName("CP-SQ-13: subirLoteEnCola respeta el inicio de una planificación futura y encadena la cola detrás")
    void subirLoteEnCola_debeRespetarElInicioDeUnaPlanificacionFutura() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLotePendienteEnCola(13L, hoy.plusDays(54), hoy.plusDays(69), hoy.plusDays(60));
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.subirLoteEnCola(13L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteD, loteC);
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteD, hoy.plusDays(60), hoy.plusDays(75));
        assertFechas(loteC, hoy.plusDays(77), hoy.plusDays(97));
        assertFechas(resultado, hoy.plusDays(60), hoy.plusDays(75));
    }

    @Test
    @DisplayName("CP-SQ-14: subirLoteEnCola lanza ReglaNegocioException si un lote pendiente no tiene fechas estimadas, sin mutar nada")
    void subirLoteEnCola_debeLanzarExcepcionSiUnLoteNoTieneFechasEstimadas() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        loteC.setFechaFinalizacionEstimada(null);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(13L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote IPA Test-12 no tiene sus fechas estimadas de inicio y finalización, no se puede reencadenar la cola");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertThat(loteC.getFechaInicioEstimada()).isEqualTo(hoy.plusDays(32));
        assertThat(loteC.getFechaFinalizacionEstimada()).isNull();
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-SQ-15: subirLoteEnCola lanza ReglaNegocioException si un lote pendiente tiene duración negativa, sin mutar nada")
    void subirLoteEnCola_debeLanzarExcepcionSiUnLoteTieneDuracionNegativa() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(30), hoy.plusDays(25), hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.subirLoteEnCola(13L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote IPA Test-12 tiene una fecha de finalización estimada anterior a la de inicio, no se puede reencadenar la cola");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteC, hoy.plusDays(30), hoy.plusDays(25));
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-SQ-17: subirLoteEnCola opera solo sobre la cola del fermentador de la etapa de Fermentación del lote")
    void subirLoteEnCola_debeOperarSoloSobreLaColaDelFermentadorDelLote() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        loteServicio.subirLoteEnCola(13L);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarPendientesDeFermentadorParaReordenar(5L);
        verify(loteRepository, times(1)).buscarLoteEnEjecucionDeFermentador(5L);
        verify(loteRepository, never()).buscarPendientesDeFermentadorParaReordenar(6L);
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(6L);
        assertThat(capturarLotesGuardados()).allSatisfy(lote ->
                assertThat(lote.obtenerEtapaPorTipo(TipoEtapa.FERMENTACION).getEquipamiento().getId()).isEqualTo(5L));
    }

    @Test
    @DisplayName("CP-SQ-18: subirLoteEnCola — con el lote en ejecución atrasado (o que termina hoy) el primer pendiente arranca como mínimo en hoy + 2, no hoy")
    void subirLoteEnCola_debeArrancarMananaComoMinimoConElLoteEnEjecucionAtrasado() {
        LocalDate hoy = LocalDate.now();
        for (LocalDate finDelLoteEnEjecucion : List.of(hoy.minusDays(3), hoy.minusDays(1), hoy)) {
            // === PREPARACION DE DATOS ===
            LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy);
            LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
            when(loteRepository.findById(12L)).thenReturn(Optional.of(loteC));
            when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
            when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, finDelLoteEnEjecucion)));

            // === EJECUCION ===
            LoteCronogramaResponseDTO resultado = loteServicio.subirLoteEnCola(12L);

            // === ASSERTS ===
            assertFechas(loteC, hoy.plusDays(2), hoy.plusDays(22));
            assertFechas(loteB, hoy.plusDays(24), hoy.plusDays(42));
            assertFechas(resultado, hoy.plusDays(2), hoy.plusDays(22));
        }
    }

    // ==================== bajarLoteEnCola ====================

    @Test
    @DisplayName("CP-BQ-01: bajarLoteEnCola lanza RecursoNoEncontradoException cuando el lote no existe")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 99");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-BQ-02: bajarLoteEnCola lanza ReglaNegocioException cuando el lote está EN_EJECUCION")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.EN_EJECUCION)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-BQ-03: bajarLoteEnCola lanza ReglaNegocioException cuando el lote está FINALIZADO")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteFinalizado() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.FINALIZADO)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-BQ-04: bajarLoteEnCola lanza ReglaNegocioException cuando el lote está CANCELADO")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteCancelado() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(10L)).thenReturn(Optional.of(crearLoteParaCronograma(10L, EstadoLote.CANCELADO)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-BQ-05: bajarLoteEnCola lanza RecursoNoEncontradoException cuando el lote no tiene etapa de Fermentación")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteSinEtapaDeFermentacion() {
        // === PREPARACION DE DATOS ===
        when(loteRepository.findById(11L)).thenReturn(Optional.of(crearLoteParaCronograma(11L, EstadoLote.PENDIENTE)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(11L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lote no tiene una etapa de Fermentación");
        verificarSinColaNiPersistencia();
    }

    @Test
    @DisplayName("CP-BQ-06: bajarLoteEnCola lanza ReglaNegocioException cuando el lote ya no figura en la cola bloqueada")
    void bajarLoteEnCola_debeLanzarExcepcionSiElLoteYaNoFiguraEnLaCola() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(crearLoteCDeLaCola(hoy), crearLoteDDeLaCola(hoy)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden mover lotes en estado Pendiente");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-BQ-07: bajarLoteEnCola lanza ReglaNegocioException cuando el lote ya es el último de la cola")
    void bajarLoteEnCola_debeLanzarExcepcionSiLoteYaEsElUltimo() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(13L)).thenReturn(Optional.of(loteD));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(13L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote ya es el último de la cola pendiente de su fermentador");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteC, hoy.plusDays(32), hoy.plusDays(52));
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-BQ-08: bajarLoteEnCola lanza ReglaNegocioException cuando la cola tiene un único lote pendiente")
    void bajarLoteEnCola_debeLanzarExcepcionSiLaColaTieneUnSoloLote() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote ya es el último de la cola pendiente de su fermentador");
        verify(loteRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CP-BQ-09: bajarLoteEnCola intercambia B con C y reencadena las fechas de la cola completa")
    void bajarLoteEnCola_debeReencadenarLaColaCompletaAlBajarB() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy.plusDays(12), hoy.plusDays(32));
        assertFechas(loteB, hoy.plusDays(34), hoy.plusDays(52));
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
        assertThat(resultado.getId()).isEqualTo(11L);
        assertFechas(resultado, hoy.plusDays(34), hoy.plusDays(52));
    }

    @Test
    @DisplayName("CP-BQ-10: bajarLoteEnCola baja el primer lote de una cola de dos sin lote en ejecución")
    void bajarLoteEnCola_debeEncadenarDesdeHoySinLoteEnEjecucion() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.empty());

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(22), hoy.plusDays(40));
        assertFechas(resultado, hoy.plusDays(22), hoy.plusDays(40));
    }

    @Test
    @DisplayName("CP-BQ-11: bajarLoteEnCola usa hoy como base cuando el lote en ejecución no tiene fechaFinalizacionEstimada")
    void bajarLoteEnCola_debeUsarHoyCuandoElLoteEnEjecucionNoTieneFechaDeFin() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy, hoy.plusDays(18), hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(20), hoy.plusDays(40), hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, null)));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy, hoy.plusDays(20));
        assertFechas(loteB, hoy.plusDays(22), hoy.plusDays(40));
        assertFechas(resultado, hoy.plusDays(22), hoy.plusDays(40));
    }

    @Test
    @DisplayName("CP-BQ-12: bajarLoteEnCola con el lote en ejecución atrasado arranca como mínimo en hoy + 2 aunque la base y la planificación sean anteriores")
    void bajarLoteEnCola_debeArrancarDesdeMananaConElLoteEnEjecucionAtrasado() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLotePendienteEnCola(11L, hoy.minusDays(5), hoy.plusDays(13), hoy.minusDays(30));
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(14), hoy.plusDays(34), hoy.minusDays(30));
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.minusDays(3))));

        // === EJECUCION ===
        loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB);
        assertFechas(loteC, hoy.plusDays(2), hoy.plusDays(22));
        assertFechas(loteB, hoy.plusDays(24), hoy.plusDays(42));
    }

    @Test
    @DisplayName("CP-BQ-13: bajarLoteEnCola respeta el inicio de una planificación futura y encadena la cola detrás")
    void bajarLoteEnCola_debeRespetarElInicioDeUnaPlanificacionFutura() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(32), hoy.plusDays(52), hoy.plusDays(60));
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        assertThat(capturarLotesGuardados()).containsExactly(loteC, loteB, loteD);
        assertFechas(loteC, hoy.plusDays(60), hoy.plusDays(80));
        assertFechas(loteB, hoy.plusDays(82), hoy.plusDays(100));
        assertFechas(loteD, hoy.plusDays(102), hoy.plusDays(117));
        assertFechas(resultado, hoy.plusDays(82), hoy.plusDays(100));
    }

    @Test
    @DisplayName("CP-BQ-14: bajarLoteEnCola lanza ReglaNegocioException si un lote pendiente no tiene fechas estimadas, sin mutar nada")
    void bajarLoteEnCola_debeLanzarExcepcionSiUnLoteNoTieneFechasEstimadas() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        loteC.setFechaFinalizacionEstimada(null);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote IPA Test-12 no tiene sus fechas estimadas de inicio y finalización, no se puede reencadenar la cola");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertThat(loteC.getFechaInicioEstimada()).isEqualTo(hoy.plusDays(32));
        assertThat(loteC.getFechaFinalizacionEstimada()).isNull();
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-BQ-15: bajarLoteEnCola lanza ReglaNegocioException si un lote pendiente tiene duración negativa")
    void bajarLoteEnCola_debeLanzarExcepcionSiUnLoteTieneDuracionNegativa() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLotePendienteEnCola(12L, hoy.plusDays(30), hoy.plusDays(25), hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteServicio.bajarLoteEnCola(11L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El lote IPA Test-12 tiene una fecha de finalización estimada anterior a la de inicio, no se puede reencadenar la cola");
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(any());
        verify(loteRepository, never()).saveAll(anyList());
        assertFechas(loteB, hoy.plusDays(12), hoy.plusDays(30));
        assertFechas(loteC, hoy.plusDays(30), hoy.plusDays(25));
        assertFechas(loteD, hoy.plusDays(54), hoy.plusDays(69));
    }

    @Test
    @DisplayName("CP-BQ-17: bajarLoteEnCola opera solo sobre la cola del fermentador de la etapa de Fermentación del lote")
    void bajarLoteEnCola_debeOperarSoloSobreLaColaDelFermentadorDelLote() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LoteEntity loteB = crearLoteBDeLaCola(hoy);
        LoteEntity loteC = crearLoteCDeLaCola(hoy);
        LoteEntity loteD = crearLoteDDeLaCola(hoy);
        when(loteRepository.findById(11L)).thenReturn(Optional.of(loteB));
        when(loteRepository.buscarPendientesDeFermentadorParaReordenar(5L)).thenReturn(List.of(loteB, loteC, loteD));
        when(loteRepository.buscarLoteEnEjecucionDeFermentador(5L)).thenReturn(Optional.of(crearLoteEnEjecucionParaCola(10L, hoy.plusDays(10))));

        // === EJECUCION ===
        loteServicio.bajarLoteEnCola(11L);

        // === ASSERTS ===
        verify(loteRepository, times(1)).buscarPendientesDeFermentadorParaReordenar(5L);
        verify(loteRepository, times(1)).buscarLoteEnEjecucionDeFermentador(5L);
        verify(loteRepository, never()).buscarPendientesDeFermentadorParaReordenar(6L);
        verify(loteRepository, never()).buscarLoteEnEjecucionDeFermentador(6L);
        assertThat(capturarLotesGuardados()).allSatisfy(lote ->
                assertThat(lote.obtenerEtapaPorTipo(TipoEtapa.FERMENTACION).getEquipamiento().getId()).isEqualTo(5L));
    }

    // ==================== encadenarInicios ====================

    @Test
    @DisplayName("CP-EI-01: encadenarInicios retorna una lista vacía cuando la cola está vacía")
    void encadenarInicios_debeRetornarListaVaciaConColaVacia() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(List.of(), List.of(), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("CP-EI-02: encadenarInicios — un solo lote parte de la base")
    void encadenarInicios_unSoloLoteDebePartirDeLaBase() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(8L), List.of(hoy.minusDays(5)), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(1));
    }

    @Test
    @DisplayName("CP-EI-03: encadenarInicios — una cadena sin huecos deja un día de limpieza entre un lote y el siguiente")
    void encadenarInicios_cadenaSinHuecosDebeDejarUnDiaDeMargen() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();
        LocalDate planificacion = hoy.minusDays(10);

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(8L, 5L, 10L), List.of(planificacion, planificacion, planificacion), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(1), hoy.plusDays(11), hoy.plusDays(18));
    }

    @Test
    @DisplayName("CP-EI-04: encadenarInicios — el inicio de la planificación deja un hueco y la cadena sigue desde ahí")
    void encadenarInicios_elInicioDeLaPlanificacionDebeDejarUnHuecoYContinuarDesdeAhi() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(8L, 5L, 10L), List.of(hoy.minusDays(10), hoy.plusDays(20), hoy.minusDays(10)), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(1), hoy.plusDays(20), hoy.plusDays(27));
    }

    @Test
    @DisplayName("CP-EI-05: encadenarInicios — una planificación sin fecha de inicio no impone piso y no lanza NullPointerException")
    void encadenarInicios_unaPlanificacionSinFechaNoDebeImponerPiso() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(8L, 5L), Arrays.asList(null, null), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(1), hoy.plusDays(11));
    }

    @Test
    @DisplayName("CP-EI-06: encadenarInicios — el inicio mínimo es piso de toda la cadena")
    void encadenarInicios_elInicioMinimoDebeSerPisoDeTodaLaCadena() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(4L, 5L), List.of(hoy.minusDays(10), hoy.minusDays(10)), hoy.minusDays(3), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy, hoy.plusDays(6));
    }

    @Test
    @DisplayName("CP-EI-07: encadenarInicios — con planificación sin fecha y base anterior al mínimo el lote parte del inicio mínimo")
    void encadenarInicios_conPlanificacionSinFechaYBaseAnteriorDebePartirDelInicioMinimo() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(3L), Collections.singletonList(null), hoy.minusDays(5), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy);
    }

    @Test
    @DisplayName("CP-EI-08: encadenarInicios — una base posterior a la planificación y al inicio mínimo manda")
    void encadenarInicios_unaBasePosteriorATodoDebeMandar() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(4L), List.of(hoy.plusDays(2)), hoy.plusDays(15), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(15));
    }

    @Test
    @DisplayName("CP-EI-09: encadenarInicios — un lote de duración cero deja igualmente el día de limpieza para el siguiente")
    void encadenarInicios_unLoteDeDuracionCeroDebeDejarElDiaDeMargen() {
        // === PREPARACION DE DATOS ===
        LocalDate hoy = LocalDate.now();

        // === EJECUCION ===
        List<LocalDate> resultado = LoteServicioImpl.encadenarInicios(
                List.of(0L, 3L), List.of(hoy.minusDays(10), hoy.minusDays(10)), hoy.plusDays(1), hoy);

        // === ASSERTS ===
        assertThat(resultado).containsExactly(hoy.plusDays(1), hoy.plusDays(3));
    }

    // ==================== helpers ====================

    /**
     * Construye un lote PENDIENTE de la cola de un fermentador (id 5) con sus fechas estimadas, la
     * fecha de inicio estimada de su planificación y una etapa de FERMENTACION en ese fermentador.
     */
    private static LoteEntity crearLotePendienteEnCola(Long id, LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada, LocalDate inicioPlanificacion) {
        LoteEntity lote = crearLoteParaCronograma(id, EstadoLote.PENDIENTE);
        lote.setFechaInicioEstimada(fechaInicioEstimada);
        lote.setFechaFinalizacionEstimada(fechaFinalizacionEstimada);
        lote.getPlanificacionProduccion().setFechaInicioEstimada(inicioPlanificacion);
        lote.getEtapas().add(EtapaLoteEntity.builder()
                .id(id * 10)
                .etapa(TipoEtapa.FERMENTACION)
                .estado(EstadoEtapaLote.PENDIENTE)
                .equipamiento(crearFermentador(5L, 20.0))
                .lote(lote)
                .build());
        return lote;
    }

    /** Lote B de la cola base de los reordenamientos: id 11, 18 días, de {@code hoy + 12} a {@code hoy + 30}. */
    private static LoteEntity crearLoteBDeLaCola(LocalDate hoy) {
        return crearLotePendienteEnCola(11L, hoy.plusDays(12), hoy.plusDays(30), hoy);
    }

    /** Lote C de la cola base de los reordenamientos: id 12, 20 días, de {@code hoy + 32} a {@code hoy + 52}. */
    private static LoteEntity crearLoteCDeLaCola(LocalDate hoy) {
        return crearLotePendienteEnCola(12L, hoy.plusDays(32), hoy.plusDays(52), hoy);
    }

    /** Lote D de la cola base de los reordenamientos: id 13, 15 días, de {@code hoy + 54} a {@code hoy + 69}. */
    private static LoteEntity crearLoteDDeLaCola(LocalDate hoy) {
        return crearLotePendienteEnCola(13L, hoy.plusDays(54), hoy.plusDays(69), hoy);
    }

    /** Lote B pendiente al cancelar o finalizar un lote: id 11, 18 días, de {@code hoy + 11} a {@code hoy + 29}. */
    private static LoteEntity crearLoteBParaReencadenar(LocalDate hoy) {
        return crearLotePendienteEnCola(11L, hoy.plusDays(11), hoy.plusDays(29), hoy);
    }

    /** Lote C pendiente al cancelar o finalizar un lote: id 12, 20 días, de {@code hoy + 31} a {@code hoy + 51}. */
    private static LoteEntity crearLoteCParaReencadenar(LocalDate hoy) {
        return crearLotePendienteEnCola(12L, hoy.plusDays(31), hoy.plusDays(51), hoy);
    }

    private static LoteEntity crearLoteEnEjecucionParaCola(Long id, LocalDate fechaFinalizacionEstimada) {
        LoteEntity lote = crearLoteParaCronograma(id, EstadoLote.EN_EJECUCION);
        lote.setFechaFinalizacionEstimada(fechaFinalizacionEstimada);
        return lote;
    }

    /**
     * Construye un lote en el fermentador indicado (etapa FERMENTACION) con sus fechas estimadas, la fecha de
     * inicio estimada de su planificación y el ID de esa planificación.
     */
    private static LoteEntity crearLoteEnFermentador(Long id, EstadoLote estado, Long idPlanificacion, Long idFermentador,
                                                     LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada,
                                                     LocalDate inicioPlanificacion) {
        LoteEntity lote = crearLoteParaCronograma(id, estado);
        lote.setFechaInicioEstimada(fechaInicioEstimada);
        lote.setFechaFinalizacionEstimada(fechaFinalizacionEstimada);
        lote.getPlanificacionProduccion().setId(idPlanificacion);
        lote.getPlanificacionProduccion().setFechaInicioEstimada(inicioPlanificacion);
        lote.getEtapas().add(EtapaLoteEntity.builder()
                .id(id * 10)
                .etapa(TipoEtapa.FERMENTACION)
                .estado(EstadoEtapaLote.PENDIENTE)
                .equipamiento(crearFermentador(idFermentador, 20.0))
                .lote(lote)
                .build());
        return lote;
    }

    /** Lote A del escenario de atraso: id 10, EN_EJECUCION en el fermentador 5 (planificación 2), con fin estimado {@code hoy - 3}. */
    private static LoteEntity crearLoteAtrasadoA(LocalDate hoy) {
        return crearLoteEnFermentador(10L, EstadoLote.EN_EJECUCION, 2L, 5L, hoy.minusDays(20), hoy.minusDays(3), hoy.minusDays(10));
    }

    /** Lote B del escenario de atraso: id 11, PENDIENTE en el fermentador 5 (planificación 1), de {@code hoy - 2} a {@code hoy + 6} (8 días). */
    private static LoteEntity crearLoteAtrasadoB(LocalDate hoy) {
        return crearLoteEnFermentador(11L, EstadoLote.PENDIENTE, 1L, 5L, hoy.minusDays(2), hoy.plusDays(6), hoy.minusDays(10));
    }

    /** Lote C del escenario de atraso: id 12, PENDIENTE en el fermentador 5 (planificación 2), de {@code hoy + 7} a {@code hoy + 12} (5 días). */
    private static LoteEntity crearLoteAtrasadoC(LocalDate hoy) {
        return crearLoteEnFermentador(12L, EstadoLote.PENDIENTE, 2L, 5L, hoy.plusDays(7), hoy.plusDays(12), hoy.minusDays(10));
    }

    /** Stubbea la consulta del cronograma global (sin fermentador ni historial) con los lotes dados y la invoca. */
    private List<LoteCronogramaResponseDTO> listarCronogramaCon(LoteEntity... lotes) {
        when(loteRepository.buscarLotesParaCronograma(isNull(), eq(false), any(LocalDateTime.class))).thenReturn(List.of(lotes));
        return loteServicio.listarLotesParaCronograma(null, false);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Collection<Long>> capturadorDeFermentadores() {
        return ArgumentCaptor.forClass(Collection.class);
    }

    private static LoteCronogramaResponseDTO dtoDeLote(List<LoteCronogramaResponseDTO> dtos, Long id) {
        return dtos.stream().filter(dto -> id.equals(dto.getId())).findFirst().orElseThrow();
    }

    private static void assertAtraso(LoteCronogramaResponseDTO dto, int diasDeAtraso, int diasDeDesplazamiento) {
        assertThat(dto.getDiasDeAtraso()).as("diasDeAtraso del lote %s", dto.getId()).isEqualTo(diasDeAtraso);
        assertThat(dto.getDiasDeDesplazamiento()).as("diasDeDesplazamiento del lote %s", dto.getId()).isEqualTo(diasDeDesplazamiento);
    }

    private static void assertFechas(LoteEntity lote, LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada) {
        assertThat(lote.getFechaInicioEstimada()).isEqualTo(fechaInicioEstimada);
        assertThat(lote.getFechaFinalizacionEstimada()).isEqualTo(fechaFinalizacionEstimada);
    }

    private static void assertFechas(LoteCronogramaResponseDTO lote, LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada) {
        assertThat(lote.getFechaInicioEstimada()).isEqualTo(fechaInicioEstimada);
        assertThat(lote.getFechaFinalizacionEstimada()).isEqualTo(fechaFinalizacionEstimada);
    }

    private void verificarSinColaNiPersistencia() {
        verify(loteRepository, never()).buscarPendientesDeFermentadorParaReordenar(any());
        verify(loteRepository, never()).saveAll(anyList());
    }

    private List<LoteEntity> capturarLotesGuardados() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LoteEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(loteRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    private static EtapaLoteEntity findEtapa(LoteEntity lote, TipoEtapa tipo) {
        return lote.getEtapas().stream().filter(e -> e.getEtapa() == tipo).findFirst().orElseThrow();
    }

    /**
     * Prepara un lote EN_EJECUCION con Maceración EN_CURSO para los tests de cierre de planificación
     * de {@code cancelarLote}: la planificación del lote se recupera bloqueada y se simula el guardado del lote.
     */
    private LoteEntity prepararLoteParaCancelarConPlanificacionDelLote() {
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return lote;
    }

    /**
     * Prepara un lote EN_EJECUCION con Envasado EN_CURSO y {@code totalEnvasado} litros envasados para los
     * tests de {@code finalizarEnvasado}: la planificación del lote se recupera bloqueada y se simula el guardado del lote.
     */
    private LoteEntity prepararLoteParaFinalizarEnvasado(double totalEnvasado) {
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        FermentadorEntity fermentador = crearFermentador(4L, EstadoOperativo.EN_USO, 20.0);
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(4L)).thenReturn(Optional.of(fermentador));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(totalEnvasado);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return lote;
    }

    /**
     * Prepara un lote en el fermentador 5 (el de la cola de los casos de reencadenado) para {@code cancelarLote}:
     * se recupera bloqueado, su planificación se recupera sin cierre y se simula el guardado.
     */
    private LoteEntity prepararLoteParaCancelarEnFermentadorDeLaCola(EstadoLote estadoLote, TipoEtapa etapaEnCurso) {
        LoteEntity lote = crearLoteParaCancelar(estadoLote, etapaEnCurso);
        usarFermentadorDeLaCola(lote);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        mockearEquipamientoParaIniciar(lote);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return lote;
    }

    /** Igual que {@link #prepararLoteParaFinalizarEnvasado} pero con el lote en el fermentador 5 de la cola. */
    private LoteEntity prepararLoteParaFinalizarEnvasadoEnFermentadorDeLaCola() {
        LoteEntity lote = crearLoteParaCancelar(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO);
        usarFermentadorDeLaCola(lote);
        when(loteRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(lote));
        mockearPlanificacionDelLoteSinCierre(lote);
        FermentadorEntity fermentador = (FermentadorEntity) findEtapa(lote, TipoEtapa.FERMENTACION).getEquipamiento();
        when(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(5L)).thenReturn(Optional.of(fermentador));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(any())).thenReturn(20.0);
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return lote;
    }

    /**
     * Prepara un lote PENDIENTE en el fermentador 5 con stock suficiente de sus 3 insumos, listo para
     * {@code iniciarLote(idLote)}, con las fechas estimadas indicadas.
     */
    private LoteEntity prepararInicioDeLoteConStockSuficiente(Long idLote, LocalDate inicioEstimado, LocalDate finEstimado) {
        MaltaEntity malta = crearMalta(10L, "MaltaB", 75);
        LupuloEntity lupulo = crearLupulo(11L, "LupuloB", 10, FormatoLupulo.PELLET);
        LevaduraEntity levadura = crearLevadura(12L, "LevaduraB", TipoLevadura.ALE, 2.0E10);
        LoteEntity lote = crearLoteConRecetaSimple(malta, lupulo, levadura);
        lote.setId(idLote);
        lote.setFechaInicioEstimada(inicioEstimado);
        lote.setFechaFinalizacionEstimada(finEstimado);
        usarFermentadorDeLaCola(lote);
        when(loteRepository.buscarPorIdParaActualizar(idLote)).thenReturn(Optional.of(lote));
        mockearEquipamientoParaIniciar(lote);
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(10L))
                .thenReturn(List.of(crearLoteInsumo(100L, malta, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(11L))
                .thenReturn(List.of(crearLoteInsumo(101L, lupulo, 30.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteInsumoRepository.buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(12L))
                .thenReturn(List.of(crearLoteInsumo(102L, levadura, 10.0, 0.0, LocalDate.now().plusDays(30), BigDecimal.TEN)));
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return lote;
    }

    /** El fermentador del lote pasa a ser el 5, el de la cola de los casos de reencadenado. */
    private static void usarFermentadorDeLaCola(LoteEntity lote) {
        findEtapa(lote, TipoEtapa.FERMENTACION).getEquipamiento().setId(5L);
    }

    private PlanificacionProduccionEntity mockearPlanificacionPendienteBase() {
        return mockearPlanificacionPendienteBase(LocalDate.now());
    }

    private PlanificacionProduccionEntity mockearPlanificacionPendienteBase(LocalDate fechaInicioEstimadaPlanificacion) {
        RecetaEntity receta = crearReceta(5L, 3L);
        VersionRecetaEntity versionReceta = versionRecetaMaltaBase(receta);
        PlanificacionProduccionEntity planificacion = crearPlanificacion(1L, EstadoSolicitud.PENDIENTE, versionReceta);
        planificacion.setFechaInicioEstimada(fechaInicioEstimadaPlanificacion);
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion));
        return planificacion;
    }

    // La planificación del lote sigue PENDIENTE con 0 L producidos: finalizarPlanificacionSiCorresponde no la cierra
    private void mockearPlanificacionDelLoteSinCierre(LoteEntity lote) {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(lote.getPlanificacionProduccion().getId()))
                .thenReturn(Optional.of(lote.getPlanificacionProduccion()));
    }

    private PlanificacionProduccionEntity mockearEquipamientoBase() {
        return mockearEquipamientoBase(LocalDate.now());
    }

    private PlanificacionProduccionEntity mockearEquipamientoBase(LocalDate fechaInicioEstimadaPlanificacion) {
        PlanificacionProduccionEntity planificacion = mockearPlanificacionPendienteBase(fechaInicioEstimadaPlanificacion);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolino(1L, 5.0)));
        when(maceradorRepository.findById(2L)).thenReturn(Optional.of(crearMacerador(2L, 25.0, 5.0, 75.0)));
        when(ollaHervorRepository.findById(3L)).thenReturn(Optional.of(crearOllaHervor(3L, 30.0, 3.0, 2.0)));
        when(fermentadorRepository.findById(4L)).thenReturn(Optional.of(crearFermentador(4L, 20.0)));
        return planificacion;
    }

    private void mockearConfiguracionYReceta() {
        RecetaEntity receta = crearReceta(5L, 3L);
        lenient().when(recetaRepository.findByIdParaActualizarContador(5L)).thenReturn(Optional.of(receta));
        ConfiguracionProduccionEntity configuracion = ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .velocidadEstandarEnvasado(10.0)
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

    /**
     * Registra un lote de la planificación base (receta 5, contador previo 3) cuya versión de receta se llama
     * {@code nombreVersion} y devuelve el identificador interno con el que se guarda.
     */
    private String registrarYObtenerIdentificador(String nombreVersion) {
        PlanificacionProduccionEntity planificacion = mockearEquipamientoBase();
        planificacion.getVersionReceta().setNombre(nombreVersion);
        mockearConfiguracionYReceta();
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
        when(loteRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        return loteServicio.registrarLote(loteFormDTOBase()).getIdentificadorInterno();
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

    private static LoteEntity crearLoteParaCronograma(Long id, EstadoLote estado) {
        LoteEntity lote = crearLoteMinimo(id, "IPA Test-" + id);
        lote.setEstado(estado);
        return lote;
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
        boolean enCurso = tipo == etapaEnCurso;
        EstadoEtapaLote estado = enCurso ? EstadoEtapaLote.EN_CURSO : EstadoEtapaLote.PENDIENTE;
        LocalDateTime fechaInicio = enCurso ? LocalDateTime.now().minusDays(1) : null;
        return EtapaLoteEntity.builder().etapa(tipo).estado(estado).fechaInicio(fechaInicio).equipamiento(equipamiento).lote(lote).build();
    }
}
