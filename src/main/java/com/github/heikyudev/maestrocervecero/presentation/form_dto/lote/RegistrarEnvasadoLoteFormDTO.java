package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para registrar el envasado de un lote, es decir, el traspaso de cerveza
 * desde el fermentador hacia un barril durante la etapa de Envasado.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEnvasadoLoteFormDTO {

    /**
     * ID del barril al que se traspasa la cerveza.
     */
    @NotNull(message = "El barril es obligatorio")
    private Long idBarril;

    /**
     * Cantidad de cerveza envasada, en litros.
     */
    @NotNull(message = "La cantidad a envasar es obligatoria")
    private Double cantidad;
}
