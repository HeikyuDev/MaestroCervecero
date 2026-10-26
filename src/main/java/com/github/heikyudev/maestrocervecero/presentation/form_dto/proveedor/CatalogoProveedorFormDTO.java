package com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para un ítem del catálogo de un proveedor.
 * <p>
 * Representa la asociación entre un insumo y la presentación comercial en la que
 * el proveedor lo ofrece. Se utiliza embebido dentro de {@link VersionProveedorFormDTO}
 * y expone setters para que Spring MVC pueda enlazar las filas del formulario.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProveedorFormDTO {

    /**
     * Identificador de la presentación comercial en la que se ofrece el insumo.
     */
    @NotNull(message = "La presentación comercial de cada ítem del catálogo es obligatoria")
    private Long idPresentacionComercial;

    /**
     * Identificador del insumo ofrecido (Malta, Lúpulo o Levadura).
     */
    @NotNull(message = "El insumo de cada ítem del catálogo es obligatorio")
    private Long idInsumo;
}
