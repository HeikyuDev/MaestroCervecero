package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de formulario para el alta y la modificación de una versión de receta.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario y expone setters para que Spring MVC pueda enlazar los campos del formulario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionRecetaFormDTO {

    /**
     * Nombre de la versión de receta.
     * <p>
     * Se espera que este nombre sea único dentro del contexto de la receta a la que pertenece.
     * </p>
     */
    @NotBlank(message = "El nombre de la receta es obligatorio")
    private String nombre;


    /***
     * Volumen Base de la receta en litros.
     */
    @NotNull(message = "El volumen base es obligatorio")
    @Positive(message = "El volumen base debe ser mayor a 0")
    private Double volumenBase;

    /**
     * Relación de empaste de la receta en litros por kilogramo.
     */
    @NotNull(message = "La relación de empaste es obligatoria")
    @Positive(message = "La relación de empaste debe ser mayor a 0")
    private Double relacionDeEmpaste;

    /**
     * Densidad Original Objetivo de la receta.
     */
    @NotNull(message = "La OG objetivo es obligatoria")
    @Positive(message = "La OG objetivo debe ser mayor a 0")
    private Double ogObjetivo;

    /**
     * Densidad Final Objetivo de la receta.
     */
    @NotNull(message = "La FG objetivo es obligatoria")
    @Positive(message = "La FG objetivo debe ser mayor a 0")
    private Double fgObjetivo;

    /**
     * IBU Objetivo de la receta.
     */
    @NotNull(message = "El IBU objetivo es obligatorio")
    @PositiveOrZero(message = "El IBU objetivo no puede ser negativo")
    private Integer ibuObjetivo;

    /**
     * Duración de la maceración en minutos.
     */
    @NotNull(message = "La duración de la maceración es obligatoria")
    @Positive(message = "La duración de la maceración debe ser mayor a 0")
    private Integer duracionMaceracion;

    /**
     * Duración del hervido en minutos.
     */
    @NotNull(message = "La duración del hervido es obligatoria")
    @Positive(message = "La duración del hervido debe ser mayor a 0")
    private Integer duracionHervido;

    /**
     * Duración de la fermentación en días.
     */
    @NotNull(message = "La duración de la fermentación es obligatoria")
    @Positive(message = "La duración de la fermentación debe ser mayor a 0")
    private Integer duracionFermentacion;

    /**
     * Duración de la maduración en días.
     */
    @NotNull(message = "La duración de la maduración es obligatoria")
    @Positive(message = "La duración de la maduración debe ser mayor a 0")
    private Integer duracionMaduracion;

    /**
     * Lista de detalles de malta asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de malta específico y su cantidad planificada para la receta.
     * </p>
     */
    @Valid
    @NotEmpty(message = "La receta debe tener al menos una malta")
    @Builder.Default
    private List<DetalleMaltaFormDTO> detallesMalta = new ArrayList<>();

    /**
     * Lista de detalles de lúpulo asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de lúpulo específico y su cantidad planificada para la receta.
     * </p>
     */
    @Valid
    @NotEmpty(message = "La receta debe tener al menos un lúpulo")
    @Builder.Default
    private List<DetalleLupuloFormDTO> detallesLupulo = new ArrayList<>();

    /**
     * Lista de detalles de levadura asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de levadura específico y su cantidad planificada para la receta.
     * </p>
     */
    @Valid
    @NotEmpty(message = "La receta debe tener al menos una levadura")
    @Builder.Default
    private List<DetalleLevaduraFormDTO> detallesLevadura = new ArrayList<>();

    /**
     * Lista de planes de monitoreo de etapa asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un plan de monitoreo específico para una etapa de control
     * y sus parámetros de control planificados para la receta.
     * </p>
     */
    @Valid
    @Builder.Default
    private List<PlanMonitoreoEtapaFormDTO> planesMonitoreo = new ArrayList<>();
}
