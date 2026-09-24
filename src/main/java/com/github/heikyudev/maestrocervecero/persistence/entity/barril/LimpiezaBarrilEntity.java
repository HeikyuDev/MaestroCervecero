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
 * Registra la operación de limpieza de un barril.
 * Un barril entra en estado EN_LIMPIEZA tras ser devuelto por un cliente o cuando un fraccionamiento deja su contenido en cero.
 * El registro exitoso de esta entidad marca la finalización del proceso de limpieza, dejando al barril en estado operativo DISPONIBLE o EN_MANTENIMIENTO según la cantidad de usos acumulados.
 * <p>
 * Los datos comunes a toda operación del ciclo de vida (fecha, observaciones, estado
 * transaccional, datos de anulación y la relación con el barril afectado) los hereda de
 * {@link OperacionCicloVidaBarril}.
 * </p>
 */
@Entity
@Table(name = "limpieza_barril")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LimpiezaBarrilEntity extends OperacionCicloVidaBarril {

    /**
     * Estado operativo al que quedó el barril como resultado de esta limpieza puntual
     * (DISPONIBLE o EN_MANTENIMIENTO, según la cantidad de usos evaluada al momento del registro).
     * <p>
     * Se persiste para que {@code anularLimpiezaBarril} pueda validar que el barril sigue
     * exactamente en el estado que dejó ESTA limpieza, y no en cualquiera de los dos estados
     * posibles en general — evita revertir una limpieza vieja cuando el estado actual del barril
     * en realidad lo dejó un mantenimiento u otra limpieza posterior.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo_resultante", nullable = false)
    private EstadoOperativoBarril estadoOperativoResultante;
}
