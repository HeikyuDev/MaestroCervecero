package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de cotización consolidada que el sistema genera al registrar una planificación de producción que necesita
 * más insumo del que hay disponible o en camino. Nace pendiente de confirmación: los proveedores reciben el correo recién
 * cuando el gerente de compras confirma el envío.
 * <p>
 * Tiene una línea por insumo faltante ({@link LineaSolicitudCotizacionEntity}), una invitación por cada proveedor
 * activo que ofrece alguno de esos insumos ({@link InvitacionCotizacionEntity}, con las ofertas que respondió) y, una
 * vez cerrada, los planes de órdenes de compra generados ({@link PlanCompraEntity}). Su estado se guarda en la base de
 * datos: el cierre por vencimiento del plazo no depende de que la aplicación siga corriendo.
 * </p>
 */
@Entity
@Table(name = "solicitud_cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class SolicitudCotizacionEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_produccion_id", nullable = false)
    private PlanificacionProduccionEntity planificacionProduccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudCotizacion estado;

    // Momento hasta el que los proveedores pueden responder; al alcanzarlo la solicitud se cierra. Se fija al confirmar
    // el envío (el plazo corre desde ese momento): nula mientras está pendiente de confirmación
    @Column(name = "fecha_limite_respuesta")
    private LocalDateTime fechaLimiteRespuesta;

    // Momento en que el gerente de compras confirmó el envío a los proveedores; nula mientras no lo confirmó
    @Column(name = "fecha_confirmacion_envio")
    private LocalDateTime fechaConfirmacionEnvio;

    // Momento en que se cerró la solicitud; nula mientras está abierta
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // Momento en que se generaron los planes; nula si todavía no tiene planes
    @Column(name = "fecha_generacion_planes")
    private LocalDateTime fechaGeneracionPlanes;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    // Intentos fallidos del cierre programado; al llegar al máximo se marca la falla y se deja de reintentar
    @Column(name = "intentos_cierre", nullable = false)
    @Builder.Default
    private Integer intentosCierre = 0;

    @Column(name = "falla_cierre", nullable = false)
    private boolean fallaCierre;

    @Column(name = "motivo_falla_cierre")
    private String motivoFallaCierre;

    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<LineaSolicitudCotizacionEntity> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<InvitacionCotizacionEntity> invitaciones = new ArrayList<>();

    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    @Builder.Default
    private List<PlanCompraEntity> planes = new ArrayList<>();
}
