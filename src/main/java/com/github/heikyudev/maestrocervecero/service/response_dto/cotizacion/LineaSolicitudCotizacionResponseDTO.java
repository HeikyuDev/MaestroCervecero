package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con un insumo faltante de una solicitud de cotización. Las cantidades están en la unidad de medida del insumo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineaSolicitudCotizacionResponseDTO {

    /**
     * Identificador de la línea.
     */
    private Long id;

    /**
     * Identificador del insumo.
     */
    private Long idInsumo;

    /**
     * Nombre del insumo.
     */
    private String nombreInsumo;

    /**
     * Tipo del insumo para mostrar (Malta, Lúpulo o Levadura).
     */
    private String tipoInsumo;

    /**
     * Símbolo de la unidad de medida del insumo (ej. {@code kg}).
     */
    private String unidad;

    /**
     * Cantidad que necesitan los lotes del plan de producción.
     */
    private Double cantidadRequerida;

    /**
     * Stock vigente sin reservar al generar la solicitud.
     */
    private Double cantidadDisponible;

    /**
     * Lo que iban a consumir los lotes registrados sin iniciar que todavía no reservaron.
     */
    private Double cantidadComprometidaLotes;

    /**
     * Cantidad que restaba recibir de órdenes de compra pendientes.
     */
    private Double cantidadPendienteRecepcion;

    /**
     * Cantidad solicitada a los proveedores.
     */
    private Double cantidadFaltante;

    /**
     * {@code true} si ningún proveedor activo ofrecía el insumo al generar la solicitud.
     */
    private boolean sinProveedor;
}
