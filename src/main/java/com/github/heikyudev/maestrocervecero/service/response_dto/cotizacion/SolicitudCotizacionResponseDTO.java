package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con una solicitud de cotización. Los listados la informan sin líneas, invitaciones ni plan.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCotizacionResponseDTO {

    /**
     * Identificador de la solicitud.
     */
    private Long id;

    /**
     * Estado de la solicitud.
     */
    private EstadoSolicitudCotizacion estado;

    /**
     * Identificador de la planificación de producción que la originó.
     */
    private Long idPlanificacion;

    /**
     * Texto con el que se muestra la planificación (ej. {@code Plan #7 - Pale Ale}).
     */
    private String etiquetaPlanificacion;

    /**
     * Momento en que se generó la solicitud.
     */
    private LocalDateTime fechaSolicitud;

    /**
     * Momento hasta el que los proveedores pueden responder, o {@code null} mientras está pendiente de confirmación.
     */
    private LocalDateTime fechaLimiteRespuesta;

    /**
     * Momento en que se cerró, o {@code null}.
     */
    private LocalDateTime fechaCierre;

    /**
     * Momento en que se generó el plan, o {@code null}.
     */
    private LocalDateTime fechaGeneracionPlan;

    /**
     * Momento en que se canceló, o {@code null}.
     */
    private LocalDateTime fechaCancelacion;

    /**
     * Motivo de la cancelación, o {@code null}.
     */
    private String motivoCancelacion;

    /**
     * Cantidad de insumos solicitados.
     */
    private int cantidadInsumos;

    /**
     * Cantidad de proveedores invitados.
     */
    private int cantidadInvitaciones;

    /**
     * Cantidad de proveedores que respondieron.
     */
    private int cantidadRespuestas;

    /**
     * Situaciones que el gerente de compras debe conocer (insumos sin proveedor, sin respuestas, plan que no cubre todo).
     */
    private List<String> avisos;

    /**
     * Insumos solicitados; {@code null} en los listados.
     */
    private List<LineaSolicitudCotizacionResponseDTO> lineas;

    /**
     * Proveedores invitados; {@code null} en los listados.
     */
    private List<InvitacionCotizacionResponseDTO> invitaciones;

    /**
     * Plan generado; {@code null} en los listados o si todavía no tiene plan.
     */
    private PlanCompraResponseDTO plan;

    /**
     * {@code true} si el cierre programado falló el máximo de intentos y dejó de reintentarse.
     */
    private boolean fallaCierre;

    /**
     * Motivo de la última falla del cierre programado, o {@code null}.
     */
    private String motivoFallaCierre;

    /**
     * Tiempo que queda hasta la fecha límite de respuesta (ej. {@code 2 días y 5 horas}) mientras la solicitud está
     * abierta; {@code Vencida} si ya pasó; {@code null} si no está abierta.
     */
    private String tiempoRestante;

    /**
     * Usuario que canceló la solicitud, según la auditoría de la última modificación; {@code null} si no está cancelada.
     */
    private String usuarioCancelacion;

    /**
     * Momento en que el gerente de compras confirmó el envío a los proveedores, o {@code null}.
     */
    private LocalDateTime fechaConfirmacionEnvio;

    /**
     * Por qué se generó la solicitud (un renglón por cada insumo que el stock no cubre) y, mientras está pendiente de
     * confirmación, qué falta hacer, o {@code null} si la solicitud no tiene líneas.
     */
    private ExplicacionSolicitudCotizacionResponseDTO explicacion;
}
