package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "limpieza_equipamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LimpiezaEquipamientoEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_limpieza", nullable = false)
    private LocalDateTime fechaLimpieza;

    @Column(nullable = false)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTransaccion estado;

    /**
     * Estado operativo al que quedó el equipamiento como resultado de esta limpieza puntual
     * (DISPONIBLE o EN_MANTENIMIENTO, según la cantidad de usos evaluada al momento del registro).
     * <p>
     * Se persiste para que {@code anularLimpiezaEquipamiento} pueda validar que el equipamiento
     * sigue exactamente en el estado que dejó ESTA limpieza, y no en cualquiera de los dos
     * estados posibles en general — evita revertir una limpieza vieja cuando el estado actual del
     * equipamiento en realidad lo dejó un mantenimiento u otra limpieza posterior.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo_resultante", nullable = false)
    private EstadoOperativo estadoOperativoResultante;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamiento_id", nullable = false)
    private EquipamientoEntity equipamiento;
}
