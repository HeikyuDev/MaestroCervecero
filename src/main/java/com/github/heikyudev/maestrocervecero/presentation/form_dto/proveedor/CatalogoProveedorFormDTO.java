package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import lombok.*;

/**
 * DTO de formulario para un ítem del catálogo de un proveedor.
 * <p>
 * Representa la asociación entre un insumo y la presentación comercial en la que
 * el proveedor lo ofrece. Se utiliza embebido dentro de {@link VersionProveedorFormDTO}.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProveedorFormDTO {

    /**
     * Identificador de la presentación comercial en la que se ofrece el insumo.
     */
    private Long idPresentacionComercial;

    /**
     * Identificador del insumo ofrecido (Malta, Lúpulo o Levadura).
     */
    private Long idInsumo;
}
