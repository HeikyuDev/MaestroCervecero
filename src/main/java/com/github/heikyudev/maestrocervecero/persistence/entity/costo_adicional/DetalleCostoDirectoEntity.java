package com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_costo_directo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetalleCostoDirectoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "costo_por_litro_aplicado", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoPorLitroAplicado;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 4)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "costo_directo_adicional_id", nullable = false)
    private CostoDirectoAdicionalEntity costoDirectoAdicional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteEntity lote;
}
