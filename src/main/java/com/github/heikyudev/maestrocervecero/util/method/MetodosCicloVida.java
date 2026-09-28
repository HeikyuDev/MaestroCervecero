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
        if (hayFechaEstrictamentePosterior(fechaEstaOperacion, fechasOtrasOperaciones)) {
            throw new ReglaNegocioException("Solo se puede anular la operación más reciente registrada sobre este " + nombreEntidad);
        }
    }

    /**
     * Valida que la fecha de una operación del ciclo de vida a registrar sea posterior a la de
     * todas las demás operaciones ya registradas sobre su barril/equipamiento asociado, sin
     * permitir tampoco que coincida exactamente con ninguna. Evita cargar una operación con fecha
     * retroactiva (o ambigua, a la misma fecha y hora) a algo que ya pasó sobre ese barril/
     * equipamiento — dos operaciones distintas no pueden haber ocurrido en el mismo instante.
     * <p>
     * A diferencia de {@link #validarEsOperacionMasReciente}, acá sí se puede exigir "posterior o
     * igual" sin riesgo de autorrechazo: la operación que se está registrando todavía no existe en
     * ninguna de las tablas consultadas, así que nunca se compara contra sí misma.
     * </p>
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @param nombreEntidad Nombre de la entidad asociada ("barril" o "equipamiento"), usado en el mensaje de error.
     * @param fechasOperacionesExistentes Fecha de la última operación registrada de cada uno de los tipos del ciclo de vida (cualquiera puede ser {@code null} si esa entidad nunca tuvo una).
     * @throws ReglaNegocioException Si alguna de las fechas dadas es posterior o igual a {@code fechaNuevaOperacion}.
     */
    public static void validarFechaPosteriorAUltimaOperacion(LocalDateTime fechaNuevaOperacion, String nombreEntidad, LocalDateTime... fechasOperacionesExistentes) {
        if (hayFechaPosteriorOIgual(fechaNuevaOperacion, fechasOperacionesExistentes)) {
            throw new ReglaNegocioException("La fecha de esta operación debe ser posterior a la última operación registrada sobre este " + nombreEntidad);
        }
    }

    /**
     * Valida que la fecha de una operación del ciclo de vida a registrar no sea posterior a la
     * fecha y hora actual.
     * <p>
     * Una fecha futura envenenaría {@link #validarFechaPosteriorAUltimaOperacion}: quedaría como
     * "la última operación registrada" sobre ese barril/equipamiento, y bloquearía cualquier
     * operación real posterior hasta que, en el mundo real, se alcance esa fecha futura.
     * </p>
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @throws ReglaNegocioException Si la fecha es posterior a la fecha y hora actual.
     */
    public static void validarFechaNoFutura(LocalDateTime fechaNuevaOperacion) {
        if (fechaNuevaOperacion.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de esta operación no puede ser posterior a la fecha y hora actual");
        }
    }

    private static boolean hayFechaEstrictamentePosterior(LocalDateTime fechaReferencia, LocalDateTime... fechas) {
        for (LocalDateTime fecha : fechas) {
            if (fecha != null && fecha.isAfter(fechaReferencia)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hayFechaPosteriorOIgual(LocalDateTime fechaReferencia, LocalDateTime... fechas) {
        for (LocalDateTime fecha : fechas) {
            if (fecha != null && !fecha.isBefore(fechaReferencia)) {
                return true;
            }
        }
        return false;
    }
}
