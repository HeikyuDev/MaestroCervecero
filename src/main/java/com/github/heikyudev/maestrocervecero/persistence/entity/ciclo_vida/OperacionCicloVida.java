package com.github.heikyudev.maestrocervecero.persistence.entity.ciclo_vida;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Superclase mapeada (no genera tabla propia) de toda operación del ciclo de vida de un Barril
 * o un Equipamiento (Falla, Mantenimiento, Limpieza, y a futuro Despacho/Devolución de Barril).
 * <p>
 * Agrupa los datos y el comportamiento transaccional 100% comunes a esas operaciones: cuándo se
 * registró, sus observaciones, su estado (REGISTRADO/ANULADO) y, si fue anulada, cuándo y por
 * qué motivo.
 * </p>
 * <p>
 * No incluye la relación con el Barril/Equipamiento afectado: esa relación apunta a un tipo
 * distinto según el agregado, así que la agrega cada rama concreta ({@code OperacionCicloVidaBarril},
 * {@code OperacionCicloVidaEquipamiento}), que sí son {@code @Entity} con herencia {@code JOINED}
 * y permiten, por agregado, una única consulta polimórfica de "última operación registrada".
 * </p>
 * <p>
 * Extiende {@link AuditableEntity} para heredar auditoría automática (quién y cuándo se creó/
 * modificó cada registro).
 * </p>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@MappedSuperclass
public abstract class OperacionCicloVida extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTransaccion estado;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;
}
