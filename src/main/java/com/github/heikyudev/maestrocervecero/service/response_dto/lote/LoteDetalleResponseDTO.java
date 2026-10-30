package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con el detalle de un lote para su pantalla: la cabecera, las seis etapas en orden del proceso con su
 * equipo y sus fechas, la versión de receta de su planificación y la etapa que la pantalla abre por defecto.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class LoteDetalleResponseDTO {
    /**
     * Identificador único del lote.
     */
    private Long id;
    /**
     * Código autogenerado del lote ("LOT-R" + id de la receta + "-" + número de lote de esa receta con dos dígitos como mínimo). Ej: "LOT-R7-03".
     */
    private String identificadorInterno;
    /**
     * Estado del lote.
     */
    private EstadoLote estado;
    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;
    /**
     * Fecha estimada de inicio del lote.
     */
    private LocalDate fechaInicioEstimada;
    /**
     * Fecha estimada de finalización del lote.
     */
    private LocalDate fechaFinalizacionEstimada;
    /**
     * Fecha y hora en que efectivamente arrancó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaInicio;
    /**
     * Fecha y hora en que efectivamente finalizó el lote. Nula hasta que eso ocurre.
     */
    private LocalDateTime fechaFinalizacion;
    /**
     * Fecha y hora en que se canceló el lote. Nula salvo que haya sido cancelado.
     */
    private LocalDateTime fechaCancelacion;
    /**
     * Motivo de cancelación del lote. Nulo salvo que haya sido cancelado.
     */
    private String motivoCancelacion;
    /**
     * Usuario que canceló el lote, según la auditoría de la última modificación. Nulo salvo que haya sido cancelado.
     */
    private String usuarioCancelacion;
    /**
     * ID de la planificación de producción del lote.
     */
    private Long idPlanificacionProduccion;
    /**
     * Versión de receta de la planificación, con su posición ("versión N de M") y todos sus datos (relación de empaste,
     * OG, FG, duraciones y detalles de insumos).
     */
    private VersionRecetaDePlanificacionResponseDTO versionReceta;
    /**
     * ID del fermentador del lote (equipamiento de su etapa de Fermentación), o {@code null} si no tiene.
     */
    private Long idFermentador;
    /**
     * Identificador interno del fermentador del lote, o {@code null} si no tiene.
     */
    private String identificadorInternoFermentador;
    /**
     * Las seis etapas del lote, en el orden del proceso (Molienda a Envasado).
     */
    private List<EtapaLoteResponseDTO> etapas;
    /**
     * Etapa que la pantalla abre por defecto: la que está en curso si el lote está En ejecución; Molienda si está
     * Pendiente; Envasado si está Finalizado; si está Cancelado, la primera etapa Cancelada (donde se interrumpió) o,
     * si ninguna lo está, la última etapa iniciada, o Molienda si no empezó.
     */
    private TipoEtapa etapaPorDefecto;
}
