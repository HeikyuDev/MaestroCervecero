package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.Escenario;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.FermentadorLibre;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link GeneradorPlanes}, el algoritmo puro de la generación de planes: no usa repositorios ni
 * mocks. La masa de malta se calcula en 0,2 kg por litro y los equipos del lado caliente no limitan el volumen.
 */
class GeneradorPlanesTest {

    private static final LocalDate HOY = LocalDate.now();

    @Test
    @DisplayName("CP-GEN-01: invariante: el plan secuencial siempre está disponible salvo por el máximo de lotes, y todo plan disponible cumple las reglas del volumen total de la confirmación (también con excedente 0)")
    void todoPlanDisponibleCumpleLasReglasDeLaConfirmacion() {
        double[] litrosPosibles = {0.5, 1, 2, 7.5, 99, 100, 101, 250.25, 999, 1000, 1001, 2000, 3000.04, 4567.8};
        double[][] capacidadesPosibles = {{100}, {100, 100}, {500, 300, 223}, {1000, 37}, {5, 5, 5}, {223.5, 1000, 400}, {1, 1}, {1.5}, {10.25, 7.75}};
        double[] excedentesPosibles = {0, 0.25, 1, 300};
        int disponibles = 0;

        for (double litros : litrosPosibles) {
            for (double[] capacidades : capacidadesPosibles) {
                for (double excedente : excedentesPosibles) {
                    GeneradorPlanes generador = new GeneradorPlanes(escenario(litros, excedente, capacidades));
                    List<PlanProduccionResponseDTO> planes = new ArrayList<>();
                    for (CriterioSeleccionPlanSecuencial criterio : CriterioSeleccionPlanSecuencial.values()) {
                        planes.add(generador.generarSecuencial(criterio));
                    }
                    for (CriterioSeleccionPlanConcurrente criterio : CriterioSeleccionPlanConcurrente.values()) {
                        planes.add(generador.generarConcurrente(criterio));
                    }
                    for (PlanProduccionResponseDTO plan : planes) {
                        if (!plan.isDisponible()) {
                            assertThat(plan.getMotivoNoDisponible()).isNotBlank();
                            // Con equipos activos, el secuencial solo deja de estar disponible por el máximo de lotes
                            if (plan.getModalidad() == ModalidadPlanProduccion.SECUENCIAL) {
                                assertThat(plan.getMotivoNoDisponible()).as("L=" + litros + " " + java.util.Arrays.toString(capacidades) + " excedente=" + excedente)
                                        .startsWith("El plan secuencial necesita").endsWith("lotes y el máximo es de 100");
                            }
                            continue;
                        }
                        disponibles++;
                        String escenario = "L=" + litros + " capacidades=" + java.util.Arrays.toString(capacidades) + " excedente=" + excedente
                                + " " + plan.getModalidad() + " " + plan.getCriterio();
                        List<Double> volumenes = plan.getLotes().stream().map(LotePlanProduccionResponseDTO::getVolumenObjetivo).toList();
                        assertThat(GeneradorPlanes.problemasDelVolumenTotal(volumenes, litros, excedente)).as(escenario).isEmpty();
                        assertThat(BigDecimal.valueOf(plan.getVolumenTotal())).as(escenario).isGreaterThanOrEqualTo(BigDecimal.valueOf(litros));
                        assertThat(plan.getLotes()).as(escenario)
                                .allMatch(l -> l.getVolumenObjetivo() > 0 && l.getVolumenObjetivo() <= l.getCapacidadFermentador());
                        assertThat(BigDecimal.valueOf(plan.getExcedente())).as(escenario).isLessThan(BigDecimal.ONE);
                        // El único aviso posible de un plan generado es el de finalización tardía, que no impide confirmarlo
                        assertThat(plan.getAvisos()).as(escenario).allMatch(aviso -> aviso.startsWith("El plan finaliza el "));
                    }
                }
            }
        }
        assertThat(disponibles).isPositive();
    }

