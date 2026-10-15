package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para registrar el consumo efectivo de un insumo.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario.
 * Lo usan tanto {@code registrarConsumoInsumoReservado} (descuenta de una reserva de insumo ya
 * existente para esa etapa y lote de insumo) como {@code registrarConsumoInsumoDirecto} (descuenta
 * directamente de la cantidad disponible del lote de insumo, sin pasar por una reserva).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoInsumoFormDTO {

    /**
     * ID del insumo elegido (el primer paso del formulario): el lote de insumo debe ser de este insumo y este insumo debe
     * ser uno de los que la etapa requiere.
     */
    @NotNull(message = "El insumo es obligatorio")
    private Long idInsumo;

    /**
     * ID del lote de insumo físico del que se descuenta el consumo.
     */
    @NotNull(message = "El lote de insumo es obligatorio")
    private Long idLoteInsumo;

    /**
     * Cantidad efectivamente consumida del insumo.
     */
    @NotNull(message = "La cantidad consumida es obligatoria")
    private Double cantidadConsumida;
}
