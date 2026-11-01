package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OperacionCicloVidaEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperControlEquipamiento;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
import java.util.function.Function;

/**
 * Implementación del servicio de consultas de las pantallas de control de equipamiento.
 * <p>
 * Es de solo lectura y no toma locks. Cada consulta resuelve todo lo que necesita con un número fijo de consultas, sin
 * importar cuántos equipamientos u operaciones haya: el listado agrega los usos y la última operación de la página
 * completa con una consulta cada uno (por lista de IDs), y el historial trae las etapas junto con su lote.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ControlEquipamientoServicioImpl implements IControlEquipamientoServicio {

    private final IEquipamientoRepository equipamientoRepository;
    private final ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    private final IFallaEquipamientoRepository fallaEquipamientoRepository;
    private final IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    private final IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

    /**
     * Obtiene una página de equipamientos activos con los datos de control: tipo, estado operativo, usos desde el
     * último mantenimiento y máximo, y última operación registrada.
     * <p>
     * El identificador buscado se normaliza igual que el guardado (sin espacios de más y en mayúsculas); si queda
     * vacío no filtra. Los usos y las últimas operaciones de toda la página se obtienen con una consulta cada uno.
     * </p>
     *
     * @param identificador Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por tipo.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de equipamientos activos en formato DTO de control.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EquipamientoControlResponseDTO> filtrarEquipamientosParaControl(String identificador, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipo != null ? tipo.getEntityClass() : null;
        String identificadorNormalizado = NormalizacionTexto.codigo(identificador);
        if (identificadorNormalizado != null && identificadorNormalizado.isEmpty()) {
            identificadorNormalizado = null;
        }

        Page<EquipamientoEntity> pagina = equipamientoRepository.filtrarEquipamientos(identificadorNormalizado, tipoClase, estadoOperativo, pageable);
        List<Long> ids = pagina.getContent().stream().map(EquipamientoEntity::getId).toList();

        Map<Long, Long> usosPorEquipamiento = new HashMap<>();
        Map<Long, OperacionCicloVidaEquipamiento> ultimaOperacionPorEquipamiento = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] fila : limpiezaEquipamientoRepository.contarUsosDesdeUltimoMantenimiento(ids)) {
                usosPorEquipamiento.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
            }
            for (OperacionCicloVidaEquipamiento operacion : operacionCicloVidaEquipamientoRepository.buscarUltimasOperacionesRegistradas(ids)) {
                // Ante un empate de fecha (datos anteriores a la regla de orden estricto) se queda con la de mayor ID
                ultimaOperacionPorEquipamiento.merge(operacion.getEquipamiento().getId(), operacion,
                        (actual, nueva) -> nueva.getId() > actual.getId() ? nueva : actual);
            }
        }

        return pagina.map(equipamiento -> MapperControlEquipamiento.toControlDTO(
                equipamiento,
                usosPorEquipamiento.getOrDefault(equipamiento.getId(), 0L),
                ultimaOperacionPorEquipamiento.get(equipamiento.getId())));
    }

    /**
     * Obtiene el resumen de un equipamiento activo: sus datos, los parámetros propios de su tipo, sus usos desde el
     * último mantenimiento y las fechas de su última limpieza y su último mantenimiento registrados.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return El resumen del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe o está dado de baja.
     */
    @Override
    @Transactional(readOnly = true)
    public EquipamientoResumenResponseDTO buscarResumen(Long idEquipamiento) {
        EquipamientoEntity equipamiento = equipamientoRepository.findById(idEquipamiento)
                .map(entidad -> (EquipamientoEntity) Hibernate.unproxy(entidad))
                .filter(entidad -> entidad.getEstado() == Estado.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + idEquipamiento));

        LocalDateTime fechaUltimoMantenimiento = mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(idEquipamiento).orElse(null);
        LocalDateTime fechaUltimaLimpieza = limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(idEquipamiento).orElse(null);
        long usos = limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(idEquipamiento, fechaUltimoMantenimiento);

        return MapperControlEquipamiento.toResumenDTO(equipamiento, usos, fechaUltimaLimpieza, fechaUltimoMantenimiento);
    }

    /**
     * Lista las operaciones de un equipamiento agrupadas por tipo, anuladas incluidas, de la de fecha más reciente a
     * la más antigua. Solo la última operación registrada (mayor fecha y, a igualdad, mayor ID, de cualquier tipo) se marca como anulable.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return Las operaciones del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public OperacionesEquipamientoResponseDTO listarOperaciones(Long idEquipamiento) {
        validarQueExisteElEquipamiento(idEquipamiento);

        Long idUltimaOperacion = operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(idEquipamiento).orElse(null);

        return OperacionesEquipamientoResponseDTO.builder()
                .limpiezas(mapearOperaciones(limpiezaEquipamientoRepository.listarPorEquipamiento(idEquipamiento), idUltimaOperacion))
                .fallas(mapearOperaciones(fallaEquipamientoRepository.listarPorEquipamiento(idEquipamiento), idUltimaOperacion))
                .mantenimientos(mapearOperaciones(mantenimientoEquipamientoRepository.listarPorEquipamiento(idEquipamiento), idUltimaOperacion))
                .build();
    }

    /**
     * Lista el historial de un equipamiento: una fila por cada etapa de lote con uso real y por cada operación
     * registrada (nunca las anuladas), de la más reciente a la más antigua.
     * <p>
     * Las dos fuentes se traen con una consulta cada una (las etapas junto con su lote) y se unen en memoria: el
     * historial de un equipamiento es acotado y la unión ordenada de dos tablas distintas no se puede paginar en una
     * sola consulta. Ante igual fecha el orden es por tipo y, dentro del tipo, por identificador de mayor a menor.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param tipo Tipo de actividad a mostrar, o {@code null} para todas.
     * @param desde Límite inferior (inclusive) de la fecha, o {@code null} para no acotarlo.
     * @param hasta Límite superior (inclusive) de la fecha, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página del historial.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     * @throws ReglaNegocioException Si {@code hasta} es anterior a {@code desde}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ActividadEquipamientoResponseDTO> listarHistorial(Long idEquipamiento, TipoActividadEquipamiento tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde", "hasta");
        }
        validarQueExisteElEquipamiento(idEquipamiento);

        List<ActividadEquipamientoResponseDTO> actividades = new ArrayList<>();
        if (tipo == null || tipo == TipoActividadEquipamiento.LOTE) {
            etapaLoteRepository.buscarEtapasConUsoRealPorInicio(idEquipamiento, desde, hasta).stream()
                    .map(MapperControlEquipamiento::toActividadDTO)
                    .forEach(actividades::add);
        }
        if (tipo == null || tipo != TipoActividadEquipamiento.LOTE) {
            operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(idEquipamiento, desde, hasta).stream()
                    .map(MapperControlEquipamiento::toActividadDTO)
                    .filter(actividad -> tipo == null || actividad.getTipo() == tipo)
                    .forEach(actividades::add);
        }

        actividades.sort(Comparator.comparing(ActividadEquipamientoResponseDTO::getFechaInicio).reversed()
                .thenComparing(ActividadEquipamientoResponseDTO::getTipo)
                .thenComparing(Comparator.comparing(ActividadEquipamientoResponseDTO::getIdReferencia).reversed()));

        int inicio = (int) Math.min(pageable.getOffset(), actividades.size());
        int fin = Math.min(inicio + pageable.getPageSize(), actividades.size());
        return new PageImpl<>(actividades.subList(inicio, fin), pageable, actividades.size());
    }

    private void validarQueExisteElEquipamiento(Long idEquipamiento) {
        if (!equipamientoRepository.existsById(idEquipamiento)) {
            throw new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + idEquipamiento);
        }
    }

    private List<OperacionEquipamientoResponseDTO> mapearOperaciones(Collection<? extends OperacionCicloVidaEquipamiento> operaciones, Long idUltimaOperacion) {
        Function<OperacionCicloVidaEquipamiento, Boolean> esAnulable = operacion ->
                operacion.getEstado() == EstadoTransaccion.REGISTRADO && operacion.getId().equals(idUltimaOperacion);
        return operaciones.stream()
                .map(operacion -> MapperControlEquipamiento.toOperacionDTO(operacion, esAnulable.apply(operacion)))
                .toList();
    }
}
