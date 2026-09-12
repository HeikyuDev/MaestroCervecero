package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

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
     * Cantidad reservada de ese lote de insumo.
     */
    private Double cantidadReservada;

    /**
     * Etapa del lote para la que se reservó este insumo.
     */
    private EtapaLoteResponseDTO etapaLote;

    /**
     * Lote de insumo físico del que proviene la cantidad reservada.
     */
    private LoteInsumoResponseDTO loteInsumo;
}
