package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con lo que ve un proveedor al abrir el enlace de una solicitud de cotización: solo los insumos solicitados que ofrece.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionProveedorResponseDTO {

    /**
     * Identificador de la solicitud de cotización.
     */
    private Long idSolicitud;

    /**
     * Razón social vigente del proveedor.
     */
    private String razonSocial;

    /**
     * Nombre comercial vigente del proveedor.
     */
    private String nombreComercial;

    /**
     * Momento hasta el que puede responder y editar la respuesta.
     */
    private LocalDateTime fechaLimiteRespuesta;

    /**
     * Momento del último envío de su respuesta, o {@code null}.
     */
    private LocalDateTime fechaRespuesta;

    /**
     * Insumos solicitados que el proveedor ofrece.
     */
    private List<LineaCotizacionProveedorResponseDTO> lineas;
}
