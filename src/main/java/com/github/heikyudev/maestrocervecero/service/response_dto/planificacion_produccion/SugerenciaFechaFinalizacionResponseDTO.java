package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con la fecha de finalización sugerida para una planificación de producción,
 * resultado de simular una secuencia de lotes en un único fermentador. No representa nada
 * persistido.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class SugerenciaFechaFinalizacionResponseDTO {

    /**
     * Fecha de inicio estimada con la que se hizo la simulación (la misma que recibió el servicio).
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha de finalización sugerida: la fecha en que termina el último lote simulado.
     */
    private LocalDate fechaFinalizacionSugerida;

    /**
     * Cantidad de lotes simulados: la cantidad a producir dividida por la capacidad de lote
     * estándar, redondeada hacia arriba.
     */
    private int cantidadLotes;

    /**
     * Capacidad de lote estándar usada en la simulación, en litros (tomada de la configuración de producción).
     */
    private Double capacidadLoteEstandar;

    /**
     * Duración de cada lote simulado, en días: día de brewing (maceración + hervido, redondeado
     * hacia arriba a días completos) más fermentación y maduración de la última versión activa de la receta.
     */
    private long duracionLoteDias;
}
