package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.lote.EstimacionLote;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanProduccion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoublePredicate;

/**
 * Algoritmo puro (sin acceso a datos) de la generación automática de planes de producción: capacidad efectiva de los
 * fermentadores, plan secuencial, plan concurrente (modelo de olas y asignación por liberación), reparto de los litros
 * y elección de los equipos del lado caliente de cada lote.
 * <p>
 * Recibe todo lo que necesita en un {@link Escenario} que arma {@link PlanProduccionServicioImpl}. Las fechas y los
 * límites de capacidad salen de {@link EstimacionLote}, la misma fuente que usa el registro de un lote. Nunca ofrece
 * como disponible un plan que la confirmación rechazaría por su volumen total.
 * </p>
 */
final class GeneradorPlanes {

    /** Cantidad mínima de fermentadores utilizables que exige un plan concurrente. */
    static final int MINIMO_FERMENTADORES_CONCURRENTE = 2;

    /**
     * Capacidad efectiva mínima de un fermentador para recibir lotes de un plan, en litros: con al menos 1 L por lugar,
     * un plan secuencial nunca tiene más lotes que litros enteros a repartir.
     */
    static final double VOLUMEN_MINIMO_LOTE_LITROS = 1.0;

    static final String MENSAJE_SIN_FERMENTADORES = "No hay fermentadores activos para generar el plan";
    static final String MENSAJE_SIN_MOLINOS = "No hay molinos activos para generar el plan";
    static final String MENSAJE_SIN_MACERADORES = "No hay maceradores activos para generar el plan";
    static final String MENSAJE_SIN_OLLAS = "No hay ollas de hervor activas para generar el plan";
    static final String MENSAJE_SIN_CAPACIDAD = "Ningún fermentador activo puede recibir un lote: los maceradores y las ollas de hervor activos no admiten un lote de al menos 1 L con la receta de la planificación";
    static final String MENSAJE_CONCURRENTE_SIN_FERMENTADORES = "El plan concurrente necesita al menos dos fermentadores activos que puedan recibir un lote";

    /** Calcula la masa de malta escalada de un lote del volumen indicado con el macerador indicado. */
    @FunctionalInterface
    interface CalculoMasaMalta {
        double calcular(double volumen, MaceradorEntity macerador);
    }

    /**
     * Datos de la generación: la receta, las fechas de inicio y de finalización estimadas de la planificación, hoy, la velocidad de envasado, los litros a planificar,
     * el excedente máximo, los fermentadores activos con la fecha de fin de su último lote vivo, los equipos activos del
     * lado caliente y el cálculo de la masa de malta.
     */
    record Escenario(VersionRecetaEntity versionReceta, LocalDate inicioPlanificacion, LocalDate finPlanificacion, LocalDate hoy, double velocidadEnvasado,
                     double litrosAPlanificar, double excedenteMaximo, List<FermentadorLibre> fermentadores,
                     List<MolinoEntity> molinos, List<MaceradorEntity> maceradores, List<OllaHervorEntity> ollas,
                     CalculoMasaMalta masaMalta) {
    }

    /** Fermentador activo y la fecha de finalización estimada de su último lote vivo ({@code null} si no tiene). */
    record FermentadorLibre(FermentadorEntity fermentador, LocalDate finUltimoLoteVivo) {
    }

    /** Lote con su volumen y sus equipos, todavía sin fechas. */
    record LoteDefinido(FermentadorEntity fermentador, double volumen, MolinoEntity molino, MaceradorEntity macerador,
                        OllaHervorEntity ollaHervor, double masaMalta) {
    }

    /** Lote con sus fechas estimadas en el cronograma de su fermentador. */
    record LotePlanificado(LoteDefinido lote, LocalDate inicio, LocalDate fin) {
    }

    /**
     * Fermentador utilizable: su capacidad efectiva, la fecha de fin de su último lote vivo y la fecha en que podría
     * arrancar su próximo lote.
     */
    private record Candidato(FermentadorEntity fermentador, double capacidad, LocalDate finUltimoLoteVivo, LocalDate proximoInicio) {
        String identificador() {
            return identificadorDe(fermentador);
        }
    }

    /** Combinación del modelo de olas: los k fermentadores de mayor capacidad, sus olas y los lotes resultantes. */
    private record OpcionDeOlas(int cantidadLotes, int olas, LocalDate fin, List<LotePlanificado> lotes) {
    }

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Comparator<OpcionDeOlas> MEJOR_OPCION_DE_OLAS = Comparator.comparingInt(OpcionDeOlas::cantidadLotes)
            .thenComparingInt(OpcionDeOlas::olas)
            .thenComparing(OpcionDeOlas::fin);

    private final Escenario escenario;
    private final List<Candidato> candidatos;
    private final String motivoNoDisponible;

