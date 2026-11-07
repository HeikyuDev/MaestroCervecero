package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de las pantallas de control de barriles: la lista con su estado y sus usos, el resumen de un barril, sus
 * operaciones y su historial.
 * <p>
 * Es un servicio de solo lectura: las operaciones del ciclo de vida se registran y anulan con sus propios servicios.
 * </p>
 */
public interface IControlBarrilServicio {

    /**
     * Obtiene una página de barriles activos con los datos de control. Un parámetro nulo no restringe por ese criterio.
     * El orden es natural por identificador interno.
     *
     * @param identificador Texto a buscar dentro del identificador interno, o {@code null}.
     * @param idFabricante ID del fabricante a filtrar, o {@code null}.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null}.
     * @param pageable La configuración de paginación.
     * @return Una página de barriles activos.
     */
    Page<BarrilControlResponseDTO> filtrarBarrilesParaControl(String identificador, Long idFabricante, EstadoOperativoBarril estadoOperativo, Pageable pageable);

    /**
     * Obtiene el resumen de un barril activo: sus datos, sus usos desde el último mantenimiento y las fechas de su
     * última limpieza y su último mantenimiento registrados.
     *
     * @param idBarril El ID del barril.
     * @return El resumen del barril.
     * @throws RecursoNoEncontradoException Si el barril no existe o está dado de baja.
     */
    BarrilResumenResponseDTO buscarResumen(Long idBarril);

    /**
     * Lista las operaciones de un tipo de un barril, anuladas incluidas, de la de fecha más reciente a la más antigua.
     * Solo la última operación registrada del barril (de cualquier tipo; mayor fecha y, a igualdad, mayor ID) se marca
     * como anulable, y solo si el barril no está en uso por un lote y la operación es posterior a su último envasado.
     *
     * @param idBarril El ID del barril.
     * @param tipo Tipo de operación a listar.
     * @return Las operaciones del barril de ese tipo.
     * @throws RecursoNoEncontradoException Si el barril no existe.
     */
    List<OperacionBarrilResponseDTO> listarOperaciones(Long idBarril, TipoOperacionBarril tipo);

    /**
     * Lista el historial de un barril: una fila por cada envasado registrado y por cada operación registrada (nunca las
     * anuladas), de la más reciente a la más antigua. Cada fila informa el estado operativo en el que dejó al barril.
     *
     * @param idBarril El ID del barril.
     * @param tipo Tipo de actividad a mostrar, o {@code null} para todas.
     * @param desde Límite inferior (inclusive) de la fecha, o {@code null}.
     * @param hasta Límite superior (inclusive) de la fecha, o {@code null}.
     * @param pageable La configuración de paginación (el orden lo define el servicio).
     * @return Una página del historial.
     * @throws RecursoNoEncontradoException Si el barril no existe.
     * @throws ReglaNegocioException Si {@code hasta} es anterior a {@code desde}.
     */
    Page<ActividadBarrilResponseDTO> listarHistorial(Long idBarril, TipoActividadBarril tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
