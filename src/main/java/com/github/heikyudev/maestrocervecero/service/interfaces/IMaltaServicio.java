package com.github.heikyudev.maestrocervecero.service.interfaces;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.MaltaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de maltas.
 * <p>
 * Todas las operaciones actúan únicamente sobre maltas activas: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IMaltaServicio {

    /**
     * Obtiene una página de maltas activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de maltas activas en formato DTO.
     */
    Page<MaltaResponseDTO> findAll(Pageable pageable);

    /**
     * Obtiene una malta activa por su ID.
     *
     * @param id El ID de la malta.
     * @return La malta correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una malta activa con el ID especificado.
     */
    MaltaResponseDTO obtenerPorId(Long id);

    /**
     * Registra una nueva malta.
     *
     * @param maltaFormDTO Los datos de la malta a registrar.
     * @return La malta registrada.
     * @throws ReglaNegocioException Si el rendimiento no se encuentra entre 0 y 100 inclusive.
     * @throws RecursoDuplicadoException Si ya existe una malta activa con el mismo nombre (case-insensitive).
     */
    MaltaResponseDTO altaMalta(MaltaFormDTO maltaFormDTO);

    /**
     * Modifica una malta existente.
     *
     * @param id El ID de la malta a modificar.
     * @param maltaFormDTO Los nuevos datos de la malta.
     * @return La malta modificada.
     * @throws ReglaNegocioException Si el rendimiento no se encuentra entre 0 y 100 inclusive.
     * @throws RecursoNoEncontradoException Si no existe una malta activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otra malta activa (case-insensitive).
     */
    MaltaResponseDTO modificarMalta(Long id, MaltaFormDTO maltaFormDTO);

    /**
     * Elimina lógicamente una malta por su ID.
     *
     * @param id El ID de la malta a eliminar.
     * @return La malta eliminada.
     * @throws RecursoNoEncontradoException Si no existe una malta activa con el ID especificado.
     */
    MaltaResponseDTO bajaMalta(Long id);
}
