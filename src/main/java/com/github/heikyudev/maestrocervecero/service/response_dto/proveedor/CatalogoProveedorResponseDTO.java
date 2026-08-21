package com.github.heikyudev.maestrocervecero.service.response_dto.proveedor;

import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para representar un ítem del catálogo de un proveedor.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CatalogoProveedorResponseDTO {

    /**
     * Identificador único del ítem del catálogo.
     */
    private Long id;

    /**
     * Presentación comercial en la que se ofrece el insumo.
     */
    private PresentacionComercialResponseDTO presentacionComercial;

    /**
     * Insumo ofrecido. El tipo concreto (Malta, Lúpulo o Levadura) se resuelve
     * polimórficamente en tiempo de ejecución.
     */
    private InsumoResponseDTO insumo;

    /**
     * Indica si el proveedor ofrece actualmente este ítem.
     */
    private boolean seleccionado;
}
