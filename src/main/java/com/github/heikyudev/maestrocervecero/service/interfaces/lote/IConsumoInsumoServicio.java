package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interfaz que define los servicios relacionados con el registro del consumo efectivo de
 * insumos durante la producción de un lote, ya sea contra una reserva existente o de forma
 * directa sobre cualquier lote de insumo del insumo requerido por la etapa.
 */
public interface IConsumoInsumoServicio {

    /**
     * Filtra los consumos de insumo de una etapa de lote puntual.
     * <p>
     * {@code idEtapaLote} no lo tipea el usuario: lo determina el contexto de la pantalla de
     * gestión de consumos, igual que en {@code filtrarInsumosRequeridos}. No tiene sentido mostrar
     * consumos de otros lotes o de otras etapas mezclados: el usuario solo debe ver los que él
     * mismo registró en la etapa en la que está parado.
     * </p>
     * <p>
     * {@code estado} sigue la misma regla que en {@code filtrarMedicionesLote}: si no se
     * especifica, el service asume {@code REGISTRADO} por defecto.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param tipoConsumo El tipo de consumo a filtrar (RESERVADO/DIRECTO), o {@code null} para no filtrar por él.
     * @param idInsumo El ID del insumo requerido a filtrar (útil cuando la etapa requiere más de
     *                 uno), o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para asumir {@code REGISTRADO}.
     * @param pageable La configuración de paginación.
     * @return Una página de consumos en formato DTO que cumplen los criterios indicados.
     */
    Page<ConsumoInsumoResponseDTO> filtrarConsumosInsumo(Long idEtapaLote, TipoConsumo tipoConsumo, Long idInsumo, EstadoTransaccion estado, Pageable pageable);

    /**
     * Obtiene un consumo de insumo por su ID.
     *
     * @param id El ID del consumo.
     * @return El consumo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un consumo con el ID especificado.
     */
    ConsumoInsumoResponseDTO buscarPorId(Long id);

    /**
     * Registra el consumo efectivo de un insumo, descontándolo de una reserva de insumo puntual
     * ya existente para esa etapa de lote y ese lote de insumo.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el consumo (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado.
     */
    ConsumoInsumoResponseDTO registrarConsumoInsumoReservado(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO);

    /**
     * Registra el consumo efectivo de un insumo por fuera de cualquier reserva, descontándolo
     * directamente de la cantidad disponible de un lote de insumo.
     * <p>
     * No requiere que la cantidad reservada de ese insumo para esa etapa esté agotada: el
     * operario puede elegir libremente registrar un consumo directo sobre cualquier lote de
     * insumo del insumo requerido por la etapa, en cualquier momento.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el consumo (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado.
     */
    ConsumoInsumoResponseDTO registrarConsumoInsumoDirecto(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO);

    /**
     * Obtiene, para una etapa de lote determinada, cuánto requiere de cada insumo según el
     * escalado de la receta y cuánto de eso ya fue consumido.
     * <p>
     * {@code idEtapaLote} no lo tipea el usuario: lo determina el contexto de la pantalla de
     * gestión de consumos (una etapa de lote puntual), igual que en {@code filtrarMedicionesLote}.
     * Deliberadamente NO se basa en {@code ReservaInsumoEntity} para la cantidad requerida: las
     * reservas son una asignación de stock que un ajuste (merma) puede reducir a 0, mientras que lo
     * que la receta realmente requiere no cambia — por eso se recalcula desde la receta cada vez,
     * acotado únicamente a esta etapa puntual ({@code IEscaladoInsumoServicio.calcularRequerimientosEtapa}).
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan consumos (obligatorio).
     * @return Un DTO por cada insumo requerido en esa etapa, con su cantidad requerida y consumida.
     */
    List<InsumoRequeridoResponseDTO> filtrarInsumosRequeridos(Long idEtapaLote);

    /**
     * Anula un consumo de insumo existente, devolviendo el stock que había descontado.
     * <p>
     * Qué se revierte depende de por qué camino se registró el consumo ({@code
     * ConsumoInsumoEntity.tipoConsumo}): uno {@code RESERVADO} devuelve la cantidad tanto a la
     * cantidad actual como a la reservada del lote de insumo, y además a la reserva puntual de esa
     * etapa; uno {@code DIRECTO} devuelve la cantidad únicamente a la cantidad actual.
     * </p>
     * <p>
     * No existe la baja lógica para este registro: un consumo solo puede pasar de
     * {@code REGISTRADO} a {@code ANULADO}, nunca eliminarse.
     * </p>
     *
     * @param id El ID del consumo de insumo a anular.
     * @param anularConsumoInsumoFormDTO Los datos de la anulación (motivo).
     * @return El consumo de insumo anulado.
     * @throws RecursoNoEncontradoException Si el consumo de insumo con el ID especificado no existe.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el consumo no se
     *                               encuentra en estado {@code REGISTRADO}, si el lote asociado no
     *                               se encuentra en estado {@code EN_EJECUCION}, o si la etapa no se
     *                               encuentra en estado {@code EN_CURSO}.
     */
    ConsumoInsumoResponseDTO anularConsumoInsumo(Long id, AnularConsumoInsumoFormDTO anularConsumoInsumoFormDTO);
}
