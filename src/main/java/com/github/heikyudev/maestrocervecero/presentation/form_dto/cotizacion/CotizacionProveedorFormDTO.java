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
 * DTO de formulario con la respuesta de un proveedor a una solicitud de cotización: una fila por cada presentación de su
 * catálogo vigente de cada insumo solicitado que ofrece (un mismo insumo puede cotizarse en varias presentaciones a la
 * vez). Una fila sin precio ni cantidad no cotiza esa presentación.
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
     * Ofertas por insumo solicitado y presentación.
     */
    @Valid
    @Size(max = 100, message = "La cotización no puede superar 100 presentaciones")
    @Builder.Default
    private List<OfertaCotizacionFormDTO> ofertas = new ArrayList<>();
}
