package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Línea de detalle que asocia una {@link LevaduraEntity} específica a una
 * {@link VersionRecetaEntity}, indicando la cantidad planificada (en gramos,
 * unidad de medida propia de Levadura) para esa versión de la receta.
 */
@Entity
@Table(name = "detalle_levadura")
@Getter
@Setter
@Builder
@SoftDelete
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetalleLevaduraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Double cantidad;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "levadura_id", nullable = false)
    private LevaduraEntity levadura;
}
