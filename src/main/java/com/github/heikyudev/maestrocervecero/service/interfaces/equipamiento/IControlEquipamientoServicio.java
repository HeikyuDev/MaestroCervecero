package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define las consultas de las pantallas de control de equipamiento: el listado con su estado y su
 * actividad reciente, el resumen de un equipo, sus operaciones y su historial.
 * <p>
 * Es un servicio de solo lectura: las operaciones del ciclo de vida (limpieza, falla y mantenimiento) se registran y
 * anulan con sus propios servicios.
 * </p>
 */
public interface IControlEquipamientoServicio {

    /**
     * Obtiene una página de equipamientos activos con los datos de control: tipo, estado operativo, usos desde el
     * último mantenimiento y máximo. Un parámetro nulo no restringe por ese criterio.
     * El orden es natural por identificador interno.
     *
     * @param identificador Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por tipo.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de equipamientos activos en formato DTO de control.
     */
    Page<EquipamientoControlResponseDTO> filtrarEquipamientosParaControl(String identificador, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Obtiene el resumen de un equipamiento activo: sus datos, los parámetros propios de su tipo, sus usos desde el
     * último mantenimiento y las fechas de su última limpieza y su último mantenimiento registrados.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return El resumen del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe o está dado de baja.
     */
    EquipamientoResumenResponseDTO buscarResumen(Long idEquipamiento);

    /**
     * Lista las operaciones de un equipamiento agrupadas por tipo (limpiezas, fallas y mantenimientos), anuladas
     * incluidas, de la de fecha más reciente a la más antigua. Solo la última operación registrada del equipamiento
     * (la de mayor fecha y, a igualdad, la de mayor ID; de cualquier tipo) se marca como anulable, y solo si el
     * equipamiento no está en uso por un lote.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return Las operaciones del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     */
    OperacionesEquipamientoResponseDTO listarOperaciones(Long idEquipamiento);

    /**
     * Lista el historial de un equipamiento: una fila por cada etapa de lote en la que se usó de verdad y por cada
     * operación registrada (nunca las anuladas), de la más reciente a la más antigua. Ante igual fecha el orden es
     * determinista (por tipo y por identificador, de mayor a menor). Cada fila informa el estado operativo en el que
     * dejó al equipamiento.
     * <p>
     * Los filtros de fecha aplican a la fecha de la operación o al inicio de la etapa; un extremo nulo no acota por ese
     * lado y un tipo nulo no filtra por tipo.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param tipo Tipo de actividad a mostrar, o {@code null} para todas.
     * @param desde Límite inferior (inclusive) de la fecha, o {@code null} para no acotarlo.
     * @param hasta Límite superior (inclusive) de la fecha, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación (el orden lo define el servicio).
     * @return Una página del historial.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     * @throws ReglaNegocioException Si {@code hasta} es anterior a {@code desde}.
     */
    Page<ActividadEquipamientoResponseDTO> listarHistorial(Long idEquipamiento, TipoActividadEquipamiento tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
