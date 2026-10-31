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
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperControlEquipamiento;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
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
 * completa con una consulta cada uno (por lista de IDs), y la línea de tiempo trae las etapas junto con su lote.
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
                // Ante un empate de fecha de fin (datos anteriores a la regla de orden estricto) se queda con la de mayor ID
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
     * último mantenimiento y las fechas de fin de su última limpieza y su último mantenimiento registrados.
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

        LocalDateTime fechaFinUltimoMantenimiento = mantenimientoEquipamientoRepository.buscarFechaFinUltimoMantenimientoRegistrado(idEquipamiento).orElse(null);
        LocalDateTime fechaFinUltimaLimpieza = limpiezaEquipamientoRepository.buscarFechaFinUltimaLimpiezaRegistrada(idEquipamiento).orElse(null);
        long usos = limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(idEquipamiento, fechaFinUltimoMantenimiento);

        return MapperControlEquipamiento.toResumenDTO(equipamiento, usos, fechaFinUltimaLimpieza, fechaFinUltimoMantenimiento);
    }

    /**
     * Lista las operaciones de un equipamiento agrupadas por tipo, anuladas incluidas, de la de inicio más reciente a
     * la más antigua. Solo la última operación registrada (mayor fecha de fin y, a igualdad, mayor ID, de cualquier tipo) se marca como anulable.
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
     * Arma la línea de tiempo de un equipamiento para un período: un bloque por cada etapa de lote con uso real y por
     * cada operación registrada (nunca las anuladas) que se superponga con el período, ordenados por inicio.
     * <p>
     * Tienen uso real las etapas {@code EN_CURSO}, {@code FINALIZADA} y {@code CANCELADA} con fecha de inicio. La
     * etapa en curso usa el momento de la consulta como fin provisorio. Los bloques conservan sus fechas reales, sin
     * recortarlas al período. Ante igual inicio, el bloque que termina antes va primero.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param desde Inicio del período, o {@code null} para no acotarlo.
     * @param hasta Fin del período, o {@code null} para no acotarlo.
     * @return Los bloques de la línea de tiempo.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     * @throws ReglaNegocioException Si {@code hasta} es anterior a {@code desde}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BloqueLineaTiempoEquipamientoResponseDTO> listarLineaDeTiempo(Long idEquipamiento, LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde", "hasta");
        }
        validarQueExisteElEquipamiento(idEquipamiento);

        LocalDateTime ahora = LocalDateTime.now();
        List<BloqueLineaTiempoEquipamientoResponseDTO> bloques = new ArrayList<>();

        etapaLoteRepository.buscarEtapasConUsoRealEnPeriodo(idEquipamiento, desde, hasta).stream()
                .map(etapa -> MapperControlEquipamiento.toBloqueDTO(etapa, ahora))
                // Una etapa en curso tiene fin provisorio: si el período empieza después de ahora, no lo toca
                .filter(bloque -> desde == null || !bloque.getFechaFin().isBefore(desde))
                .forEach(bloques::add);

        operacionCicloVidaEquipamientoRepository.buscarRegistradasEnPeriodo(idEquipamiento, desde, hasta).stream()
                .map(MapperControlEquipamiento::toBloqueDTO)
                .forEach(bloques::add);

        bloques.sort(Comparator.comparing(BloqueLineaTiempoEquipamientoResponseDTO::getFechaInicio)
                .thenComparing(BloqueLineaTiempoEquipamientoResponseDTO::getFechaFin));
        return bloques;
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
