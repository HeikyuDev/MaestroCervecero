package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un insumo sin stock disponible, en el panel del encargado de depósito.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsumoSinStockResponseDTO {

    /** Identificador único del insumo; con él se arma el enlace a su stock. */
    private Long id;

    /** Nombre del insumo. */
    private String nombre;

    /** Tipo del insumo. */
    private TipoInsumo tipo;
}