    GeneradorPlanes(Escenario escenario) {
        this.escenario = escenario;
        String motivo = motivoPorEquiposFaltantes(escenario);
        this.candidatos = motivo == null ? calcularCandidatos() : List.of();
        this.motivoNoDisponible = motivo == null && candidatos.isEmpty() ? MENSAJE_SIN_CAPACIDAD : motivo;
    }

    /**
     * Motivo por el que no se puede generar ningún plan (faltan equipos activos o ningún fermentador puede recibir un
     * lote), o {@code null} si se pueden generar.
     */
    String motivoNoDisponible() {
        return motivoNoDisponible;
    }

    // ==================== Plan secuencial ====================

    /**
     * Genera el plan secuencial: un único fermentador, elegido por el criterio, con {@code ceil(litros / capacidad)} lotes.
     */
    PlanProduccionResponseDTO generarSecuencial(CriterioSeleccionPlanSecuencial criterio) {
        if (motivoNoDisponible != null) {
            return noDisponible(ModalidadPlanProduccion.SECUENCIAL, criterio.getEtiqueta(), motivoNoDisponible, escenario.litrosAPlanificar());
        }
        Comparator<Candidato> porCapacidadDescendente = Comparator.comparingDouble(Candidato::capacidad).reversed();
        Comparator<Candidato> porLiberacion = Comparator.comparing(Candidato::proximoInicio);
        Comparator<Candidato> orden = criterio == CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD
                ? porCapacidadDescendente.thenComparing(porLiberacion)
                : porLiberacion.thenComparing(porCapacidadDescendente);
        Candidato elegido = candidatos.stream().min(orden.thenComparing(Candidato::identificador)).orElseThrow();

        int cantidadLotes = lotesNecesarios(escenario.litrosAPlanificar(), elegido.capacidad());
        if (cantidadLotes > LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES) {
            return noDisponible(ModalidadPlanProduccion.SECUENCIAL, criterio.getEtiqueta(),
                    mensajeMaximoDeLotes("secuencial", cantidadLotes), escenario.litrosAPlanificar());
        }
        return planValidado(ModalidadPlanProduccion.SECUENCIAL, criterio.getEtiqueta(), Collections.nCopies(cantidadLotes, elegido));
    }

    // ==================== Plan concurrente ====================

    /**
     * Genera el plan concurrente con el criterio indicado. Exige al menos {@link #MINIMO_FERMENTADORES_CONCURRENTE}
     * fermentadores utilizables.
     */
    PlanProduccionResponseDTO generarConcurrente(CriterioSeleccionPlanConcurrente criterio) {
        String etiqueta = criterio.getEtiqueta();
        if (motivoNoDisponible != null) {
            return noDisponible(ModalidadPlanProduccion.CONCURRENTE, etiqueta, motivoNoDisponible, escenario.litrosAPlanificar());
        }
        if (candidatos.size() < MINIMO_FERMENTADORES_CONCURRENTE) {
            return noDisponible(ModalidadPlanProduccion.CONCURRENTE, etiqueta, MENSAJE_CONCURRENTE_SIN_FERMENTADORES, escenario.litrosAPlanificar());
        }
        if (criterio == CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA) {
            try {
                return planValidado(ModalidadPlanProduccion.CONCURRENTE, etiqueta, asignarPorLiberacion());
            } catch (ReglaNegocioException e) {
                return noDisponible(ModalidadPlanProduccion.CONCURRENTE, etiqueta, e.getMessage(), escenario.litrosAPlanificar());
            }
        }
        return generarMenorCantidadDeLotes(etiqueta);
    }

