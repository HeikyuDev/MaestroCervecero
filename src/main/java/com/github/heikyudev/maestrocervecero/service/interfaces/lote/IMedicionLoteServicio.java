package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularMedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.MedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.MedicionLoteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con el registro de mediciones de parámetros de
 * control sobre las etapas de un lote en ejecución.
 */
public interface IMedicionLoteServicio {

    /**
     * Obtiene una página de mediciones de lote.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de mediciones en formato DTO.
     */
    Page<MedicionLoteResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una medición de lote por su ID.
     *
     * @param id El ID de la medición.
     * @return La medición correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     */
    MedicionLoteResponseDTO buscarPorId(Long id);

    /**
     * Registra una medición de un parámetro de control sobre una etapa de un lote.
     *
     * @param medicionLoteFormDTO Los datos de la medición a registrar.
     * @return La medición registrada.
     * @throws RecursoNoEncontradoException Si el detalle de parámetro de control o la etapa de
     *                                      lote referenciados no existen.
     * @throws ReglaNegocioException Si la etapa referenciada no se encuentra en curso, o si el
     *                               detalle de parámetro de control no corresponde a la etapa
     *                               referenciada.
     */
    MedicionLoteResponseDTO registrarMedicion(MedicionLoteFormDTO medicionLoteFormDTO);

    /**
     * Anula una medición de lote previamente registrada.
     *
     * @param id El ID de la medición a anular.
     * @param anularMedicionLoteFormDTO Los datos de la anulación (motivo).
     * @return La medición anulada.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la medición no
     *                               se encuentra en estado REGISTRADO, o si el lote asociado no se
     *                               encuentra en estado EN_EJECUCION.
     */
    MedicionLoteResponseDTO anularMedicion(Long id, AnularMedicionLoteFormDTO anularMedicionLoteFormDTO);
}
