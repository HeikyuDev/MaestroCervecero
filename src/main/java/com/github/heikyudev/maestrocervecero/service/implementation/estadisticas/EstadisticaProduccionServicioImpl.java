package com.github.heikyudev.maestrocervecero.service.implementation.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.IEstadisticaProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas.LoteReporteProyeccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCostos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteInsumos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteLotes;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteTiempos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
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
import com.github.heikyudev.maestrocervecero.util.method.MetodosEstimacionEtapa;
import static com.github.heikyudev.maestrocervecero.service.implementation.estadisticas.SoporteReportes.porcentaje;
import static com.github.heikyudev.maestrocervecero.service.implementation.estadisticas.SoporteReportes.redondear;
import static com.github.heikyudev.maestrocervecero.service.implementation.estadisticas.SoporteReportes.textoPorcentaje;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación de {@link IEstadisticaProduccionServicio}. Cada reporte se calcula con consultas agrupadas o
 * proyecciones, sin cargar entidades ni consultar fila por fila: "Lotes" pagina en la base; los demás reportes arman su
 * tabla en memoria a partir de esas consultas (el universo de un período es acotado) y la paginan.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticaProduccionServicioImpl implements IEstadisticaProduccionServicio {

    static final String TITULO_LOTES = "Lotes";
    static final String AYUDA_EN_PLAZO = "Porcentaje de los lotes finalizados cuya fecha de fin real no es posterior a la fecha de fin estimada";
    static final String AYUDA_VALOR = "Cantidad consumida por el costo promedio ponderado (PPP) registrado en cada consumo";
    static final String AYUDA_LOTES_DE_PRODUCCION = "Cantidad de lotes de producción en los que se consumió el insumo en el período";
    static final String SIN_ESTIMAR = "Sin estimar";

    private final IEstadisticaProduccionRepository estadisticaProduccionRepository;
    private final IConfiguracionProduccionRepository configuracionProduccionRepository;

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeLotes(CriteriosReporteLotes criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());
        LocalDate desde = criterios.desde();
        LocalDate hasta = criterios.hasta();

        // 2. Cantidad de lotes por estado
        Map<EstadoLote, Long> lotesPorEstado = new EnumMap<>(EstadoLote.class);
        for (Object[] fila : estadisticaProduccionRepository.contarLotesPorEstado(desde, hasta, criterios.idReceta(), criterios.estado())) {
            lotesPorEstado.put((EstadoLote) fila[0], ((Number) fila[1]).longValue());
        }
        long total = lotesPorEstado.values().stream().mapToLong(Long::longValue).sum();
        long finalizados = lotesPorEstado.getOrDefault(EstadoLote.FINALIZADO, 0L);

        // 3. Litros envasados del período
        Double sumaLitros = estadisticaProduccionRepository.sumarLitrosEnvasados(desde, hasta, criterios.idReceta(), criterios.estado());
        double litrosEnvasados = redondear(sumaLitros == null ? 0.0 : sumaLitros);

        // 4. Finalizados en plazo: fecha de fin real no posterior a la estimada
        long conFechas = 0;
        long enPlazo = 0;
        for (Object[] fila : estadisticaProduccionRepository.buscarFechasDeFinalizados(desde, hasta, criterios.idReceta(), criterios.estado())) {
            LocalDate estimada = (LocalDate) fila[0];
            LocalDateTime real = (LocalDateTime) fila[1];
            if (estimada == null || real == null) {
                continue;
            }
            conFechas++;
            if (!real.toLocalDate().isAfter(estimada)) {
                enPlazo++;
            }
        }

        // 5. Página de la tabla y litros envasados de sus lotes, en una sola consulta
        Page<LoteReporteProyeccion> lotes = estadisticaProduccionRepository.buscarLotes(desde, hasta, criterios.idReceta(),
                criterios.estado(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        Map<Long, Double> litrosPorLote = litrosEnvasadosDe(lotes.getContent());
        Page<FilaReporteResponseDTO> filas = lotes.map(lote -> filaDeLote(lote, litrosPorLote.get(lote.id())));

        return ReporteEstadisticoResponseDTO.builder()
                .titulo(TITULO_LOTES)
                .descripcion(PestanaEstadistica.LOTES.getDescripcion())
                .desde(desde)
                .hasta(hasta)
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Receta", criterios.textoReceta(), "Todas"),
                        CriterioAplicadoResponseDTO.de("Estado", criterios.estado() == null ? null : criterios.estado().getEtiqueta(), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("lotes", "Lotes del período", FormatoCantidad.formatear(total),
                                "Por fecha de inicio estimada", null),
                        new IndicadorResponseDTO("finalizados", "Finalizados", FormatoCantidad.formatear(finalizados),
                                total > 0 ? textoPorcentaje(porcentaje(finalizados, total)) + " de los lotes" : null, null),
                        new IndicadorResponseDTO("litros-envasados", "Litros envasados", FormatoCantidad.conUnidad(litrosEnvasados, "L"),
                                "Envasados registrados", null),
                        new IndicadorResponseDTO("en-plazo", "Terminados en plazo",
                                conFechas > 0 ? textoPorcentaje(porcentaje(enPlazo, conFechas)) : CeldaReporteResponseDTO.SIN_DATO,
                                conFechas > 0 ? enPlazo + " de " + conFechas + (conFechas == 1 ? " finalizado" : " finalizados") : "Sin lotes finalizados",
                                AYUDA_EN_PLAZO)))
                .grafico(new GraficoReporteResponseDTO("Lotes por estado", Arrays.stream(EstadoLote.values())
                        .map(estado -> new DatoGraficoResponseDTO(estado.getEtiqueta(), lotesPorEstado.getOrDefault(estado, 0L)))
                        .toList(), null))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Lote", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Estado", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Litros envasados", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Fin estimado", TipoColumna.FECHA),
                        ColumnaReporteResponseDTO.de("Fin real", TipoColumna.FECHA),
                        ColumnaReporteResponseDTO.enlace("Acciones", "/lotes/{id}", Permiso.LOTE_CONSULTAR.name())),
                        filas,
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto("Totales del período"),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.numero(litrosEnvasados),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia())),
                        "lote|lotes"))
                .build();
    }

    // ==================== Costos ====================

    /** Un lote del reporte "Costos" con sus litros y sus costos. */
    private record CostoLote(Long id, String lote, String receta, Long idReceta, LocalDateTime finEnvasado, double litros,
                             BigDecimal insumos, BigDecimal total) {
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeCostos(CriteriosReporteCostos criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Lotes del universo y sus costos, con una consulta agrupada por concepto
        List<Object[]> filas = estadisticaProduccionRepository.buscarLotesConEnvasadoFinalizado(periodo.inicio(), periodo.finExclusivo(), criterios.idReceta());
        List<Long> ids = filas.stream().map(f -> (Long) f[0]).toList();
        Map<Long, BigDecimal> insumosPorLote = ids.isEmpty() ? Map.of() : importesPorId(estadisticaProduccionRepository.sumarCostoDeInsumosPorLote(ids));
        Map<Long, BigDecimal> adicionalesPorLote = ids.isEmpty() ? Map.of() : importesPorId(estadisticaProduccionRepository.sumarCostosAdicionalesPorLote(ids));
        List<CostoLote> lotes = new ArrayList<>();
        for (Object[] f : filas) {
            Long id = (Long) f[0];
            BigDecimal insumos = insumosPorLote.getOrDefault(id, SoporteReportes.aImporte(null));
            BigDecimal total = insumos.add(adicionalesPorLote.getOrDefault(id, SoporteReportes.aImporte(null)));
            lotes.add(new CostoLote(id, (String) f[1], (String) f[2], (Long) f[3], (LocalDateTime) f[4],
                    redondear(SoporteReportes.aDouble(f[5])), insumos, total));
        }
        lotes.sort(Comparator.comparing(CostoLote::finEnvasado).thenComparing(CostoLote::id).reversed());

        // 3. Totales del período
        double litros = redondear(lotes.stream().mapToDouble(CostoLote::litros).sum());
        BigDecimal insumos = lotes.stream().map(CostoLote::insumos).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        BigDecimal total = lotes.stream().map(CostoLote::total).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        BigDecimal porLitro = SoporteReportes.dividir(total, litros);

        // 4. Costo por litro por receta: las más caras, sin sumar el resto (un costo por litro no se suma)
        Map<Long, String> nombrePorReceta = new LinkedHashMap<>();
        Map<Long, BigDecimal> costoPorReceta = new HashMap<>();
        Map<Long, Double> litrosPorReceta = new HashMap<>();
        for (CostoLote lote : lotes) {
            nombrePorReceta.putIfAbsent(lote.idReceta(), lote.receta());
            costoPorReceta.merge(lote.idReceta(), lote.total(), BigDecimal::add);
            litrosPorReceta.merge(lote.idReceta(), lote.litros(), Double::sum);
        }
        List<DatoGraficoResponseDTO> datos = nombrePorReceta.entrySet().stream()
                .map(e -> new DatoGraficoResponseDTO(e.getValue(), SoporteReportes.dividir(costoPorReceta.get(e.getKey()), litrosPorReceta.get(e.getKey())).doubleValue()))
                .sorted(Comparator.comparingDouble(DatoGraficoResponseDTO::getValor).reversed().thenComparing(DatoGraficoResponseDTO::getEtiqueta))
                .toList();

        List<FilaReporteResponseDTO> tabla = lotes.stream().map(l -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(l.lote(), l.receta()),
                CeldaReporteResponseDTO.fecha(l.finEnvasado()),
                CeldaReporteResponseDTO.numero(l.litros()),
                CeldaReporteResponseDTO.moneda(l.insumos()),
                CeldaReporteResponseDTO.moneda(l.total()),
                CeldaReporteResponseDTO.moneda(SoporteReportes.dividir(l.total(), l.litros()))))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Costos")
                .descripcion(PestanaEstadistica.COSTOS.getDescripcion())
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(CriterioAplicadoResponseDTO.de("Receta", criterios.textoReceta(), "Todas")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("costo-total", "Costo total", SoporteReportes.textoImporte(total), "Insumos y costos adicionales", null),
                        new IndicadorResponseDTO("litros-envasados", "Litros envasados", FormatoCantidad.conUnidad(litros, "L"),
                                lotes.size() + (lotes.size() == 1 ? " lote" : " lotes"), null),
                        new IndicadorResponseDTO("costo-por-litro", "Costo por litro", SoporteReportes.textoImporte(porLitro), null,
                                "Costo total de los lotes del período dividido por sus litros envasados"),
                        new IndicadorResponseDTO("peso-insumos", "Peso de los insumos", SoporteReportes.textoPorcentaje(SoporteReportes.porcentaje(insumos, total)),
                                "Del costo total", "Porcentaje del costo total que corresponde a los insumos consumidos; el resto son costos adicionales")))
                .grafico(GraficoReporteResponseDTO.moneda("Costo por litro por receta", SoporteReportes.principales(datos, null), "L"))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Lote", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Fin de envasado", TipoColumna.FECHA),
                        ColumnaReporteResponseDTO.de("Litros", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Costo de insumos", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Costo total", TipoColumna.MONEDA),
                        ColumnaReporteResponseDTO.de("Costo por litro", TipoColumna.MONEDA)),
                        SoporteReportes.paginar(tabla, pageable),
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto(SoporteReportes.TOTALES),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.numero(litros),
                                CeldaReporteResponseDTO.moneda(insumos),
                                CeldaReporteResponseDTO.moneda(total),
                                CeldaReporteResponseDTO.moneda(porLitro))),
                        "lote|lotes"))
                .build();
    }

    // ==================== Tiempos ====================

    /** Una etapa del reporte "Tiempos" con su duración real y la estimada ({@code null} si no se puede estimar). */
    private record TiempoEtapa(String lote, String receta, TipoEtapa etapa, String equipo, long real, Long estimada) {
        /** El desvío sobre lo estimado, en porcentaje y sin redondear, o {@code null} si no se puede estimar. */
        Double desvioExacto() {
            return estimada == null || estimada <= 0 ? null : (real - estimada) * 100.0 / estimada;
        }

        /** El desvío redondeado para mostrarlo. */
        Double desvio() {
            Double exacto = desvioExacto();
            return exacto == null ? null : redondear(exacto);
        }
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeTiempos(CriteriosReporteTiempos criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Etapas finalizadas cuya fecha de finalización cae en el período (pueden haber empezado antes) y, en una consulta por dato, lo que hace falta para estimar Molienda y Envasado
        List<Object[]> filas = estadisticaProduccionRepository.buscarEtapasFinalizadas(periodo.inicio(), periodo.finExclusivo(),
                criterios.etapa(), criterios.idReceta());
        Set<Long> molinos = new HashSet<>();
        Set<Long> fermentadores = new HashSet<>();
        Set<Long> moliendas = new HashSet<>();
        for (Object[] f : filas) {
            if (f[3] == TipoEtapa.MOLIENDA) {
                molinos.add((Long) f[4]);
                moliendas.add((Long) f[0]);
            } else if (f[3] == TipoEtapa.ENVASADO) {
                fermentadores.add((Long) f[4]);
            }
        }
        Map<Long, Double> rendimientos = molinos.isEmpty() ? Map.of() : numerosPorId(estadisticaProduccionRepository.buscarRendimientosDeMolinos(molinos));
        Map<Long, Double> capacidades = fermentadores.isEmpty() ? Map.of() : numerosPorId(estadisticaProduccionRepository.buscarCapacidadesDeFermentadores(fermentadores));
        Map<Long, Double> maltaKg = new HashMap<>();
        if (!moliendas.isEmpty()) {
            for (Object[] f : estadisticaProduccionRepository.sumarMaltaConsumidaPorEtapa(moliendas)) {
                double kg = ((UnidadDeMedida) f[1]).aGramos(SoporteReportes.aDouble(f[2])) / 1000.0;
                maltaKg.merge((Long) f[0], kg, Double::sum);
            }
        }
        Double velocidad = fermentadores.isEmpty() ? null : configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .map(ConfiguracionProduccionEntity::getVelocidadEstandarEnvasado).orElse(null);

        List<TiempoEtapa> etapas = new ArrayList<>();
        for (Object[] f : filas) {
            TipoEtapa tipo = (TipoEtapa) f[3];
            Long idEquipo = (Long) f[4];
            Long estimada = switch (tipo) {
                case MOLIENDA -> MetodosEstimacionEtapa.minutosDeMolienda(maltaKg.get((Long) f[0]), primero((Double) f[12], rendimientos.get(idEquipo)));
                case MACERACION -> MetodosEstimacionEtapa.minutosDeReceta((Integer) f[8]);
                case HERVIDO -> MetodosEstimacionEtapa.minutosDeReceta((Integer) f[9]);
                case FERMENTACION -> MetodosEstimacionEtapa.minutosDeDias((Integer) f[10]);
                case MADURACION -> MetodosEstimacionEtapa.minutosDeDias((Integer) f[11]);
                case ENVASADO -> MetodosEstimacionEtapa.minutosDeEnvasado(primero((Double) f[13], capacidades.get(idEquipo)), velocidad);
            };
            long real = Math.max(0L, Duration.between((LocalDateTime) f[6], (LocalDateTime) f[7]).toMinutes());
            etapas.add(new TiempoEtapa((String) f[1], (String) f[2], tipo, (String) f[5], real, estimada));
        }

        // 3. Indicadores sobre las etapas con estimación: todos salen de los mismos valores sin redondear
        List<TiempoEtapa> estimables = etapas.stream().filter(e -> e.desvioExacto() != null).toList();
        long dentro = estimables.stream().filter(e -> e.real() <= e.estimada()).count();
        Double desvioPromedio = estimables.isEmpty() ? null : redondear(estimables.stream().mapToDouble(TiempoEtapa::desvioExacto).average().orElse(0));
        Map<TipoEtapa, Double> desvioPorEtapa = new EnumMap<>(TipoEtapa.class);
        Map<TipoEtapa, Double> proporcionPorEtapa = new EnumMap<>(TipoEtapa.class);
        for (TipoEtapa tipo : TipoEtapa.values()) {
            List<TiempoEtapa> deLaEtapa = estimables.stream().filter(e -> e.etapa() == tipo).toList();
            if (!deLaEtapa.isEmpty()) {
                desvioPorEtapa.put(tipo, deLaEtapa.stream().mapToDouble(TiempoEtapa::desvioExacto).average().orElse(0));
                proporcionPorEtapa.put(tipo, redondear(deLaEtapa.stream().mapToDouble(e -> e.real() * 100.0 / e.estimada()).average().orElse(0)));
            }
        }
        // La etapa de mayor desvío promedio; ante un empate, la primera en el orden del proceso (las etapas se recorren en ese orden)
        TipoEtapa etapaMayorDesvio = null;
        for (Map.Entry<TipoEtapa, Double> entrada : desvioPorEtapa.entrySet()) {
            if (etapaMayorDesvio == null || entrada.getValue() > desvioPorEtapa.get(etapaMayorDesvio)) {
                etapaMayorDesvio = entrada.getKey();
            }
        }

        List<FilaReporteResponseDTO> tabla = etapas.stream().map(e -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(e.lote(), e.receta()),
                CeldaReporteResponseDTO.texto(e.etapa().getEtiqueta()),
                CeldaReporteResponseDTO.texto(e.equipo()),
                CeldaReporteResponseDTO.texto(MetodosEstimacionEtapa.formatearDuracion(e.real())),
                CeldaReporteResponseDTO.texto(e.estimada() == null ? SIN_ESTIMAR : MetodosEstimacionEtapa.formatearDuracion(e.estimada())),
                CeldaReporteResponseDTO.variacion(e.desvio())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Tiempos")
                .descripcion(PestanaEstadistica.TIEMPOS.getDescripcion())
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Etapa", criterios.etapa() == null ? null : criterios.etapa().getEtiqueta(), "Todas"),
                        CriterioAplicadoResponseDTO.de("Receta", criterios.textoReceta(), "Todas")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("etapas", "Etapas finalizadas", FormatoCantidad.formatear(etapas.size()),
                                (etapas.size() - estimables.size()) + " sin estimar", null),
                        new IndicadorResponseDTO("dentro-de-lo-estimado", "Dentro de lo estimado",
                                SoporteReportes.textoPorcentaje(SoporteReportes.porcentaje(dentro, estimables.size())),
                                estimables.isEmpty() ? "Sin etapas estimadas" : dentro + " de " + estimables.size() + " estimadas",
                                "Porcentaje de las etapas con estimación cuya duración real no superó la estimada"),
                        new IndicadorResponseDTO("desvio-promedio", "Desvío promedio", SoporteReportes.textoVariacion(desvioPromedio),
                                "Sobre lo estimado", "Promedio de cuánto se apartó la duración real de la estimada, en porcentaje de la estimada (positivo si duró más)"),
                        new IndicadorResponseDTO("etapa-mayor-desvio", "Etapa con mayor desvío", etapaMayorDesvio == null ? CeldaReporteResponseDTO.SIN_DATO : etapaMayorDesvio.getEtiqueta(),
                                etapaMayorDesvio == null ? null : SoporteReportes.textoVariacion(redondear(desvioPorEtapa.get(etapaMayorDesvio))) + " en promedio", null)))
                .grafico(new GraficoReporteResponseDTO("Duración real como % de la estimada", proporcionPorEtapa.entrySet().stream()
                        .map(e -> new DatoGraficoResponseDTO(e.getKey().getEtiqueta(), e.getValue())).toList(), "%"))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Lote", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Etapa", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Equipo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Duración real", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Duración estimada", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Desvío", TipoColumna.PORCENTAJE)),
                        SoporteReportes.paginar(tabla, pageable), null, "etapa|etapas"))
                .build();
    }

    // ==================== Insumos ====================

    /** Un insumo del reporte "Insumos" con lo consumido en el período. */
    private record ConsumoInsumo(Long id, String nombre, UnidadDeMedida unidad, double cantidad, BigDecimal valor, long lotes,
                                 BigDecimal directo) {
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeInsumos(CriteriosReporteInsumos criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());

        // 2. Consumos agrupados por insumo y el tipo de cada insumo, ranking por valor
        List<ConsumoInsumo> insumos = new ArrayList<>(estadisticaProduccionRepository.resumirConsumosPorInsumo(periodo.inicio(), periodo.finExclusivo(),
                        criterios.tipoInsumo() == null ? null : criterios.tipoInsumo().getEntityClass(), criterios.idReceta()).stream()
                .map(f -> new ConsumoInsumo((Long) f[0], (String) f[1], (UnidadDeMedida) f[2], redondear(SoporteReportes.aDouble(f[3])),
                        SoporteReportes.aImporte(f[4]), SoporteReportes.aLong(f[5]), SoporteReportes.aImporte(f[6])))
                .toList());
        insumos.sort(Comparator.comparing(ConsumoInsumo::valor).reversed().thenComparing(ConsumoInsumo::nombre, String.CASE_INSENSITIVE_ORDER));
        Map<Long, String> tipos = new HashMap<>();
        if (!insumos.isEmpty()) {
            for (Object[] f : estadisticaProduccionRepository.buscarTiposDeInsumos(insumos.stream().map(ConsumoInsumo::id).toList())) {
                tipos.put((Long) f[0], etiquetaDeTipoInsumo((Class<?>) f[1]));
            }
        }

        // 3. Indicadores
        BigDecimal total = insumos.stream().map(ConsumoInsumo::valor).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        BigDecimal directo = insumos.stream().map(ConsumoInsumo::directo).reduce(SoporteReportes.aImporte(null), BigDecimal::add);
        ConsumoInsumo mayor = insumos.isEmpty() ? null : insumos.get(0);

        List<DatoGraficoResponseDTO> datos = insumos.stream().map(i -> new DatoGraficoResponseDTO(i.nombre(), i.valor().doubleValue())).toList();
        List<FilaReporteResponseDTO> tabla = insumos.stream().map(i -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(i.nombre()),
                CeldaReporteResponseDTO.texto(tipos.get(i.id())),
                CeldaReporteResponseDTO.numero(i.cantidad(), i.unidad().getSimbolo()),
                CeldaReporteResponseDTO.moneda(i.valor()),
                CeldaReporteResponseDTO.porcentaje(SoporteReportes.porcentaje(i.valor(), total)),
                CeldaReporteResponseDTO.numero(i.lotes())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Insumos")
                .descripcion(PestanaEstadistica.INSUMOS.getDescripcion())
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Tipo de insumo", criterios.tipoInsumo() == null ? null : criterios.tipoInsumo().getEtiqueta(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Receta", criterios.textoReceta(), "Todas")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("valor-consumido", "Valor consumido", SoporteReportes.textoImporte(total), "Al costo de cada consumo", AYUDA_VALOR),
                        new IndicadorResponseDTO("insumos-distintos", "Insumos distintos", FormatoCantidad.formatear(insumos.size()), null, null),
                        new IndicadorResponseDTO("insumo-mayor-valor", "Insumo de mayor valor", mayor == null ? CeldaReporteResponseDTO.SIN_DATO : mayor.nombre(),
                                mayor == null ? null : SoporteReportes.textoImporte(mayor.valor()), null),
                        new IndicadorResponseDTO("consumo-sin-reserva", "Consumo sin reserva", SoporteReportes.textoPorcentaje(SoporteReportes.porcentaje(directo, total)),
                                "Del valor consumido", "Porcentaje del valor consumido que se registró como consumo directo, sin una reserva previa")))
                .grafico(GraficoReporteResponseDTO.moneda("Insumos de mayor valor consumido", SoporteReportes.principales(datos, "Otros"), null))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Insumo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Tipo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Cantidad", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Valor", TipoColumna.MONEDA, AYUDA_VALOR),
                        ColumnaReporteResponseDTO.de("% del total", TipoColumna.PORCENTAJE),
                        ColumnaReporteResponseDTO.de("Lotes de producción", TipoColumna.NUMERO, AYUDA_LOTES_DE_PRODUCCION)),
                        SoporteReportes.paginar(tabla, pageable),
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto(SoporteReportes.TOTALES),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.vacia(),
                                CeldaReporteResponseDTO.moneda(total),
                                CeldaReporteResponseDTO.porcentaje(total.signum() > 0 ? 100 : null),
                                CeldaReporteResponseDTO.vacia())),
                        "insumo|insumos"))
                .build();
    }

    // ==================== Equipamiento ====================

    /** Un equipo del reporte "Equipamiento" con su actividad del período. */
    private record UsoEquipo(String identificador, TipoEquipamiento tipo, boolean dadoDeBaja, long etapas, long minutos,
                             long limpiezas, long fallas, long mantenimientos) {
        boolean tieneActividad() {
            return etapas > 0 || limpiezas > 0 || fallas > 0 || mantenimientos > 0;
        }

        double horas() {
            return redondear(minutos / 60.0);
        }
    }

    @Override
    public ReporteEstadisticoResponseDTO generarReporteDeEquipamiento(CriteriosReporteEquipamiento criterios, Pageable pageable) {
        // 1. Valido el período
        PeriodoEstadistica periodo = PeriodoEstadistica.validar(criterios.desde(), criterios.hasta());
        LocalDateTime inicio = periodo.inicio();
        LocalDateTime fin = periodo.finExclusivo();
        LocalDateTime ahora = LocalDateTime.now();

        // 2. Etapas con uso real del equipo superpuestas con el período (recortadas; las de un lote cancelado terminan en su cancelación) e intervenciones, una consulta agrupada por concepto
        Map<Long, Long> etapasPorEquipo = new HashMap<>();
        Map<Long, Long> minutosPorEquipo = new HashMap<>();
        for (Object[] f : estadisticaProduccionRepository.buscarEtapasDelPeriodo(inicio, fin, criterios.idEquipo())) {
            LocalDateTime desdeEtapa = (LocalDateTime) f[1];
            LocalDateTime hastaEtapa = f[2] == null ? ahora : (LocalDateTime) f[2];
            etapasPorEquipo.merge((Long) f[0], 1L, Long::sum);
            minutosPorEquipo.merge((Long) f[0], SoporteReportes.minutosEnElPeriodo(desdeEtapa, hastaEtapa, inicio, fin), Long::sum);
        }
        Map<Long, Long> limpiezas = cantidadesPorId(estadisticaProduccionRepository.contarLimpiezasPorEquipo(inicio, fin));
        Map<Long, Long> fallas = cantidadesPorId(estadisticaProduccionRepository.contarFallasPorEquipo(inicio, fin));
        Map<Long, Long> mantenimientos = cantidadesPorId(estadisticaProduccionRepository.contarMantenimientosPorEquipo(inicio, fin));

        // 3. Equipos activos y dados de baja con actividad, por tipo e identificador
        List<UsoEquipo> equipos = new ArrayList<>();
        for (Object[] f : estadisticaProduccionRepository.buscarEquipos(criterios.tipo() == null ? null : criterios.tipo().getEntityClass(), criterios.idEquipo())) {
            Long id = (Long) f[0];
            UsoEquipo uso = new UsoEquipo((String) f[1], tipoDeEquipo((Class<?>) f[2]), f[3] == Estado.BAJA,
                    etapasPorEquipo.getOrDefault(id, 0L), minutosPorEquipo.getOrDefault(id, 0L),
                    limpiezas.getOrDefault(id, 0L), fallas.getOrDefault(id, 0L), mantenimientos.getOrDefault(id, 0L));
            if (!uso.dadoDeBaja() || uso.tieneActividad()) {
                equipos.add(uso);
            }
        }
        equipos.sort(Comparator.comparing((UsoEquipo u) -> u.tipo() == null ? Integer.MAX_VALUE : u.tipo().ordinal())
                .thenComparing(UsoEquipo::identificador, String.CASE_INSENSITIVE_ORDER));

        long conActividad = equipos.stream().filter(UsoEquipo::tieneActividad).count();
        long etapas = equipos.stream().mapToLong(UsoEquipo::etapas).sum();
        double horas = redondear(equipos.stream().mapToLong(UsoEquipo::minutos).sum() / 60.0);
        long totalLimpiezas = equipos.stream().mapToLong(UsoEquipo::limpiezas).sum();
        long totalFallas = equipos.stream().mapToLong(UsoEquipo::fallas).sum();
        long totalMantenimientos = equipos.stream().mapToLong(UsoEquipo::mantenimientos).sum();

        List<DatoGraficoResponseDTO> datos = equipos.stream().filter(u -> u.minutos() > 0)
                .sorted(Comparator.comparingLong(UsoEquipo::minutos).reversed().thenComparing(UsoEquipo::identificador))
                .map(u -> new DatoGraficoResponseDTO(u.identificador(), u.horas())).toList();
        List<FilaReporteResponseDTO> tabla = equipos.stream().map(u -> new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(u.dadoDeBaja() ? u.identificador() + " (dado de baja)" : u.identificador(),
                        u.tipo() == null ? null : u.tipo().getEtiqueta()),
                CeldaReporteResponseDTO.numero(u.etapas()),
                CeldaReporteResponseDTO.numero(u.horas()),
                CeldaReporteResponseDTO.numero(u.limpiezas()),
                CeldaReporteResponseDTO.numero(u.fallas()),
                CeldaReporteResponseDTO.numero(u.mantenimientos())))).toList();

        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Equipamiento")
                .descripcion(PestanaEstadistica.EQUIPAMIENTO.getDescripcion())
                .desde(periodo.desde())
                .hasta(periodo.hasta())
                .criterios(List.of(
                        CriterioAplicadoResponseDTO.de("Tipo de equipo", criterios.tipo() == null ? null : criterios.tipo().getEtiqueta(), "Todos"),
                        CriterioAplicadoResponseDTO.de("Equipo", criterios.textoEquipo(), "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("equipos-con-actividad", "Equipos con actividad", FormatoCantidad.formatear(conActividad),
                                "De " + equipos.size() + (equipos.size() == 1 ? " equipo" : " equipos"), null),
                        new IndicadorResponseDTO("etapas-atendidas", "Etapas atendidas", FormatoCantidad.formatear(etapas),
                                FormatoCantidad.conUnidad(horas, "h") + " en uso",
                                "Etapas de lote iniciadas que se superponen con el período; las horas en uso son su duración real dentro del período"),
                        new IndicadorResponseDTO("limpiezas", "Limpiezas", FormatoCantidad.formatear(totalLimpiezas), "Registradas", null),
                        new IndicadorResponseDTO("fallas-y-mantenimientos", "Fallas y mantenimientos", FormatoCantidad.formatear(totalFallas + totalMantenimientos),
                                totalFallas + (totalFallas == 1 ? " falla y " : " fallas y ") + totalMantenimientos
                                        + (totalMantenimientos == 1 ? " mantenimiento" : " mantenimientos"), null)))
                .grafico(new GraficoReporteResponseDTO("Horas en uso por equipo", SoporteReportes.principales(datos, null), "h"))
                .tabla(new TablaReporteResponseDTO(List.of(
                        ColumnaReporteResponseDTO.de("Equipo", TipoColumna.TEXTO),
                        ColumnaReporteResponseDTO.de("Etapas atendidas", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Horas en uso", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Limpiezas", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Fallas", TipoColumna.NUMERO),
                        ColumnaReporteResponseDTO.de("Mantenimientos", TipoColumna.NUMERO)),
                        SoporteReportes.paginar(tabla, pageable),
                        new FilaReporteResponseDTO(List.of(
                                CeldaReporteResponseDTO.texto(SoporteReportes.TOTALES),
                                CeldaReporteResponseDTO.numero(etapas),
                                CeldaReporteResponseDTO.numero(horas),
                                CeldaReporteResponseDTO.numero(totalLimpiezas),
                                CeldaReporteResponseDTO.numero(totalFallas),
                                CeldaReporteResponseDTO.numero(totalMantenimientos))),
                        "equipo|equipos"))
                .build();
    }

    // ==================== Apoyo ====================

    private static Map<Long, BigDecimal> importesPorId(List<Object[]> filas) {
        Map<Long, BigDecimal> importes = new HashMap<>();
        filas.forEach(f -> importes.put((Long) f[0], SoporteReportes.aImporte(f[1])));
        return importes;
    }

    private static Map<Long, Double> numerosPorId(List<Object[]> filas) {
        Map<Long, Double> numeros = new HashMap<>();
        filas.forEach(f -> numeros.put((Long) f[0], f[1] == null ? null : SoporteReportes.aDouble(f[1])));
        return numeros;
    }

    private static Map<Long, Long> cantidadesPorId(List<Object[]> filas) {
        Map<Long, Long> cantidades = new HashMap<>();
        filas.forEach(f -> cantidades.put((Long) f[0], SoporteReportes.aLong(f[1])));
        return cantidades;
    }

    /** El primer valor no nulo: el de la instantánea de escalado y, si no la tiene, el vigente del equipo. */
    private static Double primero(Double instantanea, Double vigente) {
        return instantanea != null ? instantanea : vigente;
    }

    private static TipoEquipamiento tipoDeEquipo(Class<?> clase) {
        return Arrays.stream(TipoEquipamiento.values()).filter(t -> t.getEntityClass().equals(clase)).findFirst().orElse(null);
    }

    private static String etiquetaDeTipoInsumo(Class<?> clase) {
        return Arrays.stream(TipoInsumo.values()).filter(t -> t.getEntityClass().equals(clase)).findFirst()
                .map(TipoInsumo::getEtiqueta).orElse(null);
    }

    private static FilaReporteResponseDTO filaDeLote(LoteReporteProyeccion lote, Double litros) {
        return new FilaReporteResponseDTO(List.of(
                CeldaReporteResponseDTO.texto(lote.identificadorInterno(), lote.nombreReceta()),
                CeldaReporteResponseDTO.texto(lote.estado() == null ? null : lote.estado().getEtiqueta()),
                CeldaReporteResponseDTO.numero(redondear(litros == null ? 0.0 : litros)),
                CeldaReporteResponseDTO.fecha(lote.fechaFinalizacionEstimada()),
                CeldaReporteResponseDTO.fecha(lote.fechaFinalizacion()),
                CeldaReporteResponseDTO.enlace(lote.id(), "Abrir " + lote.identificadorInterno())));
    }

    private Map<Long, Double> litrosEnvasadosDe(List<LoteReporteProyeccion> lotes) {
        Map<Long, Double> litrosPorLote = new HashMap<>();
        if (lotes.isEmpty()) {
            return litrosPorLote;
        }
        List<Long> ids = lotes.stream().map(LoteReporteProyeccion::id).toList();
        for (Object[] fila : estadisticaProduccionRepository.sumarLitrosEnvasadosPorLote(ids)) {
            litrosPorLote.put((Long) fila[0], ((Number) fila[1]).doubleValue());
        }
        return litrosPorLote;
    }
}
