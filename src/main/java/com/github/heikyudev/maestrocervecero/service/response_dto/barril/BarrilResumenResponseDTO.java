package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
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

    /** Cerveza que contiene el barril (lote y receta), o {@code null} si el barril no está Con cerveza. */
    private ContenidoBarrilResponseDTO contenido;

    /** Estado lógico del barril (activo o dado de baja). Un barril dado de baja sigue mostrando su información y su historial, en solo lectura. */
    private Estado estado;

    /**
     * Indica si el barril está dado de baja.
     *
     * @return {@code true} si su estado lógico es {@link Estado#BAJA}.
     */
    public boolean isDadoDeBaja() {
        return estado == Estado.BAJA;
    }
}
