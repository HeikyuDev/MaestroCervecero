package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.CostoProduccionLoteResponseDTO;

/**
 * Cálculo del costo de producción de un lote.
 */
public interface ICostoProduccionLoteServicio {

    /**
     * Calcula el costo de producción de un lote: el costo de los insumos consumidos (cantidad por costo promedio ponderado al
     * consumir, sin los consumos anulados, de todas las etapas y de los dos tipos de consumo), los costos directos adicionales que se
     * aplicaron al finalizar el envasado y, con el envasado finalizado y litros envasados mayores a cero, el costo total y el costo por
     * litro. Un lote en curso informa solo lo acumulado de insumos y uno cancelado, lo que alcanzó a consumir. Todos los importes
     * tienen 4 decimales con redondeo {@code HALF_UP}. No guarda nada.
     *
     * @param idLote El ID del lote.
     * @return El costo de producción del lote.
     * @throws RecursoNoEncontradoException Si no existe el lote.
     */
    CostoProduccionLoteResponseDTO calcularCostoProduccion(Long idLote);
}
