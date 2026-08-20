package com.github.heikyudev.maestrocervecero.service.response_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import lombok.*;

/**
 * DTO de respuesta que representa un detalle de lupulo asociado a una versión
 * de receta, incluyendo la cantidad planificada y la información del lupulo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DetalleLupuloResponseDTO {

    /**
     * Identificador único del detalle de lupulo.
     */
    private Long id;

    /**
     * Cantidad planificada de lupulo para la versión de receta, en gramos.
     */
    private Double cantidad;

    /**
     * Uso del lúpulo en la receta.
     * HERVOR: se agrega al hervor, con tiempo de hervor obligatorio.
     * WHIRLPOOL: se agrega al whirlpool, sin tiempo de hervor.
     * DRY_HOP: se agrega en seco, en fermentación o maduración, sin tiempo de hervor.
     */
    private UsoLupulo uso;

    /**
     * Etapa de uso del lúpulo en la receta.
     * HERVIDO: para HERVOR o WHIRLPOOL.
     * FERMENTACION o MADURACION: para DRY_HOP, a elección del usuario.
     */
    private TipoEtapa etapaDeUso;

    /**
     * Tiempo de hervor del lúpulo en la receta, en minutos.
     * Solo aplica cuando {@code uso = HERVOR}; queda en 0 si es WHIRLPOOL o DRY_HOP.
     */
    private Double tiempoDeHervor;

    /**
     * Información del lupulo asociado a este detalle.
     */
    private LupuloResponseDTO lupulo;
}
