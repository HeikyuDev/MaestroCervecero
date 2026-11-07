package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con el resumen de un barril para sus pantallas de control: su identidad, su capacidad y contenido, su
 * estado, sus usos desde el último mantenimiento y las fechas de su última limpieza y su último mantenimiento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilResumenResponseDTO {

    /** Identificador único del barril. */
    private Long id;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Número de serie del barril. */
    private String numeroSerie;

    /** Nombre del fabricante del barril. */
    private String fabricante;

    /** Capacidad total del barril, en litros. */
    private Double capacidad;

    /** Contenido actual del barril, en litros. */
    private Double contenidoActual;

    /** Estado operativo actual del barril. */
    private EstadoOperativoBarril estadoOperativo;

    /** Cantidad de usos desde el último mantenimiento. */
    private Long usosDesdeUltimoMantenimiento;

    /** Cantidad máxima de usos antes de requerir mantenimiento preventivo. */
    private Integer usosMaximosAntesMantenimiento;

    /** Fecha y hora de la última limpieza registrada, o {@code null} si no tiene. */
    private LocalDateTime fechaUltimaLimpieza;

    /** Fecha y hora del último mantenimiento registrado, o {@code null} si no tiene. */
    private LocalDateTime fechaUltimoMantenimiento;
}
