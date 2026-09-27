package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de devoluciones de barril.
 * <p>
 * Una devolución registra que un barril despachado a un cliente volvió a la fábrica, dejándolo en
 * estado operativo {@code EN_LIMPIEZA}: todo barril que vuelve de afuera pasa por limpieza, por
 * motivos sanitarios, sin importar el contenido restante. No es un CRUD: solo admite su registro
 * inicial y una anulación excepcional, nunca modificación.
 * </p>
 */
public interface IDevolucionBarrilServicio {

    /**
     * Filtra las devoluciones de barril, opcionalmente por estado, barril y/o rango de fecha de
     * devolución.
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDevolucionDesde Límite inferior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param fechaDevolucionHasta Límite superior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de devoluciones de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<DevolucionBarrilResponseDTO> filtrarDevolucionesBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaDevolucionDesde, LocalDateTime fechaDevolucionHasta, Pageable pageable);

    /**
     * Obtiene una devolución de barril por su ID.
     *
     * @param id El ID de la devolución de barril.
     * @return La devolución de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna devolución de barril con el ID especificado.
     */
    DevolucionBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva devolución de barril.
     *
     * @param devolucionBarrilFormDTO Los datos de la devolución a registrar.
     * @return La devolución de barril registrada.
     * @throws ReglaNegocioException Si la fecha de devolución no fue informada, si las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code DESPACHADO}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    DevolucionBarrilResponseDTO registrarDevolucionBarril(DevolucionBarrilFormDTO devolucionBarrilFormDTO);

    /**
     * Anula una devolución de barril existente.
     *
     * @param id El ID de la devolución de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La devolución de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la devolución no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si la devolución de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    DevolucionBarrilResponseDTO anularDevolucionBarril(Long id, AnulacionDevolucionBarrilFormDTO anulacionFormDTO);
}
