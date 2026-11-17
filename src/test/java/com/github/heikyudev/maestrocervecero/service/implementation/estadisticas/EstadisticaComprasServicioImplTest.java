package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.IEstadisticaComprasRepository;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCompras;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReportePrecios;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteProveedores;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteRecepcion;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.CeldaReporteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.DatoGraficoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.IndicadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.TipoColumna;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reportes "Compras", "Proveedores", "Precios" y "Recepción" de las estadísticas de compras.
 */
@ExtendWith(MockitoExtension.class)
class EstadisticaComprasServicioImplTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);
    private static final LocalDateTime INICIO = DESDE.atStartOfDay();
    private static final LocalDateTime FIN = HASTA.plusDays(1).atStartOfDay();
    private static final Pageable PAGINA = PageRequest.of(0, 20);

    @Mock
    private IEstadisticaComprasRepository repository;

    @InjectMocks
    private EstadisticaComprasServicioImpl servicio;

    private static String pesos(String importe) {
        return FormatoImporte.conSimbolo(new BigDecimal(importe));
    }

    private static List<String> valores(ReporteEstadisticoResponseDTO reporte) {
        return reporte.getIndicadores().stream().map(IndicadorResponseDTO::getValor).toList();
    }

    private static List<String> textos(List<CeldaReporteResponseDTO> celdas) {
        return celdas.stream().map(CeldaReporteResponseDTO::getTexto).toList();
    }

    @Test
    @DisplayName("CP-ESC-01: un período inválido se rechaza en los cuatro reportes con el mensaje del período y sin consultar nada")
    void periodoInvalido() {
        assertThatThrownBy(() -> servicio.generarReporteDeCompras(new CriteriosReporteCompras(null, HASTA, null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_DESDE_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDeProveedores(new CriteriosReporteProveedores(DESDE, null, null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_HASTA_OBLIGATORIA);
        assertThatThrownBy(() -> servicio.generarReporteDePrecios(new CriteriosReportePrecios(HASTA, DESDE, null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_INVERTIDO);
        assertThatThrownBy(() -> servicio.generarReporteDeRecepcion(new CriteriosReporteRecepcion(DESDE, DESDE.plusDays(366), null, null, null), PAGINA))
                .isInstanceOf(ReglaNegocioException.class).hasMessage(PeriodoEstadistica.MENSAJE_PERIODO_EXCEDIDO);
        verifyNoInteractions(repository);
    }

    // ==================== Compras ====================

    private static Object[] orden(long id, LocalDateTime emision, long idProveedor, String proveedor, EstadoSolicitud estado, long items, String monto) {
        return new Object[]{id, emision, idProveedor, proveedor, estado, items, new BigDecimal(monto)};
    }

    @Test
    @DisplayName("CP-ESC-02: Compras suma el monto de las órdenes no anuladas, cuenta las emitidas, calcula el monto promedio y el porcentaje de anuladas, y arma la tabla de la más reciente a la más antigua")
    void compras_indicadoresYTabla() {
        when(repository.buscarOrdenes(INICIO, FIN, null, null)).thenReturn(List.of(
                orden(4, LocalDateTime.of(2026, 11, 20, 9, 0), 2, "Lupulera", EstadoSolicitud.ANULADA, 1, "999"),
                orden(3, LocalDateTime.of(2026, 11, 10, 9, 0), 1, "Maltería", EstadoSolicitud.PENDIENTE, 3, "300"),
                orden(2, LocalDateTime.of(2026, 11, 5, 9, 0), 2, "Lupulera", EstadoSolicitud.FINALIZADA, 2, "100"),
                orden(1, LocalDateTime.of(2026, 11, 2, 9, 0), 1, "Maltería", EstadoSolicitud.FINALIZADA, 1, "100")));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCompras(new CriteriosReporteCompras(DESDE, HASTA, null, null, null), PAGINA);
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Órdenes de compra emitidas dentro del período");

        assertThat(reporte.getDescripcion()).isEqualTo("Cuánto se compró y a qué proveedores");
        assertThat(valores(reporte)).containsExactly(pesos("500"), "4", pesos("166.6667"), "25 %");
        assertThat(reporte.getIndicadores().get(3).getAclaracion()).isEqualTo("1 de 4");
        assertThat(reporte.getCriterios()).extracting("etiqueta", "valor").containsExactly(tuple("Proveedor", "Todos"), tuple("Estado", "Todos"));
        assertThat(textos(reporte.getTabla().getFilas().getContent().get(0).getCeldas()))
                .containsExactly("N.º 4", "20/11/2026", "Lupulera", "Anulada", "1", pesos("999"));
        assertThat(textos(reporte.getTabla().getTotales().getCeldas())).containsExactly("Total sin anuladas", "", "", "", "", pesos("500"));
        // El gráfico solo suma las órdenes no anuladas: Maltería 400 y Lupulera 100
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Maltería", 400.0), tuple("Lupulera", 100.0));
        assertThat(reporte.getGrafico().isEnMoneda()).isTrue();
    }

    @Test
    @DisplayName("CP-ESC-03: Compras sin órdenes muestra ceros y guiones: no divide por cero y no lleva totales")
    void compras_vacio() {
        when(repository.buscarOrdenes(INICIO, FIN, 5L, EstadoSolicitud.PENDIENTE)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeCompras(
                new CriteriosReporteCompras(DESDE, HASTA, 5L, "Maltería (30-12345678-9)", EstadoSolicitud.PENDIENTE), PAGINA);

        assertThat(valores(reporte)).containsExactly(pesos("0"), "0", "-", "-");
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Maltería (30-12345678-9)", "Pendiente");
    }

    @Test
    @DisplayName("CP-ESC-04: el gráfico de Compras muestra los ocho proveedores de mayor monto y suma el resto en 'Otros'")
    void compras_graficoConOtros() {
        List<Object[]> ordenes = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            ordenes.add(orden(i, LocalDateTime.of(2026, 11, 2, 9, 0), i, "Proveedor " + i, EstadoSolicitud.PENDIENTE, 1, String.valueOf(i * 10)));
        }
        when(repository.buscarOrdenes(INICIO, FIN, null, null)).thenReturn(ordenes);

        List<DatoGraficoResponseDTO> datos = servicio.generarReporteDeCompras(new CriteriosReporteCompras(DESDE, HASTA, null, null, null), PAGINA).getGrafico().getDatos();

        assertThat(datos).hasSize(9);
        assertThat(datos.get(0).getEtiqueta()).isEqualTo("Proveedor 10");
        assertThat(datos.get(8)).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor).containsExactly("Otros", 30.0);
    }

    // ==================== Proveedores y Precios ====================

    /** Un ítem de 25 kg por presentación de un insumo medido en kilogramos. */
    private static Object[] item(long id, long idOrden, LocalDateTime emision, LocalDate entrega, EstadoSolicitud estado, String motivo,
                                 long idProveedor, String proveedor, long idInsumo, String insumo, int cantidad, String costo) {
        return new Object[]{id, idOrden, emision, entrega, estado, motivo, idProveedor, proveedor, idInsumo, insumo, UnidadDeMedida.KILOGRAMO,
                cantidad, new BigDecimal(costo), 25.0, UnidadDeMedida.KILOGRAMO};
    }

    @Test
    @DisplayName("CP-ESC-05: Proveedores calcula las entregas a tiempo y fuera de plazo (complementarias sobre las órdenes completas) y, por proveedor, el plazo promedio y lo recibido sobre lo pedido")
    void proveedores_indicadoresYTabla() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, null, null)).thenReturn(List.of(
                // Orden 1 de Maltería: pidió 10 presentaciones, llegaron 250 kg (10) el 8/11, estimada el 10/11: a tiempo, plazo 7 días
                item(1, 1, LocalDateTime.of(2026, 11, 1, 9, 0), LocalDate.of(2026, 11, 10), EstadoSolicitud.FINALIZADA, null, 1, "Maltería", 1, "Pilsen", 10, "250"),
                // Orden 2 de Maltería: pidió 4, llegaron 100 kg (4) el 9/11, estimada el 5/11: tarde, plazo 6 días
                item(2, 2, LocalDateTime.of(2026, 11, 3, 9, 0), LocalDate.of(2026, 11, 5), EstadoSolicitud.FINALIZADA, null, 1, "Maltería", 1, "Pilsen", 4, "250"),
                // Orden 3 de Lupulera: pidió 10, llegaron 50 kg (2), cerrada a mano: no está completa
                item(3, 3, LocalDateTime.of(2026, 11, 4, 9, 0), LocalDate.of(2026, 11, 20), EstadoSolicitud.FINALIZADA, "Proveedor sin stock", 2, "Lupulera", 2, "Cascade", 10, "500")));
        when(repository.resumirIngresosPorDetalle(List.of(1L, 2L, 3L))).thenReturn(List.of(
                new Object[]{1L, 250.0, LocalDate.of(2026, 11, 8)},
                new Object[]{2L, 100.0, LocalDate.of(2026, 11, 9)},
                new Object[]{3L, 50.0, LocalDate.of(2026, 11, 12)}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeProveedores(new CriteriosReporteProveedores(DESDE, HASTA, null, null, null), PAGINA);
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Órdenes de compra no anuladas emitidas dentro del período");

        assertThat(valores(reporte)).containsExactly("50 %", "50 %");
        assertThat(reporte.getIndicadores()).extracting(IndicadorResponseDTO::getNombre).containsExactly("Entregas a tiempo", "Entregas fuera de plazo");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("1 de 2 órdenes completas");
        assertThat(reporte.getIndicadores().get(1).getAyuda()).isEqualTo(EstadisticaComprasServicioImpl.AYUDA_FUERA_DE_PLAZO);
        assertThat(reporte.getIndicadores().get(0).getAclaracion()).isEqualTo("1 de 2 órdenes completas");
        assertThat(reporte.getIndicadores().get(0).getAyuda()).isEqualTo(EstadisticaComprasServicioImpl.AYUDA_A_TIEMPO);
        assertThat(reporte.getTabla().getColumnas()).hasSize(6);
        assertThat(reporte.getTabla().getFilas().getContent()).extracting(f -> textos(f.getCeldas()))
                .containsExactly(
                        List.of("Lupulera", "1", pesos("5000"), "-", "-", "20 %"),
                        List.of("Maltería", "2", pesos("3500"), "50 %", "6,5", "100 %"));
        assertThat(textos(reporte.getTabla().getTotales().getCeldas())).containsExactly("Totales del período", "3", pesos("8500"), "50 %", "6,5", "66,67 %");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Maltería", 50.0));
    }

    @Test
    @DisplayName("CP-ESC-06: Proveedores sin órdenes completas muestra guiones, y con el tipo de insumo elegido consulta por su clase")
    void proveedores_sinEntregasCompletas() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, 5L, MaltaEntity.class)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeProveedores(
                new CriteriosReporteProveedores(DESDE, HASTA, 5L, "Maltería", TipoInsumo.MALTA), PAGINA);

        assertThat(valores(reporte)).containsExactly("-", "-");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("Sin órdenes completas");
        assertThat(reporte.getIndicadores().get(0).getAclaracion()).isEqualTo("Sin órdenes completas");
        assertThat(reporte.getTabla().getFilas().getContent()).isEmpty();
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Maltería", "Malta");
    }

    @Test
    @DisplayName("CP-ESC-07: una orden sin ingresos o con ítems sin recibir por completo no cuenta como completa y su fecha de entrega vencida no suma a tiempo")
    void proveedores_ordenSinIngresos() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, null, null)).thenReturn(List.<Object[]>of(
                item(1, 1, LocalDateTime.of(2026, 11, 1, 9, 0), LocalDate.of(2026, 11, 10), EstadoSolicitud.PENDIENTE, null, 1, "Maltería", 1, "Pilsen", 10, "250")));
        when(repository.resumirIngresosPorDetalle(List.of(1L))).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeProveedores(new CriteriosReporteProveedores(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte)).containsExactly("-", "-");
    }

    @Test
    @DisplayName("CP-ESC-08: Precios compara el precio por unidad del insumo (no por presentación): mínimo, máximo, promedio ponderado, primer y último precio y variación; con una sola compra la variación es un guion")
    void precios_indicadoresYTabla() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, null, null)).thenReturn(List.of(
                // Pilsen: 250 por 25 kg = 10 por kg, luego 300 = 12 por kg (+20 %)
                item(1, 1, LocalDateTime.of(2026, 11, 1, 9, 0), LocalDate.of(2026, 11, 10), EstadoSolicitud.PENDIENTE, null, 1, "Maltería", 1, "Pilsen", 10, "250"),
                item(2, 2, LocalDateTime.of(2026, 11, 5, 9, 0), LocalDate.of(2026, 11, 10), EstadoSolicitud.PENDIENTE, null, 1, "Maltería", 1, "Pilsen", 30, "300"),
                // Cascade: 500 = 20 por kg, luego 400 = 16 por kg (-20 %)
                item(3, 1, LocalDateTime.of(2026, 11, 1, 9, 0), LocalDate.of(2026, 11, 10), EstadoSolicitud.PENDIENTE, null, 2, "Lupulera", 2, "Cascade", 1, "500"),
                item(4, 3, LocalDateTime.of(2026, 11, 8, 9, 0), LocalDate.of(2026, 11, 20), EstadoSolicitud.PENDIENTE, null, 2, "Lupulera", 2, "Cascade", 1, "400"),
                // Mosaic: una sola compra
                item(5, 4, LocalDateTime.of(2026, 11, 9, 9, 0), LocalDate.of(2026, 11, 20), EstadoSolicitud.PENDIENTE, null, 2, "Lupulera", 3, "Mosaic", 1, "250")));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDePrecios(new CriteriosReportePrecios(DESDE, HASTA, null, null, null), PAGINA);
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Ítems de órdenes de compra no anuladas emitidas dentro del período");

        assertThat(valores(reporte)).containsExactly("3", "1", "1");
        assertThat(reporte.getIndicadores().get(1).getAclaracion()).isEqualTo("De 2 con más de una compra");
        assertThat(reporte.getIndicadores()).extracting(IndicadorResponseDTO::getNombre).containsExactly("Insumos comprados", "Insumos con suba", "Insumos con baja");
        assertThat(reporte.getIndicadores().get(2).getAclaracion()).isEqualTo("De 2 con más de una compra");
        assertThat(reporte.getIndicadores().get(2).getAyuda()).isNotBlank();
        // Pilsen: 10 por kg × 250 kg y 12 por kg × 750 kg: promedio ponderado (2.500 + 9.000) / 1.000 = 11,5
        assertThat(reporte.getTabla().getFilas().getContent()).extracting(f -> textos(f.getCeldas()))
                .containsExactly(
                        List.of("Cascade", pesos("16"), pesos("20"), pesos("18"), pesos("20") + " → " + pesos("16"), "-20 %"),
                        List.of("Mosaic", pesos("10"), pesos("10"), pesos("10"), pesos("10") + " → " + pesos("10"), "-"),
                        List.of("Pilsen", pesos("10"), pesos("12"), pesos("11.5"), pesos("10") + " → " + pesos("12"), "+20 %"));
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas().get(0).getDetalle()).isEqualTo("Por kg");
        assertThat(reporte.getTabla().getColumnas().get(5).getTipo()).isEqualTo(TipoColumna.PORCENTAJE);
        assertThat(reporte.getGrafico().isConSigno()).isTrue();
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Cascade", -20.0), tuple("Pilsen", 20.0));
    }

    @Test
    @DisplayName("CP-ESC-12: Precios no redondea antes de calcular: un insumo en gramos comprado en bolsas de 25 kg a 12 y a 13 varía +8,33 %, no 0 %")
    void precios_noRedondeaElPrecioPorUnidad() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, null, null)).thenReturn(List.of(
                itemEnGramos(1, 1, LocalDateTime.of(2026, 11, 1, 9, 0), "12"),
                itemEnGramos(2, 2, LocalDateTime.of(2026, 11, 5, 9, 0), "13")));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDePrecios(new CriteriosReportePrecios(DESDE, HASTA, null, null, null), PAGINA);

        assertThat(valores(reporte)).containsExactly("1", "1", "0");
        assertThat(reporte.getTabla().getFilas().getContent().get(0).getCeldas().get(5).getTexto()).isEqualTo("+8,33 %");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getValor).containsExactly(8.33);
    }

    /** Una bolsa de 25 kg de un insumo medido en gramos: 12 por bolsa son 0,00048 por gramo. */
    private static Object[] itemEnGramos(long id, long idOrden, LocalDateTime emision, String costo) {
        return new Object[]{id, idOrden, emision, LocalDate.of(2026, 11, 10), EstadoSolicitud.PENDIENTE, null, 1L, "Maltería", 1L, "Pilsen",
                UnidadDeMedida.GRAMO, 1, new BigDecimal(costo), 25.0, UnidadDeMedida.KILOGRAMO};
    }

    @Test
    @DisplayName("CP-ESC-09: Precios sin compras muestra ceros y guiones y no lleva totales")
    void precios_vacio() {
        when(repository.buscarItemsDeOrdenesNoAnuladas(INICIO, FIN, 5L, MaltaEntity.class)).thenReturn(List.of());

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDePrecios(new CriteriosReportePrecios(DESDE, HASTA, TipoInsumo.MALTA, 5L, "Maltería"), PAGINA);

        assertThat(valores(reporte)).containsExactly("0", "0", "0");
        assertThat(reporte.getTabla().getFilas().getContent()).isEmpty();
        assertThat(reporte.getTabla().getTotales()).isNull();
    }

    // ==================== Recepción ====================

    private static Object[] ingreso(long id, LocalDate fecha, String insumo, double cantidad, String costo, TipoIngreso tipo, Long idOrden, String proveedor) {
        return new Object[]{id, fecha, insumo, UnidadDeMedida.KILOGRAMO, cantidad, new BigDecimal(costo), tipo, idOrden, proveedor};
    }

    @Test
    @DisplayName("CP-ESC-10: Recepción calcula el valor recibido (cantidad por costo unitario), arma la tabla con 'Ver' la orden solo en los ingresos por compra y agrupa los directos como 'Sin orden de compra'; lo pendiente es de hoy")
    void recepcion_indicadoresYTabla() {
        when(repository.buscarIngresos(DESDE, HASTA, null, null)).thenReturn(List.of(
                ingreso(2, LocalDate.of(2026, 11, 8), "Cascade", 10, "3", TipoIngreso.DIRECTO, null, null),
                ingreso(1, LocalDate.of(2026, 11, 2), "Pilsen", 100, "2.5", TipoIngreso.COMPRA, 5L, "Maltería")));
        when(repository.contarOrdenesPendientes(any(LocalDate.class), any())).thenReturn(List.<Object[]>of(new Object[]{4L, 1L}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeRecepcion(new CriteriosReporteRecepcion(DESDE, HASTA, null, null, null), PAGINA);
        assertThat(reporte.getCriterioDeFecha()).isEqualTo("Ingresos con fecha de ingreso dentro del período");

        assertThat(valores(reporte)).containsExactly("2", pesos("280"), "4", "1");
        assertThat(reporte.getTabla().getColumnas().get(5).isVer()).isTrue();
        assertThat(reporte.getTabla().getColumnas().get(5).getPermiso()).isEqualTo("ORDEN_COMPRA_CONSULTAR");
        assertThat(reporte.getTabla().getColumnas().get(5).getRuta()).isEqualTo("/ordenes-compra/{id}");
        List<CeldaReporteResponseDTO> primera = reporte.getTabla().getFilas().getContent().get(0).getCeldas();
        assertThat(textos(primera).subList(0, 5)).containsExactly("08/11/2026", "Cascade", "10 kg", pesos("30"), "Directo");
        assertThat(primera.get(5).getId()).isNull();
        CeldaReporteResponseDTO conOrden = reporte.getTabla().getFilas().getContent().get(1).getCeldas().get(5);
        assertThat(conOrden.getId()).isEqualTo(5L);
        assertThat(textos(reporte.getTabla().getTotales().getCeldas())).containsExactly("Totales del período", "", "", pesos("280"), "", "");
        assertThat(reporte.getGrafico().getDatos()).extracting(DatoGraficoResponseDTO::getEtiqueta, DatoGraficoResponseDTO::getValor)
                .containsExactly(tuple("Maltería", 250.0), tuple("Sin orden de compra", 30.0));
    }

    @Test
    @DisplayName("CP-ESC-11: Recepción sin ingresos muestra ceros y mantiene los pendientes de hoy; sin filas de pendientes (nulas) cuenta cero")
    void recepcion_vacio() {
        when(repository.buscarIngresos(DESDE, HASTA, 5L, TipoIngreso.COMPRA)).thenReturn(List.of());
        when(repository.contarOrdenesPendientes(any(LocalDate.class), any())).thenReturn(List.<Object[]>of(new Object[]{0L, null}));

        ReporteEstadisticoResponseDTO reporte = servicio.generarReporteDeRecepcion(
                new CriteriosReporteRecepcion(DESDE, HASTA, 5L, "Maltería", TipoIngreso.COMPRA), PAGINA);

        assertThat(valores(reporte)).containsExactly("0", pesos("0"), "0", "0");
        assertThat(reporte.getTabla().getTotales()).isNull();
        assertThat(reporte.getCriterios()).extracting("valor").containsExactly("Maltería", "Compra");
    }
}
