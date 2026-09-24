package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FallaBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de fallas de barril.
 * <p>
 * No es un CRUD: una falla es un registro transaccional que solo admite su registro inicial y
 * una anulación excepcional, nunca modificación.
 * </p>
 */
public interface IFallaBarrilServicio {

    /**
     * Filtra las fallas de barril, opcionalmente por estado, barril y/o rango de fecha de falla.
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de fallas de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<FallaBarrilResponseDTO> filtrarFallasBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaFallaDesde, LocalDateTime fechaFallaHasta, Pageable pageable);

    /**
     * Obtiene una falla de barril por su ID.
     *
     * @param id El ID de la falla de barril.
     * @return La falla de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna falla de barril con el ID especificado.
     */
    FallaBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva falla de barril.
     *
     * @param fallaBarrilFormDTO Los datos de la falla a registrar.
     * @return La falla de barril registrada.
     * @throws ReglaNegocioException Si la fecha de falla no fue informada, si las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    FallaBarrilResponseDTO registrarFallaBarril(FallaBarrilFormDTO fallaBarrilFormDTO);

    /**
     * Anula una falla de barril existente.
     *
     * @param id El ID de la falla de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la falla no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    FallaBarrilResponseDTO anularFallaBarril(Long id, AnulacionFallaBarrilFormDTO anulacionFormDTO);
}
