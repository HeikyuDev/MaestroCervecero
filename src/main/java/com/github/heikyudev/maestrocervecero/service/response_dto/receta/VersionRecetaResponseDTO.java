package com.github.heikyudev.maestrocervecero.service.response_dto.receta;
import lombok.*;

import java.util.List;

/**
 * DTO de respuesta que representa una version
 * de receta.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class VersionRecetaResponseDTO {

    /**
     * Identificador único de la versión de receta.
     */
    private Long id;

    /**
     * Nombre de la versión de receta.
     */
    private String nombre;

    /**
     * Volumen base de la receta en litros.
     */
    private Double volumenBase;

    /**
     * Relación de empaste de la receta.
     */
    private Double relacionDeEmpaste;

    /**
     * Densidad original objetivo de la receta.
     */
    private Double ogObjetivo;

    /**
     * Densidad final objetivo de la receta.
     */
    private Double fgObjetivo;

    /**
     * IBU objetivo de la receta.
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
     * Duración de la fermentación en dias.
     */
    private Integer duracionFermentacion;

    /**
     * Duración de la maduración en dias.
     */
    private Integer duracionMaduracion;

    /**
     * Indica si esta versión es la última versión de la receta.
     */
    private boolean esUltimaVersion;

    /**
     * Lista de detalles de malta asociados a la versión de receta.
     */
    private List<DetalleMaltaResponseDTO> detallesMalta;

    /**
     * Lista de detalles de lúpulo asociados a la versión de receta.
     */
    private List<DetalleLupuloResponseDTO> detallesLupulo;

    /**
     * Lista de detalles de levadura asociados a la versión de receta.
     */
    private List<DetalleLevaduraResponseDTO> detallesLevadura;

    /**
     * Lista de planes de monitoreo de etapa asociados a la versión de receta.
     */
    private List<PlanMonitoreoEtapaResponseDTO> planesMonitoreo;
}
