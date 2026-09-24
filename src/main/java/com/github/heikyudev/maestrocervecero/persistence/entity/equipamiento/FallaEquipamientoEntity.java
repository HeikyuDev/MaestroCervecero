package com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Registra una falla ocurrida sobre un equipamiento, dejándolo en estado operativo
 * {@code EN_MANTENIMIENTO} hasta que se registre el mantenimiento correspondiente.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el equipamiento afectado) los hereda de
 * {@link OperacionCicloVidaEquipamiento}.
 * </p>
 */
@Entity
@Table(name = "falla_equipamiento")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FallaEquipamientoEntity extends OperacionCicloVidaEquipamiento {
}
