package com.github.heikyudev.maestrocervecero.persistence.entity.distribucion;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Día en el que ya se completó el envío diario de los recordatorios de retiro de barriles. Su existencia es lo que
 * impide repetir el envío ese día, aunque la aplicación se reinicie.
 */
@Entity
@Table(name = "ejecucion_recordatorios_retiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EjecucionRecordatoriosRetiroEntity {

    @Id
    @EqualsAndHashCode.Include
    private LocalDate fecha;

    // Momento en que terminó el envío de ese día
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;
}
