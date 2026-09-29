package com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.ConfiguracionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;

/**
 * Interfaz que define los servicios relacionados con la configuración general de producción.
 * <p>
 * No es un CRUD: es una entidad singleton, creada una única vez al arrancar la aplicación. Solo
 * admite consultarla y actualizarla por completo, nunca alta ni baja.
 * </p>
 */
public interface IConfiguracionProduccionServicio {

    /**
     * Obtiene la configuración general de producción vigente.
     *
     * @return La configuración de producción actual.
     */
    ConfiguracionProduccionResponseDTO buscarConfiguracion();

    /**
     * Actualiza la configuración general de producción.
     * <p>
     * No admite actualización parcial: el gerente de producción siempre reenvía el conjunto
     * completo de parámetros.
     * </p>
     *
     * @param configuracionProduccionFormDTO Los nuevos valores de configuración.
     * @return La configuración de producción actualizada.
     * @throws ReglaNegocioException Si algún parámetro no fue informado, si alguna velocidad, tiempo o capacidad no es mayor a cero, o si el porcentaje mínimo de consumo no está entre 0 y 100.
     */
    ConfiguracionProduccionResponseDTO actualizarConfiguracion(ConfiguracionProduccionFormDTO configuracionProduccionFormDTO);
}
