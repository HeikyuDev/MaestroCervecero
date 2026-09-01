package com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "costo_directo_adicional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CostoDirectoAdicionalEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "costo_por_litro",nullable = false, precision = 14, scale = 4)
    private BigDecimal costoPorLitro;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Estado estado;
}
