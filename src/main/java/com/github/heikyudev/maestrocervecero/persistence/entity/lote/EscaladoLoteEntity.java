package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Instantánea de los datos con los que se escaló un lote, tomada al registrar el lote: los parámetros de sus cuatro equipos y,
 * en {@link #insumos}, los de cada insumo de su receta. Una vez tomada, las cantidades del lote no cambian aunque
 * después se modifiquen los equipos o los insumos.
 */
@Entity
@Table(name = "escalado_lote")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EscaladoLoteEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Molino
    @Column(name = "rendimiento_molienda", nullable = false)
    private Double rendimientoMolienda;

    // Macerador
    @Column(name = "eficiencia_maceracion", nullable = false)
    private Double eficienciaMaceracion;

    @Column(name = "espacio_muerto_macerador", nullable = false)
    private Double espacioMuertoMacerador;

    @Column(name = "capacidad_util_macerador", nullable = false)
    private Double capacidadUtilMacerador;

    // Olla de hervor
    @Column(name = "perdida_por_trub_olla", nullable = false)
    private Double perdidaPorTrubOlla;

    @Column(name = "evaporacion_olla", nullable = false)
    private Double evaporacionOlla;

    @Column(name = "capacidad_util_olla", nullable = false)
    private Double capacidadUtilOlla;

    // Fermentador
    @Column(name = "capacidad_util_fermentador", nullable = false)
    private Double capacidadUtilFermentador;

    @OneToMany(mappedBy = "escaladoLote", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EscaladoLoteInsumoEntity> insumos = new ArrayList<>();

    // == METODOS DE DOMINIO ==

    /**
     * Busca los parámetros con los que se escaló un insumo del lote.
     *
     * @param idInsumo El ID del insumo.
     * @return Los parámetros del insumo guardados al registrar el lote.
     * @throws ReglaNegocioException Si el escalado del lote no guardó parámetros de ese insumo.
     */
    public EscaladoLoteInsumoEntity buscarInsumo(Long idInsumo) {
        return insumos.stream()
                .filter(insumo -> insumo.getInsumo().getId().equals(idInsumo))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("El escalado del lote no tiene los parámetros del insumo con ID: " + idInsumo));
    }
}
