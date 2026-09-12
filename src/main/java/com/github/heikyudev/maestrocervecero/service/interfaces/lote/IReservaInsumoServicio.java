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
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @param idInsumo El ID del insumo.
     * @return Las reservas correspondientes, en formato DTO.
     */
    List<ReservaInsumoResponseDTO> buscarPorEtapaEInsumo(Long idEtapaLote, Long idInsumo);
}
