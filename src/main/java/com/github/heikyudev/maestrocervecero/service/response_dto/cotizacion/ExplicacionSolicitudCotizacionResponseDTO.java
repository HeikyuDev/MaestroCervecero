package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Por qué se generó una solicitud de cotización y qué falta hacer, en partes para mostrarla como una lista: una
 * introducción, un renglón por cada insumo que el stock no cubre y, mientras la solicitud está pendiente de
 * confirmación, la acción que falta.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplicacionSolicitudCotizacionResponseDTO {

    /**
     * Introducción, por ejemplo: "Se generó la solicitud de cotización N° 12 para la planificación N° 45. El stock actual
     * no cubre:".
     */
    private String introduccion;

    /**
     * Un renglón por cada insumo que el stock no cubre, en el orden de las líneas, por ejemplo: "Malta Pilsen: faltan 120 kg".
     */
    @Builder.Default
    private List<String> insumosNoCubiertos = List.of();

    /**
     * Lo que falta hacer ("Confirme el envío a los proveedores o cancele la solicitud") mientras la solicitud está
     * pendiente de confirmación, o {@code null} en los demás estados.
     */
    private String accion;
}
