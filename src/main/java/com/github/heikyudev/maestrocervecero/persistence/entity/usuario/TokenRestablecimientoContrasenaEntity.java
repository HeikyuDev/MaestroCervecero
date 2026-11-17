package com.github.heikyudev.maestrocervecero.persistence.entity.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Enlace de un solo uso para restablecer la contraseña de un usuario, enviado por correo a la dirección registrada
 * del usuario cuando la solicita desde la pantalla de inicio de sesión.
 * <p>
 * Solo se guarda el hash SHA-256 del token, en hexadecimal: el token en sí viaja únicamente en el enlace del correo,
 * de modo que quien lea la tabla no puede armar un enlace válido. Un enlace sirve mientras no se usó
 * ({@code fechaUso} nula) y no venció ({@code fechaExpiracion} posterior al momento actual). La fecha de creación
 * ({@code createdDate}, heredada de {@link AuditableEntity}) es el momento en que se envió el enlace.
 * </p>
 */
@Entity
@Table(name = "token_restablecimiento_contrasena")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class TokenRestablecimientoContrasenaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Usuario cuya contraseña se restablece con este enlace
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    // Hash SHA-256 del token, en hexadecimal. Único: lo garantiza uk_token_restablecimiento_contrasena_token_hash (ver V27).
    // Nunca se copia al detalle de auditoría
    @ExcluidoDeAuditoria
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    // Momento a partir del cual el enlace deja de servir
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    // Momento en que se restableció la contraseña con este enlace; nula mientras no se usó
    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;
}