    /**
     * Modelo de olas: para k = 2..n toma los k fermentadores de mayor capacidad, calcula las olas necesarias
     * {@code w = ceil(litros / suma de capacidades)} y los lotes {@code k · w} (cada fermentador recibe w lotes encolados
     * sobre sí mismo). Descarta las combinaciones que superan el máximo de lotes, cuyos equipos no se pueden asignar o
     * cuyo volumen total la confirmación rechazaría, y entre las demás elige la menor cantidad de lotes; a igualdad, la
     * menor cantidad de olas y luego el fin de plan más temprano.
     */
    private PlanProduccionResponseDTO generarMenorCantidadDeLotes(String etiqueta) {
        List<Candidato> porCapacidad = candidatos.stream()
                .sorted(Comparator.comparingDouble(Candidato::capacidad).reversed()
                        .thenComparing(Candidato::proximoInicio)
                        .thenComparing(Candidato::identificador))
                .toList();

        List<OpcionDeOlas> opciones = new ArrayList<>();
        String ultimoRechazo = null;
        int lotesMinimos = Integer.MAX_VALUE;
        double sumaCapacidades = 0.0;
        for (int k = 1; k <= porCapacidad.size(); k++) {
            sumaCapacidades += porCapacidad.get(k - 1).capacidad();
            if (k < MINIMO_FERMENTADORES_CONCURRENTE) {
                continue;
            }
            int olas = lotesNecesarios(escenario.litrosAPlanificar(), sumaCapacidades);
            long cantidadLotes = (long) k * olas;
            lotesMinimos = (int) Math.min(lotesMinimos, Math.min(cantidadLotes, Integer.MAX_VALUE));
            if (cantidadLotes > LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES) {
                continue;
            }

            List<Candidato> asignaciones = new ArrayList<>();
            for (int ola = 0; ola < olas; ola++) {
                asignaciones.addAll(porCapacidad.subList(0, k));
            }
            try {
                List<LotePlanificado> lotes = planificarLotes(asignaciones);
                List<String> problemas = problemasDelVolumenTotal(volumenesDe(lotes), escenario.litrosAPlanificar(), escenario.excedenteMaximo());
                if (!problemas.isEmpty()) {
                    ultimoRechazo = problemas.get(0);
                    continue;
                }
                opciones.add(new OpcionDeOlas((int) cantidadLotes, olas, fechaFinDe(lotes), lotes));
            } catch (ReglaNegocioException e) {
                ultimoRechazo = e.getMessage();
            }
        }

        Optional<OpcionDeOlas> mejor = opciones.stream().min(MEJOR_OPCION_DE_OLAS);
        if (mejor.isPresent()) {
            return aPlanDisponible(ModalidadPlanProduccion.CONCURRENTE, etiqueta, mejor.get().lotes());
        }
        String motivo = ultimoRechazo != null
                ? "Ninguna combinación de fermentadores en paralelo cubre los " + ValidacionesNumericas.formatearNumero(escenario.litrosAPlanificar())
                + " L que faltan planificar con un volumen total válido (" + ultimoRechazo + ")"
                : mensajeMaximoDeLotes("concurrente", lotesMinimos);
        return noDisponible(ModalidadPlanProduccion.CONCURRENTE, etiqueta, motivo, escenario.litrosAPlanificar());
    }

    /**
     * Asignación por liberación: cada lote va al fermentador que se libera antes, contando los lotes del plan ya
     * asignados a él (a igualdad, el de mayor capacidad), con un volumen igual a su capacidad, hasta que la capacidad
     * acumulada cubra los litros a planificar.
     *
     * @throws ReglaNegocioException Si el plan supera el máximo de lotes o no se pueden asignar sus equipos.
     */
    private List<Candidato> asignarPorLiberacion() {
        Map<Long, LocalDate> proximoInicio = new HashMap<>();
        candidatos.forEach(c -> proximoInicio.put(c.fermentador().getId(), c.proximoInicio()));
        Comparator<Candidato> orden = Comparator.comparing((Candidato c) -> proximoInicio.get(c.fermentador().getId()))
                .thenComparing(Comparator.comparingDouble(Candidato::capacidad).reversed())
                .thenComparing(Candidato::identificador);

        BigDecimal litros = BigDecimal.valueOf(escenario.litrosAPlanificar());
        BigDecimal capacidadAcumulada = BigDecimal.ZERO;
        List<Candidato> asignaciones = new ArrayList<>();
        while (capacidadAcumulada.compareTo(litros) < 0) {
            if (asignaciones.size() == LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES) {
                throw new ReglaNegocioException(mensajeMaximoDeLotes("concurrente", asignaciones.size() + 1));
            }
            Candidato elegido = candidatos.stream().min(orden).orElseThrow();
            asignaciones.add(elegido);
            capacidadAcumulada = capacidadAcumulada.add(BigDecimal.valueOf(elegido.capacidad()));

            // El fermentador queda ocupado por un lote de su capacidad: su próximo inicio es tras el fin de ese lote
            LoteDefinido loteACapacidad = definirLote(elegido.fermentador(), elegido.capacidad());
            LocalDate fin = calcularFin(proximoInicio.get(elegido.fermentador().getId()), loteACapacidad);
            proximoInicio.put(elegido.fermentador().getId(),
                    EstimacionLote.calcularFechaInicioEstimada(fin, escenario.inicioPlanificacion(), escenario.hoy()));
        }
        return asignaciones;
    }

    // ==================== Lotes de un plan ====================

