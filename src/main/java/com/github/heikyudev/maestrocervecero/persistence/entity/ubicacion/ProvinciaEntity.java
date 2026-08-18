package com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa una provincia, estado o división administrativa principal dentro de un país.
 * <p>
 * Cada provincia está asociada a un único {@link PaisEntity} y puede contener
 * múltiples {@link LocalidadEntity}.
 */

@Entity
@Table(name = "provincia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@SoftDelete
public class ProvinciaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    // Se utiliza EAGER ya que el pais tiene la anotacion del Softdelete
    // Se utiliza optional = false ya que una provincia siempre debe pertenecer a un pais
    @JoinColumn(name = "pais_id", nullable = false)
    private PaisEntity pais;
}
