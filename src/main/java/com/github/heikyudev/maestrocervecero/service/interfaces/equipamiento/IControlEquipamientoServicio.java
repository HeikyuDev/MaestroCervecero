package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz que define las consultas de las pantallas de control de equipamiento: el listado con su estado y su
 * actividad reciente, el resumen de un equipo, el historial de sus operaciones y su línea de tiempo.
 * <p>
 * Es un servicio de solo lectura: las operaciones del ciclo de vida (limpieza, falla y mantenimiento) se registran y
 * anulan con sus propios servicios.
 * </p>
 */
public interface IControlEquipamientoServicio {

    /**
     * Obtiene una página de equipamientos activos con los datos de control: tipo, estado operativo, usos desde el
     * último mantenimiento y máximo, y última operación registrada. Un parámetro nulo no restringe por ese criterio.
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
     * último mantenimiento y las fechas de fin de su última limpieza y su último mantenimiento registrados.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return El resumen del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe o está dado de baja.
     */
    EquipamientoResumenResponseDTO buscarResumen(Long idEquipamiento);

    /**
     * Lista las operaciones de un equipamiento agrupadas por tipo (limpiezas, fallas y mantenimientos), anuladas
     * incluidas, de la de inicio más reciente a la más antigua. Solo la última operación registrada del equipamiento
     * (la de mayor fecha de fin y, a igualdad, la de mayor ID; de cualquier tipo) se marca como anulable.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @return Las operaciones del equipamiento.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     */
    OperacionesEquipamientoResponseDTO listarOperaciones(Long idEquipamiento);

    /**
     * Arma la línea de tiempo de un equipamiento para un período: un bloque por cada etapa de lote con uso real y por
     * cada operación registrada (nunca las anuladas) que se superponga con el período, ordenados por inicio. Un
     * extremo nulo no acota el período por ese lado. Los bloques conservan sus fechas reales, sin recortarlas al
     * período.
     *
     * @param idEquipamiento El ID del equipamiento.
     * @param desde Inicio del período, o {@code null} para no acotarlo.
     * @param hasta Fin del período, o {@code null} para no acotarlo.
     * @return Los bloques de la línea de tiempo.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     * @throws ReglaNegocioException Si {@code hasta} es anterior a {@code desde}.
     */
    List<BloqueLineaTiempoEquipamientoResponseDTO> listarLineaDeTiempo(Long idEquipamiento, LocalDateTime desde, LocalDateTime hasta);
}
