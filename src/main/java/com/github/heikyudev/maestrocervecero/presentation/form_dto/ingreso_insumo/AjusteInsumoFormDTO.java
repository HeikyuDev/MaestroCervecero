package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO de formulario para el registro de un ajuste de insumo.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y solo exige lo que falta; los límites y las
 * reglas de negocio las valida el service. No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInsumoFormDTO {

    /**
     * ID del lote de insumo sobre el que se aplica el ajuste.
     */
    @NotNull(message = "El lote de insumo es obligatorio")
    private Long idLoteInsumo;

    /**
     * ID del motivo de ajuste. Su {@code tipoAjuste} (INGRESO/EGRESO) determina si la cantidad
     * se suma o se resta a la cantidad disponible del lote de insumo.
     */
    @NotNull(message = "El motivo de ajuste es obligatorio")
    private Long idMotivoAjuste;

    /**
     * Cantidad afectada por el ajuste. Debe ser mayor a 0.
     */
    @NotNull(message = "La cantidad es obligatoria")
    private Double cantidad;

    /**
     * Observación que describe el ajuste realizado.
     */
    @NotBlank(message = "La observación es obligatoria")
    @Size(max = LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, message = "La observación no puede superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres")
    private String observacion;
}
