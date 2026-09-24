package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionLimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.LimpiezaBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de limpiezas de barril.
 * <p>
 * Al registrar una limpieza, el service decide si el barril vuelve a estado operativo
 * {@code DISPONIBLE} o pasa a {@code EN_MANTENIMIENTO}, según la cantidad de limpiezas
 * registradas desde el último mantenimiento (o desde siempre, si nunca tuvo uno) comparada
 * contra {@code usosMaximosAntesMantenimiento} del barril. No es un CRUD: solo admite su
 * registro inicial y una anulación excepcional, nunca modificación.
 * </p>
 */
public interface ILimpiezaBarrilServicio {

    /**
     * Filtra las limpiezas de barril, opcionalmente por estado, barril y/o rango de fecha de
     * limpieza.
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de limpiezas de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<LimpiezaBarrilResponseDTO> filtrarLimpiezasBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaLimpiezaDesde, LocalDateTime fechaLimpiezaHasta, Pageable pageable);

    /**
     * Obtiene una limpieza de barril por su ID.
     *
     * @param id El ID de la limpieza de barril.
     * @return La limpieza de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna limpieza de barril con el ID especificado.
     */
    LimpiezaBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva limpieza de barril.
     *
     * @param limpiezaBarrilFormDTO Los datos de la limpieza a registrar.
     * @return La limpieza de barril registrada.
     * @throws ReglaNegocioException Si la fecha de limpieza no fue informada, si las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    LimpiezaBarrilResponseDTO registrarLimpiezaBarril(LimpiezaBarrilFormDTO limpiezaBarrilFormDTO);

    /**
     * Anula una limpieza de barril existente.
     *
     * @param id El ID de la limpieza de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La limpieza de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la limpieza no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en el estado operativo que dejó esta limpieza.
     * @throws RecursoNoEncontradoException Si la limpieza de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    LimpiezaBarrilResponseDTO anularLimpiezaBarril(Long id, AnulacionLimpiezaBarrilFormDTO anulacionFormDTO);
}
