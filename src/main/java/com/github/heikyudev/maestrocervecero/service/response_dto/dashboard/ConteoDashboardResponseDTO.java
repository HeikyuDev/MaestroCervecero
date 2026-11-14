package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la cantidad de registros de un estado o una situación en el panel del gerente de producción:
 * estados operativos de equipos y de barriles y situaciones de stock de insumos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConteoDashboardResponseDTO {

    /** Nombre del valor del enum (por ejemplo, EN_LIMPIEZA); con él se arma el enlace a la lista filtrada. */
    private String codigo;

    /** Nombre del estado o de la situación para mostrar en pantalla. */
    private String etiqueta;

    /** Cantidad real de registros. Nunca es negativa. */
    private long total;
}
