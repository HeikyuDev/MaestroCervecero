package com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de países.
 * <p>
 * Todas las operaciones actúan únicamente sobre países activos: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IPaisServicio {

    /**
     * Obtiene una página de países activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de países activos en formato DTO.
     */
    Page<PaisResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un país activo por su ID.
     *
     * @param id El ID del país.
     * @return El país correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un país activo con el ID especificado.
     */
    PaisResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo país.
     *
     * @param paisFormDTO Los datos del país a registrar.
     * @return El país registrado.
     * @throws RecursoDuplicadoException Si ya existe un país activo con el mismo nombre (case-insensitive).
     */
    PaisResponseDTO altaPais(PaisFormDTO paisFormDTO);

    /**
     * Modifica un país existente.
     *
     * @param id El ID del país a modificar.
     * @param paisFormDTO Los nuevos datos del país.
     * @return El país modificado.
     * @throws RecursoNoEncontradoException Si no existe un país activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro país activo (case-insensitive).
     */
    PaisResponseDTO modificarPais(Long id, PaisFormDTO paisFormDTO);

    /**
     * Elimina lógicamente un país por su ID.
     *
     * @param id El ID del país a eliminar.
     * @return El país eliminado.
     * @throws RecursoNoEncontradoException Si no existe un país activo con el ID especificado.
     * @throws ReglaNegocioException Si el país tiene al menos una provincia activa asociada.
     */
    PaisResponseDTO bajaPais(Long id);
}
