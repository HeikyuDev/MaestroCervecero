package com.github.heikyudev.maestrocervecero.persistence.entity.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import jakarta.persistence.*;
import lombok.*;

/**
 * Reserva de insumo de una etapa de lote sobre un lote de insumo físico. Nunca se borra: conserva la cantidad
 * reservada original y lo que pasó con ella.
 * <p>
 * Invariante: {@code cantidadReservada = cantidadConsumida + cantidadLiberada + cantidadReducidaPorAjuste +
 * cantidadPendiente}.
 * </p>
 */
@Entity
@Table(name = "reserva_insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReservaInsumoEntity {

    /**
     * Saldo a partir del cual una reserva se considera agotada (la mitad de la última cifra de las cantidades).
     */
    public static final double SALDO_MINIMO = 0.00005;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Cantidad reservada original. No cambia después de crear la reserva.
     */
    @Column(name = "cantidad_reservada", nullable = false)
    private Double cantidadReservada;

    /**
     * Cantidad consumida de la reserva (acumulada). Baja si se anula un consumo reservado.
     */
    @Column(name = "cantidad_consumida", nullable = false)
    @Builder.Default
    private Double cantidadConsumida = 0.0;

    /**
     * Saldo devuelto al stock al liberar la reserva sin consumirlo.
     */
    @Column(name = "cantidad_liberada", nullable = false)
    @Builder.Default
    private Double cantidadLiberada = 0.0;

    /**
     * Cantidad que una merma (ajuste de insumo) le quitó a la reserva.
     */
    @Column(name = "cantidad_reducida_por_ajuste", nullable = false)
    @Builder.Default
    private Double cantidadReducidaPorAjuste = 0.0;

    /**
     * Saldo que todavía está reservado.
     */
    @Column(name = "cantidad_pendiente", nullable = false)
    private Double cantidadPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoReservaInsumo estado = EstadoReservaInsumo.ACTIVA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_lote_id", nullable = false)
    private EtapaLoteEntity etapaLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_insumo_id", nullable = false)
    private LoteInsumoEntity loteInsumo;

    /**
     * Crea una reserva activa: todo lo reservado queda pendiente.
     */
    public static ReservaInsumoEntity nueva(EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo, double cantidad) {
        return ReservaInsumoEntity.builder()
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .cantidadReservada(cantidad)
                .cantidadPendiente(cantidad)
                .build();
    }

    /**
     * Registra un consumo reservado: baja el saldo pendiente y suma a lo consumido.
     */
    public void consumir(double cantidad) {
        this.cantidadPendiente = Redondeo.redondear(this.cantidadPendiente - cantidad);
        this.cantidadConsumida = Redondeo.redondear(this.cantidadConsumida + cantidad);
        actualizarEstado();
    }

    /**
     * Revierte un consumo reservado anulado: devuelve la cantidad al saldo pendiente y la descuenta de lo consumido.
     */
    public void revertirConsumo(double cantidad) {
        this.cantidadPendiente = Redondeo.redondear(this.cantidadPendiente + cantidad);
        this.cantidadConsumida = Math.max(0.0, Redondeo.redondear(this.cantidadConsumida - cantidad));
        actualizarEstado();
    }

    /**
     * Libera la reserva: todo el saldo pendiente pasa a liberado y la reserva queda cerrada.
     *
     * @return El saldo que se liberó (el que hay que devolver al stock reservado del lote de insumo).
     */
    public double liberar() {
        double liberado = this.cantidadPendiente;
        this.cantidadLiberada = Redondeo.redondear(this.cantidadLiberada + liberado);
        this.cantidadPendiente = 0.0;
        actualizarEstado();
        return liberado;
    }

    /**
     * Resta a la reserva la cantidad que le quitó una merma: baja el saldo pendiente (sin pasar de cero) y suma a lo
     * reducido por ajuste.
     */
    public void reducirPorAjuste(double cantidad) {
        double reducido = Math.min(Redondeo.redondear(cantidad), this.cantidadPendiente);
        this.cantidadPendiente = Math.max(0.0, Redondeo.redondear(this.cantidadPendiente - reducido));
        this.cantidadReducidaPorAjuste = Redondeo.redondear(this.cantidadReducidaPorAjuste + reducido);
        actualizarEstado();
    }

    /**
     * Estado según las cantidades: ACTIVA con saldo pendiente; sin saldo, CONSUMIDA si se consumió todo lo reservado y
     * LIBERADA si no (se devolvió el saldo o una merma la agotó).
     */
    private void actualizarEstado() {
        if (this.cantidadPendiente > SALDO_MINIMO) {
            this.estado = EstadoReservaInsumo.ACTIVA;
        } else if (Math.abs(this.cantidadReservada - this.cantidadConsumida) <= SALDO_MINIMO) {
            this.estado = EstadoReservaInsumo.CONSUMIDA;
        } else {
            this.estado = EstadoReservaInsumo.LIBERADA;
        }
    }
}
