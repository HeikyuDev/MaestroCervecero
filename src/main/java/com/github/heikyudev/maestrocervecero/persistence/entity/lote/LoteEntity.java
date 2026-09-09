package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lote")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LoteEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Ej: "PILSEN_CLASICA-3" (3er lote cocinado de esa receta). Ver generación abajo.
    @Column(name = "identificador_interno", nullable = false, unique = true)
    private String identificadorInterno;

    @Column(name = "volumen_objetivo",nullable = false)
    private Double volumenObjetivo;

    @Column(name = "estado",nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoLote estado;

    @Column(name = "fecha_inicio_estimada", nullable = false)
    private LocalDate fechaInicioEstimada;

    @Column(name = "fecha_finalizacion_estimada", nullable = false)
    private LocalDate fechaFinalizacionEstimada;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_produccion_id", nullable = false)
    private PlanificacionProduccionEntity planificacionProduccion;

    @OneToMany(mappedBy = "lote", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EtapaLoteEntity> etapas = new ArrayList<>();

    @OneToMany(mappedBy = "lote", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleCostoDirectoEntity> detallesCostoDirecto = new ArrayList<>();
}
