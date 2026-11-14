package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la cola de un fermentador en el panel del gerente de producción: el lote que tiene en ejecución
 * y el próximo lote pendiente.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColaFermentadorResponseDTO {

    /** Identificador único del fermentador; con él se arma el enlace a su detalle. */
    private Long id;

    /** Identificador interno del fermentador (por ejemplo, FER-01). */
    private String identificadorInterno;

    /** Estado operativo actual del fermentador. */
    private EstadoOperativo estadoOperativo;

    /** Lote En ejecución del fermentador. Es {@code null} si no tiene ninguno. */
    private LoteEnProduccionResponseDTO loteEnEjecucion;

    /** Próximo lote Pendiente de la cola, el de fecha de inicio estimada más temprana. Es {@code null} si no hay. */
    private LoteEnProduccionResponseDTO proximoLote;

    /** Cantidad de lotes Pendiente que esperan en la cola del fermentador, incluido el próximo. Nunca es negativa. */
    private long lotesPendientes;
}
