package com.github.heikyudev.maestrocervecero.persistence.entity.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

/**
 * Representa un barril físico utilizado para almacenar cerveza, resultado del proceso de envasado de un lote.
 * Este barril puede ser sometido a diversas operaciones como despachos, limpiezas y fraccionamientos.
 */
@Entity
@Table(name = "barril")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class BarrilEntity extends AuditableEntity<String> {

    /**
     * Identificador único del barril.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Etiqueta única asignada internamente para identificar el barril.
     */
    @Column(name = "identificador_interno", nullable = false)
    private String identificadorInterno;

    /**
     * Capacidad total del barril en litros.
     */
    @Column(nullable = false)
    private Double capacidad;

    /**
     * Contenido actual de cerveza en el barril, medido en litros.
     */
    @Column(name = "contenido_actual",nullable = false)
    @Builder.Default
    private Double contenidoActual = 0.0;

    /**
     * Estado operativo actual del barril, que define su ciclo de vida.
     * Los estados pueden ser:
     * - CON_CERVEZA: El barril contiene cerveza.
     * - DESPACHADO: El barril ha sido entregado a un cliente.
     * - DISPONIBLE: El barril está vacío y listo para ser utilizado.
     * - EN_LIMPIEZA: El barril se encuentra en proceso de limpieza.
     */
    @Column(name = "estado_operativo",nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoOperativoBarril estadoOperativo;


}
