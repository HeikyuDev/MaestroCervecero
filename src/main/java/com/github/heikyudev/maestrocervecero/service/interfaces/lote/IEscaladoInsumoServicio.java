package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;

import java.util.List;

/**
 * Interfaz que define el cálculo del escalado de insumos de una receta (Pasos descritos en
 * docs/Dominio/Escalado/EscaladoDeMalta.md, EscaladoDelLupulo.md y EscaladoDeLevadura.md), en
 * base al volumen objetivo de un lote.
 * <p>
 * Esta lógica es compartida por más de un consumidor: {@code LoteServicioImpl} la usa para
 * reservar insumos al iniciar un lote (y para validar capacidad del macerador al registrarlo),
 * y {@code ConsumoInsumoServicioImpl} la usa para reportar cuánto requiere cada etapa,
 * independientemente de cuánto stock haya reservado — de ahí que viva en un servicio propio en
 * vez de duplicarse.
 * </p>
 */
public interface IEscaladoInsumoServicio {

    /**
     * Calcula, para cada malta de la receta, la masa escalada necesaria para alcanzar el OG
     * objetivo (ver docs/Dominio/Escalado/EscaladoDeMalta.md, Pasos 1 a 5): primero el total de
     * kilos de malta necesarios para el lote completo, y luego el reparto entre las distintas
     * maltas manteniendo la misma proporción definida en la receta.
     *
     * @param versionReceta   La versión de receta vigente del lote.
     * @param volumenObjetivo El volumen objetivo del lote, en litros.
     * @param macerador       El macerador seleccionado (su eficiencia de maceración afecta el cálculo).
     * @return Un requerimiento por cada malta de la receta, sin etapa asociada (nula).
     */
    List<RequerimientoInsumo> calcularRequerimientosMalta(VersionRecetaEntity versionReceta, double volumenObjetivo, MaceradorEntity macerador);

    /**
     * Calcula, para cada lúpulo de la receta, la masa escalada necesaria
     * (ver docs/Dominio/Escalado/EscaladoDelLupulo.md): los lúpulos de uso HERVOR se calculan con
     * la fórmula de Tinseth para alcanzar el IBU objetivo (Pasos 1 a 6); los de uso WHIRLPOOL o
     * DRY_HOP, al ser exclusivamente aromáticos, escalan de forma lineal respecto al volumen base
     * de la receta (Paso 7).
     *
     * @param versionReceta   La versión de receta vigente del lote.
     * @param volumenObjetivo El volumen objetivo del lote, en litros.
     * @param lote            El lote, para resolver la etapa correspondiente a cada detalle de lúpulo.
     * @return Un requerimiento por cada lúpulo de la receta, ya asociado a su etapa de uso.
     * @throws RecursoNoEncontradoException Si el lote no tiene una etapa del tipo indicado por algún detalle.
     */
    List<RequerimientoInsumo> calcularRequerimientosLupulo(VersionRecetaEntity versionReceta, double volumenObjetivo, LoteEntity lote);

    /**
     * Calcula, para cada levadura de la receta, la masa escalada necesaria
     * (ver docs/Dominio/Escalado/EscaladoDeLevadura.md): primero la cantidad total de células
     * viables necesarias (según los grados Plato del OG objetivo y la tasa de inoculación
     * promedio ponderada de la mezcla), luego el reparto entre las distintas levaduras, y
     * finalmente la conversión a gramos según la concentración celular propia de cada una.
     *
     * @param versionReceta   La versión de receta vigente del lote.
     * @param volumenObjetivo El volumen objetivo del lote, en litros.
     * @return Un requerimiento por cada levadura de la receta, sin etapa asociada (nula).
     */
    List<RequerimientoInsumo> calcularRequerimientosLevadura(VersionRecetaEntity versionReceta, double volumenObjetivo);

    /**
     * Devuelve una copia de los requerimientos con la etapa indicada asociada a cada uno.
     *
     * @param requerimientos Los requerimientos a copiar (típicamente sin etapa asociada todavía).
     * @param etapa          La etapa a asociarles.
     * @return Una nueva lista de requerimientos, cada uno con la misma cantidad requerida y la etapa indicada.
     */
    List<RequerimientoInsumo> asociarEtapa(List<RequerimientoInsumo> requerimientos, EtapaLoteEntity etapa);

    /**
     * Calcula el requerimiento total de insumos de un lote, escalado al volumen objetivo,
     * asociando cada uno a la etapa donde efectivamente se usa: Maceración para la malta,
     * Fermentación para la levadura, y la etapa configurada en cada detalle para el lúpulo.
     * <p>
     * Un mismo insumo (típicamente un lúpulo) puede usarse en más de una etapa dentro de la
     * misma receta (por ejemplo, HERVOR en Hervido y DRY_HOP en Maduración); estos requerimientos
     * se mantienen separados, uno por etapa, y nunca se fusionan entre etapas distintas — solo se
     * fusionan (sumando la cantidad) dentro de la misma etapa.
     * </p>
     * <p>
     * Este cálculo es de solo lectura: no bloquea equipamiento ni modifica ningún estado.
     * </p>
     * <p>
     * Calcula el escalado de las 3 categorías de insumo (malta, lúpulo y levadura) para las 6
     * etapas del lote: se usa desde {@code iniciarLote}, que necesita reservar stock de todas a la
     * vez. Para obtener el requerimiento de una única etapa puntual, usar
     * {@link #calcularRequerimientosEtapa(EtapaLoteEntity)}, que evita el cálculo de las categorías
     * que no aplican a esa etapa.
     * </p>
     *
     * @param lote El lote sobre el que calcular los requerimientos.
     * @return La lista de requerimientos totales, cada uno asociado a su etapa correspondiente.
     * @throws RecursoNoEncontradoException Si el lote no tiene alguna de las etapas necesarias.
     */
    List<RequerimientoInsumo> calcularRequerimientosTotales(LoteEntity lote);

    /**
     * Calcula el requerimiento de insumos de una única etapa de lote puntual, evitando el trabajo
     * de las categorías de insumo que no aplican a esa etapa: si no es Maceración, no calcula
     * malta; si no es Fermentación, no calcula levadura; y el lúpulo solo se calcula si algún
     * detalle de la receta efectivamente apunta a esta etapa (la fórmula de Tinseth sigue
     * agrupando, quien corresponda, todos los lúpulos de HERVOR de la receta para ese cálculo, ya
     * que el IBU objetivo se reparte entre todos ellos en conjunto — solo se descarta por completo
     * cuando ningún detalle de lúpulo apunta a esta etapa).
     * <p>
     * Este cálculo es de solo lectura. Lo usa {@code ConsumoInsumoServicio.filtrarInsumosRequeridos}
     * para reportar, sin recalcular de más, cuánto requiere y cuánto ya se consumió en la etapa que
     * el usuario está gestionando.
     * </p>
     *
     * @param etapaLote La etapa de lote sobre la que calcular los requerimientos.
     * @return Los requerimientos de esa etapa puntual, cada uno asociado a ella.
     */
    List<RequerimientoInsumo> calcularRequerimientosEtapa(EtapaLoteEntity etapaLote);
}
