package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OperacionCicloVidaEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardOperarioServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardOperarioResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardOperario;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperLoteCronograma;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCronograma;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IDashboardOperarioServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Los lotes se paginan por ID con las consultas del
 * panel y se cargan con sus relaciones en una sola consulta por bloque; los equipos y los barriles salen de los
 * servicios de control (que ya devuelven las filas y el total en una sola llamada) y la causa de cada mantenimiento se
 * deriva de la última operación registrada de todas las filas visibles, con una consulta por tipo de activo. El
 * consumo de la etapa en curso se calcula solo para los lotes en ejecución visibles, con el porcentaje mínimo leído una
 * sola vez de la configuración; si el cálculo de un lote falla, ese lote se muestra sin falta de consumo y el resto del
 * panel no se ve afectado.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOperarioServicioImpl implements IDashboardOperarioServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE);

    private final ILoteRepository loteRepository;
    private final ILoteServicio loteServicio;
    private final IControlEquipamientoServicio controlEquipamientoServicio;
    private final IControlBarrilServicio controlBarrilServicio;
    private final IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOperarioResponseDTO obtenerDashboardOperario() {
        LocalDate hoy = LocalDate.now();

        return DashboardOperarioResponseDTO.builder()
                .lotesEnEjecucion(armarBloqueDeLotes(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(TOPE_DE_FILAS),
                        lotes -> mapearLotesEnEjecucion(lotes, hoy)))
                .lotesAIniciar(armarBloqueDeLotes(loteRepository.buscarIdsDeLotesAIniciarParaPanel(hoy, TOPE_DE_FILAS),
                        lotes -> lotes.stream().map(lote -> MapperDashboardOperario.toLoteDelDiaDTO(lote, 0, false)).toList()))
                .equiposEnLimpieza(armarBloqueDeEquipos(EstadoOperativo.EN_LIMPIEZA))
                .barrilesEnLimpieza(armarBloqueDeBarriles(EstadoOperativoBarril.EN_LIMPIEZA))
                .equiposEnMantenimiento(armarBloqueDeEquipos(EstadoOperativo.EN_MANTENIMIENTO))
                .barrilesEnMantenimiento(armarBloqueDeBarriles(EstadoOperativoBarril.EN_MANTENIMIENTO))
                .build();
    }

    /**
     * Carga los lotes de la página de IDs con sus relaciones (en el orden de los IDs) y los mapea con el mapeo dado. Si
     * la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y se deja una advertencia en el log.
     */
    private BloqueDashboardResponseDTO<LoteDelDiaResponseDTO> armarBloqueDeLotes(Page<Long> idsDeLaPagina,
                                                                                  Function<List<LoteCronogramaResponseDTO>, List<LoteDelDiaResponseDTO>> mapeo) {
        List<LoteDelDiaResponseDTO> filas = List.of();
        if (!idsDeLaPagina.isEmpty()) {
            List<Long> ids = idsDeLaPagina.getContent();
            Map<Long, LoteEntity> lotesPorId = loteRepository.buscarParaListadoPorIds(ids).stream()
                    .collect(Collectors.toMap(LoteEntity::getId, lote -> lote));
            List<LoteCronogramaResponseDTO> lotes = ids.stream()
                    .map(lotesPorId::get)
                    .filter(Objects::nonNull)
                    .map(MapperLoteCronograma::toDTO)
                    .toList();
            if (lotes.size() != ids.size()) {
                log.warn("El panel del operario esperaba {} lotes y la carga devolvió {}: no se muestran los lotes {}",
                        ids.size(), lotes.size(), ids.stream().filter(id -> !lotesPorId.containsKey(id)).toList());
            }
            filas = mapeo.apply(lotes);
        }
        return BloqueDashboardResponseDTO.<LoteDelDiaResponseDTO>builder()
                .total(idsDeLaPagina.getTotalElements())
                .filas(filas)
                .build();
    }

    /**
     * Mapea los lotes en ejecución con su atraso y su falta de consumo (la etapa en curso no alcanza el mínimo). El
     * porcentaje mínimo se lee una sola vez para todos los lotes; si no se puede leer, o si el cálculo de un lote
     * falla, ese dato queda sin falta de consumo y se deja una advertencia en el log, sin quitar filas del panel.
     */
    private List<LoteDelDiaResponseDTO> mapearLotesEnEjecucion(List<LoteCronogramaResponseDTO> lotes, LocalDate hoy) {
        Double porcentajeMinimo = null;
        if (lotes.stream().anyMatch(lote -> lote.getEtapaActual() != null)) {
            try {
                porcentajeMinimo = loteServicio.obtenerPorcentajeMinimoConsumoParaAvanzarEtapa();
            } catch (RuntimeException e) {
                log.warn("El panel del operario no pudo leer el porcentaje mínimo de consumo: {}", e.getMessage());
            }
        }

        List<LoteDelDiaResponseDTO> filas = new ArrayList<>();
        for (LoteCronogramaResponseDTO lote : lotes) {
            int diasDeAtraso = MetodosCronograma.calcularDiasDeAtraso(lote.getFechaFinalizacionEstimada(), hoy);
            filas.add(MapperDashboardOperario.toLoteDelDiaDTO(lote, diasDeAtraso, faltaConsumo(lote, porcentajeMinimo)));
        }
        return filas;
    }

    private boolean faltaConsumo(LoteCronogramaResponseDTO lote, Double porcentajeMinimo) {
        if (lote.getEtapaActual() == null || porcentajeMinimo == null) {
            return false;
        }
        try {
            return !loteServicio.calcularAvanceDeEtapa(lote.getId(), lote.getEtapaActual(), porcentajeMinimo).isCumpleMinimo();
        } catch (RuntimeException e) {
            log.warn("El panel del operario no pudo calcular el consumo de la etapa {} del lote {}: {}",
                    lote.getEtapaActual(), lote.getId(), e.getMessage());
            return false;
        }
    }

    private BloqueDashboardResponseDTO<EquipoPendienteResponseDTO> armarBloqueDeEquipos(EstadoOperativo estadoOperativo) {
        return armarBloque(
                controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, estadoOperativo, TOPE_DE_FILAS),
                estadoOperativo == EstadoOperativo.EN_MANTENIMIENTO,
                EquipamientoControlResponseDTO::getId,
                ids -> {
                    Map<Long, CausaMantenimiento> causas = new HashMap<>();
                    for (OperacionCicloVidaEquipamiento ultima : operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(ids)) {
                        causas.put(ultima.getEquipamiento().getId(), MapperDashboardOperario.toCausaMantenimientoDeEquipo(ultima));
                    }
                    return causas;
                },
                MapperDashboardOperario::toEquipoPendienteDTO);
    }

    private BloqueDashboardResponseDTO<BarrilPendienteResponseDTO> armarBloqueDeBarriles(EstadoOperativoBarril estadoOperativo) {
        return armarBloque(
                controlBarrilServicio.filtrarBarrilesParaControl(null, null, estadoOperativo, TOPE_DE_FILAS),
                estadoOperativo == EstadoOperativoBarril.EN_MANTENIMIENTO,
                BarrilControlResponseDTO::getId,
                ids -> {
                    Map<Long, CausaMantenimiento> causas = new HashMap<>();
                    for (OperacionCicloVidaBarril ultima : operacionCicloVidaBarrilRepository.buscarUltimasOperacionesRegistradas(ids)) {
                        causas.put(ultima.getBarril().getId(), MapperDashboardOperario.toCausaMantenimientoDeBarril(ultima));
                    }
                    return causas;
                },
                MapperDashboardOperario::toBarrilPendienteDTO);
    }

    /**
     * Arma un bloque de equipos o de barriles: las filas de la página y, solo en los bloques de mantenimiento con
     * filas, la causa de cada una derivada de su última operación registrada (una consulta para todas las filas).
     */
    private <T, D> BloqueDashboardResponseDTO<D> armarBloque(Page<T> pagina, boolean derivarCausa,
                                                             Function<T, Long> idDe,
                                                             Function<List<Long>, Map<Long, CausaMantenimiento>> causasPorId,
                                                             BiFunction<T, CausaMantenimiento, D> mapeo) {
        Map<Long, CausaMantenimiento> causas = derivarCausa && pagina.hasContent()
                ? causasPorId.apply(pagina.getContent().stream().map(idDe).toList())
                : Map.of();

        return BloqueDashboardResponseDTO.<D>builder()
                .total(pagina.getTotalElements())
                .filas(pagina.getContent().stream()
                        .map(fila -> mapeo.apply(fila, causas.get(idDe.apply(fila))))
                        .toList())
                .build();
    }
}
