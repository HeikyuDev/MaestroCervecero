package com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.ConfiguracionComprasFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;

/**
 * Interfaz que define los servicios de la configuración de compras: los parámetros de la generación automática de
 * planes de órdenes de compra.
 * <p>
 * No es un CRUD: es una entidad de fila única que crea la migración de Flyway. Solo admite consultarla y actualizarla
 * por completo.
 * </p>
 */
public interface IConfiguracionComprasServicio {

    /**
     * Obtiene la configuración de compras vigente.
     *
     * @return La configuración de compras actual.
     */
    ConfiguracionComprasResponseDTO buscarConfiguracion();

    /**
     * Actualiza la configuración de compras.
     * <p>
     * Postcondiciones: la configuración queda con los cuatro valores informados. Las solicitudes de cotización ya
     * enviadas conservan su fecha límite de respuesta; la cantidad de planes y el criterio se aplican a las solicitudes
     * que se cierren desde ese momento.
     * </p>
     *
     * @param configuracionComprasFormDTO Los nuevos valores de configuración.
     * @return La configuración de compras actualizada.
     * @throws ReglaNegocioException Si algún parámetro no fue informado; si el plazo de respuesta no es mayor a 0 o supera {@code LimitesTecnicos.PLAZO_RESPUESTA_COTIZACION_MAXIMO_HORAS} horas; si la cantidad de planes no es mayor a 0 o supera {@code LimitesTecnicos.CANTIDAD_PLANES_COMPRA_MAXIMA}; o si el peso del precio no es un número entre 0 y 100.
     */
    ConfiguracionComprasResponseDTO actualizarConfiguracion(ConfiguracionComprasFormDTO configuracionComprasFormDTO);
}