    /**
     * Arma, valida y devuelve el plan de la secuencia de fermentadores asignados. Si la confirmación rechazaría su
     * volumen total, o no se pueden asignar sus equipos, el plan no está disponible.
     */
    private PlanProduccionResponseDTO planValidado(ModalidadPlanProduccion modalidad, String etiqueta, List<Candidato> asignaciones) {
        try {
            List<LotePlanificado> lotes = planificarLotes(asignaciones);
            List<String> problemas = problemasDelVolumenTotal(volumenesDe(lotes), escenario.litrosAPlanificar(), escenario.excedenteMaximo());
            if (!problemas.isEmpty()) {
                return noDisponible(modalidad, etiqueta, problemas.get(0), escenario.litrosAPlanificar());
            }
            return aPlanDisponible(modalidad, etiqueta, lotes);
        } catch (ReglaNegocioException e) {
            return noDisponible(modalidad, etiqueta, e.getMessage(), escenario.litrosAPlanificar());
        }
    }

    /**
     * Arma los lotes de un plan a partir de la secuencia de fermentadores asignados: reparte los litros en proporción a
     * la capacidad de cada lugar, elige los equipos del lado caliente de cada lote y encadena sus fechas. Devuelve los
     * lotes ordenados por fecha de inicio estimada (a igualdad, en el orden de asignación).
     *
     * @throws ReglaNegocioException Si los lugares no alcanzan para los litros o para algún lote no hay un macerador o
     *                               una olla de hervor activos compatibles.
     */
    private List<LotePlanificado> planificarLotes(List<Candidato> asignaciones) {
        List<Double> volumenes = repartirLitros(asignaciones.stream().map(Candidato::capacidad).toList(), escenario.litrosAPlanificar(),
                escenario.excedenteMaximo());
        List<LoteDefinido> lotes = new ArrayList<>();
        Map<Long, LocalDate> finUltimoLoteVivo = new HashMap<>();
        for (int i = 0; i < asignaciones.size(); i++) {
            Candidato asignado = asignaciones.get(i);
            lotes.add(definirLote(asignado.fermentador(), volumenes.get(i)));
            finUltimoLoteVivo.put(asignado.fermentador().getId(), asignado.finUltimoLoteVivo());
        }

        // Orden estable por fecha de inicio: los lotes de un mismo fermentador conservan su orden de asignación
        List<LotePlanificado> ordenados = new ArrayList<>(encadenarFechas(lotes, finUltimoLoteVivo, escenario.inicioPlanificacion(),
                escenario.hoy(), escenario.versionReceta(), escenario.velocidadEnvasado()));
        ordenados.sort(Comparator.comparing(LotePlanificado::inicio));
        return ordenados;
    }

    /**
     * Reparte los litros en proporción a la capacidad de cada lugar con el método del resto, en litros enteros:
     * <ol>
     *     <li>El total entero es {@code ceil(litros)}, cuyo excedente es siempre menor a 1 L. Si ese excedente supera el
     *     excedente máximo (por ejemplo, con un máximo de 0 L y litros con decimales), el total entero es
     *     {@code floor(litros)} y la fracción se completa en el paso 3.</li>
     *     <li>Cada lugar recibe el piso de su cuota {@code capacidad · total / suma de capacidades} y el resto se reparte
     *     de a 1 L entre los lugares de mayor parte fraccionaria (a igualdad, en orden). Con capacidades iguales es
     *     {@code floor(total / N)} por lote más 1 L en los primeros {@code total mod N} lotes. Nunca se supera la
     *     capacidad de un lugar.</li>
     *     <li>Lo que falte hasta los litros (la fracción, o lo que no entró por una capacidad no entera) se agrega desde el
     *     último lugar hacia el primero, en la capacidad libre de cada uno.</li>
     * </ol>
     * <p>
     * Así la suma cubre los litros con un excedente menor a 1 L (o ninguno), sin superar el excedente máximo, y ningún
     * lote sobra: sin el menor lote, los demás no cubren los litros.
     * </p>
     *
     * @return El volumen de cada lugar, en el mismo orden.
     * @throws ReglaNegocioException Si la capacidad de los lugares no alcanza para los litros, o si algún lugar queda sin
     *                               volumen (más lugares que litros).
     */
    static List<Double> repartirLitros(List<Double> capacidades, double litrosAPlanificar, double excedenteMaximo) {
        BigDecimal litros = BigDecimal.valueOf(litrosAPlanificar);
        BigDecimal sumaCapacidades = capacidades.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaCapacidades.compareTo(litros) < 0) {
            throw new ReglaNegocioException("La capacidad de los fermentadores del plan no alcanza para los "
                    + ValidacionesNumericas.formatearNumero(litrosAPlanificar) + " L que faltan planificar");
        }
        int cantidad = capacidades.size();

        // 1. Total entero: ceil(litros) si su excedente entra en el máximo; si no, floor(litros)
        BigDecimal techo = litros.setScale(0, RoundingMode.CEILING);
        BigDecimal totalEntero = techo.subtract(litros).compareTo(BigDecimal.valueOf(excedenteMaximo)) <= 0
                ? techo : litros.setScale(0, RoundingMode.FLOOR);

