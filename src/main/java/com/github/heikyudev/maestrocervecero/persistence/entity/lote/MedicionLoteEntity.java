package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicion_lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MedicionLoteEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "valor_medido", nullable = false)
    private Double valorMedido;

    @Column(name = "fecha_medicion", nullable = false)
    // nullable es false ya que al momento de crear la medición, se registra la fecha y hora exacta de la medición.
    private LocalDateTime fechaMedicion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTransaccion estado;

    @Column(name = "hay_alerta", nullable = false)
    private boolean hayAlerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_parametro_control_id", nullable = false) // Una medicion SI O SI se tiene que asicar a un DetalleParametroControlEntity, ya que es el que define el parámetro de control que se está midiendo.
    private DetalleParametroControlEntity detalleParametroControl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_lote_id", nullable = false)
    private EtapaLoteEntity etapaLote;
}
