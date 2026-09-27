package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FraccionamientoBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de fraccionamientos de barril.
 * <p>
 * Un fraccionamiento registra la extracción de una cantidad de cerveza de un barril (merma,
 * testeo, embotellado, etc.), descontándola de su contenido actual. Si el contenido llega a cero,
 * el barril pasa a estado operativo {@code EN_LIMPIEZA}; de lo contrario, se mantiene en
 * {@code CON_CERVEZA}. No es un CRUD: solo admite su registro inicial y una anulación
 * excepcional, nunca modificación.
 * </p>
 */
public interface IFraccionamientoBarrilServicio {

    /**
     * Filtra los fraccionamientos de barril, opcionalmente por estado, barril y/o rango de fecha
     * de fraccionamiento.
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDesde Límite inferior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de fraccionamientos de barril en formato DTO que cumplen los criterios indicados.
     */
    Page<FraccionamientoBarrilResponseDTO> filtrarFraccionamientosBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);

    /**
     * Obtiene un fraccionamiento de barril por su ID.
     *
     * @param id El ID del fraccionamiento de barril.
     * @return El fraccionamiento de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún fraccionamiento de barril con el ID especificado.
     */
    FraccionamientoBarrilResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo fraccionamiento de barril.
     *
     * @param fraccionamientoBarrilFormDTO Los datos del fraccionamiento a registrar.
     * @return El fraccionamiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha o las observaciones no fueron informadas, si la cantidad a extraer no es mayor a cero o supera el contenido actual del barril, si la fecha no es posterior a la última operación registrada del ciclo de vida del barril, o si el barril no se encuentra en estado operativo {@code CON_CERVEZA}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    FraccionamientoBarrilResponseDTO registrarFraccionamientoBarril(FraccionamientoBarrilFormDTO fraccionamientoBarrilFormDTO);

    /**
     * Anula un fraccionamiento de barril existente.
     *
     * @param id El ID del fraccionamiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El fraccionamiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el fraccionamiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en el estado operativo que dejó este fraccionamiento.
     * @throws RecursoNoEncontradoException Si el fraccionamiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    FraccionamientoBarrilResponseDTO anularFraccionamientoBarril(Long id, AnulacionFraccionamientoBarrilFormDTO anulacionFormDTO);
}
