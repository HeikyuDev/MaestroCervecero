package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

@Entity
@Table(name = "motivo_ajuste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MotivoAjusteEntity extends AuditableEntity<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "tipo_ajuste",nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoAjuste tipoAjuste;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
