package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un equipo pendiente de limpieza o de mantenimiento en el panel del operario.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipoPendienteResponseDTO {

    /** Identificador único del equipo; con él se arman los enlaces a su detalle y a sus formularios de registro. */
    private Long id;

    /** Identificador interno del equipo (por ejemplo, MOL-01). */
    private String identificadorInterno;

    /** Tipo concreto del equipo. */
    private TipoEquipamiento tipo;

    /**
     * Causa por la que el equipo está pendiente de mantenimiento. Es {@code null} en los bloques de limpieza y cuando
     * no se puede derivar de su última operación registrada.
     */
    private CausaMantenimiento causaMantenimiento;
}
