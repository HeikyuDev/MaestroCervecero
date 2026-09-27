package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.time.LocalDateTime;

/**
 * Clase de utilidades para el patrón de "ciclo de vida" (Falla/Mantenimiento/Limpieza/Despacho/
 * Devolución), compartido por Barril y Equipamiento: evita reimplementar en cada uno la
 * validación de orden temporal entre las operaciones del ciclo de vida.
 */
public class MetodosCicloVida {

    /**
     * Valida que una operación del ciclo de vida sea la más reciente registrada sobre su
     * barril/equipamiento asociado, comparando su fecha contra la de las demás operaciones del
     * ciclo de vida. Evita anular un registro viejo cuando una operación posterior, de cualquiera
     * de los otros tipos, ya dejó al barril/equipamiento en un estado distinto.
     *
     * @param fechaEstaOperacion Fecha de la operación que se quiere anular.
     * @param nombreEntidad Nombre de la entidad asociada ("barril" o "equipamiento"), usado en el mensaje de error.
     * @param fechasOtrasOperaciones Fecha de la última operación registrada de cada uno de los demás tipos del ciclo de vida (cualquiera puede ser {@code null} si esa entidad nunca tuvo una).
     * @throws ReglaNegocioException Si alguna de las fechas dadas es posterior a {@code fechaEstaOperacion}.
     */
    public static void validarEsOperacionMasReciente(LocalDateTime fechaEstaOperacion, String nombreEntidad, LocalDateTime... fechasOtrasOperaciones) {
        if (hayFechaPosterior(fechaEstaOperacion, fechasOtrasOperaciones)) {
            throw new ReglaNegocioException("Solo se puede anular la operación más reciente registrada sobre este " + nombreEntidad);
        }
    }

    /**
     * Valida que la fecha de una operación del ciclo de vida a registrar sea posterior a la de
     * todas las demás operaciones ya registradas sobre su barril/equipamiento asociado. Evita
     * cargar una operación con fecha retroactiva a algo que ya pasó sobre ese barril/equipamiento.
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @param nombreEntidad Nombre de la entidad asociada ("barril" o "equipamiento"), usado en el mensaje de error.
     * @param fechasOperacionesExistentes Fecha de la última operación registrada de cada uno de los tipos del ciclo de vida (cualquiera puede ser {@code null} si esa entidad nunca tuvo una).
     * @throws ReglaNegocioException Si alguna de las fechas dadas es posterior a {@code fechaNuevaOperacion}.
     */
    public static void validarFechaPosteriorAUltimaOperacion(LocalDateTime fechaNuevaOperacion, String nombreEntidad, LocalDateTime... fechasOperacionesExistentes) {
        if (hayFechaPosterior(fechaNuevaOperacion, fechasOperacionesExistentes)) {
            throw new ReglaNegocioException("La fecha de esta operación debe ser posterior a la última operación registrada sobre este " + nombreEntidad);
        }
    }

    private static boolean hayFechaPosterior(LocalDateTime fechaReferencia, LocalDateTime... fechas) {
        for (LocalDateTime fecha : fechas) {
            if (fecha != null && fecha.isAfter(fechaReferencia)) {
                return true;
            }
        }
        return false;
    }
}
