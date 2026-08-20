package com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.ProvinciaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de provincias.
 * <p>
 * Todas las operaciones actúan únicamente sobre provincias activas: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listadas, ni obtenidas, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IProvinciaServicio {

    /**
     * Obtiene una página de provincias activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de provincias activas en formato DTO.
     */
    Page<ProvinciaResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una provincia activa por su ID.
     *
     * @param id El ID de la provincia.
     * @return La provincia correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado.
     */
    ProvinciaResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva provincia.
     *
     * @param provinciaFormDTO Los datos de la provincia a registrar.
     * @return La provincia registrada.
     * @throws RecursoNoEncontradoException Si no existe un país activo con el ID especificado.
     * @throws RecursoDuplicadoException Si ya existe una provincia activa con el mismo nombre (case-insensitive).
     */
    ProvinciaResponseDTO altaProvincia(ProvinciaFormDTO provinciaFormDTO);

    /**
     * Modifica una provincia existente.
     *
     * @param id El ID de la provincia a modificar.
     * @param provinciaFormDTO Los nuevos datos de la provincia.
     * @return La provincia modificada.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado, o si no existe un país activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra provincia activa (case-insensitive).
     */
    ProvinciaResponseDTO modificarProvincia(Long id, ProvinciaFormDTO provinciaFormDTO);

    /**
     * Elimina lógicamente una provincia por su ID.
     *
     * @param id El ID de la provincia a eliminar.
     * @return La provincia eliminada.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado.
     * @throws ReglaNegocioException Si la provincia tiene al menos una localidad activa asociada.
     */
    ProvinciaResponseDTO bajaProvincia(Long id);
}
