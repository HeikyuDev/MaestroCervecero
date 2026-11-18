package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Parámetros de la generación automática de planes de órdenes de compra, que ajusta el gerente de compras.
 * <p>
 * Es una entidad de tipo "singleton": existe una y solo una fila, con {@code id} fijo ({@link #SINGLETON_ID}). La crea
 * la migración de Flyway que introduce la generación de planes de compra ({@code V30}) con valores por defecto y a
 * partir de ahí solo se modifica.
 * </p>
 */
@Entity
@Table(name = "configuracion_compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ConfiguracionComprasEntity extends AuditableEntity<String> {

    /**
     * ID fijo de la única fila válida de esta tabla.
     */
    public static final Long SINGLETON_ID = 1L;

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Horas que tienen los proveedores para responder una solicitud de cotización desde que se envía.
     */
    @Column(name = "plazo_respuesta_horas", nullable = false)
    private Integer plazoRespuestaHoras;

    /**
     * Cantidad de planes de órdenes de compra distintos que se generan al cerrar una solicitud de cotización.
     */
    @Column(name = "cantidad_planes", nullable = false)
    private Integer cantidadPlanes;

    /**
     * Criterio con el que se ordenan las ofertas de cada insumo.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_plan_compra", nullable = false)
    private CriterioPlanCompra criterioPlanCompra;

    /**
     * Importancia del precio (0 a 100) frente a la cantidad disponible en el criterio {@code PONDERADO}. Con 100 solo
     * cuenta el precio y con 0 solo la cantidad. Los demás criterios no lo usan.
     */
    @Column(name = "peso_precio", nullable = false)
    private Double pesoPrecio;
}
