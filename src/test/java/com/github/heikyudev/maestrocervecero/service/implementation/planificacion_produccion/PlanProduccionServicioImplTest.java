package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.LotePlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanesProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionConLitrosAPlanificarResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link PlanProduccionServicioImpl}.
 * <p>
 * La masa de malta se simula en 0,2 kg por litro (el escalado de insumos es un mock) y la receta dura 60 minutos de
 * maceración, 60 de hervor, 14 días de fermentación y 7 de maduración. Con una velocidad de envasado de 100 L/h y un
 * molino de 100 kg/h, un lote de hasta 1.000 L ocupa un día de brewing y envasado: dura 22 días.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PlanProduccionServicioImplTest {

    private static final double KG_MALTA_POR_LITRO = 0.2;
    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private ILoteRepository loteRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IConfiguracionProduccionRepository configuracionProduccionRepository;
    @Mock
    private IFermentadorRepository fermentadorRepository;
    @Mock
    private IMolinoRepository molinoRepository;
    @Mock
    private IMaceradorRepository maceradorRepository;
    @Mock
    private IOllaHervorRepository ollaHervorRepository;
    @Mock
    private IEscaladoInsumoServicio escaladoInsumoServicio;
    @Mock
    private ILoteServicio loteServicio;
    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private PlanProduccionServicioImpl planProduccionServicio;

    private ConfiguracionProduccionEntity configuracion;

    @BeforeEach
    void prepararDatosComunes() {
        configuracion = ConfiguracionProduccionEntity.builder()
                .id(ConfiguracionProduccionEntity.SINGLETON_ID)
                .velocidadEstandarEnvasado(100.0)
                .capacidadLoteEstandar(500.0)
                .porcentajeMinimoConsumoParaAvanzarEtapa(90.0)
                .criterioSeleccionPlanSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD)
                .criterioSeleccionPlanConcurrente(CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES)
                .excedenteMaximoLitros(300.0)
                .build();
        lenient().when(configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)).thenReturn(Optional.of(configuracion));
        lenient().when(escaladoInsumoServicio.calcularRequerimientosMalta(any(), anyDouble(), any()))
                .thenAnswer(inv -> List.of(new RequerimientoInsumo(null, null, KG_MALTA_POR_LITRO * (double) inv.getArgument(1))));
        lenient().when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(0.0);
        lenient().when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(0.0);
        lenient().when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(any(), anyList())).thenReturn(Optional.empty());
    }

    // ==================== generarPlanes ====================

    @Test
    @DisplayName("CP-GPL-01: generarPlanes lanza RecursoNoEncontradoException si la planificación no existe")
    void generar_planificacionInexistente() {
        when(planificacionProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planProduccionServicio.generarPlanes(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");
    }

    @Test
    @DisplayName("CP-GPL-02: generarPlanes informa ambos planes como no disponibles si la planificación no está Pendiente")
    void generar_planificacionNoPendiente() {
        mockearPlanificacion(EstadoSolicitud.FINALIZADA, 1000.0);

        PlanesProduccionResponseDTO planes = planProduccionServicio.generarPlanes(1L);

        assertNoDisponibles(planes, "La planificación de producción debe encontrarse en estado Pendiente para generar o confirmar un plan de producción");
        verify(fermentadorRepository, never()).buscarActivos();
    }

    @Test
    @DisplayName("CP-GPL-03: generarPlanes informa ambos planes como no disponibles si lo producido y los lotes vivos ya cubren la cantidad a producir")
    void generar_planificacionCubierta() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(400.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(600.0);

        PlanesProduccionResponseDTO planes = planProduccionServicio.generarPlanes(1L);

        assertThat(planes.getLitrosAPlanificar()).isEqualTo(0.0);
        assertNoDisponibles(planes, "La planificación ya tiene lotes que cubren la cantidad a producir (1.000 L comprometidos de 1.000 L solicitados)");
    }

    @Test
    @DisplayName("CP-GPL-04: generarPlanes informa ambos planes como no disponibles si no hay fermentadores activos")
    void generar_sinFermentadores() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquipos(List.of(), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        assertNoDisponibles(planProduccionServicio.generarPlanes(1L), "No hay fermentadores activos para generar el plan");
    }

    @Test
    @DisplayName("CP-GPL-05: generarPlanes informa ambos planes como no disponibles si no hay molinos activos")
    void generar_sinMolinos() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquipos(List.of(fermentador(1L, 500.0)), List.of(), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        assertNoDisponibles(planProduccionServicio.generarPlanes(1L), "No hay molinos activos para generar el plan");
    }

    @Test
    @DisplayName("CP-GPL-06: el plan secuencial con FERMENTADOR_MAYOR_CAPACIDAD usa el fermentador más grande aunque esté ocupado y encadena sus lotes")
    void secuencial_mayorCapacidad() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        FermentadorEntity grande = fermentador(1L, 500.0);
        FermentadorEntity chico = fermentador(2L, 300.0);
        mockearEquipos(List.of(grande, chico), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(eq(grande), anyList())).thenReturn(Optional.of(HOY.plusDays(30)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getCriterio()).isEqualTo("Fermentador de mayor capacidad");
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getIdFermentador).containsExactly(1L, 1L);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(500.0, 500.0);
        LotePlanProduccionResponseDTO primero = plan.getLotes().get(0);
        LotePlanProduccionResponseDTO segundo = plan.getLotes().get(1);
        assertThat(primero.getFechaInicioEstimada()).isEqualTo(HOY.plusDays(32));
        assertThat(primero.getFechaFinalizacionEstimada()).isEqualTo(HOY.plusDays(54));
        assertThat(segundo.getFechaInicioEstimada()).isEqualTo(HOY.plusDays(56));
        assertThat(plan.getFechaFinalizacionPlan()).isEqualTo(HOY.plusDays(78));
        assertThat(plan.getCantidadLotes()).isEqualTo(2);
        assertThat(plan.getVolumenTotal()).isEqualTo(1000.0);
        assertThat(plan.getExcedente()).isEqualTo(0.0);
        assertThat(plan.getFermentadoresUtilizados()).isEqualTo(1);
        assertThat(plan.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPL-07: el plan secuencial con FERMENTADOR_LIBERACION_MAS_TEMPRANA usa el que se libera antes y reparte los litros en partes iguales")
    void secuencial_liberacionMasTemprana() {
        configuracion.setCriterioSeleccionPlanSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA);
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        FermentadorEntity grande = fermentador(1L, 500.0);
        FermentadorEntity chico = fermentador(2L, 300.0);
        mockearEquipos(List.of(grande, chico), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));
        when(loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(eq(grande), anyList())).thenReturn(Optional.of(HOY.plusDays(30)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.getCriterio()).isEqualTo("Fermentador que se libera más temprano");
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getIdFermentador).containsOnly(2L).hasSize(4);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsOnly(250.0);
        assertThat(plan.getLotes().get(0).getFechaInicioEstimada()).isEqualTo(HOY);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getOrden).containsExactly(1, 2, 3, 4);
        assertThat(plan.getVolumenTotal()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("CP-GPL-08: el plan concurrente MENOR_CANTIDAD_DE_LOTES reparte 2.000 L en dos olas de 500/300/223 L como 489/293/218 L (2.000 L, método del resto)")
    void concurrente_menorCantidadDeLotes() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 2000.0);
        mockearEquipos(List.of(fermentador(1L, 500.0), fermentador(2L, 300.0), fermentador(3L, 223.0)),
                List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getConcurrente();

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getCriterio()).isEqualTo("Menor cantidad de lotes");
        assertThat(plan.getCantidadLotes()).isEqualTo(6);
        assertThat(plan.getFermentadoresUtilizados()).isEqualTo(3);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getIdFermentador).containsExactly(1L, 2L, 3L, 1L, 2L, 3L);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(489.0, 293.0, 218.0, 489.0, 293.0, 218.0);
        assertThat(plan.getVolumenTotal()).isEqualTo(2000.0);
        assertThat(plan.getExcedente()).isEqualTo(0.0);
        assertThat(plan.getLotes().get(0).getFechaInicioEstimada()).isEqualTo(HOY);
        assertThat(plan.getLotes().get(3).getFechaInicioEstimada()).isEqualTo(HOY.plusDays(24));
    }

    @Test
    @DisplayName("CP-GPL-09: el plan concurrente EQUIPOS_LIBERACION_MAS_TEMPRANA asigna cada lote al fermentador que se libera antes: A, B, C y otra vez A")
    void concurrente_liberacionMasTemprana() {
        configuracion.setCriterioSeleccionPlanConcurrente(CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA);
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1500.0);
        mockearEquipos(List.of(fermentador(1L, 500.0), fermentador(2L, 400.0), fermentador(3L, 300.0)),
                List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getConcurrente();

        assertThat(plan.getCriterio()).isEqualTo("Equipos que se liberan más temprano");
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getIdFermentador).containsExactly(1L, 2L, 3L, 1L);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(441.0, 353.0, 265.0, 441.0);
        assertThat(plan.getVolumenTotal()).isEqualTo(1500.0);
        assertThat(plan.getLotes().get(3).getFechaInicioEstimada()).isEqualTo(HOY.plusDays(24));
    }

    @Test
    @DisplayName("CP-GPL-10: con un solo fermentador utilizable el plan concurrente no está disponible y el secuencial sí")
    void concurrente_unSoloFermentador() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquipos(List.of(fermentador(1L, 500.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanesProduccionResponseDTO planes = planProduccionServicio.generarPlanes(1L);

        assertThat(planes.getSecuencial().isDisponible()).isTrue();
        assertThat(planes.getConcurrente().isDisponible()).isFalse();
        assertThat(planes.getConcurrente().getMotivoNoDisponible()).isEqualTo("El plan concurrente necesita al menos dos fermentadores activos que puedan recibir un lote");
        assertThat(planes.getConcurrente().getLotes()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPL-11: la capacidad efectiva de un fermentador se limita al mayor volumen que admite alguna olla de hervor activa")
    void capacidadEfectiva_limitadaPorLaOlla() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 800.0);
        // Pre-hervor = volumen + 5 L de trub + 5 L/h de evaporación durante 1 h: admite hasta 400 L en 410 L útiles
        mockearEquipos(List.of(fermentador(1L, 1000.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)),
                List.of(olla(1L, 410.0, 5.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(400.0, 400.0);
    }

    @Test
    @DisplayName("CP-GPL-12: cada lote usa el molino de mayor rendimiento, el macerador compatible de mayor eficiencia y la olla compatible de menor trub (a igualdad, mayor evaporación)")
    void equiposCalientes_seleccion() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 400.0);
        MolinoEntity molinoLento = molino(1L, 50.0);
        MolinoEntity molinoRapido = molino(2L, 100.0);
        MaceradorEntity maceradorCompatible = maceradorGrande(3L, 70.0);
        MaceradorEntity maceradorChico = macerador(4L, 50.0, 1.0, 85.0);
        MaceradorEntity maceradorMenosEficiente = maceradorGrande(5L, 60.0);
        OllaHervorEntity ollaPocaEvaporacion = ollaGrande(6L, 1.0, 3.0);
        OllaHervorEntity ollaMasEvaporacion = ollaGrande(7L, 1.0, 5.0);
        OllaHervorEntity ollaChica = olla(8L, 50.0, 0.5, 5.0);
        mockearEquipos(List.of(fermentador(1L, 400.0)), List.of(molinoLento, molinoRapido),
                List.of(maceradorCompatible, maceradorChico, maceradorMenosEficiente), List.of(ollaPocaEvaporacion, ollaMasEvaporacion, ollaChica));

        LotePlanProduccionResponseDTO lote = planProduccionServicio.generarPlanes(1L).getSecuencial().getLotes().get(0);

        assertThat(lote.getIdMolino()).isEqualTo(2L);
        assertThat(lote.getIdMacerador()).isEqualTo(3L);
        assertThat(lote.getIdOllaHervor()).isEqualTo(7L);
        assertThat(lote.getIdentificadorMolino()).isEqualTo("MOL-2");
    }

    @Test
    @DisplayName("CP-GPL-13: un plan que necesita más lotes que el máximo (100) no está disponible")
    void secuencial_superaElMaximoDeLotes() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquipos(List.of(fermentador(1L, 5.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.isDisponible()).isFalse();
        assertThat(plan.getMotivoNoDisponible()).isEqualTo("El plan secuencial necesita 200 lotes y el máximo es de 100");
    }

    @Test
    @DisplayName("CP-GPL-14: con un excedente máximo de 0 L el plan concurrente sigue disponible y cubre los litros exactos")
    void concurrenteConExcedenteCero() {
        configuracion.setExcedenteMaximoLitros(0.0);
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 2000.0);
        mockearEquipos(List.of(fermentador(1L, 500.0), fermentador(2L, 300.0), fermentador(3L, 223.0)),
                List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getConcurrente();

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getVolumenTotal()).isEqualTo(2000.0);
        assertThat(plan.getExcedente()).isEqualTo(0.0);
        assertThat(plan.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("CP-GPL-15: si ningún equipo del lado caliente admite 1 L, ningún fermentador es utilizable y ambos planes no están disponibles")
    void sinCapacidadDelLadoCaliente() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquipos(List.of(fermentador(1L, 500.0), fermentador(2L, 300.0)), List.of(molino(1L, 100.0)),
                List.of(maceradorGrande(1L, 75.0)), List.of(olla(1L, 5.0, 10.0, 5.0)));

        assertNoDisponibles(planProduccionServicio.generarPlanes(1L),
                "Ningún fermentador activo puede recibir un lote: los maceradores y las ollas de hervor activos no admiten un lote de al menos 1 L con la receta de la planificación");
    }

    @Test
    @DisplayName("CP-GPL-16: los litros a planificar descuentan lo producido y los lotes vivos de la planificación")
    void litrosAPlanificarDescuentanLoComprometido() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(1L)).thenReturn(200.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(300.0);
        mockearEquipos(List.of(fermentador(1L, 500.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanesProduccionResponseDTO planes = planProduccionServicio.generarPlanes(1L);

        assertThat(planes.getLitrosAPlanificar()).isEqualTo(500.0);
        assertThat(planes.getSecuencial().getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(500.0);
    }

    @Test
    @DisplayName("CP-GPL-17: con 1 L a planificar y dos fermentadores, el concurrente (un lote quedaría vacío) no está disponible y el secuencial propone un lote de 1 L")
    void litrosMinimosConVariosFermentadores() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1.0);
        mockearEquipos(List.of(fermentador(1L, 100.0), fermentador(2L, 100.0)), List.of(molino(1L, 100.0)),
                List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanesProduccionResponseDTO planes = planProduccionServicio.generarPlanes(1L);

        assertThat(planes.getSecuencial().isDisponible()).isTrue();
        assertThat(planes.getSecuencial().getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(1.0);
        assertThat(planes.getConcurrente().isDisponible()).isFalse();
        assertThat(planes.getConcurrente().getMotivoNoDisponible()).startsWith("Ninguna combinación de fermentadores en paralelo cubre los 1 L")
                .contains("Los 1 L que faltan planificar no alcanzan para repartirlos en 2 lotes");
    }

    @Test
    @DisplayName("CP-GPL-18: 3.000,04 L en un fermentador de 1.000 L necesitan 4 lotes (cociente exacto) y se reparten en 751/750/750/750 L (3.001 L)")
    void cocienteExactoAntesDelTecho() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 3000.04);
        mockearEquipos(List.of(fermentador(1L, 1000.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getCantidadLotes()).isEqualTo(4);
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(751.0, 750.0, 750.0, 750.0);
        assertThat(plan.getVolumenTotal()).isEqualTo(3001.0);
    }

    @Test
    @DisplayName("CP-GPL-19: con un excedente máximo de 0 L y litros con decimales, el último lote lleva la fracción y el total es exacto")
    void excedenteCeroConLitrosFraccionarios() {
        configuracion.setExcedenteMaximoLitros(0.0);
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.5);
        mockearEquipos(List.of(fermentador(1L, 500.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getVolumenObjetivo).containsExactly(334.0, 333.0, 333.5);
        assertThat(plan.getVolumenTotal()).isEqualTo(1000.5);
        assertThat(plan.getExcedente()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-GPL-20: un plan que termina después de la fecha de finalización estimada de la planificación sigue disponible con un aviso")
    void avisoDeFinalizacionTardia() {
        PlanificacionProduccionEntity planificacion = mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        planificacion.setFechaFinalizacionEstimada(HOY.plusDays(30));
        mockearEquipos(List.of(fermentador(1L, 500.0)), List.of(molino(1L, 100.0)), List.of(maceradorGrande(1L, 75.0)), List.of(ollaGrande(1L, 1.0, 5.0)));

        PlanProduccionResponseDTO plan = planProduccionServicio.generarPlanes(1L).getSecuencial();

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getFechaFinalizacionPlan()).isEqualTo(HOY.plusDays(46));
        assertThat(plan.getAvisos()).containsExactly("El plan finaliza el " + HOY.plusDays(46).format(formato)
                + ", después de la fecha de finalización estimada de la planificación (" + HOY.plusDays(30).format(formato) + ")");
    }

    // ==================== recalcularPlan ====================

    @Test
    @DisplayName("CP-RPL-01: recalcularPlan lanza ReglaNegocioException si falta la modalidad o el plan no tiene lotes")
    void recalcular_datosDelPlanObligatorios() {
        PlanProduccionFormDTO sinModalidad = PlanProduccionFormDTO.builder().lotes(new ArrayList<>(List.of(lote(500.0, 1L)))).build();
        PlanProduccionFormDTO sinLotes = PlanProduccionFormDTO.builder().modalidad(ModalidadPlanProduccion.SECUENCIAL).build();

        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, sinModalidad))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La modalidad del plan es obligatoria");
        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, sinLotes))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El plan debe tener al menos un lote");
        verify(planificacionProduccionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-RPL-02: recalcularPlan lanza ReglaNegocioException con el número de lote si a un lote le falta un equipo o el volumen no es mayor a 0")
    void recalcular_datosDelLoteObligatorios() {
        LotePlanProduccionFormDTO sinFermentador = lote(500.0, 1L);
        sinFermentador.setIdFermentador(null);
        PlanProduccionFormDTO conLoteIncompleto = plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L), sinFermentador);
        PlanProduccionFormDTO conVolumenCero = plan(ModalidadPlanProduccion.SECUENCIAL, lote(0.0, 1L));

        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, conLoteIncompleto))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("Lote 2 del plan: El fermentador es obligatorio");
        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, conVolumenCero))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("Lote 1 del plan: El volumen objetivo debe ser mayor a 0");
    }

    @Test
    @DisplayName("CP-RPL-03: recalcularPlan lanza RecursoNoEncontradoException con el número de lote si un equipo no existe o no está activo")
    void recalcular_equipoInexistente() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        when(fermentadorRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 9L))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Lote 1 del plan: No se encontró el fermentador con ID: 9");
    }

    @Test
    @DisplayName("CP-RPL-06: recalcularPlan rechaza con el número de lote un plan cuyo fermentador fue dado de baja después de generarlo")
    void recalcular_fermentadorDadoDeBaja() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        FermentadorEntity fermentadorDeBaja = fermentador(9L, 1000.0);
        fermentadorDeBaja.setEstado(Estado.BAJA);
        when(equipamientoRepository.findById(9L)).thenReturn(Optional.of(fermentadorDeBaja));

        assertThatThrownBy(() -> planProduccionServicio.recalcularPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 9L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Lote 1 del plan: El fermentador FER-9 fue dado de baja, genere el plan nuevamente");
    }

    @Test
    @DisplayName("CP-RPL-04: recalcularPlan encadena las fechas de los lotes de un mismo fermentador en el orden dado y avisa lo que el registro rechazaría")
    void recalcular_encadenaFechasYAvisa() {
        mockearPlanificacion(EstadoSolicitud.PENDIENTE, 1000.0);
        mockearEquiposPorId(fermentador(1L, 500.0));

        PlanProduccionResponseDTO plan = planProduccionServicio.recalcularPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(600.0, 1L), lote(500.0, 1L)));

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getLotes()).extracting(LotePlanProduccionResponseDTO::getOrden).containsExactly(1, 2);
        assertThat(plan.getLotes().get(0).getFechaInicioEstimada()).isEqualTo(HOY);
        assertThat(plan.getLotes().get(1).getFechaInicioEstimada()).isEqualTo(plan.getLotes().get(0).getFechaFinalizacionEstimada().plusDays(2));
        assertThat(plan.getVolumenTotal()).isEqualTo(1100.0);
        assertThat(plan.getExcedente()).isEqualTo(100.0);
        assertThat(plan.getAvisos()).containsExactly(
                "Lote 1 del plan: El volumen objetivo (600 L) supera la capacidad útil del fermentador FER-1 (500 L)");
    }

    @Test
    @DisplayName("CP-RPL-05: recalcularPlan avisa si el plan modificado termina después de la fecha de finalización estimada de la planificación")
    void recalcular_avisaFinalizacionTardia() {
        PlanificacionProduccionEntity planificacion = mockearPlanificacion(EstadoSolicitud.PENDIENTE, 500.0);
        planificacion.setFechaFinalizacionEstimada(HOY.plusDays(10));
        mockearEquiposPorId(fermentador(1L, 500.0));

        PlanProduccionResponseDTO plan = planProduccionServicio.recalcularPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L)));

        assertThat(plan.getAvisos()).singleElement().asString().startsWith("El plan finaliza el ");
    }

    // ==================== confirmarPlan ====================

    @Test
    @DisplayName("CP-CPL-01: confirmarPlan lanza ReglaNegocioException si la planificación no está Pendiente y no registra lotes")
    void confirmar_planificacionNoPendiente() {
        mockearPlanificacionBloqueada(EstadoSolicitud.ANULADA, 1000.0);

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L), lote(500.0, 1L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación de producción debe encontrarse en estado Pendiente para generar o confirmar un plan de producción");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-02: confirmarPlan lanza RecursoNoEncontradoException si la planificación no existe")
    void confirmar_planificacionInexistente() {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(99L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la planificación de producción con ID: 99");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-03: confirmarPlan rechaza un plan cuyo volumen total no alcanza los litros a planificar")
    void confirmar_volumenInsuficiente() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L), lote(499.0, 1L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El volumen total del plan (999 L) no alcanza los 1.000 L que faltan planificar");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-04: confirmarPlan rechaza un plan que supera los litros a planificar en más del excedente máximo")
    void confirmar_superaElExcedenteMaximo() {
        configuracion.setExcedenteMaximoLitros(10.0);
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(600.0, 1L), lote(500.0, 1L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El volumen total del plan (1.100 L) supera en 100 L a los 1.000 L que faltan planificar y el excedente máximo configurado es de 10 L");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-05: confirmarPlan rechaza un plan con un lote de más aunque el excedente esté dentro del máximo")
    void confirmar_loteDeMas() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.CONCURRENTE, lote(500.0, 1L), lote(500.0, 2L), lote(100.0, 3L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El plan tiene un lote de más: sin el lote de menor volumen (100 L) los demás ya cubren los 1.000 L que faltan planificar");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-06: confirmarPlan rechaza una planificación cuyos lotes ya cubren la cantidad a producir")
    void confirmar_planificacionCubierta() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(1L)).thenReturn(1000.0);

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La planificación ya tiene lotes que cubren la cantidad a producir (1.000 L comprometidos de 1.000 L solicitados)");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-07: confirmarPlan registra cada lote con el registro de lotes, en el orden dado y con la planificación de la URL")
    void confirmar_registraTodosLosLotes() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        when(loteServicio.registrarLote(any())).thenAnswer(inv -> LoteResponseDTO.builder()
                .volumenObjetivo(((LoteFormDTO) inv.getArgument(0)).getVolumenObjetivo()).build());

        List<LoteResponseDTO> registrados = planProduccionServicio.confirmarPlan(1L,
                plan(ModalidadPlanProduccion.CONCURRENTE, lote(600.0, 1L), lote(401.0, 2L)));

        ArgumentCaptor<LoteFormDTO> captor = ArgumentCaptor.forClass(LoteFormDTO.class);
        verify(loteServicio, times(2)).registrarLote(captor.capture());
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getIdFermentador).containsExactly(1L, 2L);
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getVolumenObjetivo).containsExactly(600.0, 401.0);
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getIdPlanificacionProduccion).containsOnly(1L);
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getIdMolino).containsOnly(10L);
        assertThat(registrados).hasSize(2);
    }

    @Test
    @DisplayName("CP-CPL-08: si el registro rechaza un lote, confirmarPlan lanza el motivo con el número de lote y no sigue registrando")
    void confirmar_rechazoDelRegistro() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        when(loteServicio.registrarLote(any()))
                .thenReturn(LoteResponseDTO.builder().build())
                .thenThrow(new ReglaNegocioException("El volumen objetivo (600 L) supera la capacidad útil del fermentador seleccionado (500 L)"));

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L,
                plan(ModalidadPlanProduccion.SECUENCIAL, lote(500.0, 1L), lote(600.0, 1L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Lote 2 del plan: El volumen objetivo (600 L) supera la capacidad útil del fermentador seleccionado (500 L)")
                .hasCauseInstanceOf(ReglaNegocioException.class);
        verify(loteServicio, times(2)).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-10: confirmarPlan bloquea primero los fermentadores distintos del plan por ID ascendente y luego registra los lotes en el orden del plan")
    void confirmar_bloqueaFermentadoresYRegistraEnOrdenDelPlan() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        when(loteServicio.registrarLote(any())).thenAnswer(inv -> {
            LoteFormDTO lote = inv.getArgument(0);
            if (lote.getVolumenObjetivo() == 251.0) {
                throw new ReglaNegocioException("El volumen objetivo (251 L) supera la capacidad útil del fermentador seleccionado (250 L)");
            }
            return LoteResponseDTO.builder().volumenObjetivo(lote.getVolumenObjetivo()).build();
        });

        // Plan: lote 1 en el fermentador 3 (250 L), lote 2 en el 1, lote 3 en el 3 (251 L), lote 4 en el 2
        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.CONCURRENTE,
                lote(250.0, 3L), lote(260.0, 1L), lote(251.0, 3L), lote(240.0, 2L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Lote 3 del plan: El volumen objetivo (251 L) supera la capacidad útil del fermentador seleccionado (250 L)");

        InOrder orden = inOrder(fermentadorRepository, loteServicio);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(3L);
        orden.verify(loteServicio, times(3)).registrarLote(any());
        verify(fermentadorRepository, times(3)).buscarPorIdParaCambiarEstadoOperativo(any());

        ArgumentCaptor<LoteFormDTO> captor = ArgumentCaptor.forClass(LoteFormDTO.class);
        verify(loteServicio, times(3)).registrarLote(captor.capture());
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getIdFermentador).containsExactly(3L, 1L, 3L);
        assertThat(captor.getAllValues()).extracting(LoteFormDTO::getVolumenObjetivo).containsExactly(250.0, 260.0, 251.0);
    }

    @Test
    @DisplayName("CP-CPL-11: confirmarPlan rechaza con el número de lote un plan cuyo equipo fue dado de baja después de generarlo y no registra ningún lote")
    void confirmar_equipoDadoDeBaja() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        MolinoEntity molinoDeBaja = molino(10L, 100.0);
        molinoDeBaja.setEstado(Estado.BAJA);
        when(equipamientoRepository.findById(1L)).thenReturn(Optional.of(fermentador(1L, 1000.0)));
        when(equipamientoRepository.findById(10L)).thenReturn(Optional.of(molinoDeBaja));

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.CONCURRENTE, lote(600.0, 1L), lote(401.0, 2L))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Lote 1 del plan: El molino " + molinoDeBaja.getIdentificadorInterno() + " fue dado de baja, genere el plan nuevamente");
        verify(loteServicio, never()).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-12: confirmarPlan bloquea los equipos de todo el plan en el orden canónico (fermentadores, molinos, maceradores y ollas de hervor, cada grupo por ID ascendente) antes de validarlos y de registrar los lotes")
    void confirmar_bloqueaTodosLosEquiposEnOrdenCanonico() {
        mockearPlanificacionBloqueada(EstadoSolicitud.PENDIENTE, 1000.0);
        when(loteServicio.registrarLote(any())).thenReturn(LoteResponseDTO.builder().build());
        LotePlanProduccionFormDTO lote1 = LotePlanProduccionFormDTO.builder().volumenObjetivo(600.0)
                .idFermentador(2L).idMolino(20L).idMacerador(11L).idOllaHervor(12L).build();
        LotePlanProduccionFormDTO lote2 = LotePlanProduccionFormDTO.builder().volumenObjetivo(401.0)
                .idFermentador(1L).idMolino(10L).idMacerador(5L).idOllaHervor(6L).build();

        planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.CONCURRENTE, lote1, lote2));

        InOrder orden = inOrder(fermentadorRepository, molinoRepository, maceradorRepository, ollaHervorRepository, equipamientoRepository, loteServicio);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(fermentadorRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(molinoRepository).buscarPorIdParaCambiarEstadoOperativo(10L);
        orden.verify(molinoRepository).buscarPorIdParaCambiarEstadoOperativo(20L);
        orden.verify(maceradorRepository).buscarPorIdParaCambiarEstadoOperativo(5L);
        orden.verify(maceradorRepository).buscarPorIdParaCambiarEstadoOperativo(11L);
        orden.verify(ollaHervorRepository).buscarPorIdParaCambiarEstadoOperativo(6L);
        orden.verify(ollaHervorRepository).buscarPorIdParaCambiarEstadoOperativo(12L);
        orden.verify(equipamientoRepository).findById(2L);
        orden.verify(loteServicio, times(2)).registrarLote(any());
    }

    @Test
    @DisplayName("CP-CPL-09: confirmarPlan rechaza un plan sin lotes o con más lotes que el máximo (100) antes de consultar nada")
    void confirmar_cantidadDeLotes() {
        LotePlanProduccionFormDTO[] lotes = new LotePlanProduccionFormDTO[101];
        Arrays.fill(lotes, lote(10.0, 1L));

        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, PlanProduccionFormDTO.builder().modalidad(ModalidadPlanProduccion.SECUENCIAL).build()))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El plan debe tener al menos un lote");
        assertThatThrownBy(() -> planProduccionServicio.confirmarPlan(1L, plan(ModalidadPlanProduccion.SECUENCIAL, lotes)))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El plan no puede tener más de 100 lotes");
        verify(planificacionProduccionRepository, never()).buscarPorIdParaActualizar(any());
    }

    // ==================== buscarPlanificacionesConLitrosAPlanificar ====================

    @Test
    @DisplayName("CP-PPL-01: buscarPlanificacionesConLitrosAPlanificar informa solo las pendientes con litros a planificar mayores a 0, en el orden de la consulta, con su etiqueta, su fecha de inicio y sus litros a planificar")
    void conLitros_debeInformarSoloLasQueTienenLitrosAPlanificar() {
        PlanificacionProduccionEntity sinLotes = planificacionPendiente(3L, 400.0, HOY.plusDays(2));
        PlanificacionProduccionEntity parcial = planificacionPendiente(1L, 1000.0, HOY.plusDays(5));
        PlanificacionProduccionEntity cubierta = planificacionPendiente(2L, 600.0, HOY.plusDays(6));
        PlanificacionProduccionEntity excedida = planificacionPendiente(4L, 500.0, HOY.plusDays(7));
        when(planificacionProduccionRepository.buscarPendientesConVersionReceta()).thenReturn(List.of(sinLotes, parcial, cubierta, excedida));
        mockearComprometidos(3L, 0.0, 0.0);
        mockearComprometidos(1L, 200.0, 300.0);
        mockearComprometidos(2L, 0.0, 600.0);
        mockearComprometidos(4L, 300.0, 400.0);

        List<PlanificacionConLitrosAPlanificarResponseDTO> resultado = planProduccionServicio.buscarPlanificacionesConLitrosAPlanificar();

        assertThat(resultado).extracting(PlanificacionConLitrosAPlanificarResponseDTO::getId).containsExactly(3L, 1L);
        assertThat(resultado).extracting(PlanificacionConLitrosAPlanificarResponseDTO::getLitrosAPlanificar).containsExactly(400.0, 500.0);
        assertThat(resultado).extracting(PlanificacionConLitrosAPlanificarResponseDTO::getEtiqueta).containsExactly("Plan #3 - Pale Ale", "Plan #1 - Pale Ale");
        assertThat(resultado).extracting(PlanificacionConLitrosAPlanificarResponseDTO::getFechaInicioEstimada).containsExactly(HOY.plusDays(2), HOY.plusDays(5));
    }

    @Test
    @DisplayName("CP-PPL-02: los litros a planificar del aviso son los mismos que usa generarPlanes, con el mismo redondeo")
    void conLitros_debeCoincidirConLaGeneracion() {
        PlanificacionProduccionEntity planificacion = planificacionPendiente(1L, 1000.004, HOY);
        when(planificacionProduccionRepository.buscarPendientesConVersionReceta()).thenReturn(List.of(planificacion));
        lenient().when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        mockearComprometidos(1L, 333.333, 0.001);
        mockearEquipos(List.of(), List.of(), List.of(), List.of());

        List<PlanificacionConLitrosAPlanificarResponseDTO> resultado = planProduccionServicio.buscarPlanificacionesConLitrosAPlanificar();
        double litrosDeLaGeneracion = planProduccionServicio.generarPlanes(1L).getLitrosAPlanificar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLitrosAPlanificar()).isEqualTo(litrosDeLaGeneracion).isEqualTo(666.67);
    }

    @Test
    @DisplayName("CP-PPL-03: sin planificaciones pendientes buscarPlanificacionesConLitrosAPlanificar devuelve una lista vacía y no suma litros")
    void conLitros_sinPendientes() {
        when(planificacionProduccionRepository.buscarPendientesConVersionReceta()).thenReturn(List.of());

        assertThat(planProduccionServicio.buscarPlanificacionesConLitrosAPlanificar()).isEmpty();
        verify(envasadoLoteRepository, never()).sumarLitrosProducidosDePlanificacion(any());
        verify(loteRepository, never()).sumarVolumenObjetivoDeLotesVivosDePlanificacion(any());
    }

    // ==================== Helpers ====================

    private void assertNoDisponibles(PlanesProduccionResponseDTO planes, String motivo) {
        assertThat(planes.getSecuencial().isDisponible()).isFalse();
        assertThat(planes.getConcurrente().isDisponible()).isFalse();
        assertThat(planes.getSecuencial().getMotivoNoDisponible()).isEqualTo(motivo);
        assertThat(planes.getConcurrente().getMotivoNoDisponible()).isEqualTo(motivo);
        assertThat(planes.getSecuencial().getLotes()).isEmpty();
    }

    private PlanificacionProduccionEntity mockearPlanificacion(EstadoSolicitud estado, double cantidad) {
        PlanificacionProduccionEntity planificacion = planificacion(estado, cantidad);
        lenient().when(planificacionProduccionRepository.findById(1L)).thenReturn(Optional.of(planificacion));
        return planificacion;
    }

    private void mockearComprometidos(Long idPlanificacion, double producidos, double lotesVivos) {
        when(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(idPlanificacion)).thenReturn(producidos);
        when(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(idPlanificacion)).thenReturn(lotesVivos);
    }

    private static PlanificacionProduccionEntity planificacionPendiente(Long id, double cantidad, LocalDate fechaInicio) {
        PlanificacionProduccionEntity base = planificacion(EstadoSolicitud.PENDIENTE, cantidad);
        return PlanificacionProduccionEntity.builder()
                .id(id)
                .estado(EstadoSolicitud.PENDIENTE)
                .cantidadAProducir(cantidad)
                .fechaInicioEstimada(fechaInicio)
                .fechaFinalizacionEstimada(fechaInicio.plusDays(200))
                .versionReceta(base.getVersionReceta())
                .build();
    }

    private void mockearPlanificacionBloqueada(EstadoSolicitud estado, double cantidad) {
        when(planificacionProduccionRepository.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(planificacion(estado, cantidad)));
    }

    private void mockearEquipos(List<FermentadorEntity> fermentadores, List<MolinoEntity> molinos, List<MaceradorEntity> maceradores, List<OllaHervorEntity> ollas) {
        lenient().when(fermentadorRepository.buscarActivos()).thenReturn(fermentadores);
        lenient().when(molinoRepository.buscarActivos()).thenReturn(molinos);
        lenient().when(maceradorRepository.buscarActivos()).thenReturn(maceradores);
        lenient().when(ollaHervorRepository.buscarActivos()).thenReturn(ollas);
    }

    /** Equipos por ID para recalcular: el fermentador indicado, el molino 10, el macerador 11 y la olla 12. */
    private void mockearEquiposPorId(FermentadorEntity fermentador) {
        when(fermentadorRepository.findById(fermentador.getId())).thenReturn(Optional.of(fermentador));
        when(molinoRepository.findById(10L)).thenReturn(Optional.of(molino(10L, 100.0)));
        when(maceradorRepository.findById(11L)).thenReturn(Optional.of(maceradorGrande(11L, 75.0)));
        when(ollaHervorRepository.findById(12L)).thenReturn(Optional.of(ollaGrande(12L, 1.0, 5.0)));
    }

    private static PlanificacionProduccionEntity planificacion(EstadoSolicitud estado, double cantidad) {
        VersionRecetaEntity version = VersionRecetaEntity.builder()
                .id(70L)
                .nombre("Pale Ale")
                .relacionDeEmpaste(3.0)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(14)
                .duracionMaduracion(7)
                .build();
        return PlanificacionProduccionEntity.builder()
                .id(1L)
                .estado(estado)
                .cantidadAProducir(cantidad)
                .fechaInicioEstimada(HOY)
                .fechaFinalizacionEstimada(HOY.plusDays(200))
                .versionReceta(version)
                .build();
    }

    private static PlanProduccionFormDTO plan(ModalidadPlanProduccion modalidad, LotePlanProduccionFormDTO... lotes) {
        return PlanProduccionFormDTO.builder().modalidad(modalidad).lotes(new ArrayList<>(Arrays.asList(lotes))).build();
    }

    private static LotePlanProduccionFormDTO lote(double volumen, Long idFermentador) {
        return LotePlanProduccionFormDTO.builder()
                .volumenObjetivo(volumen)
                .idFermentador(idFermentador)
                .idMolino(10L)
                .idMacerador(11L)
                .idOllaHervor(12L)
                .build();
    }

    private static FermentadorEntity fermentador(Long id, double capacidadUtil) {
        return FermentadorEntity.builder()
                .id(id).identificadorInterno("FER-" + id).estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(capacidadUtil + 5.0).capacidadUtil(capacidadUtil)
                .build();
    }

    private static MolinoEntity molino(Long id, double rendimiento) {
        return MolinoEntity.builder()
                .id(id).identificadorInterno("MOL-" + id).estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).rendimientoMolienda(rendimiento)
                .build();
    }

    private static MaceradorEntity maceradorGrande(Long id, double eficiencia) {
        return macerador(id, 100_000.0, 1.0, eficiencia);
    }

    private static MaceradorEntity macerador(Long id, double capacidadUtil, double espacioMuerto, double eficiencia) {
        return MaceradorEntity.builder()
                .id(id).identificadorInterno("MAC-" + id).estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(capacidadUtil + 5.0).capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto).eficienciaMaceracion(eficiencia)
                .build();
    }

    private static OllaHervorEntity ollaGrande(Long id, double perdidaPorTrub, double evaporacion) {
        return olla(id, 100_000.0, perdidaPorTrub, evaporacion);
    }

    private static OllaHervorEntity olla(Long id, double capacidadUtil, double perdidaPorTrub, double evaporacion) {
        return OllaHervorEntity.builder()
                .id(id).identificadorInterno("OLL-" + id).estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(capacidadUtil + 5.0).capacidadUtil(capacidadUtil)
                .perdidaPorTrub(perdidaPorTrub).evaporacion(evaporacion)
                .build();
    }
}
