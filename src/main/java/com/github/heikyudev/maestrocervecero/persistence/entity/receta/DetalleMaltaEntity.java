package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Línea de detalle que asocia una {@link MaltaEntity} específica a una
 * {@link VersionRecetaEntity}, indicando la cantidad planificada (en kilogramos,
 * unidad de medida propia de Malta) para esa versión de la receta.
 */
@Entity
@Table(name = "detalle_malta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetalleMaltaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Double cantidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // VERSION RECETA TIENE SOFTDELETE?? SI ENTONCES EL FETCH TIENE QUE SER EAGER
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "malta_id", nullable = false)
    private MaltaEntity malta;
}