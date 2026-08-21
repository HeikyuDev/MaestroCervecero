package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * "Contenedor" estable de una receta: agrupa todas las versiones históricas que
 * fue teniendo a lo largo del tiempo. No guarda valores productivos (volumen, OG,
 * FG, etc.) — esos viven en cada {@link VersionRecetaEntity}, porque cada
 * modificación de una receta genera una versión nueva en vez de sobrescribir la
 * anterior. Esto evita que un cambio en la receta afecte retroactivamente lotes
 * de producción que ya se iniciaron con una versión previa.
 * <p>
 */
@Entity
@Table(name = "receta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true,callSuper = false)
public class RecetaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "contador_lotes", nullable = false)
    @Builder.Default
    private Long contadorLotes = 1L;

    @Builder.Default
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VersionRecetaEntity> versiones = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}