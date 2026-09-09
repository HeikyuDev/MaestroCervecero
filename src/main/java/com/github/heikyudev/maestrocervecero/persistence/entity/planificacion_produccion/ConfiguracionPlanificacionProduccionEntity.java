package com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Configuración general del sistema para el cálculo de estimaciones de una
 * planificación de producción (velocidad de molienda, envasado, tiempo de CIP y
 * capacidad estándar de un lote).
 * <p>
 * Es una entidad de tipo "singleton": existe una y solo una fila, con
 * {@code id} fijo ({@link #SINGLETON_ID}), nunca autogenerado. No se da de
 * alta ni de baja como el resto de las entidades — se crea una única vez al
 * arrancar la aplicación (ver {@code ConfiguracionPlanificacionProduccionDataLoader})
 * y a partir de ahí el gerente de producción solo la modifica (UPDATE),
 * jamás se inserta una fila nueva ni se elimina la existente.
 * <p>
 * Extiende {@link AuditableEntity} para poder saber quién fue el último
 * gerente que ajustó estos valores y cuándo.
 */
@Entity
@Table(name = "configuracion_planificacion_produccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ConfiguracionPlanificacionProduccionEntity extends AuditableEntity<String> {

    /**
     * ID fijo de la única fila válida de esta tabla. Nunca cambia y nunca se
     * genera otro. Usalo siempre que necesites recuperar la configuración
     * actual (ej. {@code repository.findById(ConfiguracionPlanificacionProduccionEntity.SINGLETON_ID)}).
     */
    public static final Long SINGLETON_ID = 1L;

    @Id
    @EqualsAndHashCode.Include
    private Long id; // JPA Exige una ID, por lo tanto creamos una al iniciar la aplicacion utilizando la constante

    @Column(name = "velocidad_estandar_molienda", nullable = false)
    private Double velocidadEstandarMolienda;

    @Column(name = "velocidad_estandar_envasado", nullable = false)
    private Double velocidadEstandarEnvasado;

    @Column(name = "tiempo_estandar_cip", nullable = false)
    private Double tiempoEstandarCip;

    @Column(name = "capacidad_lote_estandar", nullable = false)
    private Double capacidadLoteEstandar;
}