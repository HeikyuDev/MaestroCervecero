package com.github.heikyudev.maestrocervecero.service.interfaces;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.LevaduraResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de levaduras.
 * <p>
 * Todas las operaciones actúan únicamente sobre levaduras activas: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface ILevaduraServicio {

    /**
     * Obtiene una página de levaduras activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de levaduras activas en formato DTO.
     */
    Page<LevaduraResponseDTO> findAll(Pageable pageable);

    /**
     * Obtiene una levadura activa por su ID.
     *
     * @param id El ID de la levadura.
     * @return La levadura correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una levadura activa con el ID especificado.
     */
    LevaduraResponseDTO obtenerPorId(Long id);

    /**
     * Registra una nueva levadura.
     *
     * @param levaduraFormDTO Los datos de la levadura a registrar.
     * @return La levadura registrada.
     * @throws ReglaNegocioException Si la cantidad de células por gramo no es positiva (mayor a 0).
     * @throws RecursoDuplicadoException Si ya existe una levadura activa con el mismo nombre (case-insensitive).
     */
    LevaduraResponseDTO altaLevadura(LevaduraFormDTO levaduraFormDTO);

    /**
     * Modifica una levadura existente.
     *
     * @param id El ID de la levadura a modificar.
     * @param levaduraFormDTO Los nuevos datos de la levadura.
     * @return La levadura modificada.
     * @throws ReglaNegocioException Si la cantidad de células por gramo no es positiva (mayor a 0).
     * @throws RecursoNoEncontradoException Si no existe una levadura activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra levadura activa (case-insensitive).
     */
    LevaduraResponseDTO modificarLevadura(Long id, LevaduraFormDTO levaduraFormDTO);

    /**
     * Elimina lógicamente una levadura por su ID.
     *
     * @param id El ID de la levadura a eliminar.
     * @return La levadura eliminada.
     * @throws RecursoNoEncontradoException Si no existe una levadura activa con el ID especificado.
     */
    LevaduraResponseDTO bajaLevadura(Long id);
}
