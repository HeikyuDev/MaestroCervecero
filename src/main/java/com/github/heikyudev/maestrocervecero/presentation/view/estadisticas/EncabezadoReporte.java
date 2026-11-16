package com.github.heikyudev.maestrocervecero.presentation.view.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Datos de emisión de un reporte impreso (plantilla {@code estadisticas/pdf/reporte}): quién lo emite, cuándo y desde
 * qué terminal. El
 * título, el período y los criterios aplicados los trae el propio reporte.
 */
@Getter
@AllArgsConstructor
public final class EncabezadoReporte {

    /** Quien emite el reporte: el nombre de usuario y, si se conocen, su nombre y apellido entre paréntesis. */
    private final String emitidoPor;

    /** Fecha y hora de emisión. */
    private final LocalDateTime fechaEmision;

    /** El equipo desde el que se pidió el reporte: {@code nombre-de-equipo (IP)} o solo la IP. */
    private final String terminal;

    /**
     * Arma el texto de "Emitido por".
     *
     * @param username Nombre de usuario de quien emite el reporte.
     * @param nombre Nombre de la persona, o {@code null} si no se conoce.
     * @param apellido Apellido de la persona, o {@code null} si no se conoce.
     * @return {@code "usuario (Nombre Apellido)"}, o solo el nombre de usuario si no se conocen el nombre ni el apellido.
     */
    public static String emisor(String username, String nombre, String apellido) {
        String persona = ((nombre == null ? "" : nombre.strip()) + " " + (apellido == null ? "" : apellido.strip())).strip();
        return persona.isEmpty() ? username : username + " (" + persona + ")";
    }
}
