package com.github.heikyudev.maestrocervecero.service.interfaces.costo_adicional;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional.CostoDirectoAdicionalFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de costos directos adicionales.
 * <p>
 * Todas las operaciones actúan únicamente sobre costos directos adicionales activos: los
 * registros con baja lógica (soft delete) no son listados, ni obtenidos, ni modificables, ni
 * re-eliminables.
 * </p>
 */
public interface ICostoDirectoAdicionalServicio {

    /**
     * Obtiene una página de costos directos adicionales activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de costos directos adicionales activos en formato DTO.
     */
    Page<CostoDirectoAdicionalResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un costo directo adicional activo por su ID.
     *
     * @param id El ID del costo directo adicional.
     * @return El costo directo adicional correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un costo directo adicional activo con el ID especificado.
     */
    CostoDirectoAdicionalResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo costo directo adicional.
     *
     * @param costoDirectoAdicionalFormDTO Los datos del costo directo adicional a registrar.
     * @return El costo directo adicional registrado.
     * @throws ReglaNegocioException Si el costo por litro es nulo o menor o igual a cero.
     * @throws RecursoDuplicadoException Si ya existe un costo directo adicional activo con el mismo nombre (case-insensitive).
     */
    CostoDirectoAdicionalResponseDTO altaCostoDirectoAdicional(CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO);

    /**
     * Modifica un costo directo adicional existente.
     *
     * @param id El ID del costo directo adicional a modificar.
     * @param costoDirectoAdicionalFormDTO Los nuevos datos del costo directo adicional.
     * @return El costo directo adicional modificado.
     * @throws ReglaNegocioException Si el costo por litro es nulo o menor o igual a cero.
     * @throws RecursoNoEncontradoException Si no existe un costo directo adicional activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro costo directo adicional activo (case-insensitive).
     */
    CostoDirectoAdicionalResponseDTO modificarCostoDirectoAdicional(Long id, CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO);

    /**
     * Elimina lógicamente un costo directo adicional por su ID.
     *
     * @param id El ID del costo directo adicional a eliminar.
     * @return El costo directo adicional eliminado.
     * @throws RecursoNoEncontradoException Si no existe un costo directo adicional activo con el ID especificado.
     */
    CostoDirectoAdicionalResponseDTO bajaCostoDirectoAdicional(Long id);
}
