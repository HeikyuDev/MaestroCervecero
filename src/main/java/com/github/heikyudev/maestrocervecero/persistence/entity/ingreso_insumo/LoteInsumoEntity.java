package com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    // Un insumo corresponde a un lote de insumo
    private InsumoEntity insumo;

    // == METODOS DE DOMINIO ===

    // Stock disponible para producción (no va a la base de datos)
    public Double getCantidadDisponible() {
        return (this.cantidadActual != null ? this.cantidadActual : 0.0)
                - (this.cantidadReservada != null ? this.cantidadReservada : 0.0);
    }

    public void sumarIngreso(double cantidad) {
        this.cantidadActual += cantidad;
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
