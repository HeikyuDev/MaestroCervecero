package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la versión de receta exacta que usa una planificación de producción (la que se
 * asoció al registrarla, que no es necesariamente la versión vigente de la receta), junto con los datos
 * de la receta contenedora que la pantalla de solo lectura necesita. No representa nada persistido.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class VersionRecetaDePlanificacionResponseDTO {

    /**
     * Identificador de la planificación de producción consultada.
     */
    private Long idPlanificacion;

    /**
     * Identificador de la receta contenedora de la versión.
     */
    private Long idReceta;

    /**
     * Estado lógico de la receta contenedora (activa o dada de baja).
     */
    private Estado estadoReceta;

    /**
     * Posición de la versión entre las de su receta, ordenadas por orden de registro (1 es la primera).
     */
    private int numeroVersion;

    /**
     * Cantidad total de versiones de la receta contenedora.
     */
    private int cantidadVersiones;

    /**
     * Versión completa de la receta asociada a la planificación, con sus detalles de malta, lúpulo y levadura.
     * Su {@code esUltimaVersion} indica si es la vigente o una versión histórica.
     */
    private VersionRecetaResponseDTO version;
}
