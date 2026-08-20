package com.github.heikyudev.maestrocervecero.service.response_dto.insumo;

import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;

/**
 * Contrato común que exponen los DTO de respuesta de los distintos tipos de insumo
 * ({@link MaltaResponseDTO}, {@link LupuloResponseDTO}, {@link LevaduraResponseDTO}).
 * <p>
 * Permite tratar de forma polimórfica a un insumo (por ejemplo, dentro del catálogo de un
 * proveedor) sin conocer de antemano su tipo concreto. El tipo real se determina en tiempo de
 * ejecución mediante {@code instanceof} donde sea necesario.
 * </p>
 */
public interface InsumoResponseDTO {

    /**
     * @return Identificador único del insumo.
     */
    Long getId();

    /**
     * @return Nombre del insumo.
     */
    String getNombre();

    /**
     * @return Unidad de medida del insumo.
     */
    UnidadDeMedida getUnidadDeMedida();
}
