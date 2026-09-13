package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.versionado.VersionadoEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotografía de todos los datos de un proveedor en un momento dado: razón social, nombre
 * comercial, CUIT, datos de contacto, localidad y el catálogo de productos que ofrece
 * ({@link CatalogoProveedorEntity}). Cada vez que el usuario "modifica un proveedor", el
 * service crea una nueva instancia (no actualiza la existente) y marca
 * {@code esUltimaVersion = true} en la nueva, dejando {@code false} en la anterior — así los
 * {@code DetalleCompraEntity} que ya referencian un ítem del catálogo de una versión vieja
 * no se ven afectados por cambios posteriores.
 */
@Entity
@Table(name = "version_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class VersionProveedorEntity implements VersionadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "nombre_comercial", nullable = false)
    private String nombreComercial;

    @Column(nullable = false)
    private String cuit;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String direccion;

    @Column(name = "es_ultima_version", nullable = false)
    private boolean esUltimaVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // (Una versión de proveedor debe pertenecer a un proveedor)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // (Un Proveedor debe tener una localidad)
    @JoinColumn(name = "localidad_id", nullable = false)
    private LocalidadEntity localidad;

    @OneToMany(mappedBy = "version", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CatalogoProveedorEntity> catalogoProveedor = new ArrayList<>();

}
