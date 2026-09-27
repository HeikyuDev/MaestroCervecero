package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Registra la devolución de un barril despachado, dejándolo en estado operativo
 * {@code EN_LIMPIEZA}: todo barril que vuelve de afuera pasa por limpieza, por motivos
 * sanitarios, sin importar el contenido restante.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el barril afectado) los hereda de
 * {@link OperacionCicloVidaBarril}.
 * </p>
 */
@Entity
@Table(name = "devolucion_barril")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DevolucionBarrilEntity extends OperacionCicloVidaBarril {
}