    @Test
    @DisplayName("CP-GEN-02: lotesNecesarios usa el cociente exacto: 3.000,04 / 1.000 da 4 y 3.000 / 1.000 da 3")
    void lotesNecesariosConCocienteExacto() {
        assertThat(GeneradorPlanes.lotesNecesarios(3000.04, 1000.0)).isEqualTo(4);
        assertThat(GeneradorPlanes.lotesNecesarios(3000.0, 1000.0)).isEqualTo(3);
        assertThat(GeneradorPlanes.lotesNecesarios(0.5, 1000.0)).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-GEN-03: repartirLitros usa el método del resto: piso de la cuota proporcional más 1 L por mayor parte fraccionaria, con total ceil(L)")
    void repartirLitrosPorMetodoDelResto() {
        assertThat(GeneradorPlanes.repartirLitros(List.of(500.0, 300.0, 223.0, 500.0, 300.0, 223.0), 2000.0, 300.0))
                .containsExactly(489.0, 293.0, 218.0, 489.0, 293.0, 218.0);
        // Capacidades iguales: floor(total / N) más 1 L en los primeros (total mod N) lotes
        assertThat(GeneradorPlanes.repartirLitros(List.of(1000.0, 1000.0, 1000.0, 1000.0), 3000.04, 300.0)).containsExactly(751.0, 750.0, 750.0, 750.0);
        assertThat(GeneradorPlanes.repartirLitros(List.of(300.0, 300.0, 300.0), 700.0, 300.0)).containsExactly(234.0, 233.0, 233.0);
        // Una capacidad no entera completa los litros con su fracción
        assertThat(GeneradorPlanes.repartirLitros(List.of(223.5, 223.5), 447.0, 300.0)).containsExactly(223.5, 223.5);
    }

    @Test
    @DisplayName("CP-GEN-07: con un excedente máximo menor al redondeo, el total entero es floor(L) y el último lote lleva la fracción")
    void repartirLitrosConExcedenteCero() {
        assertThat(GeneradorPlanes.repartirLitros(List.of(500.0, 500.0, 500.0), 1000.5, 0.0)).containsExactly(334.0, 333.0, 333.5);
        assertThat(GeneradorPlanes.repartirLitros(List.of(1.0, 1.0, 1.0), 2.5, 0.0)).containsExactly(1.0, 1.0, 0.5);
        assertThat(GeneradorPlanes.repartirLitros(List.of(100.0), 0.5, 0.0)).containsExactly(0.5);
        // Con un excedente de 0,5 L ya entra el total ceil(L)
        assertThat(GeneradorPlanes.repartirLitros(List.of(500.0, 500.0, 500.0), 1000.5, 0.5)).containsExactly(334.0, 334.0, 333.0);
    }

    @Test
    @DisplayName("CP-GEN-08: repartirLitros rechaza un reparto en el que algún lote quedaría vacío")
    void repartirLitrosConLoteVacio() {
        assertThatThrownBy(() -> GeneradorPlanes.repartirLitros(List.of(100.0, 100.0), 1.0, 300.0))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los 1 L que faltan planificar no alcanzan para repartirlos en 2 lotes");
    }

    @Test
    @DisplayName("CP-GEN-09: el aviso de finalización tardía aparece solo si el plan termina después de la fecha de la planificación")
    void avisoDeFinalizacionTardia() {
        LocalDate fin = LocalDate.of(2027, 8, 10);

        assertThat(GeneradorPlanes.avisoDeFinalizacionTardia(fin, LocalDate.of(2027, 8, 1)))
                .contains("El plan finaliza el 10/08/2027, después de la fecha de finalización estimada de la planificación (01/08/2027)");
        assertThat(GeneradorPlanes.avisoDeFinalizacionTardia(fin, fin)).isEmpty();
        assertThat(GeneradorPlanes.avisoDeFinalizacionTardia(null, fin)).isEmpty();
    }

    @Test
    @DisplayName("CP-GEN-04: repartirLitros rechaza lugares cuya capacidad total no alcanza los litros")
    void repartirLitrosSinCapacidadSuficiente() {
        assertThatThrownBy(() -> GeneradorPlanes.repartirLitros(List.of(100.0, 100.0), 250.0, 300.0))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad de los fermentadores del plan no alcanza para los 250 L que faltan planificar");
    }

    @Test
    @DisplayName("CP-GEN-05: un fermentador con capacidad efectiva menor al volumen mínimo de un lote (1 L) se descarta")
    void fermentadorMenorAlVolumenMinimoSeDescarta() {
        GeneradorPlanes generador = new GeneradorPlanes(escenario(10.0, 300.0, new double[]{0.5}));

        assertThat(generador.motivoNoDisponible()).isEqualTo(GeneradorPlanes.MENSAJE_SIN_CAPACIDAD);
        assertThat(generador.generarSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD).isDisponible()).isFalse();
    }

