package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.IEstadisticaComprasRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCompras;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReportePrecios;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteProveedores;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteRecepcion;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ColumnaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CriterioAplicadoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.FilaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.GraficoReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.IndicadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TablaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TipoColumna;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IEstadisticaComprasServicio}. Cada reporte se calcula con proyecciones y consultas agrupadas,
 * sin cargar entidades ni consultar fila por fila; la tabla se arma en memoria (el universo de un período es acotado)
 * y se pagina.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticaComprasServicioImpl implements IEstadisticaComprasServicio {

    static final String AYUDA_A_TIEMPO = "Porcentaje de las órdenes completas (se recibió todo lo pedido de cada ítem) cuyo último ingreso es de una fecha no posterior a su fecha de entrega estimada";
    static final String AYUDA_PLAZO = "Días desde la emisión de la orden hasta su último ingreso, en promedio de las órdenes completas";
    static final String SIN_ORDEN = "Sin orden de compra";
    private static final Map<TipoIngreso, String> ETIQUETAS_TIPO_INGRESO = Map.of(TipoIngreso.COMPRA, "Compra", TipoIngreso.DIRECTO, "Directo");

    private final IEstadisticaComprasRepository estadisticaComprasRepository;

    // ==================== Compras ====================

    /** Una orden del reporte "Compras". */
    private record Orden(Long id, LocalDateTime emision, Long idProveedor, String proveedor, EstadoSolicitud estado, long items,
                         BigDecimal monto) {
        boolean anulada() {
            return estado == EstadoSolicitud.ANULADA;
        }
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeCompras(CriteriosReporteCompras criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Órdenes emitidas en el período, de la más reciente a la más antigua
        List<Orden> ordenes = estadisticaComprasRepository.buscarOrdenes(periodo.inicio(), periodo.finExclusivo(), criterios.idProveedor(), criterios.estado())
                .stream()
                .map(f -> new Orden((Long) f[0], (LocalDateTime) f[1], (Long) f[2], (String) f[3], (EstadoSolicitud) f[4],
                        SoporteReportes.aLong(f[5]), SoporteReportes.aImporte(f[6])))
                .toList();

        // 3. Indicadores: el monto es de las órdenes no anuladas
        List<Orden> vigentes = ordenes.stream().filter(o -> !o.anulada()).toList();
        BigDecimal monto = vigentes.stream().map(Orden::monto).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        long anuladas = ordenes.size() - vigentes.size();
        BigDecimal promedio = SoporteReportes.dividir(monto, vigentes.size());

        Map<Long, String> nombrePorProveedor = new LinkedHashMap<>();
        Map<Long, BigDecimal> montoPorProveedor = new HashMap<>();
        for (Orden orden : vigentes) {
            nombrePorProveedor.putIfAbsent(orden.idProveedor(), orden.proveedor());
            montoPorProveedor.merge(orden.idProveedor(), orden.monto(), BigDecimal::add);
        }
        List<DatoGraficoResponseDTO> datos = ordenarDeMayorAMenor(nombrePorProveedor, montoPorProveedor);

        List<FilaReporteResponseDTO> tabla = ordenes.stream().map(o -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto("N.º " + o.id()),
                CeldaReporteResponseDTO.fecha(o.emision()),
                CeldaReporteResponseDTO.texto(o.proveedor()),
                CeldaReporteResponseDTO.texto(o.estado().getEtiqueta()),
                CeldaReporteResponseDTO.numero(o.items()),
                CeldaReporteResponseDTO.moneda(o.monto())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Compras")
                .descripcion(PestanaEstadistica.COMPRAS.getDescripcion())
                .criterioDeFecha("Órdenes de compra emitidas dentro del período")
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Proveedor", criterios.textoProveedor(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Estado", criterios.estado() == null ? null : criterios.estado().getEtiqueta(), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("monto-comprado", "Monto comprado", SoporteReportes.textoImporte(monto), "Sin las órdenes anuladas", null),
                        new IndicadorResponseDTO("ordenes-emitidas", "Órdenes emitidas", FormatoCantidad.formatear(ordenes.size()), "En cualquier estado", null),
                        new IndicadorResponseDTO("monto-promedio", "Monto promedio por orden", SoporteReportes.textoImporte(promedio), "De las órdenes no anuladas", null),
                        new IndicadorResponseDTO("ordenes-anuladas", "Órdenes anuladas",
                                SoporteReportes.textoPorcentaje(SoporteReportes.porcentaje(anuladas, ordenes.size())),
                                anuladas + " de " + ordenes.size(), null)))
                .grafico(GraficoReporteResponseDTO.moneda("Monto por proveedor", SoporteReportes.principales(datos, "Otros"), null))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Orden", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Fecha de emisión", TipoColumna.FECHA),
                        ColumnaReporteResponseDTO.de("Proveedor", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Estado", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Ítems", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Monto", TipoColumna.MONEDA)),
                        SoporteReportes.paginar(tabla, pageable),
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto("Total sin anuladas"),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.moneda(monto))),
                        "orden|órdenes"))
                .build();
    }

    // ==================== Proveedores ====================

    /** Un ítem de una orden de compra, con su cantidad de insumo por presentación. */
    private record Item(Long id, Long idOrden, LocalDateTime emision, LocalDate entregaEstimada, EstadoSolicitud estado,
                       String motivoFinalizacion, Long idProveedor, String proveedor, Long idInsumo, String insumo,
                       UnidadDeMedida unidadInsumo, int cantidad, BigDecimal costoUnitario, double cantidadPorPresentacion) {

        BigDecimal subtotal() {
            return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }

        /** El precio por unidad del insumo con precisión de {@link SoporteReportes#ESCALA_PRECIO} decimales, o {@code null} si la presentación no trae insumo. */
        BigDecimal precioPorUnidad() {
            return SoporteReportes.dividirConPrecision(costoUnitario, cantidadPorPresentacion);
        }

        double cantidadDeInsumo() {
            return cantidad * cantidadPorPresentacion;
        }
    }

    /** El desempeño de una orden de compra en el reporte "Proveedores". */
    private record Entrega(BigDecimal monto, double pedidas, double recibidas, boolean completa, boolean aTiempo, Long plazo,
                           boolean cierreManual) {
    }

    /** El desempeño acumulado de un proveedor. */
    private static final class Desempeno {
        private final String proveedor;
        private long ordenes;
        private BigDecimal monto = SoporteReportes.aImporte(null);
        private double pedidas;
        private double recibidas;
        private long completas;
        private long aTiempo;
        private long plazoTotal;

        private Desempeno(String proveedor) {
            this.proveedor = proveedor;
        }

        private void sumar(Entrega entrega) {
            ordenes++;
            monto = monto.add(entrega.monto());
            pedidas += entrega.pedidas();
            recibidas += entrega.recibidas();
            if (entrega.completa()) {
                completas++;
                plazoTotal += entrega.plazo();
                if (entrega.aTiempo()) {
                    aTiempo++;
                }
            }
        }

        private Double porcentajeATiempo() {
            return SoporteReportes.porcentaje(aTiempo, completas);
        }

        private Double plazoPromedio() {
            return completas > 0 ? SoporteReportes.redondear((double) plazoTotal / completas) : null;
        }

        private Double porcentajeRecibido() {
            return SoporteReportes.porcentaje(recibidas, pedidas);
        }
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeProveedores(CriteriosReporteProveedores criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Ítems de las órdenes no anuladas y lo recibido de cada uno, en una consulta agrupada
        List<Item> items = buscarItems(periodo, criterios.idProveedor(), criterios.tipoInsumo());
        Map<Long, double[]> recibidoPorItem = new HashMap<>();
        Map<Long, LocalDate> ultimoIngresoPorItem = new HashMap<>();
        if (!items.isEmpty()) {
            for (Object[] f : estadisticaComprasRepository.resumirIngresosPorDetalle(items.stream().map(Item::id).toList())) {
                recibidoPorItem.put((Long) f[0], new double[]{SoporteReportes.aDouble(f[1])});
                ultimoIngresoPorItem.put((Long) f[0], (LocalDate) f[2]);
            }
        }

        // 3. Desempeño de cada orden y de cada proveedor
        Map<Long, List<Item>> itemsPorOrden = new LinkedHashMap<>();
        items.forEach(i -> itemsPorOrden.computeIfAbsent(i.idOrden(), k -> new ArrayList<>()).add(i));
        Map<Long, Desempeno> porProveedor = new LinkedHashMap<>();
        Desempeno total = new Desempeno(SoporteReportes.TOTALES);
        long cierresManuales = 0;
        for (List<Item> deLaOrden : itemsPorOrden.values()) {
            Item primero = deLaOrden.get(0);
            Entrega entrega = entregaDe(deLaOrden, recibidoPorItem, ultimoIngresoPorItem);
            porProveedor.computeIfAbsent(primero.idProveedor(), k -> new Desempeno(primero.proveedor())).sumar(entrega);
            total.sumar(entrega);
            if (entrega.cierreManual()) {
                cierresManuales++;
            }
        }
        List<Desempeno> ranking = new ArrayList<>(porProveedor.values());
        ranking.sort(Comparator.comparing((Desempeno d) -> d.monto).reversed().thenComparing(d -> d.proveedor, String.CASE_INSENSITIVE_ORDER));

        List<DatoGraficoResponseDTO> datos = ranking.stream().filter(d -> d.completas > 0)
                .sorted(Comparator.comparingLong((Desempeno d) -> d.completas).reversed().thenComparing(d -> d.proveedor, String.CASE_INSENSITIVE_ORDER))
                .map(d -> new DatoGraficoResponseDTO(d.proveedor, d.porcentajeATiempo()))
                .toList();
        List<FilaReporteResponseDTO> tabla = ranking.stream().map(EstadisticaComprasServicioImpl::filaDeDesempeno).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Proveedores")
                .descripcion(PestanaEstadistica.PROVEEDORES.getDescripcion())
                .criterioDeFecha("Órdenes de compra no anuladas emitidas dentro del período")
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Proveedor", criterios.textoProveedor(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Tipo de insumo", criterios.tipoInsumo() == null ? null : criterios.tipoInsumo().getEtiqueta(), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("entregas-a-tiempo", "Entregas a tiempo", SoporteReportes.textoPorcentaje(total.porcentajeATiempo()),
                                total.completas == 0 ? "Sin órdenes completas" : total.aTiempo + " de " + total.completas + (total.completas == 1 ? " orden completa" : " órdenes completas"),
                                AYUDA_A_TIEMPO),
                        new IndicadorResponseDTO("plazo-promedio", "Plazo promedio de entrega",
                                total.plazoPromedio() == null ? CeldaReporteResponseDTO.SIN_DATO : FormatoCantidad.conUnidad(total.plazoPromedio(), "días"),
                                "De las órdenes completas", AYUDA_PLAZO),
                        new IndicadorResponseDTO("recibido-sobre-pedido", "Recibido sobre lo pedido", SoporteReportes.textoPorcentaje(total.porcentajeRecibido()),
                                "En presentaciones", "Presentaciones recibidas (sin pasar de lo pedido en cada ítem) sobre las pedidas, de las órdenes no anuladas del período"),
                        new IndicadorResponseDTO("cierres-manuales", "Cierres manuales", FormatoCantidad.formatear(cierresManuales),
                                "Órdenes finalizadas a mano", "Órdenes finalizadas por el gerente de compras con un motivo, sin esperar a recibir todo lo pedido")))
                .grafico(new GraficoReporteResponseDTO("Entregas a tiempo por proveedor", SoporteReportes.principales(datos, null), "%"))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Proveedor", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Órdenes", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Monto", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("A tiempo", TipoColumna.PORCENTAJE),
                        ColumnaReporteResponseDTO.de("Plazo promedio (días)", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Recibido", TipoColumna.PORCENTAJE)),
                        SoporteReportes.paginar(tabla, pageable),
                        filaDeDesempeno(total),
                        "proveedor|proveedores"))
                .build();
    }

    private static FilaReporteResponseDTO filaDeDesempeno(Desempeno d) {
        return new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(d.proveedor),
                CeldaReporteResponseDTO.numero(d.ordenes),
                CeldaReporteResponseDTO.moneda(d.monto),
                CeldaReporteResponseDTO.porcentaje(d.porcentajeATiempo()),
                CeldaReporteResponseDTO.numero(d.plazoPromedio()),
                CeldaReporteResponseDTO.porcentaje(d.porcentajeRecibido())));
    }

    /**
     * El desempeño de una orden: lo pedido y lo recibido en presentaciones (sin pasar de lo pedido por ítem), si está
     * completa, si llegó a tiempo y su plazo de entrega.
     */
    private static Entrega entregaDe(List<Item> items, Map<Long, double[]> recibidoPorItem, Map<Long, LocalDate> ultimoIngresoPorItem) {
        Item orden = items.get(0);
        BigDecimal monto = SoporteReportes.aImporte(null);
        double pedidas = 0;
        double recibidas = 0;
        boolean completa = true;
        LocalDate ultimoIngreso = null;
        for (Item item : items) {
            monto = monto.add(item.subtotal());
            double recibidoDelItem = recibidoPorItem.getOrDefault(item.id(), new double[]{0})[0];
            double presentaciones = item.cantidadPorPresentacion() > 0 ? Redondeo.redondear(recibidoDelItem / item.cantidadPorPresentacion()) : 0;
            pedidas += item.cantidad();
            recibidas += Math.min(presentaciones, item.cantidad());
            completa &= presentaciones >= item.cantidad();
            LocalDate ultimo = ultimoIngresoPorItem.get(item.id());
            if (ultimo != null && (ultimoIngreso == null || ultimo.isAfter(ultimoIngreso))) {
                ultimoIngreso = ultimo;
            }
        }
        completa &= ultimoIngreso != null;
        boolean aTiempo = completa && orden.entregaEstimada() != null && !ultimoIngreso.isAfter(orden.entregaEstimada());
        Long plazo = completa ? Math.max(0L, ChronoUnit.DAYS.between(orden.emision().toLocalDate(), ultimoIngreso)) : null;
        boolean cierreManual = orden.estado() == EstadoSolicitud.FINALIZADA && orden.motivoFinalizacion() != null && !orden.motivoFinalizacion().isBlank();
        return new Entrega(monto, pedidas, recibidas, completa, aTiempo, plazo, cierreManual);
    }

    // ==================== Precios ====================

    /** La evolución del precio de un insumo en el período. */
    private record PrecioInsumo(String insumo, UnidadDeMedida unidad, BigDecimal minimo, BigDecimal maximo, BigDecimal promedio,
                                BigDecimal primero, BigDecimal ultimo, Double variacion) {
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDePrecios(CriteriosReportePrecios criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Ítems de las órdenes no anuladas, por insumo y en orden de emisión
        Map<Long, List<Item>> itemsPorInsumo = new LinkedHashMap<>();
        for (Item item : buscarItems(periodo, criterios.idProveedor(), criterios.tipoInsumo())) {
            if (item.precioPorUnidad() != null) {
                itemsPorInsumo.computeIfAbsent(item.idInsumo(), k -> new ArrayList<>()).add(item);
            }
        }

        // 3. Precio por unidad del insumo: mínimo, máximo, promedio ponderado y variación del primero al último, calculados con el
        //    precio sin redondear (se redondea solo para mostrarlo)
        List<PrecioInsumo> precios = new ArrayList<>();
        for (List<Item> compras : itemsPorInsumo.values()) {
            BigDecimal minimo = compras.stream().map(Item::precioPorUnidad).min(Comparator.naturalOrder()).orElseThrow();
            BigDecimal maximo = compras.stream().map(Item::precioPorUnidad).max(Comparator.naturalOrder()).orElseThrow();
            double cantidad = compras.stream().mapToDouble(Item::cantidadDeInsumo).sum();
            BigDecimal importe = compras.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promedio = SoporteReportes.dividirConPrecision(importe, cantidad);
            BigDecimal primero = compras.get(0).precioPorUnidad();
            BigDecimal ultimo = compras.get(compras.size() - 1).precioPorUnidad();
            Double variacion = compras.size() > 1 && primero.signum() > 0
                    ? SoporteReportes.redondear(ultimo.subtract(primero).multiply(BigDecimal.valueOf(100))
                            .divide(primero, SoporteReportes.ESCALA_PRECIO, RoundingMode.HALF_UP).doubleValue())
                    : null;
            precios.add(new PrecioInsumo(compras.get(0).insumo(), compras.get(0).unidadInsumo(), minimo, maximo, promedio, primero, ultimo, variacion));
        }
        precios.sort(Comparator.comparing(PrecioInsumo::insumo, String.CASE_INSENSITIVE_ORDER));

        List<PrecioInsumo> conVariacion = precios.stream().filter(p -> p.variacion() != null).toList();
        long conSuba = conVariacion.stream().filter(p -> p.variacion() > 0).count();
        PrecioInsumo mayorSuba = conVariacion.stream().filter(p -> p.variacion() > 0).max(Comparator.comparingDouble(PrecioInsumo::variacion)).orElse(null);
        PrecioInsumo mayorBaja = conVariacion.stream().filter(p -> p.variacion() < 0).min(Comparator.comparingDouble(PrecioInsumo::variacion)).orElse(null);

        List<DatoGraficoResponseDTO> datos = conVariacion.stream()
                .sorted(Comparator.comparingDouble((PrecioInsumo p) -> Math.abs(p.variacion())).reversed().thenComparing(PrecioInsumo::insumo))
                .map(p -> new DatoGraficoResponseDTO(p.insumo(), p.variacion()))
                .toList();
        List<FilaReporteResponseDTO> tabla = precios.stream().map(p -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(p.insumo(), "Por " + p.unidad().getSimbolo()),
                CeldaReporteResponseDTO.moneda(SoporteReportes.paraMostrar(p.minimo())),
                CeldaReporteResponseDTO.moneda(SoporteReportes.paraMostrar(p.maximo())),
                CeldaReporteResponseDTO.moneda(SoporteReportes.paraMostrar(p.promedio())),
                CeldaReporteResponseDTO.texto(SoporteReportes.textoImporte(SoporteReportes.paraMostrar(p.primero())) + " → "
                        + SoporteReportes.textoImporte(SoporteReportes.paraMostrar(p.ultimo()))),
                CeldaReporteResponseDTO.variacion(p.variacion())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Precios")
                .descripcion(PestanaEstadistica.PRECIOS.getDescripcion())
                .criterioDeFecha("Ítems de órdenes de compra no anuladas emitidas dentro del período")
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Tipo de insumo", criterios.tipoInsumo() == null ? null : criterios.tipoInsumo().getEtiqueta(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Proveedor", criterios.textoProveedor(), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("insumos-comprados", "Insumos comprados", FormatoCantidad.formatear(precios.size()), null, null),
                        new IndicadorResponseDTO("insumos-con-suba", "Insumos con suba", FormatoCantidad.formatear(conSuba),
                                "De " + conVariacion.size() + " con más de una compra",
                                "Insumos cuyo último precio del período es mayor que el primero, por fecha de emisión de la orden"),
                        new IndicadorResponseDTO("mayor-suba", "Mayor suba", mayorSuba == null ? CeldaReporteResponseDTO.SIN_DATO : SoporteReportes.textoVariacion(mayorSuba.variacion()),
                                mayorSuba == null ? null : mayorSuba.insumo(), null),
                        new IndicadorResponseDTO("mayor-baja", "Mayor baja", mayorBaja == null ? CeldaReporteResponseDTO.SIN_DATO : SoporteReportes.textoVariacion(mayorBaja.variacion()),
                                mayorBaja == null ? null : mayorBaja.insumo(), null)))
                .grafico(GraficoReporteResponseDTO.variacion("Variación del precio por insumo", SoporteReportes.principales(datos, null)))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Insumo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Precio mínimo", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Precio máximo", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Precio promedio", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Primer → último precio", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Variación", TipoColumna.PORCENTAJE)),
                        SoporteReportes.paginar(tabla, pageable), null, "insumo|insumos"))
                .build();
    }

    // ==================== Recepción ====================

    /** Un ingreso del reporte "Recepción". */
    private record Ingreso(Long id, LocalDate fecha, String insumo, UnidadDeMedida unidad, double cantidad, BigDecimal valor,
                           TipoIngreso tipo, Long idOrden, String proveedor) {
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeRecepcion(CriteriosReporteRecepcion criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Ingresos registrados del período, del más reciente al más antiguo
        List<Ingreso> ingresos = estadisticaComprasRepository.buscarIngresos(periodo.desde(), periodo.hasta(), criterios.idProveedor(), criterios.tipoIngreso())
                .stream()
                .map(f -> {
                    double cantidad = SoporteReportes.aDouble(f[4]);
                    BigDecimal valor = SoporteReportes.aImporte(f[5]).multiply(BigDecimal.valueOf(cantidad)).setScale(SoporteReportes.ESCALA_IMPORTE, RoundingMode.HALF_UP);
                    return new Ingreso((Long) f[0], (LocalDate) f[1], (String) f[2], (UnidadDeMedida) f[3], cantidad, valor,
                            (TipoIngreso) f[6], (Long) f[7], f[8] == null ? SIN_ORDEN : (String) f[8]);
                })
                .toList();

        // 3. Lo pendiente de hoy, sin depender del período
        Object[] pendientes = estadisticaComprasRepository.contarOrdenesPendientes(LocalDate.now(), criterios.idProveedor()).stream()
                .findFirst().orElse(new Object[]{0L, 0L});
        long conSaldo = SoporteReportes.aLong(pendientes[0]);
        long vencidas = SoporteReportes.aLong(pendientes[1]);

        BigDecimal total = ingresos.stream().map(Ingreso::valor).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        Map<String, String> nombres = new LinkedHashMap<>();
        Map<String, BigDecimal> valorPorProveedor = new HashMap<>();
        for (Ingreso ingreso : ingresos) {
            nombres.putIfAbsent(ingreso.proveedor(), ingreso.proveedor());
            valorPorProveedor.merge(ingreso.proveedor(), ingreso.valor(), BigDecimal::add);
        }
        List<DatoGraficoResponseDTO> datos = ordenarDeMayorAMenor(nombres, valorPorProveedor);

        List<FilaReporteResponseDTO> tabla = ingresos.stream().map(i -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.fecha(i.fecha()),
                CeldaReporteResponseDTO.texto(i.insumo()),
                CeldaReporteResponseDTO.numero(SoporteReportes.redondear(i.cantidad()), i.unidad().getSimbolo()),
                CeldaReporteResponseDTO.moneda(i.valor()),
                CeldaReporteResponseDTO.texto(ETIQUETAS_TIPO_INGRESO.get(i.tipo())),
                CeldaReporteResponseDTO.enlace(i.idOrden(), i.idOrden() == null ? null : "Ver la orden de compra N.º " + i.idOrden())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Recepción")
                .descripcion(PestanaEstadistica.RECEPCION.getDescripcion())
                .criterioDeFecha("Ingresos con fecha de ingreso dentro del período")
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Proveedor", criterios.textoProveedor(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Tipo de ingreso", criterios.tipoIngreso() == null ? null : ETIQUETAS_TIPO_INGRESO.get(criterios.tipoIngreso()), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("ingresos-registrados", "Ingresos registrados", FormatoCantidad.formatear(ingresos.size()), null, null),
                        new IndicadorResponseDTO("valor-recibido", "Valor recibido", SoporteReportes.textoImporte(total), "Cantidad por costo unitario", null),
                        new IndicadorResponseDTO("ordenes-con-saldo", "Órdenes con saldo pendiente", FormatoCantidad.formatear(conSaldo), "Al día de hoy",
                                "Órdenes de compra pendientes hoy, con algo por recibir; no dependen del período"),
                        new IndicadorResponseDTO("ordenes-vencidas", "Órdenes con entrega vencida", FormatoCantidad.formatear(vencidas), "Al día de hoy",
                                "Órdenes de compra pendientes hoy cuya fecha de entrega estimada ya pasó; no dependen del período")))
                .grafico(GraficoReporteResponseDTO.moneda("Valor recibido por proveedor", SoporteReportes.principales(datos, "Otros"), null))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Fecha", TipoColumna.FECHA),
                        ColumnaReporteResponseDTO.de("Insumo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Cantidad", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Valor", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Tipo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.ver("Orden", "/ordenes-compra/{id}", Permiso.ORDEN_COMPRA_CONSULTAR.name())),
                        SoporteReportes.paginar(tabla, pageable),
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto(SoporteReportes.TOTALES),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.moneda(total),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia())),
                        "ingreso|ingresos"))
                .build();
    }

    // ==================== Apoyo ====================

    /** Los ítems de las órdenes no anuladas del período, con la cantidad de insumo por presentación ya convertida. */
    private List<Item> buscarItems(PeriodoEstadistica periodo, Long idProveedor, TipoInsumo tipoInsumo) {
        return estadisticaComprasRepository.buscarItemsDeOrdenesNoAnuladas(periodo.inicio(), periodo.finExclusivo(), idProveedor,
                        tipoInsumo == null ? null : tipoInsumo.getEntityClass()).stream()
                .map(f -> new Item((Long) f[0], (Long) f[1], (LocalDateTime) f[2], (LocalDate) f[3], (EstadoSolicitud) f[4], (String) f[5],
                        (Long) f[6], (String) f[7], (Long) f[8], (String) f[9], (UnidadDeMedida) f[10], ((Number) f[11]).intValue(),
                        SoporteReportes.aImporte(f[12]),
                        CantidadesDetalleCompra.calcularCantidadPorPresentacion(SoporteReportes.aDouble(f[13]), (UnidadDeMedida) f[14], (UnidadDeMedida) f[10])))
                .toList();
    }

    /** Las categorías con su importe, de mayor a menor y, ante igual importe, por nombre. */
    private static <K> List<DatoGraficoResponseDTO> ordenarDeMayorAMenor(Map<K, String> nombres, Map<K, BigDecimal> importes) {
        return nombres.entrySet().stream()
                .map(e -> new DatoGraficoResponseDTO(e.getValue(), importes.get(e.getKey()).doubleValue()))
                .sorted(Comparator.comparingDouble(DatoGraficoResponseDTO::getValor).reversed()
                        .thenComparing(DatoGraficoResponseDTO::getEtiqueta, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
