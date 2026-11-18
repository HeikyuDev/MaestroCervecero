package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.GeneradorPlanesCompra.Asignacion;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.GeneradorPlanesCompra.Demanda;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.GeneradorPlanesCompra.Oferta;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.GeneradorPlanesCompra.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorPlanesCompraTest {

    private static final Long LINEA_MALTA = 1L;
    private static final Long LINEA_LUPULO = 2L;

    // Oferta: id, línea, proveedor, catálogo, contenido por presentación (unidad del insumo), precio de la presentación, unidades
    private static Oferta oferta(long id, Long linea, long proveedor, double contenido, String precio, int unidades) {
        return new Oferta(id, linea, proveedor, 100 + id, contenido, new BigDecimal(precio), unidades);
    }

    private static String asignaciones(Plan plan) {
        return plan.asignaciones().stream().map(a -> a.oferta().idOferta() + "x" + a.unidades()).collect(Collectors.joining(","));
    }

    @Test
    @DisplayName("CP-GPC-01: sin ofertas útiles no se genera ningún plan")
    void sinOfertas_noGeneraPlanes() {
        // === PREPARACION DE DATOS ===
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 50.0));
        List<Oferta> invalidas = List.of(
                oferta(1, LINEA_MALTA, 10, 0.0, "1000", 5),
                oferta(2, LINEA_MALTA, 11, 25.0, "1000", 0),
                oferta(3, 99L, 12, 25.0, "1000", 5));

        // === EJECUCION Y VERIFICACION ===
        assertThat(GeneradorPlanesCompra.generar(demandas, List.of(), CriterioPlanCompra.MENOR_PRECIO, 50, 3)).isEmpty();
        assertThat(GeneradorPlanesCompra.generar(demandas, invalidas, CriterioPlanCompra.MENOR_PRECIO, 50, 3)).isEmpty();
    }

    @Test
    @DisplayName("CP-GPC-02: con MENOR_PRECIO se compara el precio por unidad del insumo, no el de la presentación")
    void menorPrecio_comparaPorUnidadDelInsumo() {
        // === PREPARACION DE DATOS ===
        // Bolsa de 25 kg a $25.000 = $1.000/kg; bolsa de 10 kg a $9.000 = $900/kg (más barata por kg aunque la bolsa sale menos)
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 25.0, "25000", 10),
                oferta(2, LINEA_MALTA, 11, 10.0, "9000", 10));

        // === EJECUCION ===
        List<Plan> planes = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 30.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1);

        // === ASSERTS ===
        assertThat(planes).hasSize(1);
        assertThat(asignaciones(planes.get(0))).isEqualTo("2x3");
        assertThat(planes.get(0).costoTotal()).isEqualByComparingTo("27000");
        assertThat(planes.get(0).proporcionSinCubrir()).isZero();
    }

    @Test
    @DisplayName("CP-GPC-03: se piden unidades enteras de la presentación hasta cubrir el faltante")
    void unidadesEnteras_redondeaHaciaArriba() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 25.0, "25000", 10));

        // === EJECUCION ===
        Plan exacto = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 50.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);
        Plan conResto = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 50.5)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);

        // === ASSERTS ===
        assertThat(asignaciones(exacto)).isEqualTo("1x2");
        assertThat(asignaciones(conResto)).isEqualTo("1x3");
    }

    @Test
    @DisplayName("CP-GPC-04: si la mejor oferta no alcanza, el insumo se reparte con la siguiente oferta (otro proveedor)")
    void ofertaInsuficiente_repartaEntreProveedores() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 25.0, "20000", 1),
                oferta(2, LINEA_MALTA, 11, 25.0, "30000", 10));

        // === EJECUCION ===
        Plan plan = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 60.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);

        // === ASSERTS ===
        assertThat(asignaciones(plan)).isEqualTo("1x1,2x2");
        assertThat(plan.asignaciones()).extracting(a -> a.oferta().idProveedor()).containsExactly(10L, 11L);
        assertThat(plan.costoTotal()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("CP-GPC-05: con MAYOR_CANTIDAD se prefiere la oferta que cubre más del faltante; ofrecer más de lo que falta no la mejora")
    void mayorCantidad_prefiereLaQueCubreMas() {
        // === PREPARACION DE DATOS ===
        // Oferta 1: 1 bolsa de 25 kg (cubre 25 de 40 kg), la más barata; oferta 2: 10 bolsas de 10 kg (100 kg, cubre los 40)
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 25.0, "20000", 1),
                oferta(2, LINEA_MALTA, 11, 10.0, "12000", 10));
        // Oferta 3: 50 kg y oferta 4: 100 kg, las dos cubren los 40 kg: empatan en cantidad y decide el precio
        List<Oferta> ambasCubren = List.of(
                oferta(3, LINEA_MALTA, 10, 25.0, "20000", 2),
                oferta(4, LINEA_MALTA, 11, 10.0, "12000", 10));

        // === EJECUCION ===
        Plan plan = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 40.0)), ofertas, CriterioPlanCompra.MAYOR_CANTIDAD, 50, 1).get(0);
        Plan empate = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 40.0)), ambasCubren, CriterioPlanCompra.MAYOR_CANTIDAD, 50, 1).get(0);

        // === ASSERTS ===
        assertThat(asignaciones(plan)).isEqualTo("2x4");
        // $ 800/kg contra $ 1.200/kg
        assertThat(asignaciones(empate)).isEqualTo("3x2");
    }

    @Test
    @DisplayName("CP-GPC-06: el criterio ponderado combina precio y cantidad (la que cubre del faltante) normalizados según el peso del precio")
    void ponderado_combinaPrecioYCantidad() {
        // === PREPARACION DE DATOS ===
        // Oferta 1: $800/kg con 30 kg; oferta 2: $1.000/kg con 100 kg; oferta 3: $850/kg con 90 kg
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 10.0, "8000", 3),
                oferta(2, LINEA_MALTA, 11, 10.0, "10000", 10),
                oferta(3, LINEA_MALTA, 12, 10.0, "8500", 9));
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 100.0));

        // === EJECUCION ===
        Plan soloPrecio = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.PONDERADO, 100, 1).get(0);
        Plan soloCantidad = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.PONDERADO, 0, 1).get(0);
        Plan intermedio = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.PONDERADO, 50, 1).get(0);

        // === ASSERTS ===
        // Faltan 100 kg. Peso 100: puntajes 0, 1 y 0,25 -> oferta 1 (30 kg) y el resto de la 3. Peso 0: puntajes 1, 0 y 0,14
        // -> la oferta 2 cubre todo. Peso 50: puntajes 0,5, 0,5 y 0,196 -> la 3 (90 kg) y el resto de la 1 (más barata en el empate)
        assertThat(asignaciones(soloPrecio)).isEqualTo("1x3,3x7");
        assertThat(asignaciones(soloCantidad)).isEqualTo("2x10");
        assertThat(asignaciones(intermedio)).isEqualTo("3x9,1x1");
    }

    @Test
    @DisplayName("CP-GPC-07: los planes generados son distintos entre sí y el primero usa las mejores ofertas")
    void planesDistintos_sinRepetidos() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 25.0, "25000", 10),
                oferta(2, LINEA_MALTA, 11, 25.0, "26000", 10),
                oferta(3, LINEA_LUPULO, 10, 1000.0, "50000", 5),
                oferta(4, LINEA_LUPULO, 12, 1000.0, "45000", 5));
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 50.0), new Demanda(LINEA_LUPULO, 2000.0));

        // === EJECUCION ===
        List<Plan> planes = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 10);

        // === ASSERTS ===
        assertThat(asignaciones(planes.get(0))).isEqualTo("1x2,4x2");
        Set<String> firmas = new HashSet<>();
        planes.forEach(plan -> assertThat(firmas.add(asignaciones(plan))).as("plan repetido: " + asignaciones(plan)).isTrue());
        // Dos ofertas por insumo: hay 4 combinaciones posibles
        assertThat(planes).hasSize(4);
        assertThat(planes).allMatch(plan -> plan.proporcionSinCubrir() == 0.0);
        assertThat(planes.get(0).costoTotal()).isLessThanOrEqualTo(planes.get(1).costoTotal());
    }

    @Test
    @DisplayName("CP-GPC-08: si las ofertas no cubren el faltante el plan se genera igual e informa lo que queda sin cubrir")
    void sinCobertura_completa_igualGeneraPlan() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 25.0, "25000", 2));

        // === EJECUCION ===
        List<Plan> planes = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 100.0), new Demanda(LINEA_LUPULO, 500.0)),
                ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 3);

        // === ASSERTS ===
        assertThat(planes).hasSize(1);
        assertThat(asignaciones(planes.get(0))).isEqualTo("1x2");
        // Malta: 50 de 100 sin cubrir (0,5); lúpulo: todo sin cubrir (1)
        assertThat(planes.get(0).proporcionSinCubrir()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("CP-GPC-09: se generan como máximo la cantidad de planes configurada y los alternativos se ordenan por cobertura")
    void cantidadPlanes_limitaYOrdenaPorCobertura() {
        // === PREPARACION DE DATOS ===
        // La oferta 1 cubre todo; la 2 es más cara y cubre todo; la 3 es la más cara y cubre la mitad
        List<Oferta> ofertas = List.of(
                oferta(1, LINEA_MALTA, 10, 10.0, "1000", 10),
                oferta(2, LINEA_MALTA, 11, 10.0, "3000", 10),
                oferta(3, LINEA_MALTA, 12, 10.0, "5000", 5));
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 100.0));

        // === EJECUCION ===
        List<Plan> uno = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1);
        List<Plan> todos = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 10);

        // === ASSERTS ===
        assertThat(uno).hasSize(1);
        assertThat(asignaciones(uno.get(0))).isEqualTo("1x10");
        assertThat(todos).hasSize(3);
        assertThat(asignaciones(todos.get(1))).isEqualTo("2x10");
        assertThat(asignaciones(todos.get(2))).isEqualTo("3x5");
        assertThat(todos.get(2).proporcionSinCubrir()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("CP-GPC-10: ante igual puntaje decide el otro criterio y, por último, el orden de las ofertas")
    void empates_seResuelvenPorElOtroCriterioYElOrden() {
        // === PREPARACION DE DATOS ===
        List<Oferta> mismoPrecio = List.of(
                oferta(1, LINEA_MALTA, 10, 10.0, "1000", 3),
                oferta(2, LINEA_MALTA, 11, 10.0, "1000", 8));
        List<Oferta> todoIgual = List.of(
                oferta(5, LINEA_MALTA, 10, 10.0, "1000", 8),
                oferta(4, LINEA_MALTA, 11, 10.0, "1000", 8));

        // === EJECUCION ===
        // Faltan 50 kg: la oferta 1 cubre 30 y la 2 los 50
        List<Asignacion> desempatePorCantidad = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 50.0)), mismoPrecio,
                CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0).asignaciones();
        List<Asignacion> desempatePorOrden = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 20.0)), todoIgual,
                CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0).asignaciones();

        // === ASSERTS ===
        assertThat(desempatePorCantidad).extracting(a -> a.oferta().idOferta()).containsExactly(2L);
        assertThat(desempatePorOrden).extracting(a -> a.oferta().idOferta()).containsExactly(4L);
    }

    @Test
    @DisplayName("CP-GPC-11: con peso 0 y 100 y ofertas empatadas en el criterio decide el otro valor y después el orden")
    void pesosExtremos_conEmpates() {
        // === PREPARACION DE DATOS ===
        // Mismo precio por kg, distinta cantidad; y misma cantidad, distinto precio
        List<Oferta> mismoPrecio = List.of(oferta(1, LINEA_MALTA, 10, 10.0, "1000", 3), oferta(2, LINEA_MALTA, 11, 10.0, "1000", 9));
        List<Oferta> mismaCantidad = List.of(oferta(3, LINEA_MALTA, 10, 10.0, "1200", 5), oferta(4, LINEA_MALTA, 11, 10.0, "1000", 5));
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 50.0));

        // === EJECUCION Y ASSERTS ===
        // Peso 100 con igual precio: decide... el precio normalizado (empatado) y luego el orden de las ofertas
        assertThat(asignaciones(GeneradorPlanesCompra.generar(demandas, mismoPrecio, CriterioPlanCompra.PONDERADO, 100, 1).get(0))).isEqualTo("1x3,2x2");
        // Peso 0 con igual precio: la que cubre más
        assertThat(asignaciones(GeneradorPlanesCompra.generar(demandas, mismoPrecio, CriterioPlanCompra.PONDERADO, 0, 1).get(0))).isEqualTo("2x5");
        // Peso 0 con igual cantidad: desempata el precio
        assertThat(asignaciones(GeneradorPlanesCompra.generar(demandas, mismaCantidad, CriterioPlanCompra.PONDERADO, 0, 1).get(0))).isEqualTo("4x5");
        // Peso 100 con igual cantidad: el más barato
        assertThat(asignaciones(GeneradorPlanesCompra.generar(demandas, mismaCantidad, CriterioPlanCompra.PONDERADO, 100, 1).get(0))).isEqualTo("4x5");
    }

    @Test
    @DisplayName("CP-GPC-12: los precios con 4 decimales se comparan y suman sin redondear")
    void preciosConCuatroDecimales() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 1.0, "10.0001", 10), oferta(2, LINEA_MALTA, 11, 1.0, "10.0002", 10));

        // === EJECUCION ===
        Plan plan = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 3.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);

        // === ASSERTS ===
        assertThat(asignaciones(plan)).isEqualTo("1x3");
        assertThat(plan.costoTotal()).isEqualByComparingTo("30.0003");
    }

    @Test
    @DisplayName("CP-GPC-13: la búsqueda de planes distintos se detiene al llegar al máximo de planes armados")
    void maximoEvaluaciones_cortaLaBusqueda() {
        // === PREPARACION DE DATOS ===
        List<Oferta> ofertas = new java.util.ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            ofertas.add(oferta(i, LINEA_MALTA, i, 10.0, String.valueOf(1000 + i), 1));
        }
        List<Demanda> demandas = List.of(new Demanda(LINEA_MALTA, 50.0));

        // === EJECUCION ===
        List<Plan> conUnArmado = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 10, 10, 1);
        List<Plan> conTres = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 10, 10, 3);
        List<Plan> sinTope = GeneradorPlanesCompra.generar(demandas, ofertas, CriterioPlanCompra.MENOR_PRECIO, 10, 10);

        // === ASSERTS ===
        assertThat(conUnArmado).hasSize(1);
        assertThat(asignaciones(conUnArmado.get(0))).isEqualTo("1x1,2x1,3x1,4x1,5x1");
        assertThat(conTres).hasSize(3);
        assertThat(sinTope).hasSize(10);
        assertThat(GeneradorPlanesCompra.MAXIMO_EVALUACIONES).isEqualTo(500);
    }

    @Test
    @DisplayName("CP-GPC-14: todos los planes se ordenan por cobertura, puntaje y costo, sin un primer plan fijo")
    void ordenGeneral_sinPrimeroFijo() {
        // === PREPARACION DE DATOS ===
        // Con MAYOR_CANTIDAD el primer plan armado toma la oferta grande y cara; la alternativa con la chica cubre igual
        // pero puntúa peor, así que el orden por puntaje mantiene primero al de mejor criterio aunque sea más caro
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 10.0, "5000", 10), oferta(2, LINEA_MALTA, 11, 10.0, "1000", 3),
                oferta(3, LINEA_MALTA, 12, 10.0, "1000", 3));

        // === EJECUCION ===
        List<Plan> planes = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 60.0)), ofertas, CriterioPlanCompra.MAYOR_CANTIDAD, 50, 10);

        // === ASSERTS ===
        assertThat(planes).isSortedAccordingTo(java.util.Comparator.comparingDouble(Plan::proporcionSinCubrir)
                .thenComparingDouble(Plan::puntaje).thenComparing(Plan::costoTotal));
        assertThat(asignaciones(planes.get(0))).isEqualTo("1x6");
        // Los que cubren todo van antes que el que deja faltante (solo con la oferta 3)
        assertThat(planes.get(0).proporcionSinCubrir()).isZero();
        assertThat(planes.get(planes.size() - 1).proporcionSinCubrir()).isPositive();
    }

    @Test
    @DisplayName("CP-GPC-15: el residuo de punto flotante de contenidos no binarios (0,7 y 0,1) no cuenta como faltante ni decide el orden")
    void residuoFlotante_noDecideElOrden() {
        // === PREPARACION DE DATOS ===
        // 3 x 0,7 da 2,0999999999999996: sin el redondeo a cero el plan más barato quedaría detrás por "no cubrir" 4e-16
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 0.7, "700", 3), oferta(2, LINEA_MALTA, 11, 0.1, "120", 21));

        // === EJECUCION ===
        List<Plan> planes = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 2.1)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 2);

        // === ASSERTS ===
        assertThat(planes).hasSize(2);
        assertThat(asignaciones(planes.get(0))).isEqualTo("1x3");
        assertThat(asignaciones(planes.get(1))).isEqualTo("2x21");
        assertThat(planes).allSatisfy(plan -> assertThat(plan.proporcionSinCubrir()).isEqualTo(0.0));
    }

    @Test
    @DisplayName("CP-GPC-16: un proveedor con varias presentaciones del mismo insumo las combina para cubrir el faltante")
    void variasPresentacionesDelMismoProveedor_seCombinan() {
        // === PREPARACION DE DATOS ===
        // Mismo proveedor: 2 bolsas de 25 kg a $20.000 ($800/kg) y 3 bolsas de 10 kg a $9.000 ($900/kg): 80 kg disponibles
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 25.0, "20000", 2), oferta(2, LINEA_MALTA, 10, 10.0, "9000", 3));

        // === EJECUCION ===
        Plan plan = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 60.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);

        // === ASSERTS ===
        // 2 x 25 kg + 1 x 10 kg = 60 kg: cubre todo con una asignación por presentación del mismo proveedor
        assertThat(asignaciones(plan)).isEqualTo("1x2,2x1");
        assertThat(plan.asignaciones()).allSatisfy(a -> assertThat(a.oferta().idProveedor()).isEqualTo(10L));
        assertThat(plan.proporcionSinCubrir()).isZero();
        assertThat(plan.costoTotal()).isEqualByComparingTo("49000");
    }

    @Test
    @DisplayName("CP-GPC-17: con MENOR_PRECIO se prefiere la presentación más barata por kilogramo del mismo proveedor")
    void variasPresentaciones_menorPrecioPorKilogramo() {
        // === PREPARACION DE DATOS ===
        // Bolsa de 25 kg a $30.000 ($1.200/kg) y bolsa de 10 kg a $9.000 ($900/kg), del mismo proveedor
        List<Oferta> ofertas = List.of(oferta(1, LINEA_MALTA, 10, 25.0, "30000", 4), oferta(2, LINEA_MALTA, 10, 10.0, "9000", 4));

        // === EJECUCION ===
        Plan plan = GeneradorPlanesCompra.generar(List.of(new Demanda(LINEA_MALTA, 20.0)), ofertas, CriterioPlanCompra.MENOR_PRECIO, 50, 1).get(0);

        // === ASSERTS ===
        assertThat(asignaciones(plan)).isEqualTo("2x2");
        assertThat(plan.costoTotal()).isEqualByComparingTo("18000");
    }
}
