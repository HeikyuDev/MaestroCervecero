package com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa una provincia, estado o división administrativa principal dentro de un país.
 * <p>
 * Cada provincia está asociada a un único {@link PaisEntity} y puede contener
 * múltiples {@link LocalidadEntity}.
 */

@Entity
@Table(name = "provincia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ProvinciaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pais_id", nullable = false)
    private PaisEntity pais;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
