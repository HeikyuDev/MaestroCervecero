package com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una orden de producción: la puesta en marcha de una {@link VersionRecetaEntity}
 * concreta para producir una cantidad determinada.
 */
@Entity
@Table(name = "orden_produccion")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class OrdenProduccionEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_inicio_estimada", nullable = false)
    private LocalDate fechaInicioEstimada;

    @Column(name = "fecha_finalizacion_estimada" , nullable = false)
    private LocalDate fechaFinalizacionEstimada;

    @Column(name = "cantidad_a_producir" , nullable = false)
    private Double cantidadAProducir;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_finalizacion")
    private String motivoFinalizacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(name = "estado" , nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "version_receta_id", nullable = false)
    private VersionRecetaEntity versionReceta;
}
