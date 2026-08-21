package com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa los diversos parámetros de control utilizados para realizar una futura medición en el sistema.
 * Cada parámetro define un rango (mínimo y máximo) de valores aceptables para una variable de control específica.
 */
@Entity
@Table(name = "parametro_control")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroControlEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "valor_minimo", nullable = false)
    private Double valorMinimo;

    @Column(name = "valor_maximo", nullable = false)
    private Double valorMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

}
