package com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del reporte "Lotes" tal como la devuelve la consulta: los datos del lote y de su versión de receta, sin cargar
 * ninguna entidad.
 *
 * @param id ID del lote.
 * @param identificadorInterno Código del lote.
 * @param nombreReceta Nombre de la versión de receta de la planificación del lote.
 * @param estado Estado del lote.
 * @param fechaFinalizacionEstimada Fecha de finalización estimada.
 * @param fechaFinalizacion Fecha y hora de finalización real, o {@code null}.
 */
public record LoteReporteProyeccion(Long id, String identificadorInterno, String nombreReceta, EstadoLote estado,
                                      LocalDate fechaFinalizacionEstimada, LocalDateTime fechaFinalizacion) {
}
