package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de lotes.
 */
public interface ILoteServicio {

    /**
     * Obtiene una página de lotes.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de lotes en formato DTO.
     */
    Page<LoteResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un lote por su ID.
     *
     * @param id El ID del lote.
     * @return El lote correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     */
    LoteResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo lote, reservando su lugar en el cronograma de uso de equipamiento.
     * <p>
     * Registrar un lote no es lo mismo que iniciarlo: en este paso no se valida stock de insumos
     * ni disponibilidad de los equipos seleccionados. Al registrarse, el sistema genera
     * automáticamente el identificador interno del lote (nombre de la receta + número de lote de
     * esa receta) y crea sus 6 etapas en estado PENDIENTE, cada una asociada al equipamiento
     * correspondiente (Molino para Molienda, Macerador para Maceración, Olla de Hervor para
     * Hervido, y el mismo Fermentador para Fermentación, Maduración y Envasado).
     * </p>
     *
     * @param loteFormDTO Los datos del lote a registrar.
     * @return El lote registrado.
     * @throws RecursoNoEncontradoException Si la planificación de producción o alguno de los equipamientos referenciados no existe.
     */
    LoteResponseDTO registrarLote(LoteFormDTO loteFormDTO);
}
