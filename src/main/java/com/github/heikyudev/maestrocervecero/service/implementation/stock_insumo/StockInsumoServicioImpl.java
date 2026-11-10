package com.github.heikyudev.maestrocervecero.service.implementation.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.CantidadesStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperConsumoInsumo;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperReservaInsumo;
import com.github.heikyudev.maestrocervecero.util.mapper.stock_insumo.MapperStockInsumo;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IStockInsumoServicio}.
 */
@Service
@RequiredArgsConstructor
public class StockInsumoServicioImpl implements IStockInsumoServicio {

    private final IInsumoRepository insumoRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;
    private final IConsumoInsumoRepository consumoInsumoRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Hace dos consultas por página, sin importar su tamaño: una con los insumos y otra con las sumas de los lotes
     * de esos insumos. Un insumo sin lotes tiene todas sus cantidades en 0.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StockInsumoResponseDTO> filtrarInsumos(String nombre, TipoInsumo tipo, SituacionStockInsumo situacionStock, Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        Class<? extends InsumoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        String nombreNormalizado = NormalizacionTexto.nombrePropio(nombre);
        if (nombreNormalizado != null && nombreNormalizado.isEmpty()) {
            nombreNormalizado = null;
        }
        String nombreEscapado = NormalizacionTexto.escaparComodinesLike(nombreNormalizado);

        boolean exigirConDisponible = situacionStock == SituacionStockInsumo.CON_STOCK_DISPONIBLE;
        boolean exigirSinDisponible = situacionStock == SituacionStockInsumo.SIN_STOCK_DISPONIBLE;
        boolean exigirConVencidos = situacionStock == SituacionStockInsumo.CON_VENCIDOS;

        // El orden lo define la consulta: el del pageable se ignora para que la paginación sea siempre estable
        Page<InsumoEntity> insumos = Paginacion.consultar(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted(),
                p -> insumoRepository.filtrarInsumosParaStock(nombreEscapado, tipoClase, exigirConDisponible,
                        exigirSinDisponible, exigirConVencidos, hoy, p));

        Map<Long, CantidadesStockResponseDTO> cantidadesPorInsumo = sumarCantidades(insumos.getContent(), hoy);
        return insumos.map(insumo -> MapperStockInsumo.toDTO(insumo,
                cantidadesPorInsumo.getOrDefault(insumo.getId(), MapperStockInsumo.desdeSumas(0, 0, 0, 0))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public StockInsumoResponseDTO obtenerStockInsumo(Long idInsumo) {
        LocalDate hoy = LocalDate.now();
        InsumoEntity insumo = buscarInsumoActivo(idInsumo);
        CantidadesStockResponseDTO cantidades = sumarCantidades(List.of(insumo), hoy)
                .getOrDefault(insumo.getId(), MapperStockInsumo.desdeSumas(0, 0, 0, 0));
        return MapperStockInsumo.toDTO(insumo, cantidades);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StockLoteInsumoResponseDTO> filtrarLotesDeInsumo(Long idInsumo, Pageable pageable) {
        LocalDate hoy = LocalDate.now();
        buscarInsumoActivo(idInsumo);
        // El orden lo define la consulta (FEFO): el del pageable se ignora
        return Paginacion.consultar(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted(),
                        p -> loteInsumoRepository.filtrarLotesDeInsumo(idInsumo, p))
                .map(lote -> MapperStockInsumo.toDTO(lote, hoy));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public StockLoteInsumoDetalleResponseDTO obtenerDetalleLoteInsumo(Long idLoteInsumo) {
        return MapperStockInsumo.toDetalleDTO(buscarLoteConInsumoActivo(idLoteInsumo), LocalDate.now());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReservaInsumoResponseDTO> filtrarReservasDeLoteInsumo(Long idLoteInsumo, Pageable pageable) {
        buscarLoteConInsumoActivo(idLoteInsumo);
        return Paginacion.consultar(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted(),
                        p -> reservaInsumoRepository.filtrarPorLoteInsumo(idLoteInsumo, p))
                .map(MapperReservaInsumo::toDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ConsumoInsumoResponseDTO> filtrarConsumosDeLoteInsumo(Long idLoteInsumo, Pageable pageable) {
        buscarLoteConInsumoActivo(idLoteInsumo);
        return Paginacion.consultar(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted(),
                        p -> consumoInsumoRepository.filtrarPorLoteInsumo(idLoteInsumo, p))
                .map(MapperConsumoInsumo::toDTO);
    }

    private InsumoEntity buscarInsumoActivo(Long idInsumo) {
        return insumoRepository.findById(idInsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + idInsumo));
    }

    private LoteInsumoEntity buscarLoteConInsumoActivo(Long idLoteInsumo) {
        return loteInsumoRepository.buscarConInsumoActivoPorId(idLoteInsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + idLoteInsumo));
    }

    /**
     * Suma las cantidades de los lotes de los insumos indicados con una sola consulta agrupada.
     */
    private Map<Long, CantidadesStockResponseDTO> sumarCantidades(List<InsumoEntity> insumos, LocalDate hoy) {
        Map<Long, CantidadesStockResponseDTO> cantidadesPorInsumo = new HashMap<>();
        if (insumos.isEmpty()) {
            return cantidadesPorInsumo;
        }
        List<Long> ids = insumos.stream().map(InsumoEntity::getId).toList();
        for (Object[] fila : loteInsumoRepository.sumarStockPorInsumo(ids, hoy)) {
            cantidadesPorInsumo.put((Long) fila[0], MapperStockInsumo.desdeSumas(
                    ((Number) fila[1]).doubleValue(), ((Number) fila[2]).doubleValue(),
                    ((Number) fila[3]).doubleValue(), ((Number) fila[4]).doubleValue()));
        }
        return cantidadesPorInsumo;
    }
}
