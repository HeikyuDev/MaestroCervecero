package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import lombok.*;

import java.util.List;

/**
 * DTO de formulario para el alta y la modificación del Plan de Monitoreo de Etapa de Control
 * que se va a utilizar en la receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanMonitoreoEtapaFormDTO {

    /**
     * Identificador de la etapa de control asociada a este plan de monitoreo.
     * <p>
     * Se espera que este identificador corresponda a un {@link EtapaControlEntity}
     * existente en la base de datos.
     * </p>
     */
    private Long idEtapaControl;

    /**
     * Lista de detalles de parámetros de control asociados a este plan de monitoreo.
     * <p>
     * Cada elemento de la lista representa un parámetro de control específico y sus valores
     * mínimo, máximo e ideal planificados para la receta.
     * </p>
     */
    private List<DetalleParametroControlFormDTO> detallesParametroControl;
}
