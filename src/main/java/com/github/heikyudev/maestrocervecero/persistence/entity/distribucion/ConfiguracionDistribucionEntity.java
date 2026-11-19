package com.github.heikyudev.maestrocervecero.persistence.entity.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Parámetros de la coordinación automática del retiro de barriles, que ajusta el encargado de distribución.
 * <p>
 * Es una entidad de tipo "singleton": existe una y solo una fila, con {@code id} fijo ({@link #SINGLETON_ID}). La crea
 * la migración de Flyway que introduce la coordinación del retiro de barriles ({@code V35}) con valores por defecto y a
 * partir de ahí solo se modifica.
 * </p>
 */
@Entity
@Table(name = "configuracion_distribucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ConfiguracionDistribucionEntity extends AuditableEntity<String> {

    /**
     * ID fijo de la única fila válida de esta tabla.
     */
    public static final Long SINGLETON_ID = 1L;

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Hora a partir de la cual se envía, una vez por día, el recordatorio de retiro a los clientes con barriles vencidos.
     */
    @Column(name = "hora_envio_recordatorio_retiro", nullable = false)
    private LocalTime horaEnvioRecordatorioRetiro;

    /**
     * Días hacia adelante, desde el momento en que responde, en los que el cliente puede proponer el retiro. También es
     * la vigencia del enlace del recordatorio.
     */
    @Column(name = "dias_maximos_anticipacion_retiro", nullable = false)
    private Integer diasMaximosAnticipacionRetiro;
}
