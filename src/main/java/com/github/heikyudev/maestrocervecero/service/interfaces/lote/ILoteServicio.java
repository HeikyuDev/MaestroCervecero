package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
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

    /**
     * Inicia un lote previamente registrado, dando paso a su ejecución real.
     * <p>
     * A diferencia de {@link #registrarLote(LoteFormDTO)}, este paso sí valida disponibilidad
     * real: escala todos los insumos de la receta al volumen objetivo del lote, verifica que
     * haya stock suficiente de cada uno y que el equipamiento de cada etapa (Molino, Macerador,
     * Olla de Hervor, Fermentador) esté disponible, y reserva los lotes de insumo necesarios
     * aplicando el criterio FEFO (First Expired, First Out).
     * </p>
     *
     * @param id El ID del lote a iniciar.
     * @return El lote iniciado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado PENDIENTE, si no hay
     *                               stock suficiente de algún insumo, o si algún equipamiento
     *                               requerido no está disponible.
     */
    LoteResponseDTO iniciarLote(Long id);

    /**
     * Cancela un lote en curso.
     * <p>
     * A diferencia de una anulación, cancelar un lote NO revierte los consumos de insumo ya
     * ejecutados en sus etapas — esos consumos quedan firmes. Lo que sí hace:
     * </p>
     * <ul>
     *     <li>Libera automáticamente todas las reservas de insumo del lote todavía pendientes.</li>
     *     <li>El equipamiento asociado a la etapa actualmente EN_CURSO pasa a estado
     *     "En Limpieza".</li>
     *     <li>El equipamiento asociado a etapas todavía no ejecutadas pasa a estado "Disponible".</li>
     *     <li>El resto del equipamiento (ya usado en etapas previas) mantiene su estado.</li>
     * </ul>
     *
     * @param id El ID del lote a cancelar.
     * @param cancelacionLoteFormDTO Los datos de la cancelación (motivo).
     * @return El lote cancelado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de cancelación no fue informado, o si el lote no
     *                               se encuentra en estado PENDIENTE o EN_EJECUCION.
     */
    LoteResponseDTO cancelarLote(Long id, CancelacionLoteFormDTO cancelacionLoteFormDTO);

    /**
     * Finaliza la etapa de Molienda del lote y da paso a la Maceración.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * </p>
     * <ul>
     *     <li>La etapa de Molienda se desmarca como etapa actual (pasa a FINALIZADA) y se registra
     *     su fecha y hora de fin.</li>
     *     <li>La etapa de Maceración se marca como etapa actual (pasa a EN_CURSO) y se registra su
     *     fecha y hora de inicio.</li>
     *     <li>El molino utilizado pasa a estado "En Limpieza".</li>
     * </ul>
     *
     * @param id El ID del lote cuya Molienda se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Molienda.
     */
    LoteResponseDTO finalizarMolienda(Long id);
}
