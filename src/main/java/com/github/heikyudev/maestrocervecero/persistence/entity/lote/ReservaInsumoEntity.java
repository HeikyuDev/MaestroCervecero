package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reserva_insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReservaInsumoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "cantidad_reservada", nullable = false)
    private Double cantidadReservada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_lote_id", nullable = false)
    private EtapaLoteEntity etapaLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_insumo_id", nullable = false)
    private LoteInsumoEntity loteInsumo;
}
