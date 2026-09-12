package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
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
     * Obtiene una página de consumos de insumo.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de consumos en formato DTO.
     */
    Page<ConsumoInsumoResponseDTO> buscarTodos(Pageable pageable);

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
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado.
     */
    ConsumoInsumoResponseDTO registrarConsumoInsumoReservado(ConsumoInsumoFormDTO consumoInsumoFormDTO);

    /**
     * Registra el consumo efectivo de un insumo por fuera de cualquier reserva, descontándolo
     * directamente de la cantidad disponible de un lote de insumo.
     * <p>
     * No requiere que la cantidad reservada de ese insumo para esa etapa esté agotada: el
     * operario puede elegir libremente registrar un consumo directo sobre cualquier lote de
     * insumo del insumo requerido por la etapa, en cualquier momento.
     * </p>
     *
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado.
     */
    ConsumoInsumoResponseDTO registrarConsumoInsumoDirecto(ConsumoInsumoFormDTO consumoInsumoFormDTO);

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
}
