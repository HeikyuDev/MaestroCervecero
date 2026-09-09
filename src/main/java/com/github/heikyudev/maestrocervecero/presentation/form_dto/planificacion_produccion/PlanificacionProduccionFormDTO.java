package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de formulario para el Registro de una Planificación de Producción.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionProduccionFormDTO {

    /**
     * Fecha estimada de inicio de la planificación de producción.
     */
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización de la planificación de producción.
     */
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Cantidad a producir, en litros.
     */
    private Double cantidadAProducir;

    /**
     * Identificador de la receta a utilizar en esta planificación de producción.
     * <p>
     * Se espera que este identificador corresponda a una {@link RecetaEntity} existente en la
     * base de datos. Las versiones de receta son un detalle interno invisible para el usuario:
     * este solo elige la receta, y es responsabilidad del service resolver cuál es su última
     * versión activa ({@code VersionRecetaEntity.esUltimaVersion}) y asociar esa versión puntual
     * a la planificación de producción.
     * </p>
     */
    private Long idReceta;
}
