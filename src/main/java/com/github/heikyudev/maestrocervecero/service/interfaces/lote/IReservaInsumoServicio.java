package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;

import java.util.List;

/**
 * Interfaz que define los servicios de consulta relacionados con las reservas de insumo
 * generadas al iniciar un lote.
 */
public interface IReservaInsumoServicio {

    /**
     * Busca las reservas de insumo de una etapa de lote para un insumo determinado, para
     * mostrarle al operario qué lotes de insumo físico tiene reservados y con qué cantidad,
     * antes de registrar un consumo.
     * <p>
     * Devuelve TODAS las reservas de ese insumo en esa etapa, incluidas las que ya se agotaron
     * (cantidad reservada en 0, por un consumo o por una merma): mostrar solo las que todavía
     * tienen cantidad, u ordenarlas primero, es una decisión de presentación de la vista, no un
     * filtro de este método.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio; no lo tipea el usuario, lo
     *                    determina el contexto de la pantalla, igual que en
     *                    {@code filtrarInsumosRequeridos}).
     * @param idInsumo El ID del insumo, elegido por el usuario entre los devueltos por
     *                 {@code filtrarInsumosRequeridos}.
     * @return Las reservas correspondientes, en formato DTO.
     */
    List<ReservaInsumoResponseDTO> filtrarLotesInsumoReservados(Long idEtapaLote, Long idInsumo);
}
