package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import org.hibernate.annotations.SoftDelete;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Clase padre abstracta de todo el equipamiento productivo (Molino, Macerador,
 * Olla de Hervor, Fermentador). Estrategia {@code JOINED}: cada subclase tiene su
 * propia tabla, unida a la tabla raíz {@code equipamiento} por FK ({@code equipamiento_id}),
 * evitando columnas nulas por campos que no le correspondan a cada tipo de equipo.
 * Solo las subclases concretas pueden instanciarse.
 * <p>
 * Extiende {@link AuditableEntity} para heredar auditoría automática (quién y cuándo
 * se creó/modificó cada registro), completada por {@code AuditingEntityListener} sin
 * intervención del service.
 * <p>
 * {@code estadoOperativo} representa dónde está el equipo dentro de su ciclo de vida
 * (disponible / en uso / en limpieza). La transición entre estados es responsabilidad
 * exclusiva del service (por ejemplo, al iniciar una operación de producción sobre la
 * fase asociada, o al registrar una limpieza) — esta entidad no valida ni fuerza
 * transiciones, solo persiste el valor actual.
 */
@Entity
@Table(name = "equipamiento")
@Inheritance(strategy = InheritanceType.JOINED)
@SoftDelete
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class EquipamientoEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "identificador_interno", nullable = false, unique = true)
    private String identificadorInterno;

    @Column(name = "descripcion")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false)
    private EstadoOperativo estadoOperativo;
}