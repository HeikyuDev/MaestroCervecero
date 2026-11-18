package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario con el plan de producción que el gerente de producción elige y confirma: su modalidad y sus
 * lotes, en el orden en que se registrarán. La planificación no viaja en el formulario: la resuelve el Controller
 * desde la URL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanProduccionFormDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Modalidad del plan elegido. */
    @NotNull(message = "La modalidad del plan es obligatoria")
    private ModalidadPlanProduccion modalidad;

    /** Lotes del plan, en el orden en que se registrarán. */
    @Valid
    @NotEmpty(message = "El plan debe tener al menos un lote")
    @Size(max = LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES, message = "El plan no puede tener más de " + LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES + " lotes")
    @Builder.Default
    private List<LotePlanProduccionFormDTO> lotes = new ArrayList<>();
}
