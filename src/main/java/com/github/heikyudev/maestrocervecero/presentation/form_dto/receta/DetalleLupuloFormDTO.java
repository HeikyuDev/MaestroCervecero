package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.*;

/**
 * DTO de formulario para el alta y la modificación del detalle de lupulo que se va a utilizar en la receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleLupuloFormDTO {

    /**
     * Cantidad de lúpulo a utilizar en la receta, en gramos.
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
     * Identificador del lupulo asociado a este detalle de receta.
     * <p>
     * Se espera que este identificador corresponda a una {@link LupuloEntity} existente en la base de datos.
     * </p>
     */
    private Long idLupulo;
}
