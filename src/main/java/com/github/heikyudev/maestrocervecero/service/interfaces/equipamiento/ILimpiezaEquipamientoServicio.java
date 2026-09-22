package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionLimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.LimpiezaEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interfaz que define los servicios relacionados con la gestión de limpiezas de equipamiento.
 * <p>
 * Al registrar una limpieza, el service decide si el equipamiento vuelve a estado operativo
 * {@code DISPONIBLE} o pasa a {@code EN_MANTENIMIENTO}, según la cantidad de limpiezas
 * registradas desde el último mantenimiento (o desde siempre, si nunca tuvo uno) comparada
 * contra {@code usosMaximosAntesMantenimiento} del equipamiento. No es un CRUD: solo admite su
 * registro inicial y una anulación excepcional, nunca modificación.
 * </p>
 */
public interface ILimpiezaEquipamientoServicio {

    /**
     * Filtra las limpiezas de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de limpieza.
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar (Molino/Macerador/OllaHervor/Fermentador), o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de limpiezas de equipamiento en formato DTO que cumplen los criterios indicados.
     */
    Page<LimpiezaEquipamientoResponseDTO> filtrarLimpiezasEquipamiento(Long idEquipamiento, TipoEquipamiento tipoEquipamiento, EstadoTransaccion estado, LocalDateTime fechaLimpiezaDesde, LocalDateTime fechaLimpiezaHasta, Pageable pageable);

    /**
     * Obtiene una limpieza de equipamiento por su ID.
     *
     * @param id El ID de la limpieza de equipamiento.
     * @return La limpieza de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna limpieza de equipamiento con el ID especificado.
     */
    LimpiezaEquipamientoResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva limpieza de equipamiento.
     *
     * @param limpiezaEquipamientoFormDTO Los datos de la limpieza a registrar.
     * @return La limpieza de equipamiento registrada.
     * @throws ReglaNegocioException Si la fecha de limpieza no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    LimpiezaEquipamientoResponseDTO registrarLimpiezaEquipamiento(LimpiezaEquipamientoFormDTO limpiezaEquipamientoFormDTO);

    /**
     * Anula una limpieza de equipamiento existente.
     *
     * @param id El ID de la limpieza de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La limpieza de equipamiento anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la limpieza no se encuentra en estado {@code REGISTRADO}, o si el equipamiento asociado no se encuentra en el estado operativo que dejó esta limpieza.
     * @throws RecursoNoEncontradoException Si la limpieza de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    LimpiezaEquipamientoResponseDTO anularLimpiezaEquipamiento(Long id, AnulacionLimpiezaEquipamientoFormDTO anulacionFormDTO);
}
