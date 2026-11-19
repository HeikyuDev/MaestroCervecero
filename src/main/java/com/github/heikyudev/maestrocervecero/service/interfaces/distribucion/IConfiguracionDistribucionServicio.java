package com.github.heikyudev.maestrocervecero.service.interfaces.distribucion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.ConfiguracionDistribucionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;

/**
 * Interfaz que define los servicios de la configuración de distribución: los parámetros de la coordinación automática
 * del retiro de barriles.
 * <p>
 * No es un CRUD: es una entidad de fila única que crea la migración de Flyway. Solo admite consultarla y actualizarla
 * por completo.
 * </p>
 */
public interface IConfiguracionDistribucionServicio {

    /**
     * Obtiene la configuración de distribución vigente.
     *
     * @return La configuración de distribución actual.
     */
    ConfiguracionDistribucionResponseDTO buscarConfiguracion();

    /**
     * Actualiza la configuración de distribución.
     * <p>
     * Postcondiciones: la configuración queda con los dos valores informados (la hora, sin segundos). La nueva hora rige
     * desde el próximo envío diario que todavía no se hizo (si ese día ya se envió, rige desde el día siguiente). Los días
     * de anticipación se aplican a los recordatorios que se envíen y a las respuestas que se confirmen desde ese momento.
     * </p>
     *
     * @param configuracionDistribucionFormDTO Los nuevos valores de configuración.
     * @return La configuración de distribución actualizada.
     * @throws ReglaNegocioException Si faltan los datos o algún parámetro; o si los días máximos de anticipación no están entre {@code LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MINIMO} y {@code LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MAXIMO}.
     */
    ConfiguracionDistribucionResponseDTO actualizarConfiguracion(ConfiguracionDistribucionFormDTO configuracionDistribucionFormDTO);
}
