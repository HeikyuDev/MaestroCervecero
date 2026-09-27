package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Representa la operación de préstamo de un barril a un cliente, dejándolo en estado operativo
 * {@code DESPACHADO}.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el barril afectado) los hereda de
 * {@link OperacionCicloVidaBarril}.
 * </p>
 */
@Entity
@Table(name = "despacho_barril")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DespachoBarrilEntity extends OperacionCicloVidaBarril {

    /**
     * Fecha estimada en la que se espera que el cliente devuelva el barril.
     */
    @Column(name = "fecha_devolucion_estimada", nullable = false)
    private LocalDate fechaDevolucionEstimada;

    /**
     * Cliente al que se le despachó el barril.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;
}
