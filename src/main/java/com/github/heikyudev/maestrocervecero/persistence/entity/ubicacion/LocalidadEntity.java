package com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;


/**
 * Representa una localidad, ciudad o pueblo específico dentro de una provincia.
 * <p>
 * Cada localidad está asociada a una única {@link ProvinciaEntity}.
 */
@Entity
@Table(name = "localidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LocalidadEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private ProvinciaEntity provincia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
