package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_busqueda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)

/**
 * Esta clase representa una solicitud que el cliente
 *  * Pueda solicitar la busqueda de barriles. El mismo llo ejecuta el sistema
 *  * A nombre del cliente, Donde a traves de un mail puede enviar la fecha y hora de entrega
 *  * y un campo de observaciones.
 */
public class SolicitudBusquedaEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_busqueda", nullable = false)
    private LocalDateTime fechaBusqueda;

    private String observaciones;

    @Column(nullable = false)
    private boolean buscado;
}
