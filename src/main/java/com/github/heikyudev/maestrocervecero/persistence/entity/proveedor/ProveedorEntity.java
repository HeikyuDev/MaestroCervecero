package com.github.heikyudev.maestrocervecero.persistence.entity.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa a la persona o entidad que provee los insumos necesarios para la producción.
 * Un proveedor puede ofrecer una variedad de productos, cada uno con su presentación comercial,
 * los cuales se gestionan a través de su catálogo ({@link CatalogoProveedorEntity}).
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // (Un Proveedor debe tener una localidad)
    @JoinColumn(name = "localidad_id", nullable = false)
    private LocalidadEntity localidad;

    @OneToMany(mappedBy = "proveedor", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<CatalogoProveedorEntity> catalogoProveedor = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
