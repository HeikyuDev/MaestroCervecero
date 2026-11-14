package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un barril pendiente de limpieza o de mantenimiento en el panel del operario.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilPendienteResponseDTO {

    /** Identificador único del barril; con él se arman los enlaces a sus formularios de registro. */
    private Long id;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Nombre del fabricante del barril. */
    private String fabricante;

    /**
     * Causa por la que el barril está pendiente de mantenimiento. Es {@code null} en los bloques de limpieza y cuando
     * no se puede derivar de su última operación registrada.
     */
    private CausaMantenimiento causaMantenimiento;
}
