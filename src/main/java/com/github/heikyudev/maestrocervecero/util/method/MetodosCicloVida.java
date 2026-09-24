package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

import java.time.LocalDateTime;

/**
 * Clase de utilidades para el patrón de "ciclo de vida" (Falla/Mantenimiento/Limpieza),
 * compartido por Barril y Equipamiento: evita reimplementar en cada uno la validación de que una
 * anulación solo procede sobre la operación más reciente registrada.
 */
public class MetodosCicloVida {

    /**
     * Valida que una operación del ciclo de vida (falla, mantenimiento o limpieza) sea la más
     * reciente registrada sobre su barril/equipamiento asociado, comparando su fecha contra la
     * última fecha REGISTRADO de las 3 tablas del ciclo de vida. Evita anular un registro viejo
     * cuando una operación posterior, de cualquiera de los 3 tipos, ya dejó al barril/equipamiento
     * en un estado distinto.
     *
     * @param fechaEstaOperacion Fecha de la operación que se quiere anular.
     * @param fechaUltimaFalla Fecha de la última falla REGISTRADO del barril/equipamiento, o {@code null} si nunca tuvo una.
     * @param fechaUltimoMantenimiento Fecha del último mantenimiento REGISTRADO del barril/equipamiento, o {@code null} si nunca tuvo uno.
     * @param fechaUltimaLimpieza Fecha de la última limpieza REGISTRADO del barril/equipamiento, o {@code null} si nunca tuvo una.
     * @param nombreEntidad Nombre de la entidad asociada ("barril" o "equipamiento"), usado en el mensaje de error.
     * @throws ReglaNegocioException Si existe, entre las 3 fechas, alguna posterior a {@code fechaEstaOperacion}.
     */
    public static void validarEsOperacionMasReciente(LocalDateTime fechaEstaOperacion,
                                                       LocalDateTime fechaUltimaFalla,
                                                       LocalDateTime fechaUltimoMantenimiento,
                                                       LocalDateTime fechaUltimaLimpieza,
                                                       String nombreEntidad) {
        boolean hayOperacionPosterior =
                (fechaUltimaFalla != null && fechaUltimaFalla.isAfter(fechaEstaOperacion))
                        || (fechaUltimoMantenimiento != null && fechaUltimoMantenimiento.isAfter(fechaEstaOperacion))
                        || (fechaUltimaLimpieza != null && fechaUltimaLimpieza.isAfter(fechaEstaOperacion));

        if (hayOperacionPosterior) {
            throw new ReglaNegocioException("Solo se puede anular la operación más reciente registrada sobre este " + nombreEntidad);
        }
    }
}
