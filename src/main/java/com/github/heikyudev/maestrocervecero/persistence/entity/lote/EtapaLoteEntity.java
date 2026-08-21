package com.github.heikyudev.maestrocervecero.persistence.entity.lote;


import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa una de las 6 etapas ({@link TipoEtapa}) por las que atraviesa un
 * {@link LoteEntity} durante su producción. Al registrar un lote, el sistema crea
 * automáticamente las 6 instancias correspondientes, cada una asociada al
 * {@link EquipamientoEntity} concreto que se va a usar en esa etapa (Molino para
 * Molienda, Macerador para Maceración, Olla de Hervor para Hervido, y el mismo
 * Fermentador para Fermentación, Maduración y Envasado).
 */
@Entity
@Table(name = "etapa_lote")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EtapaLoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEtapa etapa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEtapaLote estado;

    // Nulos hasta que la etapa efectivamente arranca/termina.
    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // EAGER: EquipamientoEntity tiene @SoftDelete
    @JoinColumn(name = "equipamiento_id", nullable = false)
    private EquipamientoEntity equipamiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // LAZY: LoteEntity NO tiene @SoftDelete
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteEntity lote;
}
