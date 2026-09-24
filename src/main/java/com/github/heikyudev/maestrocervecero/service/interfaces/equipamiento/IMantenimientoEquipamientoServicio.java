package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionMantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MantenimientoEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de mantenimientos de
 * equipamiento.
 * <p>
 * Un mantenimiento registra la finalización de un procedimiento (correctivo, tras una falla, o
 * preventivo) que devuelve un equipamiento al estado operativo {@code DISPONIBLE}. No es un CRUD:
 * solo admite su registro inicial y una anulación excepcional, nunca modificación.
 * </p>
 */
public interface IMantenimientoEquipamientoServicio {

    /**
     * Filtra los mantenimientos de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de mantenimiento.
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar (Molino/Macerador/OllaHervor/Fermentador), o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de mantenimientos de equipamiento en formato DTO que cumplen los criterios indicados.
     */
    Page<MantenimientoEquipamientoResponseDTO> filtrarMantenimientosEquipamiento(Long idEquipamiento, TipoEquipamiento tipoEquipamiento, EstadoTransaccion estado, LocalDateTime fechaMantenimientoDesde, LocalDateTime fechaMantenimientoHasta, Pageable pageable);

    /**
     * Obtiene un mantenimiento de equipamiento por su ID.
     *
     * @param id El ID del mantenimiento de equipamiento.
     * @return El mantenimiento de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún mantenimiento de equipamiento con el ID especificado.
     */
    MantenimientoEquipamientoResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo mantenimiento de equipamiento.
     *
     * @param mantenimientoEquipamientoFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de equipamiento registrado.
     * @throws ReglaNegocioException Si la fecha de mantenimiento no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    MantenimientoEquipamientoResponseDTO registrarMantenimientoEquipamiento(MantenimientoEquipamientoFormDTO mantenimientoEquipamientoFormDTO);

    /**
     * Anula un mantenimiento de equipamiento existente.
     *
     * @param id El ID del mantenimiento de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de equipamiento anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el equipamiento, o si el equipamiento asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    MantenimientoEquipamientoResponseDTO anularMantenimientoEquipamiento(Long id, AnulacionMantenimientoEquipamientoFormDTO anulacionFormDTO);
}
