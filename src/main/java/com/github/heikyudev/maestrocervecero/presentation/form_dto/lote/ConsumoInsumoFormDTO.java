package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para registrar el consumo efectivo de un insumo.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * Lo usan tanto {@code registrarConsumoInsumoReservado} (descuenta de una reserva de insumo ya
 * existente para esa etapa y lote de insumo) como {@code registrarConsumoInsumoDirecto} (descuenta
 * directamente de la cantidad disponible del lote de insumo, sin pasar por una reserva).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoInsumoFormDTO {

    /**
     * ID del lote de insumo físico del que se descuenta el consumo.
     */
    private Long idLoteInsumo;

    /**
     * Cantidad efectivamente consumida del insumo.
     */
    private Double cantidadConsumida;
}
