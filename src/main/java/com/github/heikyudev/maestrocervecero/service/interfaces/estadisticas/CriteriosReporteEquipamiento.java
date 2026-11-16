package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;

import java.time.LocalDate;

/**
 * Criterios del reporte "Equipamiento". El período es obligatorio; los demás criterios no restringen cuando son {@code null}.
 *
 * @param desde Primer día del período.
 * @param hasta Último día del período.
 * @param tipo Tipo de equipo.
 * @param idEquipo ID del equipo.
 * @param textoEquipo Texto con el que se muestra el equipo elegido en los criterios aplicados, o {@code null}.
 */
public record CriteriosReporteEquipamiento(LocalDate desde, LocalDate hasta, TipoEquipamiento tipo, Long idEquipo, String textoEquipo) {
}
