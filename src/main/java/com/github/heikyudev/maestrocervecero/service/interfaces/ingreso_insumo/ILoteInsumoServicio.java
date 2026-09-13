package com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;

import java.util.List;

/**
 * Interfaz que define los servicios de consulta relacionados con los lotes de insumo físicos.
 */
public interface ILoteInsumoServicio {

    /**
     * Busca los lotes de insumo de un insumo determinado que todavía tienen cantidad disponible,
     * para que el operario elija entre ellos al registrar un consumo directo.
     * <p>
     * Ordenados por fecha de vencimiento ascendente (FEFO), el mismo criterio que usa la reserva
     * automática al iniciar un lote.
     * </p>
     *
     * @param idInsumo El ID del insumo, elegido por el usuario entre los devueltos por
     *                 {@code filtrarInsumosRequeridos}.
     * @return Los lotes de insumo de ese insumo con cantidad disponible mayor a cero, en formato DTO.
     */
    List<LoteInsumoResponseDTO> filtrarLotesInsumoDisponibles(Long idInsumo);
}
