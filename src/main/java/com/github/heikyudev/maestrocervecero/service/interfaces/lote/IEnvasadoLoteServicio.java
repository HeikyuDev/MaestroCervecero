package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ResumenEnvasadoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con el registro del envasado de un lote, es
 * decir, el traspaso de cerveza desde el fermentador hacia un barril durante la etapa de
 * Envasado.
 */
public interface IEnvasadoLoteServicio {

    /**
     * Filtra los envasados de lote de una etapa de lote puntual.
     * <p>
     * {@code idEtapaLote} no lo tipea el usuario: lo determina el contexto de la pantalla de
     * gestión de envasados, igual que en {@code filtrarConsumosInsumo}. No tiene sentido mostrar
     * envasados de otros lotes o de otras etapas mezclados: el usuario solo debe ver los que él
     * mismo registró en la etapa en la que está parado.
     * </p>
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: {@code null} muestra envasados en
     * cualquier estado, igual que el resto de los {@code filtrarX} que exponen este criterio.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param idBarril El ID del barril utilizado a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de envasados en formato DTO que cumplen los criterios indicados.
     * @throws ReglaNegocioException Si no se informó la etapa de lote.
     */
    Page<EnvasadoLoteResponseDTO> filtrarEnvasadosLote(Long idEtapaLote, Long idBarril, EstadoTransaccion estado, Pageable pageable);

    /**
     * Obtiene un envasado de lote por su ID.
     *
     * @param id El ID del envasado.
     * @return El envasado correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un envasado con el ID especificado.
     */
    EnvasadoLoteResponseDTO buscarPorId(Long id);

    /**
     * Registra el traspaso de cerveza desde el fermentador hacia el barril seleccionado, durante
     * la etapa de Envasado de un lote.
     * <p>
     * Además de la capacidad del barril, el único tope es la capacidad útil del fermentador del
     * lote: el total envasado registrado (lo ya registrado más esta cantidad) no puede superarla.
     * Se puede envasar más que el volumen objetivo del lote, porque la producción real puede superar
     * lo planificado.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el envasado (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param registrarEnvasadoLoteFormDTO Los datos del envasado a registrar.
     * @return El envasado registrado.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el barril referenciados no existen.
     * @throws ReglaNegocioException Si el barril o la cantidad a envasar no son válidos, si el lote no está en ejecución, si la
     *                               etapa no está en curso o no admite envasados, si el barril no está disponible, o si la
     *                               cantidad supera la capacidad del barril o la capacidad útil del fermentador del lote.
     */
    EnvasadoLoteResponseDTO registrarEnvasadoLote(Long idEtapaLote, RegistrarEnvasadoLoteFormDTO registrarEnvasadoLoteFormDTO);

    /**
     * Anula un envasado de lote existente.
     * <p>
     * No existe la baja lógica para este registro: un envasado solo puede pasar de
     * {@code REGISTRADO} a {@code ANULADO}, nunca eliminarse.
     * </p>
     *
     * @param id El ID del envasado de lote a anular.
     * @param anularEnvasadoLoteFormDTO Los datos de la anulación (motivo).
     * @return El envasado de lote anulado.
     * @throws RecursoNoEncontradoException Si el envasado de lote con el ID especificado no existe.
     */
    EnvasadoLoteResponseDTO anularEnvasadoLote(Long id, AnularEnvasadoLoteFormDTO anularEnvasadoLoteFormDTO);

    /**
     * Resume el envasado de una etapa de Envasado: lo envasado hasta ahora (envasados en estado REGISTRADO), la capacidad
     * útil del fermentador de la etapa (el único tope del envasado), lo que resta hasta ese tope (nunca negativo), el
     * exceso sobre el volumen objetivo del lote (informativo) y el identificador del fermentador. Es de solo lectura y
     * sirve para un lote en cualquier estado.
     *
     * @param idEtapaLote El ID de la etapa de Envasado.
     * @return El resumen del envasado.
     * @throws RecursoNoEncontradoException Si no existe la etapa de lote.
     * @throws ReglaNegocioException Si no se informó la etapa, si no es una etapa que admita envasados o si no tiene un
     *                               fermentador como equipamiento.
     */
    ResumenEnvasadoResponseDTO resumirEnvasado(Long idEtapaLote);

    /**
     * Indica si el lote tiene envasados en estado REGISTRADO (en cuyo caso no se puede cancelar).
     *
     * @param idLote El ID del lote.
     * @return {@code true} si tiene al menos un envasado registrado.
     * @throws RecursoNoEncontradoException Si no existe el lote.
     * @throws ReglaNegocioException Si no se informó el lote.
     */
    boolean existenEnvasadosRegistrados(Long idLote);
}
