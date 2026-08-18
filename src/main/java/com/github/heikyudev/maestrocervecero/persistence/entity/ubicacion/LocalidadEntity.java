package com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa una localidad, ciudad o pueblo específico dentro de una provincia.
 * <p>
 * Cada localidad está asociada a una única {@link ProvinciaEntity}.
 */
@Entity
@Table(name = "localidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@SoftDelete
public class LocalidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    // Se utiliza EAGER ya que la provincia tiene la anotacion del Softdelete
    // Se utiliza optional = false ya que una localidad siempre debe pertenecer a una provincia
    @JoinColumn(name = "provincia_id", nullable = false)
    private ProvinciaEntity provincia;
}
