package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;

/**
 * Una adición puntual de lúpulo a una etapa del lote, tal como la define un detalle de la receta, con la cantidad que
 * le toca al lote antes de fusionar las adiciones del mismo insumo (los requerimientos de la etapa suman todas las
 * adiciones de un insumo; esta es la cantidad de cada una).
 * <p>
 * {@code tiempoDeHervor} son los minutos que el lúpulo hierve (solo tiene sentido con uso {@code HERVOR}; es 0 en las
 * demás adiciones). La cantidad se redondea a la escala de las cantidades ({@link Redondeo}).
 * </p>
 */
public record AdicionLupulo(InsumoEntity insumo, UsoLupulo uso, double tiempoDeHervor, double cantidad) {

    public AdicionLupulo {
        cantidad = Redondeo.redondear(cantidad);
    }
}
