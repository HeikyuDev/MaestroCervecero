package com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa los diversos parámetros de control utilizados para realizar una futura medición en el sistema.
 * Cada parámetro define un rango (mínimo y máximo) de valores aceptables para una variable de control específica.
 */
@Entity
@Table(name = "parametro_control")
@Getter
@Setter
@SoftDelete
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

}
