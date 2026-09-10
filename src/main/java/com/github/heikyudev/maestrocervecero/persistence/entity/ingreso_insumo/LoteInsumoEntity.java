package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "lote_insumo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoteInsumoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "cantidad_actual", nullable = false)
    @Builder.Default
    private Double cantidadActual = 0.0;

    @Column(name = "cantidad_reservada", nullable = false)
    @Builder.Default
    private Double cantidadReservada = 0.0;

    @Column(name = "identificacion_lote_proveedor", nullable = false)
    private String identificacionLoteProveedor;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    // Costo Promedio Ponderado (PPP): sube cuando entra stock a un precio distinto,
    // manteniendo el valor monetario total exactamente reversible ante una anulación.
    @Column(name = "costo_unitario_ppp", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal costoUnitarioPPP = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    // Un insumo corresponde a un lote de insumo
    private InsumoEntity insumo;

    // == METODOS DE DOMINIO ===

    // Stock disponible para producción (no va a la base de datos)
    public Double getCantidadDisponible() {
        return (this.cantidadActual != null ? this.cantidadActual : 0.0)
                - (this.cantidadReservada != null ? this.cantidadReservada : 0.0);
    }

    // Suma cantidad al lote y actualiza el PPP (promedio ponderado por valor monetario total)
    public void sumarIngreso(double cantidad, BigDecimal costoUnitario) {
        BigDecimal valorActual = this.costoUnitarioPPP.multiply(BigDecimal.valueOf(this.cantidadActual));
        BigDecimal valorNuevo = costoUnitario.multiply(BigDecimal.valueOf(cantidad));

        this.cantidadActual += cantidad;

        if (this.cantidadActual != 0) {
            this.costoUnitarioPPP = valorActual.add(valorNuevo)
                    .divide(BigDecimal.valueOf(this.cantidadActual), 4, RoundingMode.HALF_UP);
        }
    }

    // Reversa un ingreso anulado: descuenta de la cantidad actual lo que ese ingreso había sumado,
    // y revierte exactamente su aporte al PPP usando el costo original de ese ingreso (no el PPP actual)
    public void anularIngreso(double cantidad, BigDecimal costoUnitario) {
        BigDecimal valorActual = this.costoUnitarioPPP.multiply(BigDecimal.valueOf(this.cantidadActual));
        BigDecimal valorARevertir = costoUnitario.multiply(BigDecimal.valueOf(cantidad));

        this.cantidadActual -= cantidad;

        if (this.cantidadActual > 0) {
            this.costoUnitarioPPP = valorActual.subtract(valorARevertir)
                    .divide(BigDecimal.valueOf(this.cantidadActual), 4, RoundingMode.HALF_UP);
        }
    }

    public void reservar(double cantidad) {
        if (cantidad > getCantidadDisponible()) {
            throw new ReglaNegocioException("Stock disponible insuficiente para reservar");
        }
        this.cantidadReservada += cantidad;
    }

    public void liberarReserva(double cantidad) {
        this.cantidadReservada -= cantidad;
    }

    public void ejecutarConsumo(double cantidad) {
        this.cantidadActual -= cantidad;
        this.cantidadReservada -= cantidad;
    }
}
