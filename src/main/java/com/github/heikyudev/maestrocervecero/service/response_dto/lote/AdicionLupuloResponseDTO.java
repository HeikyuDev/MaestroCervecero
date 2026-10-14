package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa una adición puntual de lúpulo a una etapa del lote, con su propia cantidad (sin
 * fusionar con las otras adiciones del mismo lúpulo).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AdicionLupuloResponseDTO {

    /**
     * Nombre del lúpulo.
     */
    private String nombreInsumo;

    /**
     * Unidad de medida del lúpulo.
     */
    private UnidadDeMedida unidadDeMedida;

    /**
     * Uso de la adición: HERVOR, WHIRLPOOL o DRY_HOP.
     */
    private UsoLupulo uso;

    /**
     * Minutos que el lúpulo hierve. Solo aplica al uso HERVOR; es 0 en las demás adiciones.
     */
    private Double tiempoDeHervor;

    /**
     * Cantidad de esta adición, escalada al volumen del lote.
     */
    private Double cantidad;
}
