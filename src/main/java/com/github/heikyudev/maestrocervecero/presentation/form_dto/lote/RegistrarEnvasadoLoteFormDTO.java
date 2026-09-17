package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para registrar el envasado de un lote, es decir, el traspaso de cerveza
 * desde el fermentador hacia un barril durante la etapa de Envasado.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEnvasadoLoteFormDTO {

    /**
     * ID del barril al que se traspasa la cerveza.
     */
    private Long idBarril;

    /**
     * Cantidad de cerveza envasada, en litros.
     */
    private Double cantidad;
}
