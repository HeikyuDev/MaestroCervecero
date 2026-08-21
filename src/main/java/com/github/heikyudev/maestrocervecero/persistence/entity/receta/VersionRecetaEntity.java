package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fotografía de todos los valores productivos de una receta en un momento dado:
 * volumen, densidades objetivo, IBU, duraciones estimadas por etapa, y el detalle
 * de maltas, lúpulos y levaduras planificados. Cada vez que el usuario "modifica
 * una receta", el service crea una nueva instancia (no actualiza la existente) y
 * marca {@code esUltimaVersion = true} en la nueva, dejando {@code false} en la
 * anterior — así los lotes que ya arrancaron con una versión vieja no se ven
 * afectados por cambios posteriores.
 */
@Entity
@Table(name = "version_receta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VersionRecetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "volumen_base", nullable = false)
    private Double volumenBase;

    @Column(name = "relacion_de_empaste", nullable = false)
    private Integer relacionDeEmpaste;

    @Column(name = "og_objetivo", nullable = false)
    private Double ogObjetivo;

    @Column(name = "fg_objetivo", nullable = false)
    private Double fgObjetivo;

    @Column(name = "ibu_objetivo", nullable = false)
    private Integer ibuObjetivo;

    @Column(name = "duracion_maceracion", nullable = false)
    private Integer duracionMaceracion;

    @Column(name = "duracion_hervido", nullable = false)
    private Integer duracionHervido;

    @Column(name = "duracion_fermentacion", nullable = false)
    private Integer duracionFermentacion;

    @Column(name = "duracion_maduracion", nullable = false)
    private Integer duracionMaduracion;

    @Column(name = "es_ultima_version", nullable = false)
    private boolean esUltimaVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaEntity receta;

    @Builder.Default
    @OneToMany(mappedBy = "versionReceta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleMaltaEntity> detallesMalta = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "versionReceta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleLupuloEntity> detallesLupulo = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "versionReceta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleLevaduraEntity> detallesLevadura = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "versionReceta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanMonitoreoEtapaEntity> planesMonitoreo = new ArrayList<>();
}