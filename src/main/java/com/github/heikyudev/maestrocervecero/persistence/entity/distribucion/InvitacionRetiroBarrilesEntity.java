package com.github.heikyudev.maestrocervecero.persistence.entity.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recordatorio diario que recibe un cliente para coordinar el retiro de sus barriles con la devolución vencida, con el
 * enlace a la pantalla pública donde propone la fecha y hora del retiro.
 * <p>
 * Hay a lo sumo una por cliente y día ({@code fechaInvitacion}). Lista los despachos vencidos que no tenían una solicitud
 * de búsqueda pendiente al crearse. Solo se guarda el hash SHA-256 del token del enlace, en hexadecimal: el token viaja
 * únicamente en el correo. El enlace sirve hasta {@code fechaExpiracion} y mientras el cliente no reciba un recordatorio
 * más nuevo; una vez respondido ({@code fechaRespuesta}) muestra lo registrado y no admite cambios.
 * </p>
 */
@Entity
@Table(name = "invitacion_retiro_barriles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class InvitacionRetiroBarrilesEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    // Día del recordatorio. Único por cliente: lo garantiza uk_invitacion_retiro_barriles_cliente_fecha (ver V35)
    @Column(name = "fecha_invitacion", nullable = false)
    private LocalDate fechaInvitacion;

    // Hash SHA-256 del token, en hexadecimal. Único: lo garantiza uk_invitacion_retiro_barriles_token_hash (ver V35).
    // Nunca se copia al detalle de auditoría
    @ExcluidoDeAuditoria
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    // Momento hasta el que sirve el enlace
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // Resultado del envío del correo; el proceso programado reintenta los fallidos del mismo día
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

    // Momento en que el cliente confirmó el retiro; nula mientras no respondió
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "invitacion_retiro_despacho",
            joinColumns = @JoinColumn(name = "invitacion_retiro_barriles_id"),
            inverseJoinColumns = @JoinColumn(name = "despacho_barril_id"))
    @Builder.Default
    private List<DespachoBarrilEntity> despachos = new ArrayList<>();
}
