package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un motivo de ajuste.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotivoAjusteFormDTO {

    /**
     * Nombre del motivo de ajuste.
     */
    private String nombre;

    /**
     * Tipo de ajuste que representa este motivo: {@code INGRESO} (suma cantidad al lote de
     * insumo) o {@code EGRESO} (resta cantidad al lote de insumo).
     */
    private TipoAjuste tipoAjuste;
}
