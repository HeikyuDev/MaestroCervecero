package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario para el alta y la modificación del Plan de Monitoreo de Etapa de Control
 * que se va a utilizar en la receta.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar los campos del formulario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
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
    @NotNull(message = "Cada plan de monitoreo debe tener una etapa de control")
    private Long idEtapaControl;

    /**
     * Lista de detalles de parámetros de control asociados a este plan de monitoreo.
     * <p>
     * Cada elemento de la lista representa un parámetro de control específico y sus valores
     * mínimo, máximo e ideal planificados para la receta.
     * </p>
     */
    @Valid
    @Builder.Default
    private List<DetalleParametroControlFormDTO> detallesParametroControl = new ArrayList<>();
}
