package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Configuración general del sistema, agrupada por el proceso al que aplica cada parámetro.
 * <p>
 * Es una entidad de tipo "singleton": existe una y solo una fila, con
 * {@code id} fijo ({@link #SINGLETON_ID}), nunca autogenerado. No se da de
 * alta ni de baja como el resto de las entidades — se crea una única vez al
 * arrancar la aplicación (ver {@code ConfiguracionProduccionDataLoader})
 * y a partir de ahí el gerente de producción solo la modifica (UPDATE),
 * jamás se inserta una fila nueva ni se elimina la existente.
 * <p>
 * Extiende {@link AuditableEntity} para poder saber quién fue el último
 * gerente que ajustó estos valores y cuándo.
 */
@Entity
@Table(name = "configuracion_produccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ConfiguracionProduccionEntity extends AuditableEntity<String> {

    /**
     * ID fijo de la única fila válida de esta tabla. Nunca cambia y nunca se
     * genera otro. Usalo siempre que necesites recuperar la configuración
     * actual (ej. {@code repository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)}).
     */
    public static final Long SINGLETON_ID = 1L;

    @Id
    @EqualsAndHashCode.Include
    private Long id; // JPA Exige una ID, por lo tanto creamos una al iniciar la aplicacion utilizando la constante

    // === SECCION 1: estimación de la fecha de finalización de una planificación de producción ===

    @Column(name = "velocidad_estandar_molienda", nullable = false)
    private Double velocidadEstandarMolienda;

    @Column(name = "velocidad_estandar_envasado", nullable = false)
    private Double velocidadEstandarEnvasado;

    @Column(name = "tiempo_estandar_cip", nullable = false)
    private Double tiempoEstandarCip;

    @Column(name = "capacidad_lote_estandar", nullable = false)
    private Double capacidadLoteEstandar;

    // === SECCION 2: lotes ===

    /**
     * Porcentaje mínimo (0-100) que debe alcanzar la cantidad consumida de CADA insumo requerido
     * de la etapa actual (respecto de su cantidad requerida escalada) para poder finalizarla y
     * avanzar a la siguiente. Evita que un operario avance de etapa por error sin haber
     * registrado consumos, o habiéndolos registrado de forma incompleta.
     */
    @Column(name = "porcentaje_minimo_consumo_avanzar_etapa", nullable = false)
    private Double porcentajeMinimoConsumoParaAvanzarEtapa;

    // === SECCION 3: generación automática de planes de producción ===

    /**
     * Criterio para elegir el fermentador de un plan en modalidad secuencial.
     */
    @Column(name = "criterio_seleccion_plan_secuencial", nullable = false)
    @Enumerated(EnumType.STRING)
    private CriterioSeleccionPlanSecuencial criterioSeleccionPlanSecuencial;

    /**
     * Criterio para elegir la combinación de equipos de un plan en modalidad concurrente.
     */
    @Column(name = "criterio_seleccion_plan_concurrente", nullable = false)
    @Enumerated(EnumType.STRING)
    private CriterioSeleccionPlanConcurrente criterioSeleccionPlanConcurrente;
}