        // 2. Piso de la cuota proporcional de cada lugar y resto de a 1 L por mayor parte fraccionaria
        long[] enteros = new long[cantidad];
        BigDecimal[] fracciones = new BigDecimal[cantidad];
        long asignado = 0;
        for (int i = 0; i < cantidad; i++) {
            BigDecimal cuota = BigDecimal.valueOf(capacidades.get(i)).multiply(totalEntero).divide(sumaCapacidades, 10, RoundingMode.DOWN);
            BigDecimal piso = cuota.setScale(0, RoundingMode.FLOOR);
            enteros[i] = Math.min(piso.longValue(), topeEntero(capacidades.get(i)));
            fracciones[i] = cuota.subtract(piso);
            asignado += enteros[i];
        }
        List<Integer> porFraccion = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            porFraccion.add(i);
        }
        porFraccion.sort(Comparator.comparing((Integer i) -> fracciones[i]).reversed().thenComparing(i -> i));
        long resto = totalEntero.longValue() - asignado;
        boolean avanzo = true;
        while (resto > 0 && avanzo) {
            avanzo = false;
            for (int i : porFraccion) {
                if (resto > 0 && enteros[i] + 1 <= topeEntero(capacidades.get(i))) {
                    enteros[i]++;
                    resto--;
                    avanzo = true;
                }
            }
        }

        // 3. Completar lo que falte hasta los litros desde el último lugar, en la capacidad libre de cada uno
        List<BigDecimal> volumenes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (long entero : enteros) {
            volumenes.add(BigDecimal.valueOf(entero));
            total = total.add(BigDecimal.valueOf(entero));
        }
        for (int i = cantidad - 1; i >= 0 && total.compareTo(litros) < 0; i--) {
            BigDecimal libre = BigDecimal.valueOf(capacidades.get(i)).subtract(volumenes.get(i));
            BigDecimal agregado = libre.min(litros.subtract(total));
            volumenes.set(i, volumenes.get(i).add(agregado));
            total = total.add(agregado);
        }

        // 4. Ningún lote puede quedar vacío
        if (volumenes.stream().anyMatch(v -> v.signum() <= 0)) {
            throw new ReglaNegocioException("Los " + ValidacionesNumericas.formatearNumero(litrosAPlanificar)
                    + " L que faltan planificar no alcanzan para repartirlos en " + cantidad + " lotes");
        }
        return volumenes.stream().map(v -> Redondeo.redondear(v.doubleValue())).toList();
    }

    /** Mayor cantidad entera de litros que entra en una capacidad. */
    private static long topeEntero(double capacidad) {
        return (long) Math.floor(capacidad);
    }

    /**
     * Encadena las fechas de los lotes en el orden dado, en el cronograma de su fermentador: cada lote arranca tras el
     * último lote vivo de su fermentador o tras el lote anterior del plan en ese fermentador, con las fórmulas de
     * {@link EstimacionLote}.
     *
     * @param lotes Los lotes, en el orden en que se encolan.
     * @param finUltimoLoteVivo Fecha de fin del último lote vivo de cada fermentador (sin entrada o {@code null} si no tiene).
     */
    static List<LotePlanificado> encadenarFechas(List<LoteDefinido> lotes, Map<Long, LocalDate> finUltimoLoteVivo, LocalDate inicioPlanificacion,
                                                 LocalDate hoy, VersionRecetaEntity versionReceta, double velocidadEnvasado) {
        Map<Long, LocalDate> finPorFermentador = new HashMap<>(finUltimoLoteVivo);
        List<LotePlanificado> planificados = new ArrayList<>();
        for (LoteDefinido lote : lotes) {
            Long idFermentador = lote.fermentador().getId();
            LocalDate inicio = EstimacionLote.calcularFechaInicioEstimada(finPorFermentador.get(idFermentador), inicioPlanificacion, hoy);
            LocalDate fin = EstimacionLote.calcularFechaFinalizacionEstimada(inicio, lote.volumen(), lote.molino().getRendimientoMolienda(),
                    lote.masaMalta(), versionReceta, velocidadEnvasado);
            finPorFermentador.put(idFermentador, fin);
            planificados.add(new LotePlanificado(lote, inicio, fin));
        }
        return planificados;
    }

    /**
     * Define un lote del volumen indicado eligiendo sus equipos del lado caliente entre los activos compatibles. Se
     * prefieren los que no están en mantenimiento; uno en mantenimiento se elige solo si no hay otro compatible (el plan
     * lo avisa). Dentro de cada grupo, el
     * molino de mayor rendimiento, el macerador de mayor eficiencia de maceración que admite la mezcla y la olla de
     * hervor de menor pérdida por trub (a igualdad, la de mayor evaporación) que admite el volumen pre-hervor. El último
     * desempate es el identificador interno.
     *
     * @throws ReglaNegocioException Si ningún macerador u olla de hervor activos admite el volumen.
     */
    private LoteDefinido definirLote(FermentadorEntity fermentador, double volumen) {
        VersionRecetaEntity versionReceta = escenario.versionReceta();
        MolinoEntity molino = escenario.molinos().stream()
                .min(Comparator.comparing((MolinoEntity m) -> enMantenimiento(m))
                        .thenComparing(Comparator.comparingDouble(MolinoEntity::getRendimientoMolienda).reversed())
                        .thenComparing(GeneradorPlanes::identificadorDe))
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_SIN_MOLINOS));

        List<MaceradorEntity> maceradores = escenario.maceradores().stream()
                .sorted(Comparator.comparing((MaceradorEntity m) -> enMantenimiento(m))
                        .thenComparing(Comparator.comparingDouble(MaceradorEntity::getEficienciaMaceracion).reversed())
                        .thenComparing(GeneradorPlanes::identificadorDe))
                .toList();
        for (MaceradorEntity macerador : maceradores) {
            double masaMalta = escenario.masaMalta().calcular(volumen, macerador);
            if (EstimacionLote.maceradorAdmiteMasaMalta(masaMalta, macerador, versionReceta)) {
                OllaHervorEntity ollaHervor = escenario.ollas().stream()
                        .filter(olla -> EstimacionLote.ollaAdmiteVolumen(volumen, olla, versionReceta))
                        .min(Comparator.comparing((OllaHervorEntity o) -> enMantenimiento(o))
                                .thenComparing(Comparator.comparingDouble(OllaHervorEntity::getPerdidaPorTrub))
                                .thenComparing(Comparator.comparingDouble(OllaHervorEntity::getEvaporacion).reversed())
                                .thenComparing(GeneradorPlanes::identificadorDe))
                        .orElseThrow(() -> new ReglaNegocioException("Ninguna olla de hervor activa admite el volumen pre-hervor de un lote de "
                                + ValidacionesNumericas.formatearNumero(volumen) + " L"));
                return new LoteDefinido(fermentador, volumen, molino, macerador, ollaHervor, masaMalta);
            }
        }
        throw new ReglaNegocioException("Ningún macerador activo admite la mezcla de agua y grano de un lote de "
                + ValidacionesNumericas.formatearNumero(volumen) + " L");
    }

    /**
     * Calcula la capacidad efectiva de cada fermentador activo y su próxima fecha de inicio, y descarta los que no
     * pueden recibir un lote del volumen mínimo. La capacidad efectiva es el menor valor entre su capacidad útil y el
     * mayor volumen entero de lote que admiten algún macerador y alguna olla de hervor activos. Si hay al menos un
     * fermentador utilizable que no está en mantenimiento, se descartan los que sí lo están.
     */
    private List<Candidato> calcularCandidatos() {
        int limite = (int) Math.ceil(escenario.fermentadores().stream()
                .mapToDouble(f -> f.fermentador().getCapacidadUtil()).max().orElse(0.0));
        VersionRecetaEntity versionReceta = escenario.versionReceta();

        int maximoMaceradores = escenario.maceradores().stream()
                .mapToInt(macerador -> volumenMaximoAdmitido(volumen -> EstimacionLote.maceradorAdmiteMasaMalta(
                        escenario.masaMalta().calcular(volumen, macerador), macerador, versionReceta), limite))
                .max().orElse(0);
        int maximoOllas = escenario.ollas().stream()
                .mapToInt(olla -> volumenMaximoAdmitido(volumen -> EstimacionLote.ollaAdmiteVolumen(volumen, olla, versionReceta), limite))
                .max().orElse(0);
        int maximoLadoCaliente = Math.min(maximoMaceradores, maximoOllas);

        List<Candidato> resultado = new ArrayList<>();
        for (FermentadorLibre libre : escenario.fermentadores()) {
            double capacidad = Math.min(libre.fermentador().getCapacidadUtil(), maximoLadoCaliente);
            if (capacidad < VOLUMEN_MINIMO_LOTE_LITROS) {
                continue;
            }
            LocalDate proximoInicio = EstimacionLote.calcularFechaInicioEstimada(libre.finUltimoLoteVivo(), escenario.inicioPlanificacion(), escenario.hoy());
            resultado.add(new Candidato(libre.fermentador(), capacidad, libre.finUltimoLoteVivo(), proximoInicio));
        }

        // Los fermentadores en mantenimiento solo se usan si no hay otro utilizable (el plan lo avisa)
        List<Candidato> fueraDeMantenimiento = resultado.stream().filter(c -> !enMantenimiento(c.fermentador())).toList();
        return fueraDeMantenimiento.isEmpty() ? resultado : fueraDeMantenimiento;
    }

    /**
     * Busca, con una búsqueda binaria, el mayor volumen entero entre 0 y {@code limite} que el equipo admite. La
     * admisión es monótona: si admite un volumen, admite cualquiera menor.
     */
    private static int volumenMaximoAdmitido(DoublePredicate admite, int limite) {
        int minimo = 0;
        int maximo = limite;
        while (minimo < maximo) {
            int medio = minimo + (maximo - minimo + 1) / 2;
            if (admite.test(medio)) {
                minimo = medio;
            } else {
                maximo = medio - 1;
            }
        }
        return minimo;
    }

    // ==================== Reglas del volumen total ====================

    /**
     * Evalúa el volumen total de un plan contra los litros a planificar: debe cubrirlos, no superarlos en más del
     * excedente máximo y no tener un lote de más (sin el lote de menor volumen, los demás no deben cubrirlos). Las
     * mismas reglas valida la confirmación del plan.
     *
     * @return Los mensajes de cada regla que no se cumple, o una lista vacía si el plan las cumple todas.
     */
    static List<String> problemasDelVolumenTotal(List<Double> volumenes, double litrosAPlanificar, double excedenteMaximo) {
        BigDecimal total = volumenes.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal litros = BigDecimal.valueOf(Redondeo.redondear(litrosAPlanificar));
        BigDecimal excedente = total.subtract(litros);
        List<String> problemas = new ArrayList<>();

        if (total.compareTo(litros) < 0) {
            problemas.add("El volumen total del plan (" + formatear(total) + " L) no alcanza los "
                    + formatear(litros) + " L que faltan planificar");
            return problemas;
        }
        if (excedente.compareTo(BigDecimal.valueOf(excedenteMaximo)) > 0) {
            problemas.add("El volumen total del plan (" + formatear(total) + " L) supera en " + formatear(excedente)
                    + " L a los " + formatear(litros) + " L que faltan planificar y el excedente máximo configurado es de "
                    + ValidacionesNumericas.formatearNumero(excedenteMaximo) + " L");
        }
        BigDecimal menor = volumenes.stream().map(BigDecimal::valueOf).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        if (volumenes.size() > 1 && total.subtract(menor).compareTo(litros) >= 0) {
            problemas.add("El plan tiene un lote de más: sin el lote de menor volumen (" + formatear(menor)
                    + " L) los demás ya cubren los " + formatear(litros) + " L que faltan planificar");
        }
        return problemas;
    }

    // ==================== Armado de la respuesta ====================

    private PlanProduccionResponseDTO aPlanDisponible(ModalidadPlanProduccion modalidad, String criterio, List<LotePlanificado> lotes) {
        List<LotePlanProduccionResponseDTO> lotesDTO = new ArrayList<>();
        for (int i = 0; i < lotes.size(); i++) {
            lotesDTO.add(aLoteDTO(i + 1, lotes.get(i)));
        }
        List<String> avisos = new ArrayList<>(avisosDeMantenimiento(lotes));
        avisoDeFinalizacionTardia(fechaFinDe(lotes), escenario.finPlanificacion()).ifPresent(avisos::add);
        return armarPlan(modalidad, criterio, lotesDTO, escenario.litrosAPlanificar(), avisos);
    }

    /**
     * Avisos de los equipos en mantenimiento que usa el plan (solo se eligen si no hay otro compatible), uno por equipo
     * y en el orden en que aparecen. No impiden confirmarlo.
     */
    static List<String> avisosDeMantenimiento(List<LotePlanificado> lotes) {
        LinkedHashSet<String> avisos = new LinkedHashSet<>();
        for (LotePlanificado planificado : lotes) {
            LoteDefinido lote = planificado.lote();
            for (EquipamientoEntity equipo : List.of(lote.fermentador(), lote.molino(), lote.macerador(), lote.ollaHervor())) {
                if (enMantenimiento(equipo)) {
                    avisos.add("El equipo " + identificadorDe(equipo) + " se encuentra en mantenimiento");
                }
            }
        }
        return List.copyOf(avisos);
    }

    private static boolean enMantenimiento(EquipamientoEntity equipo) {
        return equipo.getEstadoOperativo() == EstadoOperativo.EN_MANTENIMIENTO;
    }

    /**
     * Aviso de un plan que termina después de la fecha de finalización estimada de su planificación. No impide
     * confirmarlo: solo informa la demora.
     *
     * @return El aviso, o vacío si el plan termina a tiempo o no hay fechas para comparar.
     */
    static Optional<String> avisoDeFinalizacionTardia(LocalDate finPlan, LocalDate finPlanificacion) {
        if (finPlan == null || finPlanificacion == null || !finPlan.isAfter(finPlanificacion)) {
            return Optional.empty();
        }
        return Optional.of("El plan finaliza el " + finPlan.format(FORMATO_FECHA)
                + ", después de la fecha de finalización estimada de la planificación (" + finPlanificacion.format(FORMATO_FECHA) + ")");
    }

    static LotePlanProduccionResponseDTO aLoteDTO(int orden, LotePlanificado planificado) {
        LoteDefinido lote = planificado.lote();
        return MapperPlanProduccion.toLoteDTO(orden, lote.volumen(), lote.fermentador(), lote.molino(), lote.macerador(),
                lote.ollaHervor(), planificado.inicio(), planificado.fin());
    }

    static PlanProduccionResponseDTO armarPlan(ModalidadPlanProduccion modalidad, String criterio, List<LotePlanProduccionResponseDTO> lotes,
                                               double litrosAPlanificar, List<String> avisos) {
        double volumenTotal = Redondeo.redondear(lotes.stream().mapToDouble(LotePlanProduccionResponseDTO::getVolumenObjetivo).sum());
        return PlanProduccionResponseDTO.builder()
                .modalidad(modalidad)
                .criterio(criterio)
                .disponible(true)
                .avisos(List.copyOf(avisos))
                .lotes(List.copyOf(lotes))
                .litrosAPlanificar(litrosAPlanificar)
                .cantidadLotes(lotes.size())
                .volumenTotal(volumenTotal)
                .excedente(Math.max(0.0, Redondeo.redondear(volumenTotal - litrosAPlanificar)))
                .fermentadoresUtilizados((int) lotes.stream().map(LotePlanProduccionResponseDTO::getIdFermentador).distinct().count())
                .fechaFinalizacionPlan(lotes.stream().map(LotePlanProduccionResponseDTO::getFechaFinalizacionEstimada)
                        .max(Comparator.naturalOrder()).orElse(null))
                .build();
    }

    static PlanProduccionResponseDTO noDisponible(ModalidadPlanProduccion modalidad, String criterio, String motivo, double litrosAPlanificar) {
        return PlanProduccionResponseDTO.builder()
                .modalidad(modalidad)
                .criterio(criterio)
                .disponible(false)
                .motivoNoDisponible(motivo)
                .litrosAPlanificar(litrosAPlanificar)
                .volumenTotal(0.0)
                .excedente(0.0)
                .build();
    }

    // ==================== Apoyo ====================

    private static String motivoPorEquiposFaltantes(Escenario escenario) {
        if (escenario.fermentadores().isEmpty()) {
            return MENSAJE_SIN_FERMENTADORES;
        }
        if (escenario.molinos().isEmpty()) {
            return MENSAJE_SIN_MOLINOS;
        }
        if (escenario.maceradores().isEmpty()) {
            return MENSAJE_SIN_MACERADORES;
        }
        if (escenario.ollas().isEmpty()) {
            return MENSAJE_SIN_OLLAS;
        }
        return null;
    }

    private LocalDate calcularFin(LocalDate inicio, LoteDefinido lote) {
        return EstimacionLote.calcularFechaFinalizacionEstimada(inicio, lote.volumen(), lote.molino().getRendimientoMolienda(),
                lote.masaMalta(), escenario.versionReceta(), escenario.velocidadEnvasado());
    }

    /**
     * Cantidad de lotes (u olas) de la capacidad indicada que cubren los litros: {@code ceil(litros / capacidad)}, con
     * aritmética exacta (un cociente de 3,00004 da 4).
     */
    static int lotesNecesarios(double litros, double capacidad) {
        BigDecimal cociente = BigDecimal.valueOf(litros).divide(BigDecimal.valueOf(capacidad), 0, RoundingMode.CEILING);
        return cociente.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : cociente.intValue();
    }

    private static String mensajeMaximoDeLotes(String modalidad, int cantidadLotes) {
        return "El plan " + modalidad + " necesita " + (cantidadLotes == Integer.MAX_VALUE ? "más de " + LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES : cantidadLotes)
                + " lotes y el máximo es de " + LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES;
    }

    private static List<Double> volumenesDe(List<LotePlanificado> lotes) {
        return lotes.stream().map(l -> l.lote().volumen()).toList();
    }

    private static LocalDate fechaFinDe(List<LotePlanificado> lotes) {
        return lotes.stream().map(LotePlanificado::fin).max(Comparator.naturalOrder()).orElse(null);
    }

    private static String identificadorDe(EquipamientoEntity equipo) {
        return Objects.toString(equipo.getIdentificadorInterno(), "");
    }

    private static String formatear(BigDecimal valor) {
        return FormatoCantidad.formatearVolumen(valor);
    }
}
