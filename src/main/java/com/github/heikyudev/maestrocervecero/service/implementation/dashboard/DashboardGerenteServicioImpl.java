package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ColaFermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EtapaEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteEnProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardGerente;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IDashboardGerenteServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Los lotes se paginan por ID con las consultas del
 * panel y se cargan con sus relaciones en una sola consulta por bloque; los equipos, los barriles y las planificaciones
 * salen de los servicios de listado, de los que solo se lee el total (con una página de un registro) o las
 * primeras filas. La cola de los fermentadores visibles se arma con una sola consulta de lotes vivos.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardGerenteServicioImpl implements IDashboardGerenteServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_GERENTE_FILAS_POR_BLOQUE);

    private static final List<EstadoOperativoBarril> ESTADOS_DE_BARRIL_EN_ORDEN = List.of(
            EstadoOperativoBarril.DISPONIBLE, EstadoOperativoBarril.EN_USO, EstadoOperativoBarril.CON_CERVEZA,
            EstadoOperativoBarril.DESPACHADO, EstadoOperativoBarril.EN_LIMPIEZA, EstadoOperativoBarril.EN_MANTENIMIENTO);

    private final ILoteRepository loteRepository;
    private final IControlEquipamientoServicio controlEquipamientoServicio;
    private final IControlBarrilServicio controlBarrilServicio;
    private final IPlanificacionProduccionServicio planificacionProduccionServicio;

    @Override
    @Transactional(readOnly = true)
    public DashboardGerenteResponseDTO obtenerDashboardGerente() {
        LocalDate hoy = LocalDate.now();

        List<ConteoDashboardResponseDTO> equiposPorEstado = contarEquiposPorEstado();
        List<ConteoDashboardResponseDTO> barrilesPorEstado = contarBarrilesPorEstado();
        Page<EquipamientoControlResponseDTO> fermentadores =
                controlEquipamientoServicio.filtrarEquipamientosParaControl(null, TipoEquipamiento.FERMENTADOR, null, TOPE_DE_FILAS);

        return DashboardGerenteResponseDTO.builder()
                .lotesEnEjecucion(loteRepository.buscarIdsDeLotesEnEjecucionParaPanel(LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements())
                .lotesAtrasados(loteRepository.contarLotesEnEjecucionAtrasados(hoy))
                .fermentadoresOcupados(controlEquipamientoServicio
                        .filtrarEquipamientosParaControl(null, TipoEquipamiento.FERMENTADOR, EstadoOperativo.EN_USO, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements())
                .fermentadoresTotal(fermentadores.getTotalElements())
                .fueraDeServicio(totalDe(equiposPorEstado, EstadoOperativo.EN_LIMPIEZA.name()) + totalDe(equiposPorEstado, EstadoOperativo.EN_MANTENIMIENTO.name())
                        + totalDe(barrilesPorEstado, EstadoOperativoBarril.EN_LIMPIEZA.name()) + totalDe(barrilesPorEstado, EstadoOperativoBarril.EN_MANTENIMIENTO.name()))
                .produccionEnCurso(armarProduccionEnCurso(hoy))
                .colaDeFermentadores(armarColaDeFermentadores(fermentadores, hoy))
                .equiposPorEstado(equiposPorEstado)
                .barrilesPorEstado(barrilesPorEstado)
                .planificacionesPendientes(armarBloqueDePlanificaciones())
                .build();
    }

    private List<EtapaEnCursoResponseDTO> armarProduccionEnCurso(LocalDate hoy) {
        List<EtapaEnCursoResponseDTO> etapas = new ArrayList<>();
        for (TipoEtapa etapa : TipoEtapa.values()) {
            Page<Long> ids = loteRepository.buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(etapa, TOPE_DE_FILAS);
            if (ids.getTotalElements() == 0) {
                continue;
            }
            etapas.add(EtapaEnCursoResponseDTO.builder()
                    .etapa(etapa)
                    .lotes(armarBloqueDeLotes(ids, lote -> MapperDashboardGerente.toLoteEnProduccionDTO(lote,
                            MetodosCronograma.calcularDiasDeAtraso(lote.getFechaFinalizacionEstimada(), hoy))))
                    .build());
        }
        return etapas;
    }

    /**
     * Carga los lotes de la página de IDs con sus relaciones (en el orden de los IDs) y los mapea con el mapeo dado. Si
     * la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y se deja una advertencia en el log.
     */
    private BloqueDashboardResponseDTO<LoteEnProduccionResponseDTO> armarBloqueDeLotes(Page<Long> idsDeLaPagina,
                                                                                      Function<LoteCronogramaResponseDTO, LoteEnProduccionResponseDTO> mapeo) {
        List<LoteEnProduccionResponseDTO> filas = List.of();
        if (!idsDeLaPagina.isEmpty()) {
            List<Long> ids = idsDeLaPagina.getContent();
            Map<Long, LoteEntity> lotesPorId = loteRepository.buscarParaListadoPorIds(ids).stream()
                    .collect(Collectors.toMap(LoteEntity::getId, lote -> lote));
            filas = ids.stream()
                    .map(lotesPorId::get)
                    .filter(Objects::nonNull)
                    .map(MapperLoteCronograma::toDTO)
                    .map(mapeo)
                    .toList();
            if (filas.size() != ids.size()) {
                log.warn("El panel del gerente esperaba {} lotes y la carga devolvió {}: no se muestran los lotes {}",
                        ids.size(), filas.size(), ids.stream().filter(id -> !lotesPorId.containsKey(id)).toList());
            }
        }
        return BloqueDashboardResponseDTO.<LoteEnProduccionResponseDTO>builder()
                .total(idsDeLaPagina.getTotalElements())
                .filas(filas)
                .build();
    }

    /**
     * Arma la cola de los fermentadores visibles con una sola consulta de lotes vivos: el lote En ejecución de cada uno,
     * su próximo lote Pendiente (los lotes llegan por fecha de inicio estimada, así que el primero es el más próximo) y
     * la cantidad de pendientes.
     */
    private BloqueDashboardResponseDTO<ColaFermentadorResponseDTO> armarColaDeFermentadores(Page<EquipamientoControlResponseDTO> fermentadores, LocalDate hoy) {
        Map<Long, List<LoteCronogramaResponseDTO>> lotesPorFermentador = Map.of();
        if (fermentadores.hasContent()) {
            List<Long> ids = fermentadores.getContent().stream().map(EquipamientoControlResponseDTO::getId).toList();
            lotesPorFermentador = loteRepository.buscarVivosDeFermentadores(ids).stream()
                    .map(MapperLoteCronograma::toDTO)
                    .filter(lote -> lote.getIdFermentador() != null)
                    .collect(Collectors.groupingBy(LoteCronogramaResponseDTO::getIdFermentador));
        }

        List<ColaFermentadorResponseDTO> filas = new ArrayList<>();
        for (EquipamientoControlResponseDTO fermentador : fermentadores.getContent()) {
            List<LoteCronogramaResponseDTO> lotes = lotesPorFermentador.getOrDefault(fermentador.getId(), List.of());
            LoteEnProduccionResponseDTO enEjecucion = lotes.stream()
                    .filter(lote -> lote.getEstado() == EstadoLote.EN_EJECUCION)
                    .findFirst()
                    .map(lote -> MapperDashboardGerente.toLoteEnProduccionDTO(lote,
                            MetodosCronograma.calcularDiasDeAtraso(lote.getFechaFinalizacionEstimada(), hoy)))
                    .orElse(null);
            List<LoteCronogramaResponseDTO> pendientes = lotes.stream()
                    .filter(lote -> lote.getEstado() == EstadoLote.PENDIENTE)
                    .toList();
            LoteEnProduccionResponseDTO proximo = pendientes.isEmpty() ? null : MapperDashboardGerente.toLoteEnProduccionDTO(pendientes.get(0), 0);
            filas.add(MapperDashboardGerente.toColaFermentadorDTO(fermentador, enEjecucion, proximo, pendientes.size()));
        }
        return BloqueDashboardResponseDTO.<ColaFermentadorResponseDTO>builder()
                .total(fermentadores.getTotalElements())
                .filas(filas)
                .build();
    }

    private BloqueDashboardResponseDTO<PlanificacionPendienteResponseDTO> armarBloqueDePlanificaciones() {
        return BloqueDashboardResponseDTO.de(
                planificacionProduccionServicio.filtrarPlanificacionesProduccion(null, EstadoSolicitud.PENDIENTE, null, TOPE_DE_FILAS),
                MapperDashboardGerente::toPlanificacionPendienteDTO);
    }

    private List<ConteoDashboardResponseDTO> contarEquiposPorEstado() {
        List<ConteoDashboardResponseDTO> conteos = new ArrayList<>();
        for (EstadoOperativo estado : EstadoOperativo.values()) {
            conteos.add(MapperDashboardGerente.toConteoDTO(estado.name(), estado.getEtiqueta(),
                    controlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, estado, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements()));
        }
        return conteos;
    }

    private List<ConteoDashboardResponseDTO> contarBarrilesPorEstado() {
        List<ConteoDashboardResponseDTO> conteos = new ArrayList<>();
        for (EstadoOperativoBarril estado : ESTADOS_DE_BARRIL_EN_ORDEN) {
            conteos.add(MapperDashboardGerente.toConteoDTO(estado.name(), estado.getEtiqueta(),
                    controlBarrilServicio.filtrarBarrilesParaControl(null, null, estado, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements()));
        }
        return conteos;
    }

    private static long totalDe(List<ConteoDashboardResponseDTO> conteos, String codigo) {
        return conteos.stream().filter(c -> c.getCodigo().equals(codigo)).mapToLong(ConteoDashboardResponseDTO::getTotal).sum();
    }
}
