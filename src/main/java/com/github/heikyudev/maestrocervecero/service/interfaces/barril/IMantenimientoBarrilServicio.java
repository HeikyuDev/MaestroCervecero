package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionMantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.MantenimientoBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de mantenimientos de barril.
 * <p>
 * Un mantenimiento registra la finalización de un procedimiento que devuelve un barril al estado
 * operativo {@code DISPONIBLE}. No es un CRUD: solo admite su registro inicial y una anulación
 * excepcional, nunca modificación.
 * </p>
 */
public interface IMantenimientoBarrilServicio {

    /**
     * Filtra los mantenimientos de barril, opcionalmente por barril, estado y/o rango de fecha de
     * mantenimiento.
     *
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mantenimientos de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<MantenimientoBarrilResponseDTO> filtrarMantenimientosBarril(Long idBarril, EstadoTransaccion estado, LocalDateTime fechaMantenimientoDesde, LocalDateTime fechaMantenimientoHasta, Pageable pageable);

    /**
     * Obtiene un mantenimiento de barril por su ID.
     *
     * @param id El ID del mantenimiento de barril.
     * @return El mantenimiento de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún mantenimiento de barril con el ID especificado.
     */
    MantenimientoBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo mantenimiento de barril.
     *
     * @param mantenimientoBarrilFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha de mantenimiento no fue informada, si las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    MantenimientoBarrilResponseDTO registrarMantenimientoBarril(MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO);

    /**
     * Anula un mantenimiento de barril existente.
     *
     * @param id El ID del mantenimiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    MantenimientoBarrilResponseDTO anularMantenimientoBarril(Long id, AnulacionMantenimientoBarrilFormDTO anulacionFormDTO);
}
