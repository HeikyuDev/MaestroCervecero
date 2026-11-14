package com.github.heikyudev.maestrocervecero.service.response_dto.auditoria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la comparación de un campo del registro auditado: su valor antes y después de la operación.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CampoAuditoriaResponseDTO {

    /**
     * Nombre legible del campo (por ejemplo "Potencial extracto"). Las asociaciones se muestran con "(ID)".
     */
    private String campo;

    /**
     * Valor del campo en el estado anterior, ya listo para mostrar, o {@code null} si el campo no existía o no tenía valor.
     */
    private String valorAnterior;

    /**
     * Valor del campo en el estado posterior, ya listo para mostrar, o {@code null} si el campo no existía o no tiene valor.
     */
    private String valorPosterior;

    /**
     * Resultado de la comparación del campo entre los dos estados.
     */
    private EstadoCampoAuditoria estado;
}
