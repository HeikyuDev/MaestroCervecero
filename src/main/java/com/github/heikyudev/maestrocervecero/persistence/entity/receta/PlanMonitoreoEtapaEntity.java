package com.github.heikyudev.maestrocervecero.persistence.entity.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.util.ArrayList;
import java.util.List;


/**
 * En esta entidad, se va a configurar las mediciones que se van a poder realizar en  la receta
 * 1. Primero se tendra que seleccionar las Etapas de control registradas para la etapa selecciona
 * Ej: En Fermentacion, Se puede seleccionar las etapas de Fermentacion Inicial, Media y Final.
 * Y bueno tambien se solita un ORDEN numerico, para evitar que un usuario pueda registrar mediciones
 * en un orden incorrecto, por ejemplo, registrar una medicion de Fermentacion Final antes de Fermentacion Inicial.
 * 2. Luego el usuario va  a tener que seleccionar los parametros de control que se desean COntrolar en esta etapa
 * de control en particular, especificando para cada una el valor minimo, maximo e ideal. Esto se hace en la entidad DetalleParametroControlEntity.
 */
@Entity
@Table(name = "plan_monitoreo_etapa")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
// ¿Porque no extiende de Auditable entity?? Porque esto es parte de la receta.
public class PlanMonitoreoEtapaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "etapa_control_id", nullable = false)
    private EtapaControlEntity etapaControl;

    @OneToMany(mappedBy = "planMonitoreoEtapa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleParametroControlEntity> detalleParametroControlList = new ArrayList<>();
}