    @Test
    @DisplayName("CP-GEN-06: encadenarFechas encola los lotes de un mismo fermentador en el orden dado, detrás de su último lote vivo")
    void encadenarFechasPorFermentador() {
        FermentadorEntity fermentador = fermentador(1L, 500.0);
        GeneradorPlanes.LoteDefinido lote = new GeneradorPlanes.LoteDefinido(fermentador, 500.0, molino(), macerador(), olla(), 100.0);

        List<GeneradorPlanes.LotePlanificado> planificados = GeneradorPlanes.encadenarFechas(List.of(lote, lote),
                Map.of(1L, HOY.plusDays(10)), HOY, HOY, receta(), 100.0);

        assertThat(planificados.get(0).inicio()).isEqualTo(HOY.plusDays(12));
        assertThat(planificados.get(0).fin()).isEqualTo(HOY.plusDays(34));
        assertThat(planificados.get(1).inicio()).isEqualTo(HOY.plusDays(36));
    }

    @Test
    @DisplayName("CP-GEN-10: del lado caliente se prefieren los equipos compatibles que no están en mantenimiento, aunque uno en mantenimiento tenga mejores propiedades")
    void ladoCalientePrefiereEquiposFueraDeMantenimiento() {
        MolinoEntity molinoEnMantenimiento = molino(20L, 200.0, EstadoOperativo.EN_MANTENIMIENTO);
        MolinoEntity molinoDisponible = molino(21L, 100.0, EstadoOperativo.DISPONIBLE);
        MaceradorEntity maceradorEnMantenimiento = macerador(30L, 90.0, EstadoOperativo.EN_MANTENIMIENTO);
        MaceradorEntity maceradorEnUso = macerador(31L, 70.0, EstadoOperativo.EN_USO);
        OllaHervorEntity ollaEnMantenimiento = olla(40L, 0.5, EstadoOperativo.EN_MANTENIMIENTO);
        OllaHervorEntity ollaDisponible = olla(41L, 1.0, EstadoOperativo.DISPONIBLE);
        GeneradorPlanes generador = new GeneradorPlanes(escenario(500.0, List.of(fermentador(1L, 500.0)),
                List.of(molinoEnMantenimiento, molinoDisponible), List.of(maceradorEnMantenimiento, maceradorEnUso), List.of(ollaEnMantenimiento, ollaDisponible)));

        PlanProduccionResponseDTO plan = generador.generarSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD);

        LotePlanProduccionResponseDTO lote = plan.getLotes().get(0);
        assertThat(lote.getIdMolino()).isEqualTo(21L);
        assertThat(lote.getIdMacerador()).isEqualTo(31L);
        assertThat(lote.getIdOllaHervor()).isEqualTo(41L);
        assertThat(plan.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("CP-GEN-11: si el único equipo compatible está en mantenimiento se usa igual y el plan lo avisa una sola vez, sin dejar de estar disponible")
    void ladoCalienteEnMantenimientoComoUltimoRecurso() {
        GeneradorPlanes generador = new GeneradorPlanes(escenario(1000.0, List.of(fermentador(1L, 500.0)),
                List.of(molino(20L, 100.0, EstadoOperativo.EN_MANTENIMIENTO)), List.of(macerador()), List.of(olla())));

        PlanProduccionResponseDTO plan = generador.generarSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD);

        assertThat(plan.isDisponible()).isTrue();
        assertThat(plan.getLotes()).hasSize(2).allMatch(l -> l.getIdMolino() == 20L);
        assertThat(plan.getAvisos()).containsExactly("El equipo MOL-20 se encuentra en mantenimiento");
    }

