package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;


import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * "Contenedor" estable de un proveedor: agrupa todas las versiones históricas que fue
 * teniendo a lo largo del tiempo. No guarda datos propios del proveedor (razón social,
 * CUIT, catálogo, etc.) — esos viven en cada {@link VersionProveedorEntity}, porque cada
 * modificación de un proveedor genera una versión nueva en vez de sobrescribir la
 * anterior. Esto evita que una modificación del proveedor afecte retroactivamente los
 * {@code DetalleCompraEntity} que ya referencian un ítem del catálogo de una versión
 * anterior.
 * <p>
 */
@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ProveedorEntity extends AuditableEntity<String> {
    @Id
    @GeneratedValue(strategy =   GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VersionProveedorEntity> versiones = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
