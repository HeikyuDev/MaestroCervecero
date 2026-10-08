package com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO de formulario para el Registro de una Planificación de Producción.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos). Las fechas viajan en formato ISO ({@code yyyy-MM-dd}), el de un campo
 * {@code type="date"}.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionProduccionFormDTO {

    /**
     * Fecha estimada de inicio de la planificación de producción.
     */
    @NotNull(message = "La fecha de inicio estimada es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicioEstimada;

    /**
     * Fecha estimada de finalización de la planificación de producción.
     */
    @NotNull(message = "La fecha de finalización estimada es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFinalizacionEstimada;

    /**
     * Cantidad a producir, en litros.
     */
    @NotNull(message = "La cantidad a producir es obligatoria")
    @Positive(message = "La cantidad a producir debe ser mayor a 0")
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
    @NotNull(message = "La receta es obligatoria")
    private Long idReceta;
}
