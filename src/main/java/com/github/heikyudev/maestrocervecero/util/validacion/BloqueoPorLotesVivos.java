package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.util.List;

/**
 * Bloqueo de la modificación de los datos de un insumo que cambian el escalado cuando existen lotes
 * Pendientes o En ejecución que lo usan. Arma el mensaje con los códigos de los lotes involucrados.
 */
public final class BloqueoPorLotesVivos {

    /**
     * Cantidad máxima de códigos de lote que se listan en el mensaje; el resto se resume como "y N más".
     */
    static final int MAXIMO_LOTES_EN_MENSAJE = 5;

    private BloqueoPorLotesVivos() {
    }

    /**
     * Rechaza la modificación si algún lote Pendiente o En ejecución usa el insumo.
     *
     * @param codigosLotes Códigos de los lotes que usan el insumo, en el orden en que deben mostrarse.
     * @param campos Frase que nombra lo que se intenta modificar (por ejemplo {@code "el extracto potencial de la malta"}).
     * @param pronombreInsumo Pronombre que reemplaza al insumo en el mensaje: {@code "lo"} o {@code "la"}.
     * @throws ReglaNegocioException Si la lista de lotes no está vacía.
     */
    public static void validar(List<String> codigosLotes, String campos, String pronombreInsumo) {
        if (codigosLotes == null || codigosLotes.isEmpty()) {
            return;
        }
        throw new ReglaNegocioException("No se puede modificar " + campos + " porque " + pronombreInsumo
                + (codigosLotes.size() == 1 ? " usa el lote " : " usan los lotes ") + listar(codigosLotes));
    }

    private static String listar(List<String> codigosLotes) {
        int total = codigosLotes.size();
        if (total > MAXIMO_LOTES_EN_MENSAJE) {
            return String.join(", ", codigosLotes.subList(0, MAXIMO_LOTES_EN_MENSAJE))
                    + " y " + (total - MAXIMO_LOTES_EN_MENSAJE) + " más";
        }
        if (total == 1) {
            return codigosLotes.get(0);
        }
        return String.join(", ", codigosLotes.subList(0, total - 1)) + " y " + codigosLotes.get(total - 1);
    }
}
