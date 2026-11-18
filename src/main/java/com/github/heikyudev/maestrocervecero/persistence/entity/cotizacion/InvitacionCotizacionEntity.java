package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Invitación de un proveedor a responder una solicitud de cotización, con el enlace que recibió por correo. Se crea al
 * generar la solicitud con los proveedores candidatos (sin token); el enlace se arma recién al enviar el correo, después
 * de que el gerente de compras confirma el envío.
 * <p>
 * Solo se guarda el hash SHA-256 del token del enlace, en hexadecimal: el token viaja únicamente en el correo. El
 * enlace sirve mientras la solicitud está abierta, no venció ({@code fechaExpiracion}, igual a la fecha límite de
 * respuesta de la solicitud) y el proveedor sigue activo. El proveedor puede responder y editar su respuesta hasta el
 * cierre: cada envío reemplaza sus ofertas ({@code fechaRespuesta} es la del último envío). La fecha de creación
 * heredada de {@link AuditableEntity} es el momento en que se generó la invitación; el correo se envía después y su
 * resultado queda en {@code estadoEnvio}.
 * </p>
 */
@Entity
@Table(name = "invitacion_cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class InvitacionCotizacionEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_cotizacion_id", nullable = false)
    private SolicitudCotizacionEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    // Hash SHA-256 del token, en hexadecimal. Único: lo garantiza uk_invitacion_cotizacion_token_hash (ver V30).
    // Nunca se copia al detalle de auditoría
    @ExcluidoDeAuditoria
    // Nulo mientras no se envió el correo: el token se genera en cada intento de envío
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    // Se fija al confirmar el envío (la fecha límite de la solicitud); nula mientras la solicitud está pendiente de confirmación
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    // Resultado del envío del correo con el enlace; el proceso programado reintenta los fallidos
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false)
    @Builder.Default
    private EstadoEnvioInvitacion estadoEnvio = EstadoEnvioInvitacion.PENDIENTE;

    // Cantidad de intentos de envío del correo; cada intento genera un token nuevo (el anterior nunca llegó)
    @Column(name = "intentos_envio", nullable = false)
    @Builder.Default
    private Integer intentosEnvio = 0;

    @Column(name = "fecha_ultimo_intento_envio")
    private LocalDateTime fechaUltimoIntentoEnvio;

    // Momento del último envío de la respuesta; nula mientras el proveedor no respondió
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    // Fecha en que el proveedor estima entregar lo que ofrece (una sola por respuesta: su orden de compra tiene una sola
    // fecha); nula mientras no respondió o si respondió sin ofertas
    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    @OneToMany(mappedBy = "invitacion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<OfertaCotizacionEntity> ofertas = new ArrayList<>();
}
