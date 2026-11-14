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
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardOperarioResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardOperario;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperLoteCronograma;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
 * mapeo de los lotes no ejecuta consultas.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOperarioServicioImpl implements IDashboardOperarioServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE);

    private final ILoteRepository loteRepository;
    private final IControlEquipamientoServicio controlEquipamientoServicio;
    private final IControlBarrilServicio controlBarrilServicio;
    private final IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOperarioResponseDTO obtenerDashboardOperario() {
        LocalDate hoy = LocalDate.now();

        return DashboardOperarioResponseDTO.builder()
                .lotesEnEjecucion(armarBloqueDeLotes(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(TOPE_DE_FILAS)))
                .lotesAIniciar(armarBloqueDeLotes(loteRepository.buscarIdsDeLotesAIniciarParaPanel(hoy, TOPE_DE_FILAS)))
                .equiposEnLimpieza(armarBloqueDeEquipos(EstadoOperativo.EN_LIMPIEZA))
                .barrilesEnLimpieza(armarBloqueDeBarriles(EstadoOperativoBarril.EN_LIMPIEZA))
                .equiposEnMantenimiento(armarBloqueDeEquipos(EstadoOperativo.EN_MANTENIMIENTO))
                .barrilesEnMantenimiento(armarBloqueDeBarriles(EstadoOperativoBarril.EN_MANTENIMIENTO))
                .build();
    }

    /**
     * Carga los lotes de la página de IDs con sus relaciones (en el orden de los IDs) y los mapea. Si
     * la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y se deja una advertencia en el log.
     */
    private BloqueDashboardResponseDTO<LoteDelDiaResponseDTO> armarBloqueDeLotes(Page<Long> idsDeLaPagina) {
        List<LoteDelDiaResponseDTO> filas = List.of();
        if (!idsDeLaPagina.isEmpty()) {
            List<Long> ids = idsDeLaPagina.getContent();
            Map<Long, LoteEntity> lotesPorId = loteRepository.buscarParaListadoPorIds(ids).stream()
                    .collect(Collectors.toMap(LoteEntity::getId, lote -> lote));
            filas = ids.stream()
                    .map(lotesPorId::get)
                    .filter(Objects::nonNull)
                    .map(MapperLoteCronograma::toDTO)
                    .map(MapperDashboardOperario::toLoteDelDiaDTO)
                    .toList();
            if (filas.size() != ids.size()) {
                log.warn("El panel del operario esperaba {} lotes y la carga devolvió {}: no se muestran los lotes {}",
                        ids.size(), filas.size(), ids.stream().filter(id -> !lotesPorId.containsKey(id)).toList());
            }
        }
        return BloqueDashboardResponseDTO.<LoteDelDiaResponseDTO>builder()
                .total(idsDeLaPagina.getTotalElements())
                .filas(filas)
                .build();
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

        return BloqueDashboardResponseDTO.de(pagina, fila -> mapeo.apply(fila, causas.get(idDe.apply(fila))));
    }
}
