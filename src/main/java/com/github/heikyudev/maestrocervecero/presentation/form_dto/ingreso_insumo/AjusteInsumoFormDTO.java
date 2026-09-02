package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import lombok.*;

/**
 * DTO de formulario para el registro de un ajuste de insumo.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInsumoFormDTO {

    /**
     * ID del lote de insumo sobre el que se aplica el ajuste.
     */
    private Long idLoteInsumo;

    /**
     * ID del motivo de ajuste. Su {@code tipoAjuste} (INGRESO/EGRESO) determina si la cantidad
     * se suma o se resta a la cantidad disponible del lote de insumo.
     */
    private Long idMotivoAjuste;

    /**
     * Cantidad afectada por el ajuste. Debe ser mayor a cero.
     */
    private Double cantidad;

    /**
     * Observación que describe el ajuste realizado.
     */
    private String observacion;
}
