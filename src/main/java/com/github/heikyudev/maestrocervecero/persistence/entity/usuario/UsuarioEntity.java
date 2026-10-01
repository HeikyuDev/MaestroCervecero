package com.github.heikyudev.maestrocervecero.persistence.entity.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entidad de persistencia de los usuarios del ERP. Cumple doble función: es el registro
 * de negocio en la tabla {@code usuarios}, y es la fuente de datos que
 * {@code UserDetailServiceImpl} traduce a un {@link UserDetails}
 * en cada login (por eso trae, además de las credenciales, los flags de estado de cuenta
 * que ese contrato exige).
 */

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity extends AuditableEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Credenciales ===
    @Column(unique = true, nullable = false)
    private String username;

    // Hash BCrypt (nunca la contraseña en texto plano), generado por el PasswordEncoder
    // configurado en SecurityConfig antes de llegar a este campo.
    @Column(nullable = false)
    private String password;

    // === Flags de estado de cuenta exigidos por el contrato UserDetails de Spring Security ===
    // UserDetailServiceImpl los pasa tal cual al construir el User(...) que usa el framework
    // para decidir si puede loguearse (enabled), si la cuenta caducó, si está bloqueada, o si
    // la contraseña venció y hay que forzar un cambio.
    // Un usuario nuevo nace habilitado, vigente y sin bloqueos: por eso los cuatro valen true por
    // defecto. Sin esto, un boolean primitivo queda en false y Spring Security rechaza el login de
    // cualquier cuenta creada sin informarlos explícitamente.
    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Builder.Default
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Builder.Default
    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = true;

    // === Autorización ===
    // Cada usuario tiene un único Rol, y cada Rol trae consigo su propio conjunto fijo de
    // Permisos granulares (ver Rol.java). No hay una relación many-to-many de permisos acá:
    // se administra a nivel de código, no de tabla intermedia.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    // ======== DATOS DEL USUARIO ====
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String apellido;
    @Column(nullable = false)
    private String correo;
    @Column(nullable = false)
    private String telefono;
}
