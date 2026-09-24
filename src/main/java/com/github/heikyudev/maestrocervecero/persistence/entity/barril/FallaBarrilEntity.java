package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Registra una falla ocurrida sobre un barril, dejándolo en estado operativo
 * {@code EN_MANTENIMIENTO} hasta que se registre el mantenimiento correspondiente.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el barril afectado) los hereda de
 * {@link OperacionCicloVidaBarril}.
 * </p>
 */
@Entity
@Table(name = "falla_barril")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FallaBarrilEntity extends OperacionCicloVidaBarril {
}
