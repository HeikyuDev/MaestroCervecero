package com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etapa_control")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaControlEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "etapa_a_controlar", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoEtapa etapaAControlar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

}
