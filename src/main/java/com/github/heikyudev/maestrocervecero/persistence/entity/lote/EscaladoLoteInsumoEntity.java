package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import jakarta.persistence.*;
import lombok.*;

/**
 * Parámetros con los que se escaló un insumo de un lote, guardados al iniciarlo. Solo se completan los datos del tipo
 * de insumo: el extracto potencial para una malta; los ácidos alfa y el formato para un lúpulo; el tipo y las células
 * por gramo para una levadura.
 */
@Entity
@Table(name = "escalado_lote_insumo")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EscaladoLoteInsumoEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escalado_lote_id", nullable = false)
    private EscaladoLoteEntity escaladoLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private InsumoEntity insumo;

    // Malta
    @Column(name = "potencial_extracto")
    private Double potencialExtracto;

    // Lúpulo
    @Column(name = "alfa_acidos")
    private Double aa;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato")
    private FormatoLupulo formato;

    // Levadura
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_levadura")
    private TipoLevadura tipoLevadura;

    @Column(name = "cantidad_celulas_por_gramo")
    private Double cantidadCelulasPorGramo;
}
