package com.github.heikyudev.maestrocervecero.persistence.entity.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Entidad de persistencia de los usuarios del ERP. Cumple doble función: es el registro
 * de negocio en la tabla {@code usuarios}, y es la fuente de datos que
 * {@code UserDetailServiceImpl} traduce a un {@link org.springframework.security.core.userdetails.UserDetails}
 * en cada login (por eso trae, además de las credenciales, los flags de estado de cuenta
 * que ese contrato exige).
 * <p>
 * {@code @SoftDelete}: Hibernate agrega automáticamente {@code WHERE deleted = false} a
 * TODAS las queries generadas sobre esta entidad (incluida la que usa el login para buscar
 * por username). Esto implica que un usuario dado de baja lógica queda excluido del login
 * sin que haya que codificar esa exclusión a mano en ningún lado.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SoftDelete
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
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired;

    // === Autorización ===
    // Cada usuario tiene un único Rol, y cada Rol trae consigo su propio conjunto fijo de
    // Permisos granulares (ver Rol.java). No hay una relación many-to-many de permisos acá:
    // se administra a nivel de código, no de tabla intermedia.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // ======== DATOS DEL USUARIO ====
    private String nombre;
    private String correo;
    private String telefono;
}
