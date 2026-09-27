package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Representa la operación de sacar una cantidad de cerveza de un barril.
 * Esto puede ocurrir por diversos motivos, como la toma de muestras para testeo,
 * el embotellado de una parte del contenido, o por merma.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el barril afectado) los hereda de
 * {@link OperacionCicloVidaBarril}.
 * </p>
 */
@Entity
@Table(name = "fraccionamiento_barril")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FraccionamientoBarrilEntity extends OperacionCicloVidaBarril {

    /**
     * La cantidad de cerveza extraída del barril, medida en litros.
     */
    @Column(name = "cantidad_fraccionada", nullable = false)
    private Double cantidadFraccionada;

    /**
     * Estado operativo al que quedó el barril como resultado de este fraccionamiento puntual
     * (CON_CERVEZA si quedó contenido restante, o EN_LIMPIEZA si el fraccionamiento vació el
     * barril por completo).
     * <p>
     * Se persiste para que {@code anularFraccionamientoBarril} pueda validar que el barril
     * sigue exactamente en el estado que dejó ESTE fraccionamiento puntual, y no en cualquiera
     * de los dos estados posibles en general — evita revertir un fraccionamiento viejo cuando el
     * estado actual del barril en realidad lo dejó otro fraccionamiento u operación posterior.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo_resultante", nullable = false)
    private EstadoOperativoBarril estadoOperativoResultante;
}