    @Test
    @DisplayName("CP-GEN-12: los fermentadores en mantenimiento se descartan si hay otro utilizable; si todos lo están, se usan y el plan lo avisa")
    void fermentadoresEnMantenimiento() {
        FermentadorEntity grandeEnMantenimiento = fermentador(1L, 1000.0, EstadoOperativo.EN_MANTENIMIENTO);
        FermentadorEntity chicoDisponible = fermentador(2L, 300.0, EstadoOperativo.DISPONIBLE);
        GeneradorPlanes conAlternativa = new GeneradorPlanes(escenario(1000.0, List.of(grandeEnMantenimiento, chicoDisponible),
                List.of(molino()), List.of(macerador()), List.of(olla())));
        GeneradorPlanes todosEnMantenimiento = new GeneradorPlanes(escenario(1000.0, List.of(grandeEnMantenimiento),
                List.of(molino()), List.of(macerador()), List.of(olla())));

        PlanProduccionResponseDTO secuencial = conAlternativa.generarSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD);
        PlanProduccionResponseDTO concurrente = conAlternativa.generarConcurrente(CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES);
        PlanProduccionResponseDTO soloEnMantenimiento = todosEnMantenimiento.generarSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD);

        assertThat(secuencial.getLotes()).hasSize(4).allMatch(l -> l.getIdFermentador() == 2L);
        assertThat(secuencial.getAvisos()).isEmpty();
        assertThat(concurrente.isDisponible()).isFalse();
        assertThat(concurrente.getMotivoNoDisponible()).isEqualTo(GeneradorPlanes.MENSAJE_CONCURRENTE_SIN_FERMENTADORES);
        assertThat(soloEnMantenimiento.isDisponible()).isTrue();
        assertThat(soloEnMantenimiento.getLotes()).allMatch(l -> l.getIdFermentador() == 1L);
        assertThat(soloEnMantenimiento.getAvisos()).containsExactly("El equipo FER-1 se encuentra en mantenimiento");
    }

    // ==================== Helpers ====================

    private static Escenario escenario(double litros, double excedente, double[] capacidades) {
        List<FermentadorLibre> fermentadores = new ArrayList<>();
        for (int i = 0; i < capacidades.length; i++) {
            fermentadores.add(new FermentadorLibre(fermentador((long) i + 1, capacidades[i]), null));
        }
        return new Escenario(receta(), HOY, HOY.plusYears(5), HOY, 100.0, litros, excedente, fermentadores,
                List.of(molino()), List.of(macerador()), List.of(olla()), (volumen, macerador) -> 0.2 * volumen);
    }

    private static Escenario escenario(double litros, List<FermentadorEntity> fermentadores, List<MolinoEntity> molinos,
                                       List<MaceradorEntity> maceradores, List<OllaHervorEntity> ollas) {
        return new Escenario(receta(), HOY, HOY.plusYears(5), HOY, 100.0, litros, 300.0,
                fermentadores.stream().map(f -> new FermentadorLibre(f, null)).toList(),
                molinos, maceradores, ollas, (volumen, macerador) -> 0.2 * volumen);
    }

    private static FermentadorEntity fermentador(Long id, double capacidadUtil, EstadoOperativo estadoOperativo) {
        FermentadorEntity fermentador = fermentador(id, capacidadUtil);
        fermentador.setEstadoOperativo(estadoOperativo);
        return fermentador;
    }

    private static MolinoEntity molino(Long id, double rendimiento, EstadoOperativo estadoOperativo) {
        return MolinoEntity.builder().id(id).identificadorInterno("MOL-" + id).estado(Estado.ACTIVO).estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100).rendimientoMolienda(rendimiento).build();
    }

    private static MaceradorEntity macerador(Long id, double eficiencia, EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder().id(id).identificadorInterno("MAC-" + id).estado(Estado.ACTIVO).estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(100_005.0).capacidadUtil(100_000.0).espacioMuerto(1.0).eficienciaMaceracion(eficiencia)
                .build();
    }

    private static OllaHervorEntity olla(Long id, double perdidaPorTrub, EstadoOperativo estadoOperativo) {
        return OllaHervorEntity.builder().id(id).identificadorInterno("OLL-" + id).estado(Estado.ACTIVO).estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(100_005.0).capacidadUtil(100_000.0).perdidaPorTrub(perdidaPorTrub).evaporacion(5.0)
                .build();
    }

    private static VersionRecetaEntity receta() {
        return VersionRecetaEntity.builder().id(70L).nombre("Pale Ale").relacionDeEmpaste(3.0)
                .duracionMaceracion(60).duracionHervido(60).duracionFermentacion(14).duracionMaduracion(7).build();
    }

    private static FermentadorEntity fermentador(Long id, double capacidadUtil) {
        return FermentadorEntity.builder()
                .id(id).identificadorInterno("FER-" + id).estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(capacidadUtil + 5.0).capacidadUtil(capacidadUtil)
                .build();
    }

    private static MolinoEntity molino() {
        return MolinoEntity.builder().id(10L).identificadorInterno("MOL-10").estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).rendimientoMolienda(100.0).build();
    }

    private static MaceradorEntity macerador() {
        return MaceradorEntity.builder().id(11L).identificadorInterno("MAC-11").estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(100_005.0).capacidadUtil(100_000.0).espacioMuerto(1.0).eficienciaMaceracion(75.0)
                .build();
    }

    private static OllaHervorEntity olla() {
        return OllaHervorEntity.builder().id(12L).identificadorInterno("OLL-12").estado(Estado.ACTIVO).estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).capacidadTotal(100_005.0).capacidadUtil(100_000.0).perdidaPorTrub(1.0).evaporacion(5.0)
                .build();
    }
}
