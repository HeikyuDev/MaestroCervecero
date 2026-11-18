package com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario con la respuesta de un proveedor a una solicitud de cotización: una fila por insumo solicitado
 * que el proveedor ofrece. Una fila sin presentación, precio ni cantidad significa que no cotiza ese insumo.
 * <p>
 * El token del enlace no forma parte del formulario: llega aparte, en la dirección de la pantalla. El máximo de filas
 * repite {@code LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS} (una anotación exige un literal).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionProveedorFormDTO {

    /**
     * Ofertas por insumo solicitado.
     */
    @Valid
    @Size(max = 100, message = "La cotización no puede superar 100 insumos")
    @Builder.Default
    private List<OfertaCotizacionFormDTO> ofertas = new ArrayList<>();
}
