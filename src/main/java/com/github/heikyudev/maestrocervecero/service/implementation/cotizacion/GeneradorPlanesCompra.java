package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Algoritmo de la generación de planes de órdenes de compra. Es puro: no consulta repositorios ni guarda nada; recibe
 * los faltantes y las ofertas ya convertidas a la unidad de medida de cada insumo y devuelve los planes.
 * <p>
 * <b>Orden de las ofertas de cada insumo.</b> Las ofertas se comparan siempre por unidad de medida del insumo: el precio
 * por kilogramo (o gramo) y la cantidad disponible en esa unidad. Dentro de cada insumo, ambos valores se normalizan
 * entre 0 y 1 como una penalización (0 es lo mejor):
 * </p>
 * <ul>
 *     <li>precio normalizado = (precio - precio mínimo) / (precio máximo - precio mínimo): la oferta más barata vale 0 y
 *     la más cara 1;</li>
 *     <li>cantidad normalizada = (cantidad máxima - cantidad) / (cantidad máxima - cantidad mínima): la oferta de mayor
 *     cantidad vale 0 y la de menor cantidad 1. La cantidad de cada oferta es la que puede cubrir: la menor entre su
 *     cantidad disponible y el faltante del insumo (ofrecer más de lo que falta no la mejora).</li>
 * </ul>
 * <p>
 * Si todas las ofertas del insumo tienen el mismo valor, su normalizado es 0. El puntaje de una oferta (menor es mejor)
 * es el precio normalizado con {@code MENOR_PRECIO}, la cantidad normalizada con {@code MAYOR_CANTIDAD}, y
 * {@code peso x precio + (1 - peso) x cantidad} con {@code PONDERADO}, donde el peso es el de la configuración dividido
 * por 100. Así un precio menor y una cantidad mayor siempre mejoran el puntaje. Los empates se resuelven por el otro
 * valor normalizado y, por último, por el orden de las ofertas.
 * </p>
 * <p>
 * <b>Armado de un plan.</b> Para cada insumo se recorren sus ofertas en orden y se toman unidades enteras de la
 * presentación hasta cubrir el faltante (la última puede excederlo por el contenido de la presentación). Si las ofertas
 * no alcanzan, el plan se arma igual con lo disponible.
 * </p>
 * <p>
 * <b>Planes distintos.</b> El primer plan armado usa todas las ofertas. Los siguientes se buscan excluyendo, de a
 * una, las ofertas que usó un plan ya armado (búsqueda a lo ancho, con un máximo de {@value #MAXIMO_EVALUACIONES}
 * armados), sin excluir nunca la última oferta de un insumo, y se descartan los que repiten exactamente las mismas
 * asignaciones. Todos los planes se ordenan por la proporción del faltante que dejan sin cubrir, luego por su puntaje
 * (el promedio, por insumo, del puntaje de sus ofertas ponderado por la parte del faltante que cubre cada una; lo que
 * queda sin cubrir cuenta con el peor puntaje) y luego por su costo total; el primero es el mejor.
 * </p>
 */
public final class GeneradorPlanesCompra {

    /** Cantidad máxima de planes que se arman al buscar planes distintos. */
    static final int MAXIMO_EVALUACIONES = 500;

    /** Saldo a partir del cual un faltante se considera cubierto (la mitad de la última cifra de las cantidades). */
    private static final double SALDO_MINIMO = 0.00005;

    private GeneradorPlanesCompra() {
    }

    /**
     * Faltante de un insumo de la solicitud.
     *
     * @param idLinea ID de la línea de la solicitud.
     * @param cantidadFaltante Cantidad a cubrir, en la unidad del insumo.
     */
    public record Demanda(Long idLinea, double cantidadFaltante) {
    }

    /**
     * Oferta de un proveedor para una línea, con el contenido de su presentación en la unidad del insumo.
     *
     * @param idOferta ID de la oferta (desempate final del orden).
     * @param idLinea ID de la línea de la solicitud.
     * @param idProveedor ID del proveedor.
     * @param idCatalogoProveedor Ítem del catálogo ofrecido.
     * @param contenidoPorPresentacion Cantidad de insumo por unidad de la presentación.
     * @param precioUnitario Precio de una unidad de la presentación.
     * @param unidadesDisponibles Unidades de la presentación que el proveedor puede entregar.
     */
    public record Oferta(Long idOferta, Long idLinea, Long idProveedor, Long idCatalogoProveedor,
                         double contenidoPorPresentacion, BigDecimal precioUnitario, int unidadesDisponibles) {

        /** Precio por unidad de medida del insumo. */
        double precioPorUnidad() {
            return precioUnitario.doubleValue() / contenidoPorPresentacion;
        }

        /** Cantidad disponible en la unidad de medida del insumo. */
        double cantidadDisponible() {
            return unidadesDisponibles * contenidoPorPresentacion;
        }
    }

    /**
     * Unidades de una oferta asignadas a un plan.
     *
     * @param oferta La oferta.
     * @param unidades Unidades de la presentación asignadas (mayor a 0).
     */
    public record Asignacion(Oferta oferta, int unidades) {

        /** Costo de la asignación: unidades por precio unitario. */
        public BigDecimal costo() {
            return oferta.precioUnitario().multiply(BigDecimal.valueOf(unidades));
        }
    }

    /**
     * Plan generado.
     *
     * @param asignaciones Unidades asignadas a cada oferta usada, por línea y en el orden en que se tomaron.
     * @param proporcionSinCubrir Suma, por línea, de la parte del faltante que el plan no cubre (0 si cubre todo).
     * @param puntaje Promedio por línea del puntaje de sus ofertas ponderado por lo que cubre cada una (menor es mejor).
     * @param costoTotal Suma del costo de sus asignaciones.
     */
    public record Plan(List<Asignacion> asignaciones, double proporcionSinCubrir, double puntaje, BigDecimal costoTotal) {
    }

    /**
     * Genera hasta {@code cantidadPlanes} planes distintos.
     *
     * @param demandas Faltante de cada línea de la solicitud.
     * @param ofertas Ofertas de los proveedores activos.
     * @param criterio Criterio de orden de las ofertas.
     * @param pesoPrecio Peso del precio (0 a 100) del criterio ponderado.
     * @param cantidadPlanes Cantidad máxima de planes.
     * @return Los planes, el mejor primero; vacío si ninguna oferta sirve para ninguna línea.
     */
    public static List<Plan> generar(List<Demanda> demandas, List<Oferta> ofertas, CriterioPlanCompra criterio,
                                     double pesoPrecio, int cantidadPlanes) {
        return generar(demandas, ofertas, criterio, pesoPrecio, cantidadPlanes, MAXIMO_EVALUACIONES);
    }

    /**
     * Igual que {@link #generar(List, List, CriterioPlanCompra, double, int)} con un máximo de planes armados propio.
     */
    static List<Plan> generar(List<Demanda> demandas, List<Oferta> ofertas, CriterioPlanCompra criterio,
                              double pesoPrecio, int cantidadPlanes, int maximoEvaluaciones) {
        Map<Long, Double> faltantePorLinea = new HashMap<>();
        demandas.forEach(demanda -> faltantePorLinea.put(demanda.idLinea(), demanda.cantidadFaltante()));

        // 1. Ordenar las ofertas útiles de cada línea por el criterio, con su puntaje
        Map<Long, List<OfertaPuntuada>> ofertasPorLinea = new HashMap<>();
        for (Demanda demanda : demandas) {
            List<Oferta> deLaLinea = ofertas.stream()
                    .filter(oferta -> demanda.idLinea().equals(oferta.idLinea()))
                    .filter(oferta -> oferta.contenidoPorPresentacion() > 0 && oferta.unidadesDisponibles() > 0
                            && oferta.precioUnitario() != null && oferta.precioUnitario().signum() > 0)
                    .toList();
            ofertasPorLinea.put(demanda.idLinea(), puntuarYOrdenar(deLaLinea, criterio, pesoPrecio, demanda.cantidadFaltante()));
        }
        if (ofertasPorLinea.values().stream().allMatch(List::isEmpty) || cantidadPlanes <= 0) {
            return List.of();
        }

        // 2. Buscar planes distintos: el primero con todas las ofertas y los siguientes excluyendo ofertas de a una
        List<Plan> planes = new ArrayList<>();
        Set<String> firmas = new HashSet<>();
        Set<Set<Long>> exclusionesVistas = new HashSet<>();
        Deque<Set<Long>> pendientes = new ArrayDeque<>();
        pendientes.add(Set.of());
        exclusionesVistas.add(Set.of());
        int evaluaciones = 0;
        while (!pendientes.isEmpty() && evaluaciones < maximoEvaluaciones && planes.size() < cantidadPlanes * 4) {
            Set<Long> excluidas = pendientes.poll();
            evaluaciones++;
            Plan plan = armarPlan(demandas, ofertasPorLinea, faltantePorLinea, excluidas);
            if (plan.asignaciones().isEmpty()) {
                continue;
            }
            if (firmas.add(firma(plan))) {
                planes.add(plan);
            }
            for (Asignacion asignacion : plan.asignaciones()) {
                Set<Long> siguiente = new TreeSet<>(excluidas);
                siguiente.add(asignacion.oferta().idOferta());
                // Nunca se excluye la última oferta de un insumo: un plan alternativo no deja sin cubrir lo que se puede cubrir
                if (quedaOtraOferta(ofertasPorLinea.get(asignacion.oferta().idLinea()), siguiente) && exclusionesVistas.add(siguiente)) {
                    pendientes.add(siguiente);
                }
            }
        }

        // 3. Ordenar todos los planes por cobertura, puntaje y costo (el orden es estable: ante un empate total queda
        //    primero el que se armó antes)
        if (planes.isEmpty()) {
            return List.of();
        }
        planes.sort(Comparator.comparingDouble(Plan::proporcionSinCubrir)
                .thenComparingDouble(Plan::puntaje)
                .thenComparing(Plan::costoTotal));
        return List.copyOf(planes.subList(0, Math.min(cantidadPlanes, planes.size())));
    }

    /** Oferta con su puntaje y el valor de desempate dentro de su línea. */
    record OfertaPuntuada(Oferta oferta, double puntaje, double desempate) {
    }

    /**
     * Normaliza precio y cantidad dentro de la línea, calcula el puntaje de cada oferta según el criterio y las ordena
     * de mejor a peor.
     */
    static List<OfertaPuntuada> puntuarYOrdenar(List<Oferta> ofertas, CriterioPlanCompra criterio, double pesoPrecio, double faltante) {
        if (ofertas.isEmpty()) {
            return List.of();
        }
        // Solo cuenta la cantidad que la oferta puede cubrir: ofrecer más de lo que falta no la mejora
        java.util.function.ToDoubleFunction<Oferta> cantidadUtil = oferta -> Math.min(oferta.cantidadDisponible(), faltante);
        double precioMinimo = ofertas.stream().mapToDouble(Oferta::precioPorUnidad).min().orElse(0);
        double precioMaximo = ofertas.stream().mapToDouble(Oferta::precioPorUnidad).max().orElse(0);
        double cantidadMinima = ofertas.stream().mapToDouble(cantidadUtil).min().orElse(0);
        double cantidadMaxima = ofertas.stream().mapToDouble(cantidadUtil).max().orElse(0);
        double peso = Math.max(0.0, Math.min(100.0, pesoPrecio)) / 100.0;

        List<OfertaPuntuada> puntuadas = new ArrayList<>();
        for (Oferta oferta : ofertas) {
            double precioNormalizado = normalizar(oferta.precioPorUnidad() - precioMinimo, precioMaximo - precioMinimo);
            double cantidadNormalizada = normalizar(cantidadMaxima - cantidadUtil.applyAsDouble(oferta), cantidadMaxima - cantidadMinima);
            double puntaje;
            double desempate;
            switch (criterio) {
                case MENOR_PRECIO -> {
                    puntaje = precioNormalizado;
                    desempate = cantidadNormalizada;
                }
                case MAYOR_CANTIDAD -> {
                    puntaje = cantidadNormalizada;
                    desempate = precioNormalizado;
                }
                default -> {
                    puntaje = peso * precioNormalizado + (1 - peso) * cantidadNormalizada;
                    desempate = precioNormalizado;
                }
            }
            puntuadas.add(new OfertaPuntuada(oferta, puntaje, desempate));
        }
        puntuadas.sort(Comparator.comparingDouble(OfertaPuntuada::puntaje)
                .thenComparingDouble(OfertaPuntuada::desempate)
                .thenComparing(puntuada -> puntuada.oferta().idOferta(), Comparator.nullsLast(Comparator.naturalOrder())));
        return puntuadas;
    }

    /** Si a la línea le queda alguna oferta sin excluir. */
    private static boolean quedaOtraOferta(List<OfertaPuntuada> ofertasDeLaLinea, Set<Long> excluidas) {
        return ofertasDeLaLinea.stream().anyMatch(puntuada -> !excluidas.contains(puntuada.oferta().idOferta()));
    }

    private static double normalizar(double diferencia, double rango) {
        return rango > 0 ? diferencia / rango : 0.0;
    }

    /**
     * Arma un plan tomando, para cada línea, las ofertas en orden (sin las excluidas) hasta cubrir el faltante.
     */
    static Plan armarPlan(List<Demanda> demandas, Map<Long, List<OfertaPuntuada>> ofertasPorLinea,
                          Map<Long, Double> faltantePorLinea, Set<Long> excluidas) {
        List<Asignacion> asignaciones = new ArrayList<>();
        double proporcionSinCubrir = 0.0;
        double sumaPuntajes = 0.0;
        BigDecimal costo = BigDecimal.ZERO;

        for (Demanda demanda : demandas) {
            double faltante = faltantePorLinea.get(demanda.idLinea());
            double restante = faltante;
            double puntajeLinea = 0.0;
            for (OfertaPuntuada puntuada : ofertasPorLinea.getOrDefault(demanda.idLinea(), List.of())) {
                if (restante <= SALDO_MINIMO) {
                    break;
                }
                Oferta oferta = puntuada.oferta();
                if (excluidas.contains(oferta.idOferta())) {
                    continue;
                }
                int necesarias = (int) Math.ceil((restante - SALDO_MINIMO) / oferta.contenidoPorPresentacion());
                int unidades = Math.min(oferta.unidadesDisponibles(), Math.max(1, necesarias));
                double cubierto = Math.min(restante, unidades * oferta.contenidoPorPresentacion());
                Asignacion asignacion = new Asignacion(oferta, unidades);
                asignaciones.add(asignacion);
                costo = costo.add(asignacion.costo());
                puntajeLinea += puntuada.puntaje() * (cubierto / faltante);
                restante -= unidades * oferta.contenidoPorPresentacion();
            }
            // Un residuo de punto flotante (por ejemplo 3 x 0,7 contra 2,1) no cuenta como faltante sin cubrir
            double sinCubrir = restante <= SALDO_MINIMO ? 0.0 : restante;
            proporcionSinCubrir += faltante > 0 ? sinCubrir / faltante : 0.0;
            // Lo que queda sin cubrir cuenta con el peor puntaje (1)
            sumaPuntajes += puntajeLinea + (faltante > 0 ? sinCubrir / faltante : 0.0);
        }
        double puntaje = demandas.isEmpty() ? 0.0 : sumaPuntajes / demandas.size();
        return new Plan(List.copyOf(asignaciones), proporcionSinCubrir, puntaje, costo);
    }

    /** Las asignaciones del plan sin importar su orden: dos planes con la misma firma son el mismo plan. */
    private static String firma(Plan plan) {
        Set<String> partes = new TreeSet<>();
        plan.asignaciones().forEach(asignacion -> partes.add(asignacion.oferta().idOferta() + "x" + asignacion.unidades()));
        return String.join(",", new LinkedHashSet<>(partes));
    }
}
