package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con el resumen de un equipamiento: sus datos, los parámetros propios de su tipo, sus usos desde el
 * último mantenimiento y las fechas de su última limpieza y su último mantenimiento.
 * <p>
 * Los parámetros propios de cada tipo que no le corresponden al equipamiento son {@code null}: un molino solo informa
 * {@code rendimientoMolienda}; un macerador, las capacidades, {@code espacioMuerto} y {@code eficienciaMaceracion}; una
 * olla de hervor, las capacidades, {@code evaporacion} y {@code perdidaPorTrub}; un fermentador, las capacidades.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipamientoResumenResponseDTO {

    /**
     * Identificador único del equipamiento.
     */
    private Long id;

    /**
     * Identificador interno del equipamiento (por ejemplo, MOL-01).
     */
    private String identificadorInterno;

    /**
     * Tipo concreto del equipamiento.
     */
    private TipoEquipamiento tipo;

    /**
     * Estado operativo actual del equipamiento.
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Cantidad máxima de usos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;

    /**
     * Cantidad de usos desde el último mantenimiento registrado (o desde siempre, si nunca tuvo uno).
     */
    private Long usosDesdeUltimoMantenimiento;

    /**
     * Fecha y hora de fin de la última limpieza registrada, o {@code null} si nunca tuvo una.
     */
    private LocalDateTime fechaUltimaLimpieza;

    /**
     * Fecha y hora de fin del último mantenimiento registrado, o {@code null} si nunca tuvo uno.
     */
    private LocalDateTime fechaUltimoMantenimiento;

    /**
     * Capacidad total en litros (macerador, olla de hervor y fermentador), o {@code null}.
     */
    private Double capacidadTotal;

    /**
     * Capacidad útil en litros (macerador, olla de hervor y fermentador), o {@code null}.
     */
    private Double capacidadUtil;

    /**
     * Rendimiento de molienda en kg/h (molino), o {@code null}.
     */
    private Double rendimientoMolienda;

    /**
     * Espacio muerto (macerador), o {@code null}.
     */
    private Double espacioMuerto;

    /**
     * Eficiencia de maceración (macerador), o {@code null}.
     */
    private Double eficienciaMaceracion;

    /**
     * Evaporación (olla de hervor), o {@code null}.
     */
    private Double evaporacion;

    /**
     * Pérdida por trub (olla de hervor), o {@code null}.
     */
    private Double perdidaPorTrub;
}
