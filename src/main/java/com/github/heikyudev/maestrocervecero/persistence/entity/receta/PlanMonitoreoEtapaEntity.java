package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Plan de monitoreo de una {@link VersionRecetaEntity}: define qué mediciones se pueden realizar
 * durante una etapa de control de la receta.
 * <p>
 * Cada plan referencia una {@link EtapaControlEntity} (por ejemplo, Fermentación Inicial, Media o
 * Final dentro de la etapa de fermentación) y agrupa los parámetros de control a medir en ella,
 * con su valor mínimo, máximo e ideal ({@link DetalleParametroControlEntity}). Una misma etapa de
 * control no puede aparecer en dos planes de la misma versión.
 * </p>
 * <p>
 * Al registrar una medición, el lote exige que el plan elegido corresponda a la etapa que tiene
 * en curso.
 * </p>
 */
@Entity
@Table(name = "plan_monitoreo_etapa")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanMonitoreoEtapaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_control_id", nullable = false)
    private EtapaControlEntity etapaControl;

    @OneToMany(mappedBy = "planMonitoreoEtapa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleParametroControlEntity> detallesParametroControl = new ArrayList<>();
}
