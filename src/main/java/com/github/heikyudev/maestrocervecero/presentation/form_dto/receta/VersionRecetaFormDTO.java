package com.github.heikyudev.maestrocervecero.presentation.form_dto.receta;

import lombok.*;

import java.util.List;

/**
 * DTO de formulario para el alta y la modificación de una versión de receta.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
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
    private String nombre;


    /***
     * Volumen Base de la receta en litros.
     */
    private Double volumenBase;

    /**
     * Relación de empaste de la receta en litros por kilogramo.
     */
    private Double relacionDeEmpaste;

    /**
     * Densidad Original Objetivo de la receta.
     */
    private Double ogObjetivo;

    /**
     * Densidad Final Objetivo de la receta.
     */
    private Double fgObjetivo;

    /**
     * IBU Objetivo de la receta.
     */
    private Integer ibuObjetivo;

    /**
     * Duración de la maceración en minutos.
     */
    private Integer duracionMaceracion;

    /**
     * Duración del hervido en minutos.
     */
    private Integer duracionHervido;

    /**
     * Duración de la fermentación en días.
     */
    private Integer duracionFermentacion;

    /**
     * Duración de la maduración en días.
     */
    private Integer duracionMaduracion;

    /**
     * Lista de detalles de malta asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de malta específico y su cantidad planificada para la receta.
     * </p>
     */
    private List<DetalleMaltaFormDTO> detallesMalta;

    /**
     * Lista de detalles de lúpulo asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de lúpulo específico y su cantidad planificada para la receta.
     * </p>
     */
    private List<DetalleLupuloFormDTO> detallesLupulo;

    /**
     * Lista de detalles de levadura asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un tipo de levadura específico y su cantidad planificada para la receta.
     * </p>
     */
    private List<DetalleLevaduraFormDTO> detallesLevadura;

    /**
     * Lista de planes de monitoreo de etapa asociados a esta versión de receta.
     * <p>
     * Cada elemento de la lista representa un plan de monitoreo específico para una etapa de control
     * y sus parámetros de control planificados para la receta.
     * </p>
     */
    private List<PlanMonitoreoEtapaFormDTO> planesMonitoreo;
}
