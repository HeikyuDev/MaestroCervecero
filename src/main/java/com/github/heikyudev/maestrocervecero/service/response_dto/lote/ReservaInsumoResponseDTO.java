package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa una reserva de insumo, generada al iniciar un lote para dejar
 * registrado qué lote de insumo cubre el requerimiento escalado de una etapa.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ReservaInsumoResponseDTO {

    /**
     * Identificador único de la reserva de insumo.
     */
    private Long id;

    /**
     * Cantidad reservada original de ese lote de insumo (no cambia).
     */
    private Double cantidadReservada;

    /**
     * Cantidad consumida de la reserva (acumulada).
     */
    private Double cantidadConsumida;

    /**
     * Saldo devuelto al stock al liberar la reserva sin consumirlo.
     */
    private Double cantidadLiberada;

    /**
     * Cantidad que una merma (ajuste de insumo) le quitó a la reserva.
     */
    private Double cantidadReducidaPorAjuste;

    /**
     * Saldo que todavía está reservado.
     */
    private Double cantidadPendiente;

    /**
     * Estado de la reserva: activa, consumida o liberada.
     */
    private EstadoReservaInsumo estado;

    /**
     * Etapa del lote para la que se reservó este insumo.
     */
    private EtapaLoteResponseDTO etapaLote;

    /**
     * Lote de insumo físico del que proviene la cantidad reservada.
     */
    private LoteInsumoResponseDTO loteInsumo;
}
