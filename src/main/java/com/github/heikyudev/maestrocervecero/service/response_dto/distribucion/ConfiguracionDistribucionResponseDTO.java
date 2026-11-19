package com.github.heikyudev.maestrocervecero.service.response_dto.distribucion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO de respuesta con la configuración de distribución vigente.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionDistribucionResponseDTO {

    /**
     * Identificador de la configuración (siempre {@code 1L}, es una entidad de fila única).
     */
    private Long id;

    /**
     * Hora a partir de la cual se envía el recordatorio diario de retiro.
     */
    private LocalTime horaEnvioRecordatorioRetiro;

    /**
     * Días hacia adelante en los que el cliente puede proponer el retiro.
     */
    private Integer diasMaximosAnticipacionRetiro;
}
