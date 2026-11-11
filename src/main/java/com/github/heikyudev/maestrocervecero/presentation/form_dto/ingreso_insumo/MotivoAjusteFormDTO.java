package com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un motivo de ajuste.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotivoAjusteFormDTO {

    /**
     * Nombre del motivo de ajuste. Debe ser único entre los motivos de ajuste activos (case-insensitive).
     * Ejemplo: "Merma por derrame".
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /**
     * Tipo de ajuste que representa este motivo: {@code INGRESO} (suma cantidad al lote de
     * insumo) o {@code EGRESO} (resta cantidad al lote de insumo). Una vez que el motivo tiene ajustes de insumo
     * asociados, no se puede cambiar.
     */
    @NotNull(message = "El tipo de ajuste es obligatorio")
    private TipoAjuste tipoAjuste;
}
