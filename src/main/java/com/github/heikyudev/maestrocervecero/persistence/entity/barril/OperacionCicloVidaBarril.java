package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.ciclo_vida.OperacionCicloVida;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Clase padre abstracta de toda operación del ciclo de vida de un Barril (Falla, Mantenimiento,
 * Limpieza, y a futuro Despacho/Devolución). Estrategia {@code JOINED}: cada subclase tiene su
 * propia tabla, unida a la tabla raíz {@code operacion_ciclo_vida_barril} por FK, evitando
 * columnas nulas por campos que no le correspondan a cada tipo de operación.
 * Solo las subclases concretas pueden instanciarse.
 * <p>
 * Extiende {@link OperacionCicloVida} para heredar los datos comunes a toda operación del ciclo
 * de vida (fecha, observaciones, estado transaccional y datos de anulación), y agrega la
 * relación con el {@link BarrilEntity} afectado, común a todas sus subclases. Esta relación es
 * lo que permite, con una única consulta polimórfica sobre esta clase, encontrar la última
 * operación registrada de cualquier tipo sobre un barril determinado.
 * </p>
 */
@Entity
@Table(name = "operacion_ciclo_vida_barril")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class OperacionCicloVidaBarril extends OperacionCicloVida {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barril_id", nullable = false)
    private BarrilEntity barril;
}
