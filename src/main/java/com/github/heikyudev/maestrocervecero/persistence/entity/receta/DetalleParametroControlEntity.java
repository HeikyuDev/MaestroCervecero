package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa la configuración detallada de un parámetro de control dentro de una etapa específica
 * de un plan de monitoreo.
 * <p>
 * Esta entidad actúa como una clase intermedia que vincula un {@link PlanMonitoreoEtapaEntity} con un
 * {@link ParametroControlEntity} específico. Define los umbrales de control para dicho parámetro,
 * incluyendo los valores mínimo, máximo e ideal esperados durante el proceso de producción.
 * <p>
 * Por ejemplo, para el parámetro 'PH' en la etapa de 'Maceración 1', esta clase podría definir
 * que el valor ideal es 5.2, con un mínimo de 5.0 y un máximo de 5.5.
 */
@Entity
@Table(name = "detalle_parametro_control")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
public class DetalleParametroControlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "valor_minimo", nullable = false)
    private Double valorMinimo;

    @Column(name = "valor_maximo", nullable = false)
    private Double valorMaximo;

    @Column(name = "valor_ideal", nullable = false)
    private Double valorIdeal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_monitoreo_etapa_id", nullable = false)
    private PlanMonitoreoEtapaEntity planMonitoreoEtapa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "parametro_control_id", nullable = false)
    private ParametroControlEntity parametroControl;
}
