package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperControlBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de consultas de las pantallas de control de barriles.
 * <p>
 * Es de solo lectura y no toma locks. Cada consulta resuelve todo lo que necesita con un número fijo de consultas, sin
 * importar cuántos barriles u operaciones haya: la lista agrega los usos de la página completa con una consulta (por
 * lista de IDs) y trae el fabricante junto con el barril, y el historial trae los envasados junto con su lote.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ControlBarrilServicioImpl implements IControlBarrilServicio {

    private final IBarrilRepository barrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IFallaBarrilRepository fallaBarrilRepository;
    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;

    /**
     * {@inheritDoc}
     * <p>
     * El identificador buscado se normaliza igual que el guardado; si queda vacío no filtra. Los usos de toda la
     * página se obtienen con una sola consulta.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BarrilControlResponseDTO> filtrarBarrilesParaControl(String identificador, Long idFabricante, EstadoOperativoBarril estadoOperativo, Pageable pageable) {
        String identificadorNormalizado = NormalizacionTexto.codigo(identificador);
        if (identificadorNormalizado != null && identificadorNormalizado.isEmpty()) {
            identificadorNormalizado = null;
        }

        Page<BarrilEntity> pagina = barrilRepository.filtrarBarrilesParaControl(identificadorNormalizado, idFabricante, estadoOperativo, pageable);
        List<Long> ids = pagina.getContent().stream().map(BarrilEntity::getId).toList();

        Map<Long, Long> usosPorBarril = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] fila : limpiezaBarrilRepository.contarUsosDesdeUltimoMantenimiento(ids)) {
                usosPorBarril.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
            }
        }
        return pagina.map(barril -> MapperControlBarril.toControlDTO(barril, usosPorBarril.getOrDefault(barril.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public BarrilResumenResponseDTO buscarResumen(Long idBarril) {
        BarrilEntity barril = barrilRepository.findById(idBarril).filter(b -> b.getEstado() == Estado.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        LocalDateTime fechaUltimoMantenimiento = mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null);
        LocalDateTime fechaUltimaLimpieza = limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null);
        long usos = limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(idBarril, fechaUltimoMantenimiento);

        return MapperControlBarril.toResumenDTO(barril, usos, fechaUltimaLimpieza, fechaUltimoMantenimiento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperacionBarrilResponseDTO> listarOperaciones(Long idBarril, TipoOperacionBarril tipo) {
        BarrilEntity barril = barrilRepository.findById(idBarril).filter(b -> b.getEstado() == Estado.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));
        boolean enUso = MetodosCicloVida.estaEnUso(barril.getEstadoOperativo());
        Long idUltimaOperacion = operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null);
        LocalDateTime fechaUltimoEnvasado = envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null);

        Collection<? extends OperacionCicloVidaBarril> operaciones = switch (tipo) {
            case LIMPIEZA -> limpiezaBarrilRepository.listarPorBarril(idBarril);
            case FALLA -> fallaBarrilRepository.listarPorBarril(idBarril);
            case FRACCIONAMIENTO -> fraccionamientoBarrilRepository.listarPorBarril(idBarril);
            case MANTENIMIENTO -> mantenimientoBarrilRepository.listarPorBarril(idBarril);
            case DESPACHO -> despachoBarrilRepository.listarPorBarril(idBarril);
            case DEVOLUCION -> devolucionBarrilRepository.listarPorBarril(idBarril);
        };
        return operaciones.stream()
                .map(operacion -> MapperControlBarril.toOperacionDTO(operacion,
                        !enUso && operacion.getEstado() == EstadoTransaccion.REGISTRADO && operacion.getId().equals(idUltimaOperacion)
                                && MetodosCicloVida.esPosteriorAlUltimoEnvasado(operacion.getFecha(), fechaUltimoEnvasado)))
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Las dos fuentes (envasados con su lote y operaciones) se traen con una consulta cada una y se unen en memoria: el
     * historial de un barril es acotado y la unión ordenada de dos tablas distintas no se puede paginar en una sola
     * consulta. Ante igual fecha el orden es por tipo y, dentro del tipo, por identificador de mayor a menor.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActividadBarrilResponseDTO> listarHistorial(Long idBarril, TipoActividadBarril tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde", "hasta");
        }
        if (barrilRepository.findById(idBarril).filter(b -> b.getEstado() == Estado.ACTIVO).isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril);
        }

        List<ActividadBarrilResponseDTO> actividades = new ArrayList<>();
        if (tipo == null || tipo == TipoActividadBarril.ENVASADO) {
            envasadoLoteRepository.buscarRegistradosDelBarrilEnPeriodo(idBarril, desde, hasta).stream()
                    .map(MapperControlBarril::toActividadDTO)
                    .forEach(actividades::add);
        }
        if (tipo == null || tipo != TipoActividadBarril.ENVASADO) {
            operacionCicloVidaBarrilRepository.buscarRegistradasEnPeriodo(idBarril, desde, hasta).stream()
                    .map(MapperControlBarril::toActividadDTO)
                    .filter(actividad -> tipo == null || actividad.getTipo() == tipo)
                    .forEach(actividades::add);
        }

        actividades.sort(Comparator.comparing(ActividadBarrilResponseDTO::getFecha).reversed()
                .thenComparing(ActividadBarrilResponseDTO::getTipo)
                .thenComparing(Comparator.comparing(ActividadBarrilResponseDTO::getIdReferencia).reversed()));

        int inicio = (int) Math.min(pageable.getOffset(), actividades.size());
        int fin = Math.min(inicio + pageable.getPageSize(), actividades.size());
        return new PageImpl<>(actividades.subList(inicio, fin), pageable, actividades.size());
    }
}
