package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con un insumo solicitado en la pantalla de cotización de un proveedor, con su respuesta actual si ya respondió.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineaCotizacionProveedorResponseDTO {

    /**
     * Identificador de la línea de la solicitud.
     */
    private Long idLinea;

    /**
     * Nombre del insumo.
     */
    private String nombreInsumo;

    /**
     * Tipo del insumo para mostrar.
     */
    private String tipoInsumo;

    /**
     * Símbolo de la unidad de medida del insumo.
     */
    private String unidad;

    /**
     * Cantidad solicitada, en la unidad del insumo.
     */
    private Double cantidadSolicitada;

    /**
     * Presentaciones del insumo que el proveedor ofrece.
     */
    private List<OpcionPresentacionResponseDTO> presentaciones;

    /**
     * Presentación de la respuesta actual, o {@code null}.
     */
    private Long idCatalogoProveedorOfertado;

    /**
     * Precio de la respuesta actual, o {@code null}.
     */
    private BigDecimal precioUnitarioOfertado;

    /**
     * Cantidad de la respuesta actual, o {@code null}.
     */
    private Integer cantidadDisponibleOfertada;
}
