package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Línea de detalle que asocia un {@link LupuloEntity} específico a una
 * {@link VersionRecetaEntity}: cantidad planificada (en gramos), momento de uso
 * y, según ese uso, la etapa correspondiente y/o el tiempo de hervor.
 * <p>
 * Reglas de negocio (aplicadas por el service, no por esta entidad):
 * <ul>
 *   <li>{@code uso = HERVOR}: {@code etapaDeUso = HERVIDO}, {@code tiempoDeHervor}
 *       obligatorio (mayor a cero, no supera la duración de hervido de la versión).</li>
 *   <li>{@code uso = WHIRLPOOL}: {@code etapaDeUso = HERVIDO}, sin tiempo de hervor.</li>
 *   <li>{@code uso = DRY_HOP}: el usuario elige {@code etapaDeUso} entre
 *       {@code FERMENTACION} o {@code MADURACION}, sin tiempo de hervor.</li>
 * </ul>
 */
@Entity
@Table(name = "detalle_lupulo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SoftDelete
public class DetalleLupuloEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Double cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsoLupulo uso;

    // Siempre se completa (HERVIDO para HERVOR/WHIRLPOOL, o FERMENTACION/MADURACION
    // para DRY_HOP a elección del usuario). Nunca queda nulo.
    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_de_uso", nullable = false)
    private TipoEtapa etapaDeUso;

    // Solo aplica cuando uso = HERVOR; queda en 0 si es WHIRLPOOL o DRY_HOP. Nunca queda nulo.
    @Column(name = "tiempo_de_hervor", nullable = false)
    private Double tiempoDeHervor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lupulo_id", nullable = false)
    private LupuloEntity lupulo;
}
