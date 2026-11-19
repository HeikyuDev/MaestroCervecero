package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_busqueda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)

/**
 * Solicitud de búsqueda de un barril despachado: la fecha y hora que el cliente propone para que se retire el barril y
 * sus observaciones. La registra el sistema a nombre del cliente cuando confirma el retiro desde el enlace del
 * recordatorio que recibe por correo ({@code invitacionRetiro}).
 * <p>
 * Queda {@code PENDIENTE} hasta que se registra la devolución del barril, que la deja {@code BUSCADA} y queda en
 * {@code devolucionBarril}; anular esa devolución la vuelve a dejar pendiente. Anular el despacho la deja
 * {@code CANCELADA}. Un despacho tiene a lo sumo una solicitud pendiente (lo garantiza
 * udx_solicitud_busqueda_despacho_pendiente, ver V35).
 * </p>
 */
public class SolicitudBusquedaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_busqueda", nullable = false)
    private LocalDateTime fechaBusqueda;

    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "despacho_barril_id", nullable = false)
    private DespachoBarrilEntity despachoBarril;

    // PENDIENTE, BUSCADA o CANCELADA. Un despacho tiene a lo sumo una pendiente (udx_solicitud_busqueda_despacho_pendiente, ver V35)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudBusqueda estado;

    // Recordatorio desde cuyo enlace el cliente confirmó el retiro; nulo si se registró por otra vía
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitacion_retiro_barriles_id")
    private InvitacionRetiroBarrilesEntity invitacionRetiro;

    // Devolución que dio por buscada la solicitud; nula mientras está pendiente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucion_barril_id")
    private DevolucionBarrilEntity devolucionBarril;
}
