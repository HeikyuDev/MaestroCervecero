package com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de localidades.
 * <p>
 * Todas las operaciones actúan únicamente sobre localidades activas: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listadas, ni obtenidas, ni modificables, ni re-eliminables.
 * </p>
 */
public interface ILocalidadServicio {

    /**
     * Obtiene una página de localidades activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de localidades activas en formato DTO.
     */
    Page<LocalidadResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una localidad activa por su ID.
     *
     * @param id El ID de la localidad.
     * @return La localidad correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado.
     */
    LocalidadResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva localidad.
     *
     * @param localidadFormDTO Los datos de la localidad a registrar.
     * @return La localidad registrada.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si ya existe una localidad activa con el mismo nombre (case-insensitive) para la misma provincia.
     */
    LocalidadResponseDTO altaLocalidad(LocalidadFormDTO localidadFormDTO);

    /**
     * Modifica una localidad existente.
     *
     * @param id El ID de la localidad a modificar.
     * @param localidadFormDTO Los nuevos datos de la localidad.
     * @return La localidad modificada.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado, o si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra localidad activa (case-insensitive) de la misma provincia.
     */
    LocalidadResponseDTO modificarLocalidad(Long id, LocalidadFormDTO localidadFormDTO);

    /**
     * Elimina lógicamente una localidad por su ID.
     *
     * @param id El ID de la localidad a eliminar.
     * @return La localidad eliminada.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID especificado.
     */
    LocalidadResponseDTO bajaLocalidad(Long id);
}
