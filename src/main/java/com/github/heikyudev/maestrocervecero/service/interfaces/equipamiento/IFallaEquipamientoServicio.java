package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de fallas de equipamiento.
 * <p>
 * No es un CRUD: una falla es un registro transaccional que solo admite su registro inicial
 * y una anulación excepcional, nunca modificación.
 * </p>
 */
public interface IFallaEquipamientoServicio {

    /**
     * Filtra las fallas de equipamiento, opcionalmente por equipamiento, estado, rango de fecha
     * de falla y/o tipo concreto de equipamiento.
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar (Molino/Macerador/OllaHervor/Fermentador), o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fallas de equipamiento en formato DTO que cumplen los criterios indicados.
     */
    Page<FallaEquipamientoResponseDTO> filtrarFallasEquipamiento(Long idEquipamiento, EstadoTransaccion estado, LocalDateTime fechaFallaDesde, LocalDateTime fechaFallaHasta, TipoEquipamiento tipoEquipamiento, Pageable pageable);

    /**
     * Obtiene una falla de equipamiento por su ID.
     *
     * @param id El ID de la falla de equipamiento.
     * @return La falla de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna falla de equipamiento con el ID especificado.
     */
    FallaEquipamientoResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva falla de equipamiento.
     *
     * @param fallaEquipamientoFormDTO Los datos de la falla a registrar.
     * @return La falla de equipamiento registrada.
     * @throws ReglaNegocioException Si la fecha de falla no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    FallaEquipamientoResponseDTO registrarFallaEquipamiento(FallaEquipamientoFormDTO fallaEquipamientoFormDTO);

    /**
     * Anula una falla de equipamiento existente.
     *
     * @param id El ID de la falla de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de equipamiento anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la falla no se encuentra en estado {@code REGISTRADO}, o si el equipamiento asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    FallaEquipamientoResponseDTO anularFallaEquipamiento(Long id, AnulacionFallaEquipamientoFormDTO anulacionFormDTO);
}
